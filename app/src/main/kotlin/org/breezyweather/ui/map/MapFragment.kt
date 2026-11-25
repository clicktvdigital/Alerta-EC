/*
 * Part of Alerta EC, based on Breezy Weather.
 * Licensed under the GNU Lesser General Public License v3.0.
 */

package org.breezyweather.ui.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.breezyweather.background.location.ActiveLocationController
import org.breezyweather.background.location.LocationHumanFormatter
import org.breezyweather.common.extensions.setSystemBarStyle
import org.breezyweather.ui.main.fragments.MainModuleFragment
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import java.time.Instant

@dagger.hilt.android.AndroidEntryPoint
class MapFragment : MainModuleFragment() {

    @javax.inject.Inject
    lateinit var addressProvider: org.breezyweather.background.location.DetailedLocationProvider

    @javax.inject.Inject
    lateinit var currentLocationStore: org.breezyweather.domain.settings.CurrentLocationStore

    @javax.inject.Inject
    lateinit var nativeLocationService: org.breezyweather.sources.android.AndroidLocationService

    private var nativeLocationRequest: io.reactivex.rxjava3.disposables.Disposable? = null
    private var nativeLocationRequestGeneration = 0L
    private var lastNativeLocationAttemptAt = -30_000L
    private var mapPermissionRequestInFlight = false
    private var mapPermissionDenied = false
    private val mapLocationPermissionRequestCode = 766
    private val mapPermissionPreferencesName = "map_location_permission_state"
    private val mapPermissionDeniedKey = "denied"

    private fun latestMapLocation(): org.breezyweather.background.location.ActiveLocationSnapshot? {
        val activeLocation = ActiveLocationController.getLatest(requireContext())
        val nativeTimestamp = currentLocationStore.measuredAtMillis
        val nativeLocation = if (currentLocationStore.isRecent && nativeTimestamp != null) {
            org.breezyweather.background.location.ActiveLocationSnapshot(
                latitude = currentLocationStore.lastKnownLatitude,
                longitude = currentLocationStore.lastKnownLongitude,
                altitude = currentLocationStore.altitudeMeters,
                accuracy = currentLocationStore.accuracyMeters,
                timestamp = nativeTimestamp,
            )
        } else {
            null
        }

        return when {
            activeLocation == null -> nativeLocation
            nativeLocation == null -> activeLocation
            nativeLocation.timestamp >= activeLocation.timestamp -> nativeLocation
            else -> activeLocation
        }
    }

    private fun ensureMapLocationPermission(
        context: android.content.Context,
        explicitUserAction: Boolean = false,
    ): Boolean {
        if (nativeLocationService.hasPermissions(context)) {
            mapPermissionDenied = false
            context.getSharedPreferences(
                mapPermissionPreferencesName,
                android.content.Context.MODE_PRIVATE
            ).edit().remove(mapPermissionDeniedKey).apply()
            return true
        }
        if (mapPermissionRequestInFlight) {
            gpsInfoLabel?.text = "📍 Solicitando permiso de ubicación..."
            return false
        }
        val permissionPreferences = context.getSharedPreferences(
            mapPermissionPreferencesName,
            android.content.Context.MODE_PRIVATE
        )
        val storedDenial = permissionPreferences.getBoolean(mapPermissionDeniedKey, false)
        if (mapPermissionDenied || storedDenial) {
            if (explicitUserAction && canRetryMapLocationPermission()) {
                mapPermissionDenied = false
                permissionPreferences.edit().remove(mapPermissionDeniedKey).apply()
            } else {
                mapPermissionDenied = true
                gpsInfoLabel?.text = "📍 Permiso de ubicación denegado; actívalo en Ajustes"
                return false
            }
        }
        mapPermissionRequestInFlight = true
        gpsInfoLabel?.text = "📍 Solicitando permiso de ubicación..."
        requestPermissions(
            arrayOf(
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ),
            mapLocationPermissionRequestCode
        )
        return false
    }

