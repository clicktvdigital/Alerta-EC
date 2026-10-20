package org.breezyweather.ui.map

import android.content.Context
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button
import android.widget.SeekBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

internal class NoaaSatellitePlayer(context: Context, private var galapagosTime: Boolean = false) : LinearLayout(context) {
    private val modeSelector = android.widget.Spinner(context)
    private val image = NoaaZoomImageView(context)
    private val timeLabel = TextView(context)
    private val playButton = Button(context)
    private val timeline = SeekBar(context)
    private var frames: List<NoaaSatelliteFrame> = emptyList()
    private var index = 0
    private var modeJob: Job? = null
    private var modeRequestId = 0L
    private var displayedMode = -1
    private var lastModeRefreshAt = 0L
    private val refreshIntervalMs = 15 * 60 * 1000L
    private val retryIntervalMs = 60 * 1000L
    private var retryAfterFailure = false

    private fun loadSelectedMode(position: Int) {
        playing = false
        handler.removeCallbacks(advance)
        playButton.text = "▶ Reproducir"
        modeJob?.cancel()
        lastModeRefreshAt = android.os.SystemClock.elapsedRealtime()
        retryAfterFailure = false
        val request = ++modeRequestId
        val sameMode = displayedMode == position
        displayedMode = position
        if (!sameMode) setFrames(emptyList())
        timeLabel.text = "Consultando imágenes NOAA..."
        modeJob = scope.launch {
            val result = runCatching {
                when (position) {
                    1 -> NoaaSatelliteFrames.lightningRecent()
                    2 -> NoaaSatelliteFrames.fullDiskRecent()
                    else -> NoaaSatelliteFrames.recent()
                }
            }.onFailure { if (it is CancellationException) throw it }
            if (request == modeRequestId) {
                val updated = result.getOrNull().orEmpty()
                if (updated.isNotEmpty()) {
                    retryAfterFailure = false
                    setFrames(updated)
                } else {
                    retryAfterFailure = true
                    val now = java.time.Instant.now()
                    val validFrames = frames.filter { frame ->
                        val age = java.time.Duration.between(frame.observedAt, now)
                        !age.isNegative && age.toMinutes() < 360
                    }
                    if (validFrames.size != frames.size) {
                        setFrames(validFrames)
                    }
                    timeLabel.text = if (frames.isNotEmpty()) {
                        "No se pudo actualizar NOAA. Se conservan las imágenes anteriores."
                    } else if (result.isFailure) {
                        "No se pudo conectar con NOAA. Comprueba Internet e inténtalo nuevamente."
                    } else {
                        "NOAA no ofrece imágenes recientes para este modo."
                    }
                }
            }
        }
    }

    private var playing = false
    private val failedFrameUrls = mutableSetOf<String>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var loadJob: Job? = null
    private var requestId = 0L
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    private val advance = object : Runnable {
        override fun run() {
            if (!playing || frames.isEmpty()) return
            if (loadJob?.isCompleted != true) {
                handler.postDelayed(this, 300L)
                return
            }
            val next = (1..frames.size).map { (index + it) % frames.size }.firstOrNull { frames[it].imageUrl !in failedFrameUrls }
            if (next == null) {
                playing = false
                playButton.text = "▶ Reproducir"
                timeLabel.text = "No se pudieron cargar las imágenes NOAA"
                return
            }
            index = next
            showFrame()
            handler.postDelayed(this, 1200L)
        }
    }

