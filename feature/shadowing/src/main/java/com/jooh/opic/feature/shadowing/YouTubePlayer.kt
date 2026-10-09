package com.jooh.opic.feature.shadowing

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.util.UUID
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject

/** 이 화면이 생성한 HTML만 로드한다. 브리지에는 값 전달만 허용하고 Kotlin 명령 실행 API는 노출하지 않는다. */
class PlayerBridge(private val nonce: String, private val onTime: (Double) -> Unit, private val onError: (Int) -> Unit) {
    @JavascriptInterface fun time(token: String, value: Double) { if (token == nonce && value.isFinite()) onTime(value) }
    @JavascriptInterface fun error(token: String, code: Int) { if (token == nonce) onError(code) }
}

class YouTubePlayer(private val web: WebView) {
    fun command(script: String) { web.evaluateJavascript(script, null) }
    fun pause() = command("player?.pauseVideo()")
    fun play() = command("player?.playVideo()")
    fun seek(seconds: Double) = command("player?.seekTo(${seconds.coerceAtLeast(0.0)}, true)")
    fun speed(value: Double) = command("player?.setPlaybackRate($value)")
    fun dispose() { web.removeJavascriptInterface("OpicPlayer"); web.destroy() }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PlayerView(id: String, modifier: Modifier = Modifier, onPlayer: (YouTubePlayer) -> Unit,
               onTime: (Double) -> Unit, onError: (Int) -> Unit) {
    val nonce = remember(id) { UUID.randomUUID().toString() }
    val html = remember(id) { """
        <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"></head>
        <body style="margin:0;background:black"><style>html,body,#player{width:100%;height:100%}</style><div id="player"></div>
        <script src="https://www.youtube.com/iframe_api"></script><script>
        var player;
        function onYouTubeIframeAPIReady() {
          player = new YT.Player('player', {height:'100%',width:'100%',videoId:${JSONObject.quote(id)},
            playerVars:{playsinline:1,origin:'https://appassets.androidplatform.net'},
            events:{onError:function(e){OpicPlayer.error(${JSONObject.quote(nonce)},e.data)}}});
          setInterval(function(){if(player && player.getCurrentTime) OpicPlayer.time(${JSONObject.quote(nonce)},player.getCurrentTime())}, 150);
        }
        </script></body></html>
    """.trimIndent() }
    AndroidView(factory = { context ->
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.domStorageEnabled = false
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = request.isForMainFrame
            }
            addJavascriptInterface(PlayerBridge(nonce, onTime, onError), "OpicPlayer")
            loadDataWithBaseURL("https://appassets.androidplatform.net/", html, "text/html", "UTF-8", null)
            onPlayer(YouTubePlayer(this))
        }
    }, modifier = modifier)
}