    private fun canRetryMapLocationPermission(): Boolean =
        shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_COARSE_LOCATION) ||
            shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_FINE_LOCATION)


    // Marcador de ubicación GPS: coordenadas medidas, no el centro visual del mapa.
    private var alertaGpsMarker: org.maplibre.android.annotations.Marker? = null
    private fun mostrarPuntoGps(pos: org.breezyweather.background.location.ActiveLocationSnapshot) {
        val mapa = currentMap ?: return
        if (!styleReady || pos.latitude !in -90.0..90.0 || pos.longitude !in -180.0..180.0) return
        val punto = LatLng(pos.latitude, pos.longitude)
        alertaGpsMarker?.let {
            it.position = punto
            return
        }
        val escala = resources.displayMetrics.density
        val tamano = (26f * escala).toInt().coerceAtLeast(26)
        val bitmap = android.graphics.Bitmap.createBitmap(
            tamano, tamano, android.graphics.Bitmap.Config.ARGB_8888
        )
        val lienzo = android.graphics.Canvas(bitmap)
        val pincel = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        val mitad = tamano / 2f
        pincel.color = android.graphics.Color.WHITE
        lienzo.drawCircle(mitad, mitad, mitad - escala, pincel)
        pincel.color = android.graphics.Color.rgb(25, 118, 235)
        lienzo.drawCircle(mitad, mitad, mitad - escala * 3f, pincel)
        val icono = org.maplibre.android.annotations.IconFactory.getInstance(requireContext())
            .fromBitmap(bitmap)
        alertaGpsMarker = mapa.addMarker(
            org.maplibre.android.annotations.MarkerOptions()
                .position(punto)
                .title("Mi posición GPS (aproximada)")
                .icon(icono)
        )
    }

    private fun correccionLocalDireccion(latitud: Double, longitud: Double): Pair<String, String>? {
        val datos = requireContext().getSharedPreferences("alerta_ec_direccion_usuario", 0)
        val a = datos.getString("lat", null)?.toDoubleOrNull() ?: return null
        val b = datos.getString("lon", null)?.toDoubleOrNull() ?: return null
        val distancia = FloatArray(1)
        android.location.Location.distanceBetween(a, b, latitud, longitud, distancia)
        if (distancia[0] > 75f) return null
        val postal = datos.getString("postal", "")?.trim().orEmpty()
        val sector = datos.getString("sector", "")?.trim().orEmpty()
        if (!Regex("[0-9]{6}").matches(postal) || sector.isBlank()) return null
        return postal to sector
    }

    private fun editarMiSector() {
        val ubicacion = latestMapLocation() ?: run {
            android.widget.Toast.makeText(requireContext(), "Primero obtiene una posición GPS", 1).show()
            return
        }
        val contexto = requireContext()
        val previa = correccionLocalDireccion(ubicacion.latitude, ubicacion.longitude)
        val caja = android.widget.LinearLayout(contexto).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(28, 14, 28, 8)
        }
        val postal = android.widget.EditText(contexto).apply {
            hint = "Código postal de 6 dígitos"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setSingleLine(true)
            setText(previa?.first.orEmpty())
        }
        val sector = android.widget.EditText(contexto).apply {
            hint = "Sector donde estás realmente"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setSingleLine(true)
            setText(previa?.second.orEmpty())
        }
        caja.addView(postal); caja.addView(sector)
        android.app.AlertDialog.Builder(contexto)
            .setTitle("Corregir mi zona en este punto GPS")
            .setMessage("Información voluntaria. Se mostrará como aportada por ti; " +
                "no sustituye la cartografía oficial ni corrige todos los lugares de Ecuador.")
            .setView(caja)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar") { _, _ ->
                val codigo = postal.text.toString().trim()
                val nombre = sector.text.toString().trim()
                if (!Regex("[0-9]{6}").matches(codigo) || nombre.isBlank()) {
                    android.widget.Toast.makeText(contexto,
                        "Indica 6 dígitos y el sector", 1).show()
                } else {
                    contexto.getSharedPreferences("alerta_ec_direccion_usuario", 0).edit()
                        .putString("lat", ubicacion.latitude.toString())
                        .putString("lon", ubicacion.longitude.toString())
                        .putString("postal", codigo)
                        .putString("sector", nombre)
                        .apply()
                    mostrarCapaZona(centrar = true)
                    addressLabel?.text = "✏️ Datos aportados por ti · GPS cercano\n" +
                        "Sector: $nombre · Código postal: $codigo\n" +
                        "Pulsa para ampliar la dirección"
                    lastAddressPosition = null
                }
            }.show()
    }

    private fun centerMapOn(position: org.breezyweather.background.location.ActiveLocationSnapshot) {
        currentMap?.cameraPosition = CameraPosition.Builder()
            .target(LatLng(position.latitude, position.longitude))
            .zoom(15.0)
            .build()
    }

    private fun requestNativeMapLocation(
        force: Boolean = false,
        explicitUserAction: Boolean = false,
    ): Boolean {
        if (!isAdded || !isResumed) return false
        if (force) {
            nativeLocationRequest?.dispose()
            nativeLocationRequest = null
            nativeLocationRequestGeneration++
            lastNativeLocationAttemptAt = -30_000L
        }
        val context = requireContext()
        if (!ensureMapLocationPermission(context, explicitUserAction)) return false
        if (nativeLocationRequest?.isDisposed == false) return true
        val now = android.os.SystemClock.elapsedRealtime()
        if (lastNativeLocationAttemptAt >= 0L && now - lastNativeLocationAttemptAt < 30_000L) return true
        lastNativeLocationAttemptAt = now
        nativeLocationRequestGeneration++
        val requestGeneration = nativeLocationRequestGeneration
        val requestedView = view
        nativeLocationRequest = nativeLocationService.requestLocation(context)
            .subscribeOn(io.reactivex.rxjava3.schedulers.Schedulers.io())
            .observeOn(io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread())
            .subscribe(
                { point ->
                    if (requestGeneration != nativeLocationRequestGeneration ||
                        view !== requestedView || !isResumed
                    ) return@subscribe
                    if (!point.latitude.isFinite() || point.latitude !in -90.0..90.0 ||
                        !point.longitude.isFinite() || point.longitude !in -180.0..180.0) return@subscribe
                    val measured = point.timestampMillis ?: System.currentTimeMillis()
                    if (measured <= 0L || measured > System.currentTimeMillis() + 10_000L ||
                        System.currentTimeMillis() - measured > 120_000L) return@subscribe
                    currentLocationStore.updateCurrentLocation(
                        longitude = point.longitude, latitude = point.latitude,
                        accuracy = point.accuracy, altitude = point.altitude,
                        timestampMillis = measured, provider = point.provider
                    )
                    if (force) {
                        latestMapLocation()?.let {
                            updateGpsInfo(it)
                            centerMapOn(it)
                        }
                    }
                    gpsHandler.removeCallbacks(gpsUpdater)
                    gpsHandler.post(gpsUpdater)
                },
                { error -> android.util.Log.w("AlertaEcGPS", "GPS puntual no disponible", error) }
            )
        return true
    }

    private var mapView: MapView? = null
    private var satellitePlayer: NoaaSatellitePlayer? = null
    private var addressLabel: android.widget.TextView? = null
    private var gpsInfoLabel: android.widget.TextView? = null
    private var inamhiStatusLabel: android.widget.TextView? = null

    private val gpsHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var lastPosition: Pair<Double, Double>? = null
    private var styleReady = false
    private var styleState = InamhiStyleState()
    private var styleFailureListener: MapView.OnDidFailLoadingMapListener? = null
    private var styleTimeout: Runnable? = null
    private var styleRetry: Runnable? = null
    private var styleMessage = "Cargando mapa base"
    private val inamhiRequests = InamhiRequestState()
    private var inamhiJob: Job? = null
    private var inamhiOverlay: InamhiMapOverlay? = null
    private var currentMap: org.maplibre.android.maps.MapLibreMap? = null
    private var inamhiSelection = InamhiMapPreferences.Selection()
    private val inamhiStates = mutableMapOf<String, InamhiLayerStatus.State>()
    private val inamhiButtons = mutableMapOf<String, android.widget.Switch>()

    // Alerta EC: INAMHI forecast animation (steps through upcoming WMS times), cached capabilities.
    private var inamhiStep = 0
    private var inamhiPlaying = false
    private var inamhiCatalog: Pair<Long, InamhiWmsTimes.Catalog>? = null
    private var inamhiPlayButton: android.widget.Button? = null
    private val inamhiAnimator = object : Runnable {
        override fun run() {
            if (!inamhiPlaying || !isResumed) return
            inamhiStep++
            loadInamhiOverlay()
            gpsHandler.postDelayed(this, INAMHI_FRAME_MS)
        }
    }

    // Overlays closed by Back before leaving the map: satellite panel, then the layer panel.
    private var rootLayout: android.widget.FrameLayout? = null
    private var satellitePanel: View? = null
    private var layersPanel: View? = null
    private var layersButton: View? = null

    private fun setInamhiPlaying(play: Boolean) {
        inamhiPlaying = play
        gpsHandler.removeCallbacks(inamhiAnimator)
        inamhiPlayButton?.text = if (play) "⏸ Pausar pronóstico INAMHI" else "▶ Animar pronóstico INAMHI"
        if (play) {
            gpsHandler.postDelayed(inamhiAnimator, INAMHI_FRAME_MS)
        } else if (inamhiStep != 0) {
            inamhiStep = 0
            loadInamhiOverlay()
        }
    }

    /** Next forecast time for [layer]; while animating, walks the upcoming times one step per frame. */
    private fun pickInamhiTime(layer: String, times: List<Instant>, now: Instant): Instant? {
        if (inamhiStep == 0) return InamhiWmsTimes.selectUpcoming(times, now, InamhiWmsTimes.horizonHoursFor(layer))
        val upcoming = times.filter { !it.isBefore(now) && !it.isAfter(now.plusSeconds(48 * 3600L)) }.sorted()
        return upcoming.getOrNull(inamhiStep % upcoming.size.coerceAtLeast(1))
    }

    private fun openSatellite(mode: Int) {
        val root = rootLayout ?: return
        satellitePlayer?.let {
            it.selectMode(mode)
            return
        }
        val context = root.context
        val panel = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.rgb(20, 30, 42))
            isClickable = true
        }
        val gpsPosition = latestMapLocation()
        val galapagosTime = gpsPosition != null &&
            EcuadorTimeFormatter.isGalapagosCoordinates(gpsPosition.latitude, gpsPosition.longitude)
        val player = NoaaSatellitePlayer(context, galapagosTime)
        satellitePlayer = player
        satellitePanel = panel
        panel.addView(android.widget.Button(context).apply {
            text = "← Volver al mapa"
            setOnClickListener { closeSatellite() }
        })
        panel.addView(player, android.widget.LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(panel, android.widget.FrameLayout.LayoutParams(-1, -1))
        neonAll(panel)
        player.selectMode(mode)
        // Garantiza la primera consulta aunque aún no haya una selección.
        player.resume()
        player.focusImage()
    }

    private fun closeSatellite() {
        satellitePlayer?.release()
        satellitePlayer = null
        satellitePanel?.let { rootLayout?.removeView(it) }
        satellitePanel = null
        layersButton?.requestFocus()
    }

    private fun neonFocus(view: View) {
        view.setOnFocusChangeListener { v, focused ->
            v.foreground = if (focused) {
                android.graphics.drawable.GradientDrawable().apply {
                    setStroke((4f * v.resources.displayMetrics.density).toInt(), android.graphics.Color.rgb(0, 229, 255))
                }
            } else {
                null
            }
        }
    }
    private var lastInamhiAttemptAt = -300_000L

    private var addressRequest: io.reactivex.rxjava3.disposables.Disposable? = null
    private var lastAddressRequestAt = -60_000L
    private var lastAddressPosition: Pair<Double, Double>? = null

    private fun updateGpsInfo(position: org.breezyweather.background.location.ActiveLocationSnapshot) {
        val measuredAt = java.text.DateFormat.getDateTimeInstance(
            java.text.DateFormat.SHORT, java.text.DateFormat.MEDIUM
        ).format(java.util.Date(position.timestamp))
        gpsInfoLabel?.text = listOf(
            "📍 ${LocationHumanFormatter.decimalCoordinates(position.latitude, position.longitude)}",
            "Precisión horizontal: ${LocationHumanFormatter.accuracy(position.accuracy)}",
            "🏔️ ${LocationHumanFormatter.altitude(position.altitude)}",
            "Precisión vertical: ${LocationHumanFormatter.accuracy(position.verticalAccuracyMeters)}",
            "🕒 Medición: $measuredAt"
        ).joinToString("\n")
    }

    private fun resolveCurrentAddress(latitude: Double, longitude: Double) {
        val now = android.os.SystemClock.elapsedRealtime()
        val previous = lastAddressPosition
        if (previous != null) {
            val distance = FloatArray(1)
            android.location.Location.distanceBetween(
                previous.first, previous.second,
                latitude, longitude, distance
            )
            if (distance[0] < 20f &&
                addressLabel?.text?.contains("no disponible") != true &&
                addressLabel?.text?.contains("verificando dirección") != true &&
                addressLabel?.text?.contains("se reintentará") != true
            ) return
        }
        if (lastAddressRequestAt >= 0L && now - lastAddressRequestAt < 60_000L) return
        addressLabel?.text = "📍 Ubicación actualizada · verificando dirección..."
        lastAddressRequestAt = now
        lastAddressPosition = latitude to longitude
        addressRequest?.dispose()
        val requestedView = view
        // defer: a failure while building the request reaches onError instead of crashing the main thread.
        addressRequest = io.reactivex.rxjava3.core.Observable.defer { addressProvider.request(latitude, longitude) }
            .subscribeOn(io.reactivex.rxjava3.schedulers.Schedulers.io())
            .observeOn(io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread())
            .subscribe(
                { address ->
                    val current = lastPosition
                    val distance = FloatArray(1)
                    if (current != null) {
                        android.location.Location.distanceBetween(
                            latitude, longitude, current.first, current.second, distance
                        )
                    }
                    if (view === requestedView && requestedView != null &&
                        lastAddressPosition == (latitude to longitude) &&
                        current != null && distance[0] < 100f) {
                        android.util.Log.d(
                            "AlertaEcGPS",
                            "Dirección obtenida correctamente"
                        )
                        val personalizado = correccionLocalDireccion(latitude, longitude)
                        val addressLines = if (personalizado != null) {
                            listOf("✏️ Sector aportado por ti: ${personalizado.second}",
                                "✏️ Código postal aportado por ti: ${personalizado.first}") +
                                address.completeAddressLines.filterNot {
                                    it.startsWith("Sector:") || it.startsWith("Código postal:")
                                }
                        } else address.completeAddressLines
                        addressLabel?.text = addressLines
                            .takeIf { it.isNotEmpty() }
                            ?.joinToString("\n")
                            ?: "📍 Dirección detallada no disponible para estas coordenadas"
                        if (addressLines.isEmpty()) {
                            lastAddressPosition = null
                        }
                    }
                },
                { error ->
                    if (view === requestedView && requestedView != null &&
                        lastAddressPosition == (latitude to longitude)) {
                        lastAddressPosition = null
                        addressLabel?.text = "📍 Dirección no disponible · se reintentará"
                    }
                    android.util.Log.w(
                        "AlertaEcGPS",
                        "Geocodificación no disponible",
                        error
                    )
                }
            )
    }

    private val gpsUpdater = object : Runnable {
        override fun run() {
            if (view == null || !isResumed) return
            val position = latestMapLocation()
            if (position != null) {
                updateGpsInfo(position)
                mostrarPuntoGps(position)
                satellitePlayer?.updateTimeZone(
                    EcuadorTimeFormatter.isGalapagosCoordinates(position.latitude, position.longitude)
                )
                val coordinates = position.latitude to position.longitude
                if (coordinates != lastPosition) {
                    lastPosition = coordinates
                }
                resolveCurrentAddress(position.latitude, position.longitude)
                requestNativeMapLocation()
            } else {
                if (requestNativeMapLocation()) {
                    gpsInfoLabel?.text = "📍 Esperando una medición GPS válida..."
                }
                addressLabel?.text = "📍 Ubicación GPS aún no disponible"
                lastPosition = null
                lastAddressPosition = null
                addressRequest?.dispose()
                addressRequest = null
                lastAddressRequestAt = -60_000L
            }
            syncInamhiPreferences()
            expireInamhiLayers()
            if (styleReady && InamhiLayers.all.any(inamhiSelection::enabled) &&
                !inamhiRequests.loading &&
                android.os.SystemClock.elapsedRealtime() - lastInamhiAttemptAt >= 300_000L) {
                loadInamhiOverlay()
            }
            gpsHandler.postDelayed(this, 5000L)
        }
    }

    private fun renderInamhiStatus() {
        actualizarLeyenda()
        val now = Instant.now()
        inamhiStatusLabel?.text = buildList {
            if (!styleReady) add(styleMessage)
            add("🇪🇨 Fuente configurada: INAMHI · Pronósticos WRF")
            InamhiLayers.all.forEach { layer ->
                val enabled = inamhiSelection.enabled(layer)
                inamhiButtons[layer]?.apply {
                    text = "${InamhiLayers.title(layer)} · " +
                        if (enabled) "ACTIVA" else "DESACTIVADA"
                    if (isChecked != enabled) isChecked = enabled
                }
                add(InamhiLayerStatus.describe(
                    InamhiLayers.title(layer), enabled,
                    inamhiStates[layer] ?: InamhiLayerStatus.State(), now
                ))
            }
            add("Dominio WRF configurado: continental; no cubre Galápagos. " +
                "La cobertura nacional de ALERTA EC depende de otras fuentes.")
            currentMap?.cameraPosition?.target?.let { center ->
                if (!InamhiLayers.covers(center.latitude, center.longitude)) {
                    add("Centro del mapa fuera del dominio continental configurado para WRF.")
                }
            }
            add("La disponibilidad por punto, unidades y leyenda no están verificadas. " +
                "Una tesela blanca no confirma ausencia de lluvia.")
            add("Fecha WMS de pronóstico, no de emisión ni de observación. " +
                "Pronóstico de modelo, no alerta oficial ni radar.")
        }.joinToString("\n")
    }

    private fun invalidateInamhiRequest() {
        // Invalidar antes de cancelar: el finally anterior no puede liberar una consulta nueva.
        inamhiRequests.invalidate()
        inamhiJob?.cancel()
        inamhiJob = null
    }

    private fun clearInamhiLayer(layer: String) {
        try {
            inamhiOverlay?.clear(layer)
        } catch (error: Exception) {
            inamhiStates[layer] = InamhiLayerStatus.State(InamhiLayerStatus.Phase.ERROR)
            android.util.Log.w("AlertaEcINAMHI", "No se pudo retirar la capa: $layer", error)
        }
    }

    private fun retainInamhiError(layer: String) {
        val time = inamhiOverlay?.forecastTime(layer)
        if (time == null || time.isBefore(Instant.now())) clearInamhiLayer(layer)
        inamhiStates[layer] = InamhiLayerStatus.retaining(
            InamhiLayerStatus.State(forecastTime = time), InamhiLayerStatus.Phase.ERROR, Instant.now()
        )
    }

    private fun syncInamhiPreferences() {
        val selection = InamhiMapPreferences.read(requireContext())
        if (selection == inamhiSelection) return
        inamhiSelection = selection
        invalidateInamhiRequest()
        InamhiLayers.all.forEach { layer ->
            if (!selection.enabled(layer)) {
                inamhiStates.remove(layer)
                clearInamhiLayer(layer)
            }
        }
        renderInamhiStatus()
        loadInamhiOverlay()
    }

    private fun expireInamhiLayers() {
        val now = Instant.now()
        InamhiLayers.all.forEach { layer ->
            val time = inamhiOverlay?.forecastTime(layer) ?: inamhiStates[layer]?.forecastTime
            try {
                if (!inamhiSelection.enabled(layer)) clearInamhiLayer(layer)
                inamhiOverlay?.discardExpired(layer, now)
            } catch (error: Exception) {
                android.util.Log.w("AlertaEcINAMHI", "Retirada pendiente: $layer", error)
            }
            if (time != null && time.isBefore(now)) {
                if (inamhiStates[layer]?.phase !in setOf(
                        InamhiLayerStatus.Phase.EXPIRED, InamhiLayerStatus.Phase.ERROR
                    )) lastInamhiAttemptAt = -300_000L
                inamhiStates[layer] = InamhiLayerStatus.State(InamhiLayerStatus.Phase.EXPIRED)
                clearInamhiLayer(layer)
            }
        }
        renderInamhiStatus()
    }

    private fun loadInamhiOverlay() {
        if (!styleReady || !isAdded || view == null || !isResumed) return
        val overlay = inamhiOverlay ?: return
        val selection = inamhiSelection
        if (InamhiLayers.all.none(selection::enabled)) {
            renderInamhiStatus()
            return
        }
        val token = inamhiRequests.begin() ?: return
        val requestedView = view
        expireInamhiLayers()
        lastInamhiAttemptAt = android.os.SystemClock.elapsedRealtime()
        InamhiLayers.all.filter(selection::enabled).forEach { layer ->
            inamhiStates[layer] = InamhiLayerStatus.retaining(
                inamhiStates[layer], InamhiLayerStatus.Phase.LOADING, Instant.now()
            )
        }
        renderInamhiStatus()
        inamhiJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                val nowMs = android.os.SystemClock.elapsedRealtime()
                val catalog = inamhiCatalog?.takeIf { nowMs - it.first < 10 * 60 * 1000L }?.second
                    ?: InamhiWmsTimes.catalogAsync().also { inamhiCatalog = nowMs to it }
                ensureActive()
                if (!inamhiRequests.isCurrent(token) || view !== requestedView ||
                    inamhiOverlay !== overlay || !isResumed) return@launch
                // Comprobar también cambios externos ocurridos mientras se consultaba la red.
                if (InamhiMapPreferences.read(requireContext()) != selection) {
                    syncInamhiPreferences()
                    return@launch
                }
                val now = Instant.now()
                InamhiLayers.all.forEach { layer ->
                    if (selection.enabled(layer)) {
                        try {
                            check(layer !in catalog.errors) { "Invalid WMS time dimension: $layer" }
                            val time = pickInamhiTime(layer, catalog.times[layer].orEmpty(), now)
                            overlay.apply(layer, time)
                            inamhiStates[layer] = InamhiLayerStatus.State(
                                if (time == null) InamhiLayerStatus.Phase.NO_DATA
                                else InamhiLayerStatus.Phase.REQUESTED,
                                time
                            )
                        } catch (error: Exception) {
                            retainInamhiError(layer)
                            android.util.Log.w("AlertaEcINAMHI", "Error de estilo: $layer", error)
                        }
                    }
                }
                renderInamhiStatus()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (inamhiRequests.isCurrent(token) && view === requestedView) {
                    InamhiLayers.all.filter(selection::enabled).forEach { layer ->
                        retainInamhiError(layer)
                    }
                    renderInamhiStatus()
                }
                android.util.Log.w("AlertaEcINAMHI", "Consulta WMS no disponible", error)
            } finally {
                if (inamhiRequests.isCurrent(token)) inamhiJob = null
                if (inamhiRequests.finish(token) && view === requestedView && isResumed) {
                    loadInamhiOverlay()
                }
            }
        }
    }

    private fun panMap(dx: Float, dy: Float): Boolean {
        val map = currentMap ?: return false
        map.scrollBy(dx, dy, 250L)
        return true
    }

    private var okZoomIn = true

    private fun zoomWithOk(): Boolean {
        val zoom = currentMap?.cameraPosition?.zoom ?: return false
        if (zoom >= 15.0) okZoomIn = false
        if (zoom <= 6.5) okZoomIn = true
        android.widget.Toast.makeText(
            requireContext(),
            if (okZoomIn) "🔍 Acercando (OK)" else "🔍 Alejando (OK)",
            android.widget.Toast.LENGTH_SHORT
        ).show()
        return zoomMap(okZoomIn)
    }

    /** Neon frame on every button / switch of the map when it has D-pad focus (Android TV) or is pressed. */
    private fun neonAll(view: View) {
        if (view is android.widget.CompoundButton || view is android.widget.Button) {
            view.isFocusable = true
            neonFocus(view)
        }
        if (view is ViewGroup) for (i in 0 until view.childCount) neonAll(view.getChildAt(i))
    }

    // Capa zona: the sector the user registered with "Zona", drawn as a 1 km circle with its name.
    private var zonaPolygon: org.maplibre.android.annotations.Polygon? = null
    private var zonaMarker: org.maplibre.android.annotations.Marker? = null

    private fun mostrarCapaZona(centrar: Boolean): Boolean {
        val mapa = currentMap ?: return false
        val prefs = requireContext().getSharedPreferences("alerta_ec_direccion_usuario", 0)
        val lat = prefs.getString("lat", null)?.toDoubleOrNull() ?: return false
        val lon = prefs.getString("lon", null)?.toDoubleOrNull() ?: return false
        val nombre = prefs.getString("sector", null).orEmpty()
        val codigo = prefs.getString("postal", null).orEmpty()
        zonaPolygon?.let { mapa.removePolygon(it) }
        zonaMarker?.let { mapa.removeMarker(it) }
        val radioGrados = 1.0 / 111.0
        val puntos = (0..48).map { i ->
            val a = Math.toRadians(i * 7.5)
            LatLng(lat + radioGrados * Math.sin(a), lon + radioGrados * Math.cos(a) / Math.cos(Math.toRadians(lat)))
        }
        zonaPolygon = mapa.addPolygon(
            org.maplibre.android.annotations.PolygonOptions()
                .addAll(puntos)
                .fillColor(android.graphics.Color.argb(60, 0, 229, 255))
                .strokeColor(android.graphics.Color.rgb(0, 229, 255))
        )
        zonaMarker = mapa.addMarker(
            org.maplibre.android.annotations.MarkerOptions()
                .position(LatLng(lat, lon))
                .title("Mi zona: $nombre")
                .snippet("Código postal $codigo · aportado por ti")
        )
        if (centrar) {
            mapa.cameraPosition = CameraPosition.Builder().target(LatLng(lat, lon)).zoom(13.5).build()
        }
        return true
    }

    private fun accionZona() {
        val contexto = requireContext()
        val tieneZona = contexto.getSharedPreferences("alerta_ec_direccion_usuario", 0).contains("sector")
        if (!tieneZona) {
            editarMiSector()
            return
        }
        android.app.AlertDialog.Builder(contexto)
            .setTitle("Capa zona")
            .setItems(arrayOf("🗺️ Ver mi zona en el mapa", "✏️ Corregir mi zona", "🚫 Ocultar la capa zona")) { _, opcion ->
                when (opcion) {
                    0 -> mostrarCapaZona(centrar = true)
                    1 -> editarMiSector()
                    else -> {
                        zonaPolygon?.let { currentMap?.removePolygon(it) }
                        zonaMarker?.let { currentMap?.removeMarker(it) }
                        zonaPolygon = null
                        zonaMarker = null
                    }
                }
            }.show()
    }

    // Colour legend of the active INAMHI layer, at the left edge.
    private var legendPanel: android.widget.LinearLayout? = null
    private var legendLayer: String? = null

    private fun actualizarLeyenda() {
        val panel = legendPanel ?: return
        val activas = InamhiLayers.all.filter(inamhiSelection::enabled)
        val capa = activas.firstOrNull()
        if (capa == null) {
            panel.visibility = View.GONE
            legendLayer = null
            return
        }
        if (capa == legendLayer && panel.childCount > 0) return
        legendLayer = capa
        viewLifecycleOwner.lifecycleScope.launch {
            val leyenda = InamhiLegend.load(capa) ?: return@launch
            if (legendPanel !== panel || legendLayer != capa) return@launch
            val contexto = panel.context
            val d = resources.displayMetrics.density
            panel.removeAllViews()
            panel.addView(android.widget.TextView(contexto).apply {
                text = leyenda.title + if (leyenda.unit.isNotEmpty()) " (${leyenda.unit})" else ""
                textSize = 11f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.WHITE)
                setPadding(0, 0, 0, (4 * d).toInt())
            })
            // Highest values on top, like a statistical scale bar.
            leyenda.entries.asReversed().forEach { entrada ->
                panel.addView(android.widget.LinearLayout(contexto).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    addView(View(contexto).apply {
                        setBackgroundColor(entrada.color)
                    }, android.widget.LinearLayout.LayoutParams((14 * d).toInt(), (10 * d).toInt()))
                    addView(android.widget.TextView(contexto).apply {
                        text = "≥ " + entrada.value
                        textSize = 10f
                        setTextColor(android.graphics.Color.rgb(225, 232, 242))
                        setPadding((6 * d).toInt(), 0, 0, 0)
                    })
                })
            }
            if (activas.size > 1) {
                panel.addView(android.widget.TextView(contexto).apply {
                    text = "También: " + activas.drop(1).joinToString { InamhiLayers.title(it) }
                    textSize = 9f
                    maxWidth = (120 * d).toInt()
                    setTextColor(android.graphics.Color.rgb(180, 190, 205))
                    setPadding(0, (4 * d).toInt(), 0, 0)
                })
            }
            panel.visibility = View.VISIBLE
        }
    }

    private fun zoomMap(zoomIn: Boolean): Boolean {
        val map = currentMap ?: return false
        map.animateCamera(
            if (zoomIn) org.maplibre.android.camera.CameraUpdateFactory.zoomIn()
            else org.maplibre.android.camera.CameraUpdateFactory.zoomOut()
        )
        return true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val gpsMap = MapView(requireContext()).also { view ->
            mapView = view
            styleReady = false
            lastPosition = null
            view.onCreate(savedInstanceState)
        }
        return android.widget.FrameLayout(requireContext()).apply {
            rootLayout = this
            addView(gpsMap)
            addressLabel = android.widget.TextView(context).apply {
                text = "📍 Obteniendo ubicación y dirección..."
                textSize = 15f
                setTextColor(android.graphics.Color.WHITE)
                setBackgroundColor(android.graphics.Color.rgb(30, 45, 55))
                setPadding(20, 12, 20, 12)
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
                setOnClickListener { maxLines = if (maxLines == 2) 16 else 2 }
            }
            addView(addressLabel, android.widget.FrameLayout.LayoutParams(
                -1, -2, android.view.Gravity.TOP
            ).apply { topMargin = (44f * resources.displayMetrics.density).toInt() })
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
                val margen = insets.getInsets(
                    androidx.core.view.WindowInsetsCompat.Type.statusBars()
                ).top + (8f * resources.displayMetrics.density).toInt()
                (addressLabel?.layoutParams as? android.widget.FrameLayout.LayoutParams)?.let {
                    it.topMargin = margen
                    addressLabel?.layoutParams = it
                }
                insets
            }
            androidx.core.view.ViewCompat.requestApplyInsets(this)
            val root = this
            val mapButtons = android.widget.LinearLayout(context).apply {
                orientation = android.widget.LinearLayout.VERTICAL
            }
            val gpsDetails = android.widget.TextView(context).apply {
                text = "📍 Esperando medición GPS..."
                textSize = 13f
                setTextColor(android.graphics.Color.WHITE)
                setBackgroundColor(android.graphics.Color.rgb(30, 45, 55))
                setPadding(20, 12, 20, 12)
            }
            gpsInfoLabel = gpsDetails
            val gpsScroll = object : android.widget.ScrollView(context) {
                override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                    val maxHeight = (resources.displayMetrics.heightPixels * 0.25f).toInt()
                    val limitedHeight = android.view.View.MeasureSpec.makeMeasureSpec(
                        maxHeight, android.view.View.MeasureSpec.AT_MOST
                    )
                    super.onMeasure(widthMeasureSpec, limitedHeight)
                }
            }.apply {
                isFillViewport = false
                visibility = android.view.View.GONE
                addView(gpsDetails)
            }
            mapButtons.addView(android.widget.Button(context).apply {
                text = "📍 Datos GPS"
                setOnClickListener {
                    gpsScroll.visibility = if (gpsScroll.visibility == android.view.View.VISIBLE) {
                        android.view.View.GONE
                    } else {
                        android.view.View.VISIBLE
                    }
                }
            }, android.widget.LinearLayout.LayoutParams(-1, -2))
            mapButtons.addView(gpsScroll, android.widget.LinearLayout.LayoutParams(-1, -2))
            mapButtons.addView(android.widget.Button(context).apply {
                text = "📍 Mi ubicación"
                setOnClickListener {
                    latestMapLocation()?.let(::centerMapOn)
                    if (isAdded && isResumed) {
                        if (nativeLocationService.hasPermissions(context)) {
                            gpsInfoLabel?.text = "📍 Actualizando ubicación GPS..."
                        }
                        requestNativeMapLocation(force = true, explicitUserAction = true)
                    }
                }
            }, android.widget.LinearLayout.LayoutParams(-1, -2))
            val inamhiDetails = android.widget.TextView(context).apply {
                text = "🇪🇨 INAMHI WRF · Consultando fechas de pronóstico..."
                textSize = 13f
                setTextColor(android.graphics.Color.WHITE)
                setBackgroundColor(android.graphics.Color.rgb(30, 45, 55))
                setPadding(20, 12, 20, 12)
            }
            inamhiStatusLabel = inamhiDetails
            val inamhiScroll = object : android.widget.ScrollView(context) {
                override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                    val maxHeight = (resources.displayMetrics.heightPixels * 0.30f).toInt()
                    val limitedHeight = android.view.View.MeasureSpec.makeMeasureSpec(
                        maxHeight, android.view.View.MeasureSpec.AT_MOST
                    )
                    super.onMeasure(widthMeasureSpec, limitedHeight)
                }
            }.apply {
                isFillViewport = false
                visibility = android.view.View.GONE
                addView(inamhiDetails)
            }
            mapButtons.addView(android.widget.Button(context).apply {
                text = "🇪🇨 Información INAMHI"
                setOnClickListener {
                    inamhiScroll.visibility =
                        if (inamhiScroll.visibility == android.view.View.VISIBLE) {
                            android.view.View.GONE
                        } else {
                            android.view.View.VISIBLE
                        }
                }
            }, android.widget.LinearLayout.LayoutParams(-1, -2))
            mapButtons.addView(inamhiScroll, android.widget.LinearLayout.LayoutParams(-1, -2))
            // CAPAS V4 INTERRUPTORES: botones visibles, seleccionables y accesibles.
            mapButtons.addView(android.widget.TextView(context).apply {
                text = "🗺️ CAPAS METEOROLÓGICAS · INAMHI"
                textSize = 17f
                setTextColor(android.graphics.Color.WHITE)
                setTypeface(null, android.graphics.Typeface.BOLD)
                setBackgroundColor(android.graphics.Color.rgb(26, 49, 71))
                setPadding(20, 14, 20, 14)
            }, android.widget.LinearLayout.LayoutParams(-1, -2))
            InamhiLayers.all.forEach { layer ->
                val toggle = android.widget.Switch(context).apply {
                    text = InamhiLayers.title(layer)
                    textSize = 15f
                    setTextColor(android.graphics.Color.WHITE)
                    setBackgroundColor(android.graphics.Color.rgb(31, 43, 60))
                    setPadding(22, 12, 22, 12)
                    minHeight = (55f * resources.displayMetrics.density).toInt()
                    isChecked = InamhiMapPreferences.read(context).enabled(layer)
                    setOnCheckedChangeListener { _, chosen ->
                        if (InamhiMapPreferences.read(context).enabled(layer) != chosen) {
                            InamhiMapPreferences.setEnabled(context, layer, chosen)
                            syncInamhiPreferences()
                        }
                    }
                    contentDescription = "Capa meteorológica ${InamhiLayers.title(layer)}"
                }
                inamhiButtons[layer] = toggle
                mapButtons.addView(toggle, android.widget.LinearLayout.LayoutParams(-1, -2))
            }
            mapButtons.addView(android.widget.TextView(context).apply {
                text = "Activada = solicitada. La imagen solo está disponible si el servidor INAMHI responde con datos vigentes. Consulta Información INAMHI para detalles."
                textSize = 12f
                setTextColor(android.graphics.Color.rgb(225, 232, 242))
                setPadding(22, 10, 22, 18)
                setBackgroundColor(android.graphics.Color.rgb(31, 43, 60))
            }, android.widget.LinearLayout.LayoutParams(-1, -2))
            mapButtons.addView(android.widget.Button(context).apply {
                text = "▶ Animar pronóstico INAMHI"
                inamhiPlayButton = this
                setOnClickListener { setInamhiPlaying(!inamhiPlaying) }
            }, android.widget.LinearLayout.LayoutParams(-1, -2))
            mapButtons.addView(android.widget.Button(context).apply {
                text = "🛰️ Satélite, lluvia y rayos"
                setOnClickListener { openSatellite(0) }
            }, android.widget.LinearLayout.LayoutParams(-1, -2))
            // Limitar todos los controles juntos para conservar mapa visible con paneles abiertos.
            val controlsScroll = object : android.widget.ScrollView(context) {
                override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                    val available = android.view.View.MeasureSpec.getSize(heightMeasureSpec)
                        .takeIf { it > 0 } ?: resources.displayMetrics.heightPixels
                    super.onMeasure(widthMeasureSpec, android.view.View.MeasureSpec.makeMeasureSpec(
                        (available * 0.45f).toInt(), android.view.View.MeasureSpec.AT_MOST
                    ))
                }
            }.apply { addView(mapButtons); visibility = android.view.View.GONE }
            layersPanel = controlsScroll
            val accesos = android.widget.LinearLayout(context).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                setBackgroundColor(android.graphics.Color.rgb(27, 37, 52))
                addView(android.widget.Button(context).apply {
                    text = "🔵 Mi GPS"
                    setOnClickListener {
                        latestMapLocation()?.let {
                            centerMapOn(it)
                            mostrarPuntoGps(it)
                        }
                        requestNativeMapLocation(force = true, explicitUserAction = true)
                    }
                }, android.widget.LinearLayout.LayoutParams(0, -2, 1f))
                addView(android.widget.Button(context).apply {
                    text = "☰ Capas"
                    layersButton = this
                    setOnClickListener {
                        controlsScroll.visibility = if (controlsScroll.visibility ==
                            android.view.View.VISIBLE) android.view.View.GONE
                        else android.view.View.VISIBLE
                    }
                }, android.widget.LinearLayout.LayoutParams(0, -2, 1f))
                addView(android.widget.Button(context).apply {
                    text = "✏️ Zona"
                    setOnClickListener { accionZona() }
                }, android.widget.LinearLayout.LayoutParams(0, -2, 1f))
            }
            // Quick access to the GOES satellite: clouds and lightning one press away.
            val accesosSatelite = android.widget.LinearLayout(context).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                setBackgroundColor(android.graphics.Color.rgb(27, 37, 52))
                listOf("🛰️ Satélite" to 0, "🌧 Lluvia" to 1, "⚡ Rayos" to 2).forEach { (label, mode) ->
                    addView(android.widget.Button(context).apply {
                        text = label
                        setOnClickListener { openSatellite(mode) }
                    }, android.widget.LinearLayout.LayoutParams(0, -2, 1f))
                }
            }
            // Zoom + / − (touch and remote).
            val zoomColumn = android.widget.LinearLayout(context).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                listOf("+" to true, "−" to false).forEach { (label, zoomIn) ->
                    addView(android.widget.Button(context).apply {
                        text = label
                        textSize = 22f
                        contentDescription = if (zoomIn) "Acercar mapa" else "Alejar mapa"
                        setOnClickListener { zoomMap(zoomIn) }
                    }, android.widget.LinearLayout.LayoutParams(
                        (56f * resources.displayMetrics.density).toInt(), -2
                    ))
                }
            }
            addView(zoomColumn, android.widget.FrameLayout.LayoutParams(
                -2, -2, android.view.Gravity.END or android.view.Gravity.CENTER_VERTICAL
            ))
            val panel = android.widget.LinearLayout(context).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                addView(controlsScroll, android.widget.LinearLayout.LayoutParams(-1, -2))
                addView(accesosSatelite, android.widget.LinearLayout.LayoutParams(-1, -2))
                addView(accesos, android.widget.LinearLayout.LayoutParams(-1, -2))
            }
            addView(panel, android.widget.FrameLayout.LayoutParams(
                -1, -2, android.view.Gravity.BOTTOM
            ))
            val d = resources.displayMetrics.density
            legendPanel = android.widget.LinearLayout(context).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                visibility = android.view.View.GONE
                setPadding((8 * d).toInt(), (8 * d).toInt(), (8 * d).toInt(), (8 * d).toInt())
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = 8 * d
                    setColor(android.graphics.Color.argb(200, 20, 30, 42))
                }
                importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_YES
                contentDescription = "Leyenda de colores de la capa INAMHI activa"
            }
            addView(legendPanel, android.widget.FrameLayout.LayoutParams(
                -2, -2, android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
            ).apply { leftMargin = (6 * d).toInt() })
            // D-pad: neon focus on every control; Up from the satellite row reaches the zoom buttons.
            neonAll(panel)
            neonAll(zoomColumn)
            val zoomOut = zoomColumn.getChildAt(1).apply { id = android.view.View.generateViewId() }
            val zoomIn = zoomColumn.getChildAt(0).apply { id = android.view.View.generateViewId() }
            gpsMap.id = android.view.View.generateViewId()
            zoomIn.nextFocusLeftId = gpsMap.id
            zoomOut.nextFocusLeftId = gpsMap.id
            zoomOut.nextFocusDownId = accesosSatelite.getChildAt(2).apply {
                id = android.view.View.generateViewId()
            }.id
            for (i in 0 until accesosSatelite.childCount) {
                accesosSatelite.getChildAt(i).nextFocusUpId = zoomOut.id
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        inamhiSelection = InamhiMapPreferences.read(requireContext())
        renderInamhiStatus()
        // Back closes the satellite panel, then the layer panel, then leaves the map focus (TV), and only
        // then returns to the home screen: it never jumps out of everything at once.
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    when {
                        satellitePanel != null -> closeSatellite()
                        layersPanel?.visibility == View.VISIBLE -> {
                            layersPanel?.visibility = View.GONE
                            layersButton?.requestFocus()
                        }
                        mapView?.hasFocus() == true && layersButton != null -> layersButton?.requestFocus()
                        else -> {
                            isEnabled = false
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        }
                    }
                }
            }
        )
        // Android TV: the map takes D-pad focus (neon frame); arrows pan, OK / +/- zoom.
        mapView?.let { focusMap ->
            focusMap.isFocusable = true
            neonFocus(focusMap)
            focusMap.setOnKeyListener { _, keyCode, event ->
                if (event.action != android.view.KeyEvent.ACTION_DOWN) return@setOnKeyListener false
                val step = 160f * resources.displayMetrics.density
                when (keyCode) {
                    // scrollBy moves the map content: the view travels the opposite way, as the arrow points.
                    android.view.KeyEvent.KEYCODE_DPAD_LEFT -> panMap(step, 0f)
                    android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> panMap(-step, 0f)
                    android.view.KeyEvent.KEYCODE_DPAD_UP -> panMap(0f, step)
                    android.view.KeyEvent.KEYCODE_DPAD_DOWN -> panMap(0f, -step)
                    // OK zooms in until street level, then back out to the whole country, and so on.
                    android.view.KeyEvent.KEYCODE_DPAD_CENTER, android.view.KeyEvent.KEYCODE_ENTER -> zoomWithOk()
                    android.view.KeyEvent.KEYCODE_PLUS, android.view.KeyEvent.KEYCODE_ZOOM_IN,
                    android.view.KeyEvent.KEYCODE_CHANNEL_UP -> zoomMap(true)
                    android.view.KeyEvent.KEYCODE_MINUS, android.view.KeyEvent.KEYCODE_ZOOM_OUT,
                    android.view.KeyEvent.KEYCODE_CHANNEL_DOWN -> zoomMap(false)
                    else -> false
                }
            }
        }
        val requestedMapView = mapView ?: return
        requestedMapView.getMapAsync { map ->
            if (this.view !== view || mapView !== requestedMapView || !isAdded) return@getMapAsync
            currentMap = map
            if (savedInstanceState == null) {
                val location = latestMapLocation()
                // Ecuador framed directly (not an empty South America): the whole country without GPS,
                // the city around the user with it.
                map.cameraPosition = CameraPosition.Builder()
                    .target(location?.let { LatLng(it.latitude, it.longitude) } ?: LatLng(-1.6, -78.4))
                    .zoom(if (location == null) 6.2 else 11.0)
                    .build()
            }
            loadInamhiBaseStyle()
        }
    }

    private fun clearStyleListeners() {
        styleFailureListener?.let { mapView?.removeOnDidFailLoadingMapListener(it) }
        styleFailureListener = null
        styleTimeout?.let(gpsHandler::removeCallbacks)
        styleTimeout = null
    }

    private fun stopStyleAttempt() {
        styleState.invalidate()
        clearStyleListeners()
        styleRetry?.let(gpsHandler::removeCallbacks)
        styleRetry = null
    }

    private fun loadInamhiBaseStyle() {
        if (styleReady || !isResumed) return
        val requestedView = view ?: return
        val requestedMapView = mapView ?: return
        val map = currentMap ?: return
        val token = styleState.begin() ?: run {
            if (styleState.exhausted) {
                styleMessage = "No se pudo cargar el mapa base; vuelve a abrir el mapa para reintentar"
                renderInamhiStatus()
            }
            return
        }
        styleMessage = "Cargando mapa base (intento ${styleState.attempts}/2)"
        renderInamhiStatus()
        fun failed() {
            if (view !== requestedView || mapView !== requestedMapView || !styleState.complete(token)) return
            clearStyleListeners()
            styleMessage = if (styleState.exhausted) {
                "No se pudo cargar el mapa base; vuelve a abrir el mapa para reintentar"
            } else {
                "Error al cargar el mapa base; reintento pendiente"
            }
            renderInamhiStatus()
            if (!styleState.exhausted && isResumed) {
                styleRetry = Runnable {
                    styleRetry = null
                    loadInamhiBaseStyle()
                }.also { gpsHandler.postDelayed(it, 2_000L) }
            }
        }
        styleFailureListener = MapView.OnDidFailLoadingMapListener { failed() }.also {
            requestedMapView.addOnDidFailLoadingMapListener(it)
        }
        styleTimeout = Runnable { failed() }.also { gpsHandler.postDelayed(it, 20_000L) }
        try {
            map.setStyle("https://tiles.openfreemap.org/styles/liberty") { style ->
                if (view === requestedView && mapView === requestedMapView && isAdded &&
                    styleState.complete(token)) {
                    clearStyleListeners()
                    invalidateInamhiRequest()
                    inamhiOverlay = InamhiMapOverlay(style)
                    inamhiStates.clear()
                    styleReady = true
                    alertaGpsMarker = null
                    latestMapLocation()?.let(::mostrarPuntoGps)
                    zonaPolygon = null
                    zonaMarker = null
                    runCatching { mostrarCapaZona(centrar = false) }
                    lastInamhiAttemptAt = -300_000L
                    renderInamhiStatus()
                    loadInamhiOverlay()
                }
            }
        } catch (error: Exception) {
            failed()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != mapLocationPermissionRequestCode) return

        mapPermissionRequestInFlight = false
        val permissionContext = context ?: return
        if (nativeLocationService.hasPermissions(permissionContext)) {
            mapPermissionDenied = false
            permissionContext.getSharedPreferences(
                mapPermissionPreferencesName,
                android.content.Context.MODE_PRIVATE
            ).edit().remove(mapPermissionDeniedKey).apply()
            lastNativeLocationAttemptAt = -30_000L
            gpsInfoLabel?.text = "📍 Permiso concedido · obteniendo medición GPS..."
            requestNativeMapLocation()
        } else {
            mapPermissionDenied = true
            permissionContext.getSharedPreferences(
                mapPermissionPreferencesName,
                android.content.Context.MODE_PRIVATE
            ).edit().putBoolean(mapPermissionDeniedKey, true).apply()
            gpsInfoLabel?.text = "📍 Permiso de ubicación denegado; actívalo en Ajustes"
        }
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        context?.let { permissionContext ->
            if (nativeLocationService.hasPermissions(permissionContext)) {
                mapPermissionDenied = false
                mapPermissionRequestInFlight = false
                permissionContext.getSharedPreferences(
                    mapPermissionPreferencesName,
                    android.content.Context.MODE_PRIVATE
                ).edit().remove(mapPermissionDeniedKey).apply()
            }
        }
        mapView?.onResume()
        satellitePlayer?.resume()
        loadInamhiBaseStyle()
        syncInamhiPreferences()
        expireInamhiLayers()
        gpsHandler.removeCallbacks(gpsUpdater)
        gpsHandler.post(gpsUpdater)
    }

    override fun onPause() {
        nativeLocationRequestGeneration++
        nativeLocationRequest?.dispose()
        nativeLocationRequest = null
        stopStyleAttempt()
        gpsHandler.removeCallbacks(gpsUpdater)
        addressRequest?.dispose()
        addressRequest = null
        if (addressLabel?.text?.contains("verificando dirección") == true) {
            addressLabel?.text = "📍 Dirección pendiente · se reintentará"
            lastAddressPosition = null
            lastAddressRequestAt = -60_000L
        }
        expireInamhiLayers()
        if (inamhiRequests.loading) {
            invalidateInamhiRequest()
            inamhiStates.replaceAll { _, state ->
                InamhiLayerStatus.retaining(state, InamhiLayerStatus.Phase.REQUESTED, Instant.now())
            }
            lastInamhiAttemptAt = -300_000L
            renderInamhiStatus()
        }
        satellitePlayer?.pause()
        setInamhiPlaying(false)
        mapView?.onPause()
        super.onPause()
    }

    override fun onStop() {
        mapView?.onStop()
        super.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        mapView?.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        nativeLocationRequestGeneration++
        nativeLocationRequest?.dispose()
        nativeLocationRequest = null
        stopStyleAttempt()
        styleState = InamhiStyleState()
        styleMessage = "Cargando mapa base"
        gpsHandler.removeCallbacks(gpsUpdater)
        styleReady = false
        invalidateInamhiRequest()
        inamhiOverlay = null
        currentMap = null
        alertaGpsMarker = null
        inamhiStates.clear()
        inamhiButtons.clear()
        lastInamhiAttemptAt = -300_000L
        lastAddressRequestAt = -60_000L
        addressRequest?.dispose()
        addressRequest = null
        lastAddressPosition = null
        addressLabel = null
        gpsInfoLabel = null
        inamhiStatusLabel = null
        satellitePlayer?.release()
        satellitePlayer = null
        gpsHandler.removeCallbacks(inamhiAnimator)
        inamhiPlaying = false
        inamhiStep = 0
        inamhiPlayButton = null
        rootLayout = null
        legendPanel = null
        legendLayer = null
        zonaPolygon = null
        zonaMarker = null
        satellitePanel = null
        layersPanel = null
        layersButton = null
        val destroyedMapView = mapView
        mapView = null
        destroyedMapView?.onDestroy()
        super.onDestroyView()
    }

    override fun setSystemBarStyle() {
        requireActivity().window.setSystemBarStyle(false)
    }

    companion object {
        private const val INAMHI_FRAME_MS = 3000L
    }
}