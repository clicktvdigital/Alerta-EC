package org.breezyweather.background.updater

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import org.breezyweather.BuildConfig
import org.breezyweather.background.updater.model.Release
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

/**
 * Descarga la APK de un release de GitHub y abre el instalador del sistema.
 * Android siempre pide confirmación al usuario; no instala en silencio.
 */
class AppUpdateInstaller @Inject constructor() {

    fun getApkUrl(release: Release): String? = release.getDownloadLink()

    /**
     * Descarga la APK en la caché privada de la app. Debe ejecutarse en un hilo de IO.
     *
     * @param onProgress porcentaje descargado (0..100), solo si el servidor informa el tamaño.
     */
    fun download(context: Context, url: String, onProgress: (Int) -> Unit = {}): File {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val target = File(dir, "Alerta-EC-update.apk")

        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 15_000
            readTimeout = 30_000
        }
        try {
            connection.connect()
            val code = connection.responseCode
            if (code !in 200..299) {
                error("No se pudo descargar la actualización (HTTP $code)")
            }
            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var copied = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        copied += read
                        if (total > 0) onProgress((copied * 100 / total).toInt())
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
        return target
    }

    fun canInstallPackages(context: Context): Boolean {
        return context.packageManager.canRequestPackageInstalls()
    }

    /** Pantalla del sistema para permitir instalar apps desde esta app. */
    fun unknownSourcesSettingsIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            "package:${context.packageName}".toUri()
        ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
    }

    /** Intent que abre el instalador de Android con la APK descargada. */
    fun installIntent(context: Context, apk: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            apk
        )
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
