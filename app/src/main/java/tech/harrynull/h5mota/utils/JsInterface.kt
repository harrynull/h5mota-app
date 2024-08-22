/***
 * Code adapted from https://github.com/ckcz123/H5mota-Android/blob/master/app/src/main/java/com/h5mota/core/component/WebScreen.kt
 */
package tech.harrynull.h5mota.utils

import android.content.pm.ActivityInfo
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.activity.ComponentActivity
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.IOException
import java.io.InputStreamReader

class JsInterface(
    private val activity: ComponentActivity,
    private val webView: WebView,
    private val saveDir: File,
    private val onRequestOrientation: (Int) -> Unit,
    private val onAlert: (String) -> Unit,
    private val onCopy: (String) -> Unit,
) {
    init {
        if (!saveDir.exists()) {
            saveDir.mkdir();
        }
    }

    @JavascriptInterface
    public fun download(filename: String, content: String) {
        onAlert("暂时不支持下载")
    }

    @JavascriptInterface
    fun copy(content: String) {
        onCopy(content)
    }

    @JavascriptInterface
    fun readFile() {
        onAlert("暂时不支持读取文件")
    }

    @JavascriptInterface
    fun setLocalForage(id: Int, name: String, data: String) {
        try {
            FileWriter(getFile(name)).use { writer ->
                writer.write(data)
                executeLocalForageCallback(id)
            }
        } catch (e: IOException) {
            Log.e("ERROR", "Unable to setLocalForage", e)
            executeLocalForageCallback(id, (e.message), null)
        }
    }

    @JavascriptInterface
    fun getLocalForage(id: Int, name: String) {
        try {
            BufferedReader(InputStreamReader(FileInputStream(getFile(name)))).use { bufferedReader ->
                val base64 = Base64.encodeToString(
                    bufferedReader.readText().toByteArray(),
                    Base64.NO_WRAP
                )
                executeLocalForageCallback(id, null, "core.decodeBase64('$base64')")
            }
        } catch (e: IOException) {
            executeLocalForageCallback(id, null, null)
        }
    }

    @JavascriptInterface
    fun removeLocalForage(id: Int, name: String) {
        getFile(name).delete()
        executeLocalForageCallback(id)
    }

    @JavascriptInterface
    fun clearLocalForage(id: Int) {
        saveDir.deleteRecursively()
        saveDir.mkdir()
        executeLocalForageCallback(id)
    }

    @JavascriptInterface
    fun iterateLocalForage(id: Int) {
        executeLocalForageIterate(id, getAllSaves())
    }

    @JavascriptInterface
    fun keysLocalForage(id: Int) {
        val builder = java.lang.StringBuilder().append('[')
        var first = true
        for (name in getAllSaves()) {
            if (!first) builder.append(", ")
            builder.append("core.decodeBase64('")
            builder.append(Base64.encodeToString(name.toByteArray(), Base64.NO_WRAP))
            builder.append("')");
            first = false
        }
        builder.append(']')
        executeLocalForageCallback(id, null, builder.toString())
    }

    @JavascriptInterface
    fun lengthLocalForage(id: Int) {
        executeLocalForageCallback(id, null, getAllSaves().size.toString())
    }

    @JavascriptInterface
    fun requestLandscape() {
        onRequestOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE)
    }

    @JavascriptInterface
    fun requestPortrait() {
        onRequestOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT)
    }

    private fun getAllSaves(): List<String> {
        val files: Array<File> = saveDir.listFiles() ?: return ArrayList()
        val names: MutableList<String> = ArrayList()
        for (f in files) {
            if (f.isDirectory) {
                for (f2 in f.listFiles()!!) {
                    if (f2.isFile) {
                        names.add(f.name + "_" + f2.name)
                    }
                }
            } else {
                names.add(f.name)
            }
        }
        return names
    }


    private fun executeLocalForageCallback(id: Int) {
        activity.runOnUiThread {
            webView.evaluateJavascript(
                """if (window.core && window.core.__callback$id) {
  var callback = core.__callback$id;
  delete core.__callback$id;
  callback();
}
""", null
            )
        }
    }

    private fun getFile(name: String): File {
        val group = "^(.+)_(save\\d+|autoSave)$".toRegex().find(name)?.groupValues
        if (group != null && group.size >= 3) {
            val dir = File(saveDir, group[1])
            if (!dir.exists()) {
                dir.mkdir()
            }
            return File(dir, group[2])
        }
        return File(saveDir, name)
    }

    private fun executeLocalForageCallback(id: Int, err: String?, data: String?) {
        activity.runOnUiThread {
            webView.evaluateJavascript(
                """if (window.core && window.core.__callback$id) {
  var callback = core.__callback$id;
  delete core.__callback$id;
  callback($err, $data);}
""", null
            )
        }
    }

    private fun executeLocalForageIterate(id: Int, keys: List<String>?) {
        val builder = StringBuilder()
        val iterName = "core.__iter$id"
        builder.append("if (window.core && window.").append(iterName).append(") {\n")
        if (keys != null) {
            for (key in keys) {
                builder.append("  ").append(iterName).append("(null, core.decodeBase64('")
                    .append(Base64.encodeToString(key.toByteArray(), Base64.NO_WRAP))
                    .append("'));\n")
            }
        }
        builder.append("  delete ").append(iterName).append(";")
        builder.append("}\n")
        activity.runOnUiThread {
            webView.evaluateJavascript(builder.toString()) { executeLocalForageCallback(id) }
        }
    }
}

