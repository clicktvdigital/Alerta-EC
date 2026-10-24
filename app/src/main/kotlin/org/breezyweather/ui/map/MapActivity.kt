package org.breezyweather.ui.map

import android.os.Bundle
import androidx.fragment.app.commit
import dagger.hilt.android.AndroidEntryPoint
import org.breezyweather.common.activities.BreezyActivity

@AndroidEntryPoint
class MapActivity : BreezyActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
