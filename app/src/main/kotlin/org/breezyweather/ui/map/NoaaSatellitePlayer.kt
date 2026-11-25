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
    private var selectedMode = 0
    private val modeLabels = listOf("☁ Nubes", "🌧 Lluvia", "⚡ Rayos", "🌎 Amplia")
    /** Android TV boxes: smaller JPEGs and fewer frames so the animation starts fast on slow Wi-Fi. */
    private val compact = context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK) ||
        (context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager).isLowRamDevice
    private val modeButtons = mutableListOf<Button>()
    private val image = NoaaZoomImageView(context).apply {
        setBackgroundColor(android.graphics.Color.rgb(8, 14, 20))
        minimumHeight = (resources.displayMetrics.density * 220f).toInt()
        contentDescription = "Imagen NOAA"
    }
    private val timeLabel = TextView(context)
    private val playButton = Button(context)
    private val retryButton = Button(context)
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

    private fun loadSelectedMode(position: Int, force: Boolean = false) {
        if (!force && displayedMode == position && modeJob?.isActive == true) return
        val wasPlaying = playing
        val sameMode = displayedMode == position
        handler.removeCallbacks(advance)
        if (!sameMode) {
            playing = false
            playButton.text = "▶ Reproducir"
        }
        modeJob?.cancel()
        lastModeRefreshAt = android.os.SystemClock.elapsedRealtime()
        retryAfterFailure = false
        requestId++
        loadJob?.cancel()
        loadingFrameUrl = null
        val request = ++modeRequestId
        displayedMode = position
        if (!sameMode) setFrames(emptyList())
        timeLabel.text = "Consultando imágenes NOAA..."
        modeJob = scope.launch {
            val result = runCatching {
                when (position) {
                    1 -> NoaaSatelliteFrames.rainRecent(compact)
                    2 -> NoaaSatelliteFrames.lightningRecent(compact)
                    3 -> NoaaSatelliteFrames.fullDiskRecent(compact)
                    else -> NoaaSatelliteFrames.recent(compact)
                }
            }.onFailure { if (it is CancellationException) throw it }
            if (request == modeRequestId) {
                val updated = result.getOrNull().orEmpty()
                if (updated.isNotEmpty()) {
                    retryAfterFailure = false
                    setFrames(updated)
                    if (sameMode && wasPlaying) {
                        playing = true
                        playButton.text = "⏸ Pausar"
                        handler.removeCallbacks(advance)
                        handler.postDelayed(advance, 1200L)
                    }
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
    private var loadingFrameUrl: String? = null
    private var requestId = 0L
    private var framesVersion = 0L
    private var displayedFrameUrl: String? = null
    private var displayedFrameVersion = -1L
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
                handler.removeCallbacks(this)
                playButton.text = "▶ Reproducir"
                timeLabel.text = "No se pudieron cargar las imágenes NOAA. Pulsa «Reintentar NOAA»."
                return
            }
            index = next
            showFrame()
            handler.postDelayed(this, 1200L)
        }
    }

    init {
        orientation = VERTICAL
        // V5: botones NOAA visibles, con estado seleccionado, sin desplegable oculto.
        val choices = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
        }
        modeLabels.forEachIndexed { position, label ->
            val button = Button(context).apply {
                textSize = 12f
                isAllCaps = false
                setPadding(2, 6, 2, 6)
                setOnClickListener {
                    if (selectedMode != position) {
                        selectedMode = position
                        refreshModeButtons()
                        loadSelectedMode(position)
                    }
                }
            }
            modeButtons.add(button)
            choices.addView(button, android.widget.LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        }
        addView(choices)
        addView(TextView(context).apply {
            text = "NOAA GOES-19 · Norte de Sudamérica (incluye Ecuador). Imágenes regionales, no de calles."
            textSize = 12f
            setPadding(12, 6, 12, 9)
        })
        refreshModeButtons()
        addView(image, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        // The image takes D-pad focus (neon frame when focused) and steps through frames with the remote.
        image.isFocusable = true
        image.setOnKeyListener { _, keyCode, event ->
            event.action == android.view.KeyEvent.ACTION_DOWN && handleRemoteKey(keyCode)
        }
        image.setOnFocusChangeListener { view, focused ->
            view.foreground = if (focused) {
                android.graphics.drawable.GradientDrawable().apply {
                    setStroke((4f * resources.displayMetrics.density).toInt(), android.graphics.Color.rgb(0, 229, 255))
                }
            } else {
                null
            }
        }
        addView(timeLabel)
        playButton.text = "▶ Reproducir"
        addView(playButton)
        retryButton.text = "↻ Reintentar NOAA"
        retryButton.setOnClickListener {
            frames.getOrNull(index)?.imageUrl?.let(failedFrameUrls::remove)
            loadSelectedMode(selectedMode, force = true)
        }
        addView(retryButton)
        addView(timeline)

        playButton.setOnClickListener {
            if (frames.isEmpty()) return@setOnClickListener
            playing = !playing
            playButton.text = if (playing) "⏸ Pausar" else "▶ Reproducir"
            handler.removeCallbacks(advance)
            if (playing) handler.postDelayed(advance, 1200L)
        }
        // Comenzar en el modo Nubes sin depender de selección de Spinner.
        loadSelectedMode(selectedMode)
        timeline.keyProgressIncrement = 1
        timeline.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    // Scrubbing (touch or ←/→ on the TV remote) stops the animation on the chosen frame.
                    if (playing) {
                        playing = false
                        handler.removeCallbacks(advance)
                        playButton.text = "▶ Reproducir"
                    }
                    index = progress
                    showFrame()
                }
            }
            override fun onStartTrackingTouch(bar: SeekBar?) = Unit
            override fun onStopTrackingTouch(bar: SeekBar?) = Unit
        })
    }

    private fun refreshModeButtons() {
        val labels = modeLabels
        modeButtons.forEachIndexed { index, button ->
            button.text = if (selectedMode == index) "✓ ${labels[index]}" else labels[index]
            button.contentDescription = "${labels[index]}" +
                if (selectedMode == index) " · seleccionada" else " · pulsar para mostrar"
        }
    }
    fun setFrames(items: List<NoaaSatelliteFrame>) {
        val previousUrl = frames.getOrNull(index)?.imageUrl
        failedFrameUrls.clear()
        frames = items.sortedBy { it.observedAt }.takeLast(48)
        if (frames.none { it.imageUrl == displayedFrameUrl }) {
            framesVersion++
        }
        index = frames.indexOfFirst { it.imageUrl == previousUrl }.takeIf { it >= 0 }
            ?: (frames.size - 1).coerceAtLeast(0)
        timeline.max = (frames.size - 1).coerceAtLeast(0)
        timeline.progress = index
        showFrame()
    }

    private fun showFrame() {
        val frame = frames.getOrNull(index) ?: run {
            requestId++
            loadJob?.cancel()
            loadingFrameUrl = null
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
                loadSelectedMode(selectedMode)
            }
            return
        }
        timeline.progress = index
        if (isCurrentFrameVisibleAndValid()) {
            timeLabel.text = EcuadorTimeFormatter.format(frame.observedAt, galapagosTime)
            return
        }
        if (loadingFrameUrl == frame.imageUrl && loadJob?.isActive == true) return
        timeLabel.text = EcuadorTimeFormatter.format(frame.observedAt, galapagosTime) + " · Cargando imagen..."
        // V5: no borrar el fotograma anterior al reproducir;
        // preservarlo conserva el zoom mientras se descarga el siguiente.
        loadJob?.cancel()
        loadingFrameUrl = frame.imageUrl
        val request = ++requestId
        loadJob = scope.launch {
            val bitmap = runCatching { NoaaImageLoader.load(frame.imageUrl) }
                .onFailure { if (it is CancellationException) throw it }
                .getOrNull()
            if (request == requestId) {
                loadingFrameUrl = null
                if (bitmap != null) {
                    failedFrameUrls.remove(frame.imageUrl)
                    displayedFrameUrl = frame.imageUrl
                    displayedFrameVersion = framesVersion
                    image.showBitmap(bitmap)
                    timeLabel.text = EcuadorTimeFormatter.format(frame.observedAt, galapagosTime)
                } else {
                    failedFrameUrls.add(frame.imageUrl)
                    timeLabel.text = EcuadorTimeFormatter.format(frame.observedAt, galapagosTime) +
                        " · Imagen no disponible"
                    if (playing) {
                        handler.removeCallbacks(advance)
                        handler.postDelayed(advance, 1200L)
                    }
                }
            }
        }
    }

    /** Selects a mode (0 Nubes, 1 Lluvia, 2 Rayos, 3 Amplia), e.g. from the map's shortcuts. */
    fun selectMode(position: Int) {
        val target = position.coerceIn(0, modeLabels.size - 1)
        if (selectedMode == target && displayedMode == target) return
        selectedMode = target
        refreshModeButtons()
        loadSelectedMode(target)
    }

    /** Remote / keyboard on the image: Left/Right step one frame (pausing), OK plays or pauses. Up/Down move focus. */
    fun handleRemoteKey(keyCode: Int): Boolean = when (keyCode) {
        android.view.KeyEvent.KEYCODE_DPAD_LEFT, android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
            if (frames.isNotEmpty()) {
                if (playing) playButton.performClick()
                val step = if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT) 1 else -1
                index = (index + step + frames.size) % frames.size
                showFrame()
            }
            true
        }
        android.view.KeyEvent.KEYCODE_DPAD_CENTER, android.view.KeyEvent.KEYCODE_ENTER,
        android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, android.view.KeyEvent.KEYCODE_SPACE -> {
            playButton.performClick()
            true
        }
        else -> false
    }

    fun updateTimeZone(isGalapagos: Boolean) {
        if (galapagosTime == isGalapagos) return
        galapagosTime = isGalapagos
        frames.getOrNull(index)?.let { frame ->
            timeLabel.text = EcuadorTimeFormatter.format(frame.observedAt, galapagosTime) +
                if (frame.imageUrl in failedFrameUrls) " · Imagen no disponible" else ""
        }
    }

    /** Gives D-pad focus to the image so Left/Right/OK work straight away on Android TV. */
    fun focusImage() {
        image.requestFocus()
    }

    fun resume() {
        val now = android.os.SystemClock.elapsedRealtime()
        val interval = if (retryAfterFailure) retryIntervalMs else refreshIntervalMs
        if (modeJob?.isActive != true && (lastModeRefreshAt == 0L || now - lastModeRefreshAt >= interval)) {
            loadSelectedMode(selectedMode)
        } else if (!isCurrentFrameVisibleAndValid()) {
            showFrame()
        }
    }

    private fun isCurrentFrameVisibleAndValid(): Boolean {
        val frame = frames.getOrNull(index) ?: return false
        val age = java.time.Duration.between(frame.observedAt, java.time.Instant.now())
        return displayedFrameVersion == framesVersion &&
            displayedFrameUrl == frame.imageUrl &&
            frame.imageUrl !in failedFrameUrls &&
            !age.isNegative && age.toMinutes() < 360
    }

    fun pause() {
        playing = false
        playButton.text = "▶ Reproducir"
        handler.removeCallbacks(advance)
        requestId++
        loadJob?.cancel()
        loadingFrameUrl = null
        modeRequestId++
        modeJob?.cancel()
        lastModeRefreshAt = 0L
    }

    fun release() {
        modeRequestId++
        modeJob?.cancel()
        requestId++
        loadJob?.cancel()
        loadingFrameUrl = null
        scope.coroutineContext[Job]?.cancel()
        playing = false
        handler.removeCallbacks(advance)
    }
}
