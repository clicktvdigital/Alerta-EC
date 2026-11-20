package org.breezyweather.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.ImageView

internal class NoaaZoomImageView(context: Context) : ImageView(context) {
    private val transform = Matrix()
    private val values = FloatArray(9)
    private var zoom = 1f

    private val scaler = ScaleGestureDetector(context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val next = (zoom * detector.scaleFactor).coerceIn(1f, 5f)
                transform.postScale(next / zoom, next / zoom, detector.focusX, detector.focusY)
                zoom = next
                constrain()
                return true
            }
        })

    private val gestures = GestureDetector(context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (zoom > 1f) {
                    resetZoom()
                } else {
                    zoom = 2f
                    transform.postScale(2f, 2f, e.x, e.y)
                    constrain()
                }
                return true
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (zoom <= 1f || scaler.isInProgress) return false
                transform.postTranslate(-distanceX, -distanceY)
                constrain()
                return true
            }
        })

    init {
        scaleType = ScaleType.MATRIX
    }

    fun showBitmap(bitmap: Bitmap) {
        // V5: mantener el zoom durante la reproducción si el siguiente
        // fotograma tiene las mismas dimensiones que el anterior.
        val previous = drawable
        val preserve = zoom > 1f && previous != null &&
            previous.intrinsicWidth == bitmap.width &&
            previous.intrinsicHeight == bitmap.height
        setImageBitmap(bitmap)
        post { if (preserve) constrain() else resetZoom() }
    }

    fun resetZoom() {
        val picture = drawable ?: return
        val w = picture.intrinsicWidth.toFloat()
        val h = picture.intrinsicHeight.toFloat()
        if (w <= 0f || h <= 0f || width <= 0 || height <= 0) return
        zoom = 1f
        transform.setRectToRect(
            RectF(0f, 0f, w, h),
            RectF(0f, 0f, width.toFloat(), height.toFloat()),
            Matrix.ScaleToFit.CENTER
        )
        imageMatrix = transform
    }

    private fun constrain() {
        val picture = drawable ?: return
        val rect = RectF(
            0f, 0f,
            picture.intrinsicWidth.toFloat(),
            picture.intrinsicHeight.toFloat()
        )
        transform.mapRect(rect)
        val dx = if (rect.width() <= width) {
            width / 2f - rect.centerX()
        } else {
            when {
                rect.left > 0f -> -rect.left
                rect.right < width -> width - rect.right
                else -> 0f
            }
        }
        val dy = if (rect.height() <= height) {
            height / 2f - rect.centerY()
        } else {
            when {
                rect.top > 0f -> -rect.top
                rect.bottom < height -> height - rect.bottom
                else -> 0f
            }
        }
        transform.postTranslate(dx, dy)
        transform.getValues(values)
        imageMatrix = transform
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        post { resetZoom() }
    }

    /** Zoom around the view center to [target] (1x–5x); used by the remote, where there is no pinch. */
    fun zoomTo(target: Float) {
        if (drawable == null || width <= 0 || height <= 0) return
        val next = target.coerceIn(1f, 5f)
        if (next == 1f) {
            resetZoom()
            return
        }
        transform.postScale(next / zoom, next / zoom, width / 2f, height / 2f)
        zoom = next
        constrain()
    }

    /**
     * Remote control (D-pad) on TV: OK cycles 1x → 2x → 4x → 1x around the center; with zoom, the
     * arrows pan 20 % of the view per press. At 1x the arrows are not consumed, so focus can still
     * leave the image (no focus trap).
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                zoomTo(
                    when {
                        zoom < 2f -> 2f
                        zoom < 4f -> 4f
                        else -> 1f
                    }
                )
                return true
            }
        }
        if (zoom > 1f) {
            val stepX = width * PAN_STEP
            val stepY = height * PAN_STEP
            val (dx, dy) = when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> stepX to 0f
                KeyEvent.KEYCODE_DPAD_RIGHT -> -stepX to 0f
                KeyEvent.KEYCODE_DPAD_UP -> 0f to stepY
                KeyEvent.KEYCODE_DPAD_DOWN -> 0f to -stepY
                else -> return super.onKeyDown(keyCode, event)
            }
            transform.postTranslate(dx, dy)
            constrain()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaler.onTouchEvent(event)
        if (!scaler.isInProgress && event.pointerCount == 1) {
            gestures.onTouchEvent(event)
        }
        return true
    }

    private companion object {
        const val PAN_STEP = 0.2f
    }
}
