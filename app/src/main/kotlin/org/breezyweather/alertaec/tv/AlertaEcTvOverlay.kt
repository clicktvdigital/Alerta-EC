package org.breezyweather.alertaec.tv

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import org.breezyweather.ui.support.isTelevisionDevice

/**
 * Brief banner over any app on Android TV / TV Box (e.g. while watching video) for WARNING and EMERGENCY only.
 *
 * - Opt-in: shown only if the user granted «Mostrar sobre otras apps» (SYSTEM_ALERT_WINDOW).
 * - Not focusable and not touchable: the remote keeps controlling the video; it hides itself after [SHOW_MS].
 * - Covers only a strip at the top, never the whole picture.
 */
object AlertaEcTvOverlay {
    private const val SHOW_MS = 20_000L
    private val handler = Handler(Looper.getMainLooper())
    private var current: View? = null
    private var pendingHide: Runnable? = null

    fun canShow(context: Context): Boolean =
        context.isTelevisionDevice() &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            Settings.canDrawOverlays(context)

    fun show(context: Context, title: String, text: String, color: Int) {
        if (!canShow(context)) return
        val app = context.applicationContext
        handler.post {
            val windowManager = app.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            hide(windowManager)
            val density = app.resources.displayMetrics.density
            val banner = LinearLayout(app).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.argb(235, 12, 16, 24))
                setPadding((24 * density).toInt(), (14 * density).toInt(), (24 * density).toInt(), (14 * density).toInt())
                addView(View(app).apply { setBackgroundColor(color) }, LinearLayout.LayoutParams(-1, (6 * density).toInt()))
                addView(TextView(app).apply {
                    this.text = "⚠ $title"
                    textSize = 22f
                    setTextColor(Color.WHITE)
                    setPadding(0, (8 * density).toInt(), 0, 0)
                })
                addView(TextView(app).apply {
                    this.text = text
                    textSize = 16f
                    maxLines = 2
                    setTextColor(Color.rgb(225, 232, 242))
                })
            }
            @Suppress("DEPRECATION")
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply { gravity = Gravity.TOP }
            runCatching { windowManager.addView(banner, params) }.onSuccess {
                current = banner
                banner.alpha = 0f
                banner.animate().alpha(1f).setDuration(180).start()
                pendingHide?.let(handler::removeCallbacks)
                pendingHide = Runnable { hide(windowManager) }.also { handler.postDelayed(it, SHOW_MS) }
            }
        }
    }

    private fun hide(windowManager: WindowManager) {
        current?.let { view -> runCatching { windowManager.removeView(view) } }
        current = null
    }
}
