package org.breezyweather.ui.main.intro

import android.app.Activity
import android.app.Application
import android.os.Bundle
import org.breezyweather.ui.main.MainActivity

/**
 * The intro plays every time the user comes back to ALERTA EC from outside (Home, another app, the TV
 * launcher), not only on a cold start (user request 2026-11-20).
 *
 * Counts the started activities of the app: when the count goes back from 0 to 1 the app returned to the
 * foreground. If that first visible screen is the Home ([MainActivity]) the intro is replayed there; moving
 * between screens inside the app (map, settings) never triggers it, and a return straight into an inner
 * screen does not leave a late intro pending for Home.
 */
object AlertaEcReturnIntro : Application.ActivityLifecycleCallbacks {
    private var started = 0
    private var wasInBackground = false
    private var pending = false

    fun register(application: Application) = application.registerActivityLifecycleCallbacks(this)

    /** True once when Home should replay the intro after a return from the background. */
    fun consume(): Boolean = pending.also { pending = false }

    override fun onActivityStarted(activity: Activity) {
        if (started == 0 && wasInBackground) {
            pending = activity is MainActivity
            wasInBackground = false
        }
        started++
    }

    override fun onActivityStopped(activity: Activity) {
        started = (started - 1).coerceAtLeast(0)
        // A configuration change (rotation) restarts the activity: that is not leaving the app.
        if (started == 0 && !activity.isChangingConfigurations) wasInBackground = true
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
