package org.breezyweather.alertaec

import org.junit.Assert.assertEquals
import org.junit.Test

class AlertaEcNewsFeedTest {

    private fun news(id: String, time: Long) = AlertaEcNewsItem(
        id = id,
        title = "Noticia de prueba",
        summary = "Informacion de prueba",
        sourceName = "Fuente de prueba",
        sourceUrl = "https://example.org",
        publishedAtMillis = time
    )

    @Test
    fun newestPublicationsComeFirst() {
        val items = listOf(
            news("antigua", 1000L),
            news("reciente", 3000L),
            news("intermedia", 2000L)
        )

        assertEquals(
            listOf("reciente", "intermedia", "antigua"),
            AlertaEcNewsFeed.mostRecent(items).map { it.id }
        )
    }

    @Test
    fun repeatedIdentifiersAreRemoved() {
        val items = listOf(
            news("repetida", 1000L),
            news("otra", 2000L),
            news("repetida", 3000L)
        )

        assertEquals(
            2,
            AlertaEcNewsFeed.mostRecent(items).size
        )
    }

    @Test
    fun unverifiedReportsAreNotAutomaticallyOfficial() {
        val item = news("ciudadano", 1000L)

        assertEquals(
            AlertaEcNewsVerification.UNVERIFIED,
            item.verification
        )
    }
}
