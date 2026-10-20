package org.breezyweather.alertaec.info

/** Explicit policy for retaining expired information as an archive. */
data class OfficialInfoArchivePolicy(val retainExpiredForMillis: Long = 0L)

enum class OfficialInfoFreshness {
    ACTIVE,
    EXPIRED,
    UNKNOWN,
    ARCHIVED
}

object OfficialInfoFreshnessEvaluator {
    fun evaluate(
        item: OfficialInfoItem,
        nowMillis: Long,
        archivePolicy: OfficialInfoArchivePolicy = OfficialInfoArchivePolicy()
    ): OfficialInfoFreshness {
        if (!item.isValid() || nowMillis < 0L) return OfficialInfoFreshness.UNKNOWN
        if (archivePolicy.retainExpiredForMillis < 0L) return OfficialInfoFreshness.UNKNOWN
        val evidenceAt = item.updatedAtMillis ?: item.publishedAtMillis ?: return OfficialInfoFreshness.UNKNOWN
        if (evidenceAt > nowMillis) return OfficialInfoFreshness.UNKNOWN
        val expiresAt = item.expiresAtMillis ?: return OfficialInfoFreshness.UNKNOWN
        if (nowMillis < expiresAt) return OfficialInfoFreshness.ACTIVE
        val archiveUntil = if (archivePolicy.retainExpiredForMillis > Long.MAX_VALUE - expiresAt) {
            Long.MAX_VALUE
        } else {
            expiresAt + archivePolicy.retainExpiredForMillis
        }
        if (archiveUntil >= expiresAt && nowMillis <= archiveUntil) {
            return if (archivePolicy.retainExpiredForMillis > 0L) {
                OfficialInfoFreshness.ARCHIVED
            } else {
                OfficialInfoFreshness.EXPIRED
            }
        }
        return OfficialInfoFreshness.EXPIRED
    }
}
