package org.breezyweather.ui.main.intro

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import kotlin.math.min

/**
 * Plays the intro video into a TextureView instead of a SurfaceView (VideoView).
 *
 * On the Indurama TV the SurfaceView path never opened the video: its surface was not handed to the
 * player, so onPrepared never fired and the intro timed out. A TextureView is an ordinary view in
 * the normal drawing tree, so it does not depend on a separate surface or on the media-overlay layer
 * that the TV compositor (HWComposer) handles differently. The MediaPlayer is only created once the
 * texture exists, and is released with the texture.
 */
internal class IntroVideoView(context: Context) : TextureView(context), TextureView.SurfaceTextureListener {

    private var uri: Uri? = null
    private var player: MediaPlayer? = null
    private var surface: Surface? = null
    private var videoWidth = 0
    private var videoHeight = 0

    private var onPrepared: ((MediaPlayer) -> Unit)? = null
    private var onCompletion: ((MediaPlayer) -> Unit)? = null
    private var onError: ((MediaPlayer, Int, Int) -> Boolean)? = null

    init {
        isOpaque = false
        surfaceTextureListener = this
    }

    fun setVideoURI(uri: Uri) {
        this.uri = uri
        openIfReady()
    }

    fun setOnPreparedListener(listener: (MediaPlayer) -> Unit) {
        onPrepared = listener
    }

    fun setOnCompletionListener(listener: (MediaPlayer) -> Unit) {
        onCompletion = listener
    }

    fun setOnErrorListener(listener: (MediaPlayer, Int, Int) -> Boolean) {
        onError = listener
    }

    /** Starts playback of the prepared player. Does nothing when there is no prepared player. */
    fun start() {
        player?.start()
    }

    fun stopPlayback() {
        release()
    }

    private fun openIfReady() {
        val target = uri ?: return
        val surf = surface ?: return
        release()
        val mp = MediaPlayer()
        player = mp
        try {
            mp.setDataSource(context, target)
            mp.setSurface(surf)
            mp.setOnVideoSizeChangedListener { p, w, h ->
                if (player === p) {
                    videoWidth = w
                    videoHeight = h
                    applyFit()
                }
            }
            mp.setOnPreparedListener { p -> if (player === p) onPrepared?.invoke(p) }
            mp.setOnCompletionListener { p -> if (player === p) onCompletion?.invoke(p) }
            mp.setOnErrorListener { p, what, extra ->
                if (player === p) onError?.invoke(p, what, extra) ?: true else true
            }
            mp.prepareAsync()
        } catch (e: Exception) {
            android.util.Log.w("AlertaEcStartup", "intro player failed: ${e.javaClass.simpleName}")
            onError?.invoke(mp, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0)
        }
    }

    /** Letterbox the video inside the view, like VideoView did. */
    private fun applyFit() {
        val vw = videoWidth
        val vh = videoHeight
        val w = width
        val h = height
        if (vw <= 0 || vh <= 0 || w <= 0 || h <= 0) return
        val scale = min(w.toFloat() / vw, h.toFloat() / vh)
        val matrix = Matrix()
        matrix.setScale(vw * scale / w, vh * scale / h, w / 2f, h / 2f)
        setTransform(matrix)
    }

    private fun release() {
        val mp = player ?: return
        player = null
        runCatching { mp.stop() }
        runCatching { mp.release() }
    }

    override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
        surface?.release()
        surface = Surface(texture)
        openIfReady()
    }

    override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
        applyFit()
    }

    override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
        release()
        surface?.release()
        surface = null
        return true
    }

    override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {
        // Nothing to do: the player draws into the surface on its own.
    }
}
