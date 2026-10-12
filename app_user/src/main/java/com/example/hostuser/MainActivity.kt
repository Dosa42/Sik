package com.example.hostuser

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.LinearLayout
import java.io.File
import java.util.concurrent.Executors

/**
 * Tek amac: girilen adresteki HOST'u acmak.
 *
 * Ustte adres satiri, altta host'un sayfasi (WebView). Klavyede "Git"e basinca adres acilir.
 *   xxx.ngrok-free.app  -> https://xxx.ngrok-free.app
 *   https://... / http://...  -> aynen
 *   192.168.1.20:8085 / localhost:8085  -> http://...   (LAN)
 *   ws:// veya wss://   -> eski yol: libuser_core.so WebSocket oturumu (sonuc logcat, etiket: user_core)
 */
class MainActivity : Activity() {

    // Tum native cagrilar tek thread'de, sirayla.
    private val io = Executors.newSingleThreadExecutor()

    private lateinit var input: EditText
    private lateinit var web: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val caPath = copyCaToFiles()
        io.execute { UserCore.nativeInit(caPath) }

        val dp = resources.displayMetrics.density
        val margin = (12 * dp).toInt()

        input = EditText(this).apply {
            hint = "NGROK URL"
            isSingleLine = true
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            imeOptions = EditorInfo.IME_ACTION_GO
            setOnEditorActionListener { v, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    open(v.text.toString())
                    true
                } else {
                    false
                }
            }
        }

        web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError
                ) {
                    if (request.isForMainFrame) {
                        showMessage("Acilamadi", request.url.toString() + "\n" + error.description)
                    }
                }
            }
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setOnApplyWindowInsetsListener { v, insets ->
                applyInsets(v, insets)
                insets
            }
        }
        root.addView(
            input,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(margin, margin / 2, margin, margin / 2) }
        )
        root.addView(
            web,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        )
        setContentView(root)
    }

    private fun open(raw: String) {
        val text = raw.trim()
        if (text.isEmpty()) return
        hideKeyboard()

        if (text.startsWith("ws://", ignoreCase = true) || text.startsWith("wss://", ignoreCase = true)) {
            io.execute { UserCore.nativeConnect(text) }
            showMessage("WebSocket", "Baglaniliyor: $text\n(durum: logcat, etiket user_core)")
            return
        }

        // ngrok'un ucretsiz "ziyaretci uyarisi" sayfasini atlar.
        web.loadUrl(toWebUrl(text), mapOf("ngrok-skip-browser-warning" to "1"))
    }

    private fun toWebUrl(s: String): String {
        val lower = s.lowercase()
        if (lower.startsWith("http://") || lower.startsWith("https://")) return s
        val host = s.substringBefore('/').substringBefore(':')
        val isLocal = host == "localhost" ||
            host.endsWith(".local") ||
            Regex("""\d{1,3}(\.\d{1,3}){3}""").matches(host)
        return (if (isLocal) "http://" else "https://") + s
    }

    private fun showMessage(title: String, body: String) {
        fun esc(x: String) = x.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        val html = "<html><body style='font-family:sans-serif;padding:16px'>" +
            "<h3>${esc(title)}</h3><pre style='white-space:pre-wrap'>${esc(body)}</pre></body></html>"
        web.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(input.windowToken, 0)
        input.clearFocus()
    }

    // targetSdk 35+ ekrani kenardan kenara cizer: durum cubugu / klavye icerigi ezmesin.
    @Suppress("DEPRECATION")
    private fun applyInsets(v: View, insets: WindowInsets) {
        if (Build.VERSION.SDK_INT >= 30) {
            val i = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.ime())
            v.setPadding(i.left, i.top, i.right, i.bottom)
        } else {
            v.setPadding(
                insets.systemWindowInsetLeft,
                insets.systemWindowInsetTop,
                insets.systemWindowInsetRight,
                insets.systemWindowInsetBottom
            )
        }
    }

    // Manifest'te enableOnBackInvokedCallback=false: geri tusu once sayfada geri gider.
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        web.destroy()
        super.onDestroy()
    }

    private fun copyCaToFiles(): String {
        val out = File(filesDir, "cacert.pem")
        assets.open("cacert.pem").use { i -> out.outputStream().use { o -> i.copyTo(o) } }
        return out.absolutePath
    }
}
