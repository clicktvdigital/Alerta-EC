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
import org.breezyweather.common.extensions.setSystemBarStyle
import org.breezyweather.ui.main.fragments.MainModuleFragment
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView

@dagger.hilt.android.AndroidEntryPoint
class MapFragment : MainModuleFragment() {

    @javax.inject.Inject
    lateinit var addressProvider: org.breezyweather.background.location.DetailedLocationProvider

    private var mapView: MapView? = null
    private var addressLabel: android.widget.TextView? = null

    private val gpsHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var lastPosition: Pair<Double, Double>? = null
    private var styleReady = false
    private var addressRequest: io.reactivex.rxjava3.disposables.Disposable? = null
    private var lastAddressRequestAt = -60_000L
    private var lastAddressPosition: Pair<Double, Double>? = null

    private fun resolveCurrentAddress(latitude: Double, longitude: Double) {
        val now = android.os.SystemClock.elapsedRealtime()
        val previous = lastAddressPosition
        if (previous != null) {
            val distance = FloatArray(1)
            android.location.Location.distanceBetween(
                previous.first, previous.second,
                latitude, longitude, distance
            )
            if (distance[0] < 20f) return
        }
        addressLabel?.text = "📍 Ubicación actualizada · verificando dirección..."
        if (now - lastAddressRequestAt < 60_000L) return
        lastAddressRequestAt = now
        lastAddressPosition = latitude to longitude
        addressRequest?.dispose()
        addressRequest = addressProvider.request(latitude, longitude)
            .subscribeOn(io.reactivex.rxjava3.schedulers.Schedulers.io())
            .observeOn(io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread())
            .subscribe(
                { address ->
                    if (lastPosition == (latitude to longitude)) {
                        android.util.Log.d(
                            "AlertaEcGPS",
                            "Dirección obtenida correctamente"
                        )
                        addressLabel?.text = listOfNotNull(
                            address.streetLine,
                            address.administrativeLine,
                            address.postalCode,
                            address.country
                        ).filter { it.isNotBlank() }.distinct().joinToString("\n")
                    }
                },
                { error ->
                    lastAddressPosition = null
                    addressLabel?.text = "📍 Dirección no disponible · toca actualizar"
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
            if (view == null) return
            val position = ActiveLocationController.getLatest(requireContext())
            if (styleReady && position != null) {
                val coordinates = position.latitude to position.longitude
                if (coordinates != lastPosition) {
                    lastPosition = coordinates
                    mapView?.getMapAsync { map ->
                        map.cameraPosition = CameraPosition.Builder()
                            .target(LatLng(position.latitude, position.longitude))
                            .zoom(15.0).build()
                    }
                }
                resolveCurrentAddress(position.latitude, position.longitude)
            }
            gpsHandler.postDelayed(this, 5000L)
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

            view.getMapAsync { map ->
                map.setStyle("asset://map/alerta_ec_base.json") {
                    styleReady = true
                    ActiveLocationController.getLatest(requireContext())?.let { location ->
                        map.cameraPosition = CameraPosition.Builder()
                            .target(LatLng(location.latitude, location.longitude))
                            .zoom(15.0)
                            .build()
                    }
                }
            }
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
        }
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
        gpsHandler.removeCallbacks(gpsUpdater)
        gpsHandler.post(gpsUpdater)
    }

    override fun onPause() {
        gpsHandler.removeCallbacks(gpsUpdater)
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
        gpsHandler.removeCallbacks(gpsUpdater)
        styleReady = false
        addressRequest?.dispose()
        addressRequest = null
        lastAddressPosition = null
        addressLabel = null
        mapView?.onDestroy()
        mapView = null
        super.onDestroyView()
    }

    override fun setSystemBarStyle() {
        requireActivity().window.setSystemBarStyle(false)
    }
}
