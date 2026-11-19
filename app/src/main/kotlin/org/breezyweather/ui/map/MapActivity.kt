package org.breezyweather.ui.map

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import org.breezyweather.ui.main.MainActivity
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
        // Atrás: primero cierra lo que esté abierto dentro del mapa; si no hay nada, vuelve a la
        // pantalla principal de Alerta EC (no al launcher).
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.popBackStackImmediate()) return
                startActivity(
                    Intent(this@MapActivity, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                )
                finish()
            }
        })
    }
}
