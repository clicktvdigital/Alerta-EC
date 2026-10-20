package org.breezyweather.alertaec.info

/** Result of selecting cache entries for an offline information view. */
data class OfficialInfoSelectionResult(
    val current: List<OfficialInfoCachedItem>,
    val history: List<OfficialInfoCachedItem>,
    val readState: OfficialInfoSelectionReadState
)

enum class OfficialInfoSelectionReadState {
    EMPTY,
    LOADED,
    READ_ERROR
}

/**
 * Reads and classifies cached information without network, location or permissions.
 * Only source-verified or official ACTIVE entries are eligible for the current list.
 */
class OfficialInfoSelector(
    private val cache: OfficialInfoCache,
    private val archivePolicy: OfficialInfoArchivePolicy = OfficialInfoArchivePolicy()
) {
    fun select(nowMillis: Long): OfficialInfoSelectionResult {
        val entries = runCatching { cache.readWithFreshness(nowMillis, archivePolicy) }
            .getOrElse {
                return OfficialInfoSelectionResult(emptyList(), emptyList(), OfficialInfoSelectionReadState.READ_ERROR)
            }
        if (entries.isEmpty()) {
            return OfficialInfoSelectionResult(emptyList(), emptyList(), OfficialInfoSelectionReadState.EMPTY)
        }
        val ordered = entries.sortedWith(
            compareByDescending<OfficialInfoCachedItem> { sortTimestamp(it.item) }
                .thenBy { it.item.sourceId }
                .thenBy { it.item.bulletinId }
        )
        val current = ordered.filter { entry ->
            entry.freshness == OfficialInfoFreshness.ACTIVE &&
                entry.item.isValid() &&
                entry.item.verification != OfficialInfoVerification.UNVERIFIED
        }
        val currentKeys = current.mapTo(HashSet()) { keyOf(it.item) }
        val history = ordered.filterNot { keyOf(it.item) in currentKeys }
        return OfficialInfoSelectionResult(current, history, OfficialInfoSelectionReadState.LOADED)
    }

    private fun sortTimestamp(item: OfficialInfoItem): Long =
        item.updatedAtMillis ?: item.publishedAtMillis ?: item.fetchedAtMillis

    private fun keyOf(item: OfficialInfoItem): String = item.sourceId + KEY_SEPARATOR + item.bulletinId

    private companion object {
        const val KEY_SEPARATOR = '\u0000'
    }
}
