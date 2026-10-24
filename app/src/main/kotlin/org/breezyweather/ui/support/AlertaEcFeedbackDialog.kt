package org.breezyweather.ui.support

import android.app.Dialog
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import org.breezyweather.background.location.ActiveLocationController
import java.io.File
import java.util.Date

/** Formulario nativo independiente de la tarjeta Compose reciclable de Inicio.
 * Las pruebas de Android deben verificar IME, URI, retorno de cámara y chooser.
 */
class AlertaEcFeedbackDialog : DialogFragment() {
    companion object {
        fun create(isReport: Boolean) = AlertaEcFeedbackDialog().apply {
            arguments = Bundle().apply { putBoolean("isReport", isReport) }
        }
    }
    private var selectedUri: Uri? = null
    private var mediaUriPending: Uri? = null
    private var attachedLabel: TextView? = null
    private var attachedMime: String? = null

    private val pickMedia = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedUri = uri
            attachedMime = requireContext().contentResolver.getType(uri) ?: "application/octet-stream"
            runCatching {
                requireContext().contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            showAttached("Archivo seleccionado")
        }
    }
    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok && mediaUriPending != null) {
            selectedUri = mediaUriPending
            attachedMime = "image/jpeg"
            showAttached("Fotografía tomada")
        } else { showMessage("La fotografía no se guardó") }
        mediaUriPending = null
    }
    private val takeVideo = registerForActivityResult(ActivityResultContracts.CaptureVideo()) { ok ->
        if (ok && mediaUriPending != null) {
            selectedUri = mediaUriPending
            attachedMime = "video/mp4"
            showAttached("Video grabado")
        } else { showMessage("El video no se guardó") }
        mediaUriPending = null
    }

    private fun showMessage(message: String) {
        if (isAdded) Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }
    private fun showAttached(label: String) {
        if (!isAdded) return
        val size = runCatching {
            requireContext().contentResolver.openFileDescriptor(selectedUri!!, "r")?.use { it.statSize }
        }.getOrNull()
        attachedLabel?.text = "$label · ${attachedMime ?: "archivo"}" +
            (size?.takeIf { it > 0 }?.let { " · ${it / 1024} KB" } ?: "")
    }
    private fun tempUri(extension: String): Uri? = runCatching {
        val ctx = requireContext()
        val dir = ctx.externalCacheDir ?: error("Almacenamiento temporal no disponible")
        val file = File.createTempFile("alerta_ec_evidencia_", ".$extension", dir)
        FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
    }.getOrNull()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val isReport = requireArguments().getBoolean("isReport")
        val dp = context.resources.displayMetrics.density
        fun d(n: Int) = (n * dp + 0.5f).toInt()
        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(d(18), d(12), d(18), d(12))
        }
        val scroll = ScrollView(context).apply {
            isFillViewport = false
            addView(column)
        }
        fun description(t: String) = TextView(context).apply {
            text = t
            textSize = 14f
            setPadding(0, d(8), 0, d(8))
            column.addView(this)
        }
        fun button(t: String, action: () -> Unit) = Button(context).apply {
            text = t
            isAllCaps = false
            setOnClickListener { action() }
            column.addView(this)
        }
        val rating = if (!isReport) RatingBar(context).apply {
            numStars = 5
            stepSize = 1f
            column.addView(this)
        } else null
        val categories = arrayOf("Lluvia intensa", "Inundación", "Deslizamiento", "Rayos", "Otro fenómeno", "Error de aplicación")
        val spinner = if (isReport) Spinner(context).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, categories)
            column.addView(this)
        } else null
        description(if (isReport) "¿Qué ocurrió y cuándo?" else "Escribe tu opinión sobre ALERTA EC")
        val input = EditText(context).apply {
            hint = if (isReport) "Describe lo observado..." else "Escribe tu reseña..."
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            isSingleLine = false
            minLines = 3
            maxLines = 6
            setHorizontallyScrolling(false)
        }
        column.addView(input)
        attachedLabel = if (isReport) description("Sin archivos adjuntos") else null
        if (isReport) {
            button("🖼️ Adjuntar fotografía") { pickMedia.launch(arrayOf("image/*")) }
            button("🎞️ Adjuntar video") { pickMedia.launch(arrayOf("video/*")) }
            button("🎵 Adjuntar audio") { pickMedia.launch(arrayOf("audio/*")) }
            button("📷 Tomar fotografía") {
                tempUri("jpg")?.let { mediaUriPending=it; takePhoto.launch(it) }
                    ?: showMessage("No se pudo preparar la cámara")
            }
            button("🎥 Grabar video") {
                tempUri("mp4")?.let { mediaUriPending=it; takeVideo.launch(it) }
                    ?: showMessage("No se pudo preparar la videocámara")
            }
            button("Quitar archivo") {
                selectedUri = null; attachedMime = null
                attachedLabel?.text = "Sin archivos adjuntos"
            }
        }
        val includeGps = if (isReport) CheckBox(context).apply {
            text = "Adjuntar ubicación GPS reciente (voluntario)"
            isChecked = false
            column.addView(this)
        } else null
        if (isReport) description("Este formulario prepara un reporte voluntario; no hay envío comunitario automático ni atención de emergencias.")
        val dialog = AlertDialog.Builder(context)
            .setTitle(if (isReport) "📢 Reporte ALERTA EC" else "⭐ Reseña ALERTA EC")
            .setView(scroll)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Elegir dónde enviar", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val body = input.text.toString().trim()
                if (body.isBlank()) { input.error="Escribe un mensaje"; input.requestFocus(); return@setOnClickListener }
                if (!isReport) {
                    val stars = rating?.rating?.toInt() ?: 0
                    if (stars !in 1..5) { showMessage("Selecciona de 1 a 5 estrellas"); return@setOnClickListener }
                    val subject = "Reseña ALERTA EC"
                    val text = "Valoración: $stars/5\nComentario: $body"
                    val uri = Uri.parse("mailto:clicktvdigital@gmail.com?subject=" + Uri.encode(subject) + "&body=" + Uri.encode(text))
                    try {
                        startActivity(Intent(Intent.ACTION_SENDTO, uri))
                        dismissAllowingStateLoss()
                    } catch (e: Exception) { showMessage("No hay aplicación de correo disponible") }
                } else {
                    val snapshot = if (includeGps?.isChecked == true) ActiveLocationController.getLatest(context) else null
                    val gps = when {
                        includeGps?.isChecked != true -> "GPS: no compartido"
                        snapshot == null -> "GPS: sin posición guardada"
                        System.currentTimeMillis() - snapshot.timestamp !in 0L..120000L -> "GPS: posición antigua, no compartida"
                        else -> "GPS reciente: ${snapshot.latitude}, ${snapshot.longitude}; precisión horizontal: ${snapshot.accuracy ?: "no disponible"} m"
                    }
                    val subject = "Reporte ALERTA EC - ${categories[spinner?.selectedItemPosition ?: 0]}"
                    val text = "Reporte voluntario ALERTA EC\n$subject\nDescripción: $body\n$gps\nHora del teléfono: ${Date()}\nEnvío pendiente de confirmación por el usuario."
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = if (selectedUri == null) "text/plain" else attachedMime ?: "application/octet-stream"
                        putExtra(Intent.EXTRA_SUBJECT, subject)
                        putExtra(Intent.EXTRA_TEXT, text)
                        selectedUri?.let { uri ->
                            putExtra(Intent.EXTRA_STREAM, uri)
                            clipData = ClipData.newRawUri("Evidencia ALERTA EC", uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    }
                    try {
                        startActivity(Intent.createChooser(intent, "Elegir aplicación para enviar el reporte"))
                        dismissAllowingStateLoss()
                    } catch (e: Exception) { showMessage("No hay aplicación para compartir el reporte") }
                }
            }
            input.requestFocus()
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE or
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            input.postDelayed({
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
            }, 180L)
        }
        return dialog
    }
}
