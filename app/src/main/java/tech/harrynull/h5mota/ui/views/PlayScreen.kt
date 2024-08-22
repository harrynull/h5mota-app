package tech.harrynull.h5mota.ui.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
import android.view.ViewGroup
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import tech.harrynull.h5mota.models.Tower
import tech.harrynull.h5mota.models.TowerRepo
import tech.harrynull.h5mota.utils.DownloadManager
import tech.harrynull.h5mota.utils.JsInterface
import tech.harrynull.h5mota.utils.maybeInjectLocalForage

fun Context.getActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}

enum class OrientationStage { Start, OrientationRequested, Reloaded, Exiting }

@Composable
fun PlayScreen(tower: Tower, snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val activity = context.getActivity()!!
    val downloadManager = DownloadManager(context)
    val towerRepo = TowerRepo(context)
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val orientationStage = rememberSaveable { mutableStateOf(OrientationStage.Start) }
    LaunchedEffect(true) {
        scope.launch { towerRepo.addRecent(tower) }
    }
    AndroidView(factory = {
        WebView(it).apply {
            addJavascriptInterface(
                JsInterface(
                    activity = activity,
                    webView = this,
                    saveDir = downloadManager.saveRoot(),
                    onRequestOrientation = { orientation ->
                        if (orientationStage.value == OrientationStage.Start) {
                            activity.requestedOrientation = orientation
                            orientationStage.value = OrientationStage.OrientationRequested
                        }
                    },
                    onAlert = { message ->
                        scope.launch { snackbarHostState.showSnackbar(message) }
                    },
                    onCopy = { content ->
                        clipboardManager.setText(AnnotatedString(content))
                        scope.launch { snackbarHostState.showSnackbar("已复制到剪贴板") }
                    }
                ), "jsinterface"
            )

            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            webViewClient =
                object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        maybeInjectLocalForage(view!!, downloadManager.saveRoot())
                    }
                }

            webChromeClient =
                object : WebChromeClient() {
                    override fun onJsAlert(
                        view: WebView?,
                        url: String?,
                        message: String?,
                        result: JsResult?
                    ): Boolean {
                        // suppress warning
                        if (message?.contains("请勿直接打开html文件") == true) {
                            result?.confirm()
                            return true
                        }
                        return super.onJsAlert(view, url, message, result)
                    }


                }.apply {
                    @SuppressLint("SetJavaScriptEnabled")
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    settings.allowFileAccess = true
                    settings.allowUniversalAccessFromFileURLs = true
                }
        }
    }, update = {
        it.loadUrl(
            if (downloadManager.downloaded(tower))
                "file://${downloadManager.getDownloadedPath(tower).absolutePath}" else
                tower.link
        )
    }, onRelease = {
        if (orientationStage.value == OrientationStage.OrientationRequested) {
            orientationStage.value = OrientationStage.Reloaded
        } else if (orientationStage.value == OrientationStage.Reloaded) {
            activity.requestedOrientation = SCREEN_ORIENTATION_UNSPECIFIED
            orientationStage.value = OrientationStage.Exiting
        }
    })
}