package org.breezyweather.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.view.GestureDetector
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
        setImageBitmap(bitmap)
        post { resetZoom() }
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

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaler.onTouchEvent(event)
        if (!scaler.isInProgress && event.pointerCount == 1) {
            gestures.onTouchEvent(event)
        }
        return true
    }
}
