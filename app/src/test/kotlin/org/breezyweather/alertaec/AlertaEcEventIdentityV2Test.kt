package org.breezyweather.alertaec

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import java.util.Locale

class AlertaEcEventIdentityV2Test {
    private val quito = AlertaEcEventIdentityV2.LocationInput(-0.180653, -78.467834)
    private val galapagos = AlertaEcEventIdentityV2.LocationInput(-0.9536, -90.9656)

    private fun forecast(
        location: AlertaEcEventIdentityV2.LocationInput = quito,
        source: String = "Fuente EC",
        severity: AlertaEcSeverity? = AlertaEcSeverity.ADVISORY,
        end: Long? = 1_700_036_000_000L,
    ) = AlertaEcEventIdentityV2.ForecastInput(
        source = source,
        location = location,
        phenomenon = AlertaEcPhenomenon.RAIN,
        startTimeMillis = 1_700_000_000_000L,
        severity = severity,
        endTimeMillis = end,
    )

    @Test
    fun differentLocationsAndSourcesProduceDifferentIds() {
        val q = AlertaEcEventIdentityV2.forecast(forecast())
        val g = AlertaEcEventIdentityV2.forecast(forecast(location = galapagos))
        val otherSource = AlertaEcEventIdentityV2.forecast(forecast(source = "Otra fuente"))
        q shouldNotBe g
        q shouldNotBe otherSource
    }

    @Test
    fun idsAreStableAndDoNotExposeInputs() {
        val input = forecast(source = "  MÉTODO  ")
        val first = AlertaEcEventIdentityV2.forecast(input)
        val second = AlertaEcEventIdentityV2.forecast(input)
        first shouldBe second
        val id = (first as AlertaEcEventIdentityV2.Result.Valid).id
        id.contains("-0.180653") shouldBe false
        id.contains("método") shouldBe false
    }

    @Test
    fun unicodeNfcAndCosmeticChangesAreStable() {
        val composed = forecast(source = "Café")
        val decomposed = forecast(source = "Cafe\u0301")
        AlertaEcEventIdentityV2.forecast(composed) shouldBe AlertaEcEventIdentityV2.forecast(decomposed)
        AlertaEcEventIdentityV2.forecast(forecast(source = "  fuente   ec ")) shouldBe
            AlertaEcEventIdentityV2.forecast(forecast(source = "FUENTE EC"))
    }

    @Test
    fun materialChangesChangeRevisionOnly() {
        val advisory = AlertaEcEventIdentityV2.forecast(forecast()) as AlertaEcEventIdentityV2.Result.Valid
        val warning = AlertaEcEventIdentityV2.forecast(forecast(severity = AlertaEcSeverity.WARNING)) as AlertaEcEventIdentityV2.Result.Valid
        val changedValidity = AlertaEcEventIdentityV2.forecast(forecast(end = 1_700_040_000_000L)) as AlertaEcEventIdentityV2.Result.Valid
        advisory.id.substringBeforeLast('-') shouldBe warning.id.substringBeforeLast('-')
        advisory.id shouldNotBe warning.id
        advisory.id shouldNotBe changedValidity.id
    }

    @Test
    fun invalidLocationAndMissingOfficialAlertIdAreExplicit() {
        val invalid = AlertaEcEventIdentityV2.forecast(
            forecast(location = AlertaEcEventIdentityV2.LocationInput(91.0, -78.0)),
        )
        invalid shouldBe AlertaEcEventIdentityV2.Result.Invalid("ubicación inválida")
        val missingId = AlertaEcEventIdentityV2.official(
            AlertaEcEventIdentityV2.OfficialInput("EC", quito, " "),
        )
        missingId shouldBe AlertaEcEventIdentityV2.Result.Invalid("alertId vacío")
    }

    @Test
    fun locationValidationCoversNullNaNInfinityAndGeographicLimits() {
        val invalidLocations = listOf(
            AlertaEcEventIdentityV2.LocationInput(null, 0.0),
            AlertaEcEventIdentityV2.LocationInput(0.0, null),
            AlertaEcEventIdentityV2.LocationInput(Double.NaN, 0.0),
            AlertaEcEventIdentityV2.LocationInput(0.0, Double.POSITIVE_INFINITY),
            AlertaEcEventIdentityV2.LocationInput(90.000001, 0.0),
            AlertaEcEventIdentityV2.LocationInput(0.0, -180.000001),
        )
        invalidLocations.forEach { location ->
            AlertaEcEventIdentityV2.forecast(forecast(location = location)) shouldBe
                AlertaEcEventIdentityV2.Result.Invalid("ubicación inválida")
        }
        AlertaEcEventIdentityV2.forecast(forecast(location = AlertaEcEventIdentityV2.LocationInput(90.0, -180.0))) shouldNotBe
            AlertaEcEventIdentityV2.Result.Invalid("ubicación inválida")
        AlertaEcEventIdentityV2.forecast(forecast(location = AlertaEcEventIdentityV2.LocationInput(-0.0, 0.0))) shouldNotBe
            AlertaEcEventIdentityV2.Result.Invalid("ubicación inválida")
    }

    @Test
    fun emptySourceAndPhenomenonAreRejectedWithoutInventingValues() {
        AlertaEcEventIdentityV2.forecast(forecast(source = " ")) shouldBe
            AlertaEcEventIdentityV2.Result.Invalid("source vacío")
        AlertaEcEventIdentityV2.forecast(
            forecast().copy(phenomenon = null),
        ) shouldBe AlertaEcEventIdentityV2.Result.Invalid("phenomenon ausente")
    }