fun maybeInjectLocalForage(view: WebView, saveDir: File) {
    view.evaluateJavascript(
        """
            (function() {
                if (!window.core || !window.core.plugin) return "";
                if (!window.localforage || !window.jsinterface) return ""
                if (core.utils._setLocalForage_set || !core.plugin._afterLoadResources) return "";
                return core.firstData.name;
            })()
        """.trimIndent()
    ) {
        val name = it.replace("\"", "")
        if (name.isEmpty()) return@evaluateJavascript
        val f = File(saveDir, name)
        if (!f.isDirectory || f.list { _, s -> s.startsWith("save") }
                .isNullOrEmpty()) return@evaluateJavascript
        Log.i("H5mota_WebActivity", "Inject localForage for $name...")
        // Inject
        view.evaluateJavascript(
            """
            (function () {              
              var _afterLoadResources = core.plugin._afterLoadResources;
              core.plugin._afterLoadResources = function () {
                console.log('Forwarding localforage...');
                core.platform.useLocalForage = true;
                Object.defineProperty(core.platform, 'useLocalForage', { writable: false });
                if (window.LZString) LZString.compress = function (s) { return s; };
                if (window.lzw_encode) lzw_encode = function (s) { return s; };
                localforage.setItem = function (name, data, callback) {
                  var id = setTimeout(null);
                  core['__callback' + id] = callback;
                  window.jsinterface.setLocalForage(id, name, data);
                }
                localforage.getItem = function (name, callback) {
                  var id = setTimeout(null);
                  core['__callback' + id] = callback;
                  window.jsinterface.getLocalForage(id, name);
                }
                localforage.removeItem = function (name, callback) {
                  var id = setTimeout(null);
                  core['__callback' + id] = callback;
                  window.jsinterface.removeLocalForage(id, name);
                }
                localforage.clear = function (callback) {
                  var id = setTimeout(null);
                  core['__callback' + id] = callback;
                  window.jsinterface.clearLocalForage(id);
                }
                localforage.iterate = function (iter, callback) {
                  var id = setTimeout(null);
                  core['__iter' + id] = iter;
                  core['__callback' + id] = callback;
                  window.jsinterface.iterateLocalForage(id);
                }
                localforage.keys = function (callback) {
                  var id = setTimeout(null);
                  core['__callback' + id] = callback;
                  window.jsinterface.keysLocalForage(id);
                }
                localforage.length = function (callback) {
                  var id = setTimeout(null);
                  core['__callback' + id] = callback;
                  window.jsinterface.lengthLocalForage(id);
                }
                core.control.getSaveIndexes(function (indexes) {
                  core.saves.ids = indexes;
                });
              }
              if (_afterLoadResources) _afterLoadResources.call(core.plugin);
            })()
                    """.trimIndent()
        ) { Log.i("WebScreen", "Forward finished!") }
    }
}