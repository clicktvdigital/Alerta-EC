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
    private val inamhiButtons = mutableMapOf<String, android.widget.Button>()
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
        addressRequest = addressProvider.request(latitude, longitude)
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
                        val addressLines = address.completeAddressLines
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
        val now = Instant.now()
        inamhiStatusLabel?.text = buildList {
            if (!styleReady) add(styleMessage)
            add("🇪🇨 Fuente configurada: INAMHI · Pronósticos WRF")
            InamhiLayers.all.forEach { layer ->
                val enabled = inamhiSelection.enabled(layer)
                inamhiButtons[layer]?.text = "${InamhiLayers.title(layer)} INAMHI: " +
                    if (enabled) "activada" else "desactivada"
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
                val catalog = InamhiWmsTimes.catalogAsync()
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
                            val time = InamhiWmsTimes.selectUpcoming(
                                catalog.times[layer].orEmpty(), now, InamhiWmsTimes.horizonHoursFor(layer)
                            )
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
            addView(gpsMap)
            addressLabel = android.widget.TextView(context).apply {
                text = "📍 Obteniendo ubicación y dirección..."
                textSize = 15f
                setTextColor(android.graphics.Color.WHITE)
                setBackgroundColor(android.graphics.Color.rgb(30, 45, 55))
                setPadding(20, 20, 20, 20)
            }
            addView(addressLabel, android.widget.FrameLayout.LayoutParams(
                -1, -2, android.view.Gravity.TOP
            ))
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
            InamhiLayers.all.forEach { layer ->
                val button = android.widget.Button(context).apply {
                    setOnClickListener {
                        val selection = InamhiMapPreferences.read(context)
                        InamhiMapPreferences.setEnabled(context, layer, !selection.enabled(layer))
                        syncInamhiPreferences()
                    }
                }
                inamhiButtons[layer] = button
                mapButtons.addView(button, android.widget.LinearLayout.LayoutParams(-1, -2))
            }
            mapButtons.addView(android.widget.Button(context).apply {
                text = "🛰️ Satélite y rayos"
                setOnClickListener {
                    if (satellitePlayer != null) return@setOnClickListener
                    val panel = android.widget.LinearLayout(context).apply {
                        orientation = android.widget.LinearLayout.VERTICAL
                        setBackgroundColor(android.graphics.Color.rgb(20, 30, 42))
                    }
                    val gpsPosition = latestMapLocation()
                    val galapagosTime = gpsPosition != null &&
                        EcuadorTimeFormatter.isGalapagosCoordinates(gpsPosition.latitude, gpsPosition.longitude)
                    val player = NoaaSatellitePlayer(context, galapagosTime)
                    satellitePlayer = player
                    panel.addView(android.widget.Button(context).apply {
                        text = "← Volver al mapa"
                        setOnClickListener {
                            player.release()
                            satellitePlayer = null
                            root.removeView(panel)
                        }
                    })
                    panel.addView(player, android.widget.LinearLayout.LayoutParams(-1, 0, 1f))
                    root.addView(panel, android.widget.FrameLayout.LayoutParams(-1, -1))
                }
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
            }.apply { addView(mapButtons) }
            addView(controlsScroll, android.widget.FrameLayout.LayoutParams(
                -1, -2, android.view.Gravity.BOTTOM
            ))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        inamhiSelection = InamhiMapPreferences.read(requireContext())
        renderInamhiStatus()
        val requestedMapView = mapView ?: return
        requestedMapView.getMapAsync { map ->
            if (this.view !== view || mapView !== requestedMapView || !isAdded) return@getMapAsync
            currentMap = map
            if (savedInstanceState == null) {
                val location = latestMapLocation()
                map.cameraPosition = CameraPosition.Builder()
                    .target(location?.let { LatLng(it.latitude, it.longitude) } ?: LatLng(-1.5, -78.5))
                    .zoom(if (location == null) 5.0 else 15.0)
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
        val destroyedMapView = mapView
        mapView = null
        destroyedMapView?.onDestroy()
        super.onDestroyView()
    }

    override fun setSystemBarStyle() {
        requireActivity().window.setSystemBarStyle(false)
    }
}
