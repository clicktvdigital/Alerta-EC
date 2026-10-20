package org.breezyweather.alertaec.support

/**
 * Estados del autosoporte. FINALIZADO indica que termino el proceso,
 * no que todos los problemas hayan sido solucionados.
 */
internal enum class AutoSupportPhase {
    INACTIVO,
    DIAGNOSTICANDO,
    REPARANDO,
    FINALIZADO,
}

internal enum class AutoSupportResult {
    CORRECTO,
    REPARADO,
    PENDIENTE,
    ERROR,
}

internal data class AutoSupportCheck(
    val id: String,
    val description: String,
    val result: AutoSupportResult,
    val detail: String,
    val acquiredAtMillis: Long? = null,
    val queueBusy: Boolean? = null,
)

internal data class AutoSupportReport(
    val checks: List<AutoSupportCheck>,
) {
    val pendingCount: Int
        get() = checks.count {
            it.result == AutoSupportResult.PENDIENTE ||
                it.result == AutoSupportResult.ERROR
        }

    val repairedCount: Int
        get() = checks.count { it.result == AutoSupportResult.REPARADO }

    val completedWithoutPendingIssues: Boolean
        get() = checks.isNotEmpty() && pendingCount == 0
}