    @Test
    fun temporalValidationIsExplicitAndOverflowSafe() {
        AlertaEcEventIdentityV2.forecast(forecast().copy(startTimeMillis = null)) shouldBe
            AlertaEcEventIdentityV2.Result.Invalid("inicio ausente")
        AlertaEcEventIdentityV2.forecast(forecast().copy(startTimeMillis = 0L)) shouldBe
            AlertaEcEventIdentityV2.Result.Invalid("inicio inválido")
        AlertaEcEventIdentityV2.forecast(forecast().copy(startTimeMillis = -1L)) shouldBe
            AlertaEcEventIdentityV2.Result.Invalid("inicio inválido")
        AlertaEcEventIdentityV2.forecast(forecast().copy(endTimeMillis = 0L)) shouldBe
            AlertaEcEventIdentityV2.Result.Invalid("fin inválido")
        AlertaEcEventIdentityV2.forecast(forecast().copy(endTimeMillis = -1L)) shouldBe
            AlertaEcEventIdentityV2.Result.Invalid("fin inválido")
        AlertaEcEventIdentityV2.forecast(forecast(end = null)) shouldNotBe
            AlertaEcEventIdentityV2.Result.Invalid("fin inválido")
        AlertaEcEventIdentityV2.forecast(forecast().copy(endTimeMillis = 1_700_000_000_000L)) shouldBe
            AlertaEcEventIdentityV2.Result.Invalid("fin no posterior al inicio")
        AlertaEcEventIdentityV2.forecast(forecast().copy(endTimeMillis = 1_699_999_999_999L)) shouldBe
            AlertaEcEventIdentityV2.Result.Invalid("fin no posterior al inicio")
        AlertaEcEventIdentityV2.forecast(forecast().copy(startTimeMillis = Long.MAX_VALUE - 1L, endTimeMillis = Long.MAX_VALUE)) shouldNotBe
            AlertaEcEventIdentityV2.Result.Invalid("inicio inválido")
        AlertaEcEventIdentityV2.official(
            AlertaEcEventIdentityV2.OfficialInput("EC", quito, "alert-7"),
        ) shouldNotBe AlertaEcEventIdentityV2.Result.Invalid("inicio ausente")
        AlertaEcEventIdentityV2.official(
            AlertaEcEventIdentityV2.OfficialInput("EC", quito, "alert-7", endTimeMillis = 1L),
        ) shouldNotBe AlertaEcEventIdentityV2.Result.Invalid("inicio ausente")
        AlertaEcEventIdentityV2.official(
            AlertaEcEventIdentityV2.OfficialInput("EC", quito, "alert-7", startTimeMillis = 2L, endTimeMillis = 2L),
        ) shouldBe AlertaEcEventIdentityV2.Result.Invalid("fin no posterior al inicio")
    }

    @Test
    fun officialInstructionChangesRevisionAndCosmeticInstructionDoesNot() {
        fun official(instruction: String) = AlertaEcEventIdentityV2.official(
            AlertaEcEventIdentityV2.OfficialInput("EC", quito, "alert-7", instruction = instruction),
        )
        official("Mantenga la calma") shouldBe official("  mantenga   la CALMA ")
        official("Mantenga la calma") shouldNotBe official("Evacúe")
    }

    @Test
    fun v1AliasesPreserveExactLiteralAdapterInputsAndStaySeparate() {
        AlertaEcEventIdentityV2.forecastAliasV1("id/ con espacios", "", Long.MAX_VALUE) shouldBe
            "forecast-id/ con espacios--9223372036854775807"
        AlertaEcEventIdentityV2.forecastAliasV1("formatted", "RAIN", -0L) shouldBe
            "forecast-formatted-RAIN-0"
        AlertaEcEventIdentityV2.officialAliasV1(" alert / 7 ") shouldBe
            "official- alert / 7 "
        AlertaEcEventIdentityV2.forecast(forecast()).let { result ->
            (result as AlertaEcEventIdentityV2.Result.Valid).id.contains("formatted") shouldBe false
        }
    }

    @Test
    fun canonicalizationUsesNfcCaseAndRootLocaleDeterministically() {
        val base = AlertaEcEventIdentityV2.OfficialInput("Fuente", quito, "alert-7")
        val otherSource = base.copy(effectiveSource = "Otra fuente")
        val otherLocation = base.copy(location = galapagos)
        AlertaEcEventIdentityV2.official(base) shouldNotBe AlertaEcEventIdentityV2.official(otherSource)
        AlertaEcEventIdentityV2.official(base) shouldNotBe AlertaEcEventIdentityV2.official(otherLocation)

        val equivalentSources = listOf("  İSTANBUL  ", "i\u0307stanbul", "  i\u0307STANBUL ")
        val expectedId = (AlertaEcEventIdentityV2.forecast(forecast(source = equivalentSources.first()))
            as AlertaEcEventIdentityV2.Result.Valid).id
        equivalentSources.forEach { source ->
            val actualId = (AlertaEcEventIdentityV2.forecast(forecast(source = source))
                as AlertaEcEventIdentityV2.Result.Valid).id
            actualId shouldBe expectedId
        }

        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale("tr", "TR"))
            val turkishLocaleId = (AlertaEcEventIdentityV2.forecast(forecast(source = "  İSTANBUL  "))
                as AlertaEcEventIdentityV2.Result.Valid).id
            turkishLocaleId shouldBe expectedId
        } finally {
            Locale.setDefault(originalLocale)
        }
    }
}
