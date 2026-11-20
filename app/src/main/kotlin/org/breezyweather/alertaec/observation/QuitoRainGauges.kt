package org.breezyweather.alertaec.observation

import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.cos
import kotlin.math.hypot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Rain actually measured in the last 2 hours by the EPMAPS rain-gauge network of Quito (CLIMATICA,
 * paramh2o.aguaquito.gob.ec), near the user's point.
 *
 * Why: forecast models (Open-Meteo 10–25 km cells, INAMHI WRF in 3 h steps) showed «Chubascos ligeros» for hours
 * while gauges 1.5–2.6 km from the user measured 0.00 mm (2026-11-20). An observation beats a model for «is it
 * raining now»; the model stays for the hours ahead.
 *
 * Uses the same public form as the CLIMATICA web page (no documented API: may change without notice), at most
 * once per 10 minutes per station. Any failure returns null and the app falls back to the model alone.
 */
object QuitoRainGauges {
    data class Station(val id: Int, val code: String, val name: String, val x: Double, val y: Double)
    data class Reading(val station: Station, val distanceKm: Double, val lastHourMm: Double)

    private const val BASE = "https://paramh2o.aguaquito.gob.ec"
    private const val FORM = "$BASE/telemetria/multiestacion_precipitacion/"
    private const val MAX_DISTANCE_KM = 5.0
    private const val CACHE_MS = 10 * 60 * 1000L
    /** Measurement window shown to the user («en 2 h»). */
    const val WINDOW_HOURS = 2
    private const val WINDOW_MS = WINDOW_HOURS * 60 * 60 * 1000L
    private val cache = HashMap<Int, Pair<Long, Double>>()

    /** Active stations of the form, TMQ coordinates (Transversa de Mercator Quito, WGS84). */
    private val stations = listOf(
        Station(2, "C02", "Rumihurco", 495826.0, 9985646.0),
        Station(4, "C04", "Rumipamba", 496516.0, 9980595.0),
        Station(5, "C05", "Bellavista", 504265.0, 9979906.0),
        Station(6, "C06", "Yaruquí", 521558.0, 9981818.0),
        Station(7, "C07", "San Antonio", 504966.0, 9997964.0),
        Station(10, "C10", "La Mica Presa", 530267.0, 9939549.0),
        Station(11, "C11", "Pita Campamento", 506873.0, 9945113.0),
        Station(13, "C13", "Salve Faccha", 538702.0, 9974481.0),
        Station(91, "C16", "Guaytaloma", 542098.0, 9967076.0),
        Station(74, "C18", "Puengasí", 500770.0, 9973819.0),
        Station(73, "C19", "El Troje", 497373.0, 9963121.0),
        Station(63, "C21", "El Cinto", 492223.0, 9972532.0),
        Station(54, "P03", "Rumihurco Machángara", 497026.0, 9985544.0),
        Station(56, "P08", "Rumipamba Bodegas", 498893.0, 9979987.0),
        Station(57, "P09", "Iñaquito INAMHI", 501372.0, 9980270.0),
        Station(59, "P11", "Antenas", 497327.0, 9981634.0),
        Station(60, "P12", "Toctiuco", 496963.0, 9977180.0),
        Station(61, "P13", "Cumbaya", 507800.0, 9976388.0),
        Station(64, "P16", "Izobamba", 493859.0, 9959517.0),
        Station(65, "P17", "El Tingo", 506337.0, 9967507.0),
        Station(67, "P19", "La Tola (Tumbaco)", 514419.0, 9974348.0),
        Station(70, "P22", "Chillogallo", 490454.0, 9969226.0),
        Station(71, "P23", "Atacazo", 488667.0, 9964786.0),
        Station(75, "P27", "San Francisco", 495580.0, 9977633.0),
        Station(76, "P28", "Cruz Loma", 495771.0, 9979839.0),
        Station(78, "P30", "Santa Rosa", 488572.0, 9971790.0),
        Station(79, "P31", "Pichán", 492032.0, 9987188.0),
        Station(82, "P34", "Papallacta", 539936.0, 9957893.0),
        Station(88, "P41", "Guayllabamba Hacienda", 516940.0, 9990666.0),
        Station(92, "P45", "El Quinche", 523255.0, 9987684.0),
        Station(95, "P52", "Pintag", 513777.0, 9959883.0),
        Station(96, "P53", "Paluguillo", 523710.0, 9970417.0),
        Station(97, "P54", "El Carmen", 515012.0, 9949730.0),
        Station(99, "P56", "Tanque Solanda", 496577.0, 9968833.0),
        Station(111, "P68", "Salve Faccha alto", 536742.0, 9974088.0),
        Station(112, "P70", "CC El Bosque", 500294.0, 9982113.0),
        Station(113, "P71", "Collaloma Medio", 502984.0, 9986436.0),
        Station(114, "P72", "Colinas Alto", 497433.0, 9988624.0),
        Station(659, "P75", "Laguna Los Patos", 544539.0, 9972457.0),
        Station(755, "ATP01PT02", "PT02 (Tung/Cham)", 514573.0, 9934879.0),
        Station(756, "ATP02PT01", "PT03 (Chamilco)", 514933.0, 9938676.0),
        Station(717, "M5021", "Yuracfaccha - Oyacachi", 543329.0, 9979180.0),
        Station(721, "M5025", "La Virgen Papallacta", 533556.0, 9963112.0),
        Station(722, "M5026", "Cotopaxi Control Norte", 506308.0, 9937680.0),
        Station(723, "M5027", "Lomahurco", 482075.0, 9936333.0),
        Station(724, "M5028", "Hcda Prado Miranda", 512165.0, 9946580.0),
        Station(725, "M5029", "El Carmen", 518546.0, 9944551.0),
        Station(726, "M5030", "Hcda. Gordillo", 515892.0, 9953761.0),
        Station(727, "M5031", "Chumillos", 532214.0, 9989522.0),
        Station(729, "M5075", "Itulcachi", 526278.0, 9967916.0),
        Station(730, "M5076", "PNC - Potrerillos", 510881.0, 9931820.0),
        Station(731, "M5077", "REI", 478938.0, 9930422.0),
        Station(732, "M5078", "Pifo", 520357.0, 9973371.0),
        Station(782, "M5124", "Campo Alegre", 512395.0, 9931901.0),
        Station(766, "M5180", "Atacazo", 488678.0, 9964831.0),
        Station(793, "E03", "Belisario", 500476.0, 9979614.0),
        Station(795, "E05", "El Camal", 498107.0, 9972128.0),
        Station(796, "E06", "El Centro", 498404.0, 9975530.0),
        Station(797, "E08", "Tumbaco", 510756.0, 9976231.0),
        Station(798, "E09", "Los Chillos", 504959.0, 9967165.0),
        Station(799, "E10", "Cotocollao", 500006.0, 9987714.0),
        Station(800, "E11", "Carapungo", 505580.0, 9989404.0),
        Station(801, "E12", "Guamaní", 494079.0, 9963023.0),
        Station(802, "E13", "San Antonio", 505776.0, 9999032.0)
    )