    init {
        orientation = VERTICAL
        modeSelector.adapter = android.widget.ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, listOf("☁️ Nubes", "⚡ Rayos", "🌎 Vista amplia"))
        addView(modeSelector)
        modeSelector.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?,
                view: android.view.View?,
                position: Int,
                id: Long
            ) {
                loadSelectedMode(position)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }
        addView(image, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        addView(timeLabel)
        playButton.text = "▶ Reproducir"
        addView(playButton)
        addView(timeline)

        playButton.setOnClickListener {
            if (frames.isEmpty()) return@setOnClickListener
            playing = !playing
            playButton.text = if (playing) "⏸ Pausar" else "▶ Reproducir"
            handler.removeCallbacks(advance)
            if (playing) handler.postDelayed(advance, 1200L)
        }
        timeline.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    index = progress
                    showFrame()
                }
            }
            override fun onStartTrackingTouch(bar: SeekBar?) = Unit
            override fun onStopTrackingTouch(bar: SeekBar?) = Unit
        })
    }

    fun setFrames(items: List<NoaaSatelliteFrame>) {
        failedFrameUrls.clear()
        frames = items.sortedBy { it.observedAt }
        index = 0
        timeline.max = (frames.size - 1).coerceAtLeast(0)
        showFrame()
    }

    private fun showFrame() {
        val frame = frames.getOrNull(index) ?: run {
            requestId++
            loadJob?.cancel()
            playing = false
            handler.removeCallbacks(advance)
            playButton.text = "▶ Reproducir"
            timeLabel.text = "Sin imágenes satelitales recientes"
            image.setImageDrawable(null)
            return
        }
        val age = java.time.Duration.between(frame.observedAt, java.time.Instant.now())
        if (age.isNegative || age.toMinutes() >= 360) {
            playing = false
            handler.removeCallbacks(advance)
            playButton.text = "▶ Reproducir"
            val validFrames = frames.filter {
                val frameAge = java.time.Duration.between(it.observedAt, java.time.Instant.now())
                !frameAge.isNegative && frameAge.toMinutes() < 360
            }
            if (validFrames.size != frames.size) {
                setFrames(validFrames)
            }
            if (validFrames.isEmpty()) {
                timeLabel.text = "Imágenes NOAA caducadas. Esperando actualización."
            }
            val nowElapsed = android.os.SystemClock.elapsedRealtime()
            if (modeJob?.isActive != true &&
                (lastModeRefreshAt == 0L || nowElapsed - lastModeRefreshAt >= if (retryAfterFailure) retryIntervalMs else refreshIntervalMs)
            ) {
                loadSelectedMode(modeSelector.selectedItemPosition)
            }
            return
        }
        timeline.progress = index
        timeLabel.text = EcuadorTimeFormatter.format(frame.observedAt, galapagosTime)
        loadJob?.cancel()
        val request = ++requestId
        image.setImageDrawable(null)
        loadJob = scope.launch {
            val bitmap = runCatching { NoaaImageLoader.load(frame.imageUrl) }
                .onFailure { if (it is CancellationException) throw it }
                .getOrNull()
            if (request == requestId) {
                if (bitmap != null) {
                    failedFrameUrls.remove(frame.imageUrl)
                    image.showBitmap(bitmap)
                } else {
                    failedFrameUrls.add(frame.imageUrl)
                    timeLabel.text = EcuadorTimeFormatter.format(frame.observedAt, galapagosTime) +
                        " · Imagen no disponible"
                }
            }
        }
    }

    fun updateTimeZone(isGalapagos: Boolean) {
        if (galapagosTime == isGalapagos) return
        galapagosTime = isGalapagos
        frames.getOrNull(index)?.let { frame ->
            timeLabel.text = EcuadorTimeFormatter.format(frame.observedAt, galapagosTime) +
                if (frame.imageUrl in failedFrameUrls) " · Imagen no disponible" else ""
        }
    }

    fun resume() {
        val now = android.os.SystemClock.elapsedRealtime()
        val interval = if (retryAfterFailure) retryIntervalMs else refreshIntervalMs
        if (modeJob?.isActive != true && (lastModeRefreshAt == 0L || now - lastModeRefreshAt >= interval)) {
            loadSelectedMode(modeSelector.selectedItemPosition)
        } else {
            showFrame()
        }
    }

    fun pause() {
        playing = false
        playButton.text = "▶ Reproducir"
        handler.removeCallbacks(advance)
        requestId++
        loadJob?.cancel()
        modeRequestId++
        modeJob?.cancel()
    }

    fun release() {
        modeRequestId++
        modeJob?.cancel()
        requestId++
        loadJob?.cancel()
        scope.coroutineContext[Job]?.cancel()
        playing = false
        handler.removeCallbacks(advance)
    }
}
