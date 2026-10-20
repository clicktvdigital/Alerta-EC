package org.breezyweather.alertaec.support

/**
 * Prepara texto localmente. No envia informacion ni consulta identificadores,
 * coordenadas, cuentas o datos privados del dispositivo.
 */
internal object AutoSupportFeedback {

    fun compose(
        description: String,
        report: AutoSupportReport?,
        includeDiagnostics: Boolean,
    ): String {
        val safeDescription = description.trim().take(4000)
        val lines = mutableListOf(
            "ALERTA EC - Informe voluntario de ayuda",
            "",
            "Problema o sugerencia:",
            if (safeDescription.isBlank()) "(Sin descripcion)" else safeDescription,
        )
        if (includeDiagnostics && report != null) {
            lines.add("")
            lines.add("Diagnostico local compartido voluntariamente:")
            report.checks.forEach { check ->
                lines.add("- ${check.description}: ${check.result.name}")
            }
            lines.add("Pendientes: ${report.pendingCount}. Reparados: ${report.repairedCount}.")
        }
        lines.add("")
        lines.add("Revise el texto antes de enviarlo. No comparta datos personales o sensibles.")
        return lines.joinToString("\n")
    }
}
