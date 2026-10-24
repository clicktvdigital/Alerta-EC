package org.breezyweather.ui.map

import android.os.Bundle
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer
import androidx.fragment.app.commit
import dagger.hilt.android.AndroidEntryPoint
import org.breezyweather.common.activities.BreezyActivity

@AndroidEntryPoint
class MapActivity : BreezyActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inicializar MapLibre antes de crear MapFragment y MapView.
        MapLibre.getInstance(applicationContext, null, WellKnownTileServer.MapLibre)
        val container = android.widget.FrameLayout(this).apply {
            id = org.breezyweather.R.id.fragment
        }
        setContentView(container)
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(container.id, MapFragment())
            }
        }
    }
}
