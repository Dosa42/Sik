package com.example.hostuser

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import java.io.File
import java.util.concurrent.Executors

/** Tek ekran, tek girdi: NGROK URL. Baska hicbir sey yok. */
class MainActivity : Activity() {

    // Tum native cagrilar tek thread'de, sirayla.
    private val io = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val caPath = copyCaToFiles()
        io.execute { UserCore.nativeInit(caPath) }

        val input = EditText(this).apply {
            hint = "NGROK URL"
            isSingleLine = true
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            imeOptions = EditorInfo.IME_ACTION_GO
            setOnEditorActionListener { v, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    val url = v.text.toString()
                    io.execute { UserCore.nativeConnect(url) }
                    true
                } else {
                    false
                }
            }
        }

        val pad = (24 * resources.displayMetrics.density).toInt()
        val root = FrameLayout(this).apply { setPadding(pad, 0, pad, 0) }
        root.addView(
            input,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL
            )
        )
        setContentView(root)
    }

    private fun copyCaToFiles(): String {
        val out = File(filesDir, "cacert.pem")
        assets.open("cacert.pem").use { i -> out.outputStream().use { o -> i.copyTo(o) } }
        return out.absolutePath
    }
}