    /** Approximate WGS84 → TMQ (central meridian -78.5°, k0 1.0004584, FE 500 000 m, FN 10 000 000 m). */
    private fun toTmq(latitude: Double, longitude: Double): Pair<Double, Double> {
        val k = 1.0004584
        val x = 500_000.0 + (longitude + 78.5) * 111_320.0 * cos(Math.toRadians(latitude)) * k
        val y = 10_000_000.0 + latitude * 110_574.0 * k
        return x to y
    }

    fun nearest(latitude: Double, longitude: Double): Pair<Station, Double>? {
        if (!latitude.isFinite() || !longitude.isFinite()) return null
        val (x, y) = toTmq(latitude, longitude)
        return stations.map { it to hypot(it.x - x, it.y - y) / 1000.0 }
            .minByOrNull { it.second }
            ?.takeIf { it.second <= MAX_DISTANCE_KM }
    }

    /** Rain measured in the last 2 hours at the nearest station within 5 km; null outside Quito or on error. */
    suspend fun lastHour(latitude: Double, longitude: Double): Reading? = withContext(Dispatchers.IO) {
        val (station, distance) = nearest(latitude, longitude) ?: return@withContext null
        val now = System.currentTimeMillis()
        synchronized(cache) { cache[station.id] }?.takeIf { now - it.first < CACHE_MS }?.let {
            return@withContext Reading(station, distance, it.second)
        }
        val mm = runCatching { fetch(station.id, now) }.getOrNull() ?: return@withContext null
        synchronized(cache) { cache[station.id] = now to mm }
        Reading(station, distance, mm)
    }

    private fun fetch(stationId: Int, now: Long): Double? {
        val cookies = CookieManager(null, CookiePolicy.ACCEPT_ALL)
        // 1) Page with the CSRF token (Django) and its cookie.
        val page = open(FORM, cookies)
        val html = page.inputStream.bufferedReader().use { it.readText() }
        storeCookies(page, cookies)
        page.disconnect()
        val token = Regex("name=\"csrfmiddlewaretoken\" value=\"([^\"]+)\"").find(html)?.groupValues?.get(1)
            ?: return null
        // 2) Last 2 hours, Ecuador local time. The form only accepts minutes in steps of 5, and the telemetry
        //    arrives with some delay: a 1 h window often answered «No hay datos para ese período».
        val zone = TimeZone.getTimeZone("America/Guayaquil")
        val step = 5 * 60 * 1000L
        val end = now - now % step
        fun fmt(pattern: String, at: Long) = SimpleDateFormat(pattern, Locale.US).apply { timeZone = zone }
            .format(Date(at))
        val from = end - WINDOW_MS
        val fields = listOf(
            "csrfmiddlewaretoken" to token,
            "estacion" to stationId.toString(),
            "fecha_inicio" to fmt("yyyy-MM-dd", from),
            "hora_inicio" to fmt("HH", from),
            "minuto_inicio" to fmt("mm", from),
            "fecha_fin" to fmt("yyyy-MM-dd", end),
            "hora_fin" to fmt("HH", end),
            "minuto_fin" to fmt("mm", end),
        ).joinToString("&") { (k, v) -> k + "=" + URLEncoder.encode(v, "UTF-8") }
        val post = open(FORM, cookies).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("X-Requested-With", "XMLHttpRequest")
            setRequestProperty("X-CSRFToken", token)
            setRequestProperty("Referer", FORM)
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        }
        post.outputStream.use { it.write(fields.toByteArray()) }
        val body = post.inputStream.bufferedReader().use { it.readText() }
        post.disconnect()
        // {"estacion": ["P03 - Rumihurco Machángara"], "valor": ["0.00"]}
        return Regex("\"valor\"\\s*:\\s*\\[\\s*\"?(-?[0-9.]+)").find(body)?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun open(url: String, cookies: CookieManager): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("User-Agent", "AlertaEC/0.1 (Android; observacion de lluvia)")
            val cookie = cookies.cookieStore.cookies.joinToString("; ") { "${it.name}=${it.value}" }
            if (cookie.isNotEmpty()) setRequestProperty("Cookie", cookie)
        }

    private fun storeCookies(connection: HttpURLConnection, cookies: CookieManager) {
        connection.headerFields["Set-Cookie"]?.forEach { header ->
            runCatching { java.net.HttpCookie.parse(header).forEach { cookies.cookieStore.add(null, it) } }
        }
    }
}
