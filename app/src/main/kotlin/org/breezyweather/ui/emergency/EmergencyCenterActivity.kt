package org.breezyweather.ui.emergency

import android.os.Bundle
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import org.breezyweather.common.activities.BreezyActivity
import org.breezyweather.ui.theme.compose.BreezyWeatherTheme

@AndroidEntryPoint
class EmergencyCenterActivity : BreezyActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BreezyWeatherTheme {
                EmergencyCenterScreen(onBack = { finish() })
            }
        }
    }
}
