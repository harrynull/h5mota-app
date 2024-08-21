package tech.harrynull.h5mota.ui.views

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import tech.harrynull.h5mota.models.Tower
import tech.harrynull.h5mota.models.TowerRepo
import tech.harrynull.h5mota.utils.DownloadManager

@Composable
fun PlayScreen(tower: Tower) {
    val downloadManager = DownloadManager(LocalContext.current)
    val towerRepo = TowerRepo(LocalContext.current)
    val scope = rememberCoroutineScope()
    LaunchedEffect(true) {
        scope.launch { towerRepo.addRecent(tower) }
    }
    AndroidView(factory = {
        WebView(it).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
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
    })
}