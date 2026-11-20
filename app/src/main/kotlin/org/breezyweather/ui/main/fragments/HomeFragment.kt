/*
 * This file is part of Breezy Weather.
 *
 * Breezy Weather is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, version 3 of the License.
 *
 * Breezy Weather is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Breezy Weather. If not, see <https://www.gnu.org/licenses/>.
 */

package org.breezyweather.ui.main.fragments

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.core.graphics.ColorUtils
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.platform.AbstractComposeView
import org.breezyweather.ui.support.TvCardActions
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import breezyweather.domain.location.model.Location
import kotlinx.coroutines.launch
import org.breezyweather.R
import org.breezyweather.common.activities.BreezyActivity
import org.breezyweather.common.activities.livedata.EqualtableLiveData
import org.breezyweather.common.extensions.density
import org.breezyweather.common.extensions.doOnApplyWindowInsets
import org.breezyweather.common.extensions.getBlocksPerRow
import org.breezyweather.common.extensions.getThemeColor
import org.breezyweather.common.extensions.isDarkMode
import org.breezyweather.common.extensions.isMotionReduced
import org.breezyweather.common.extensions.isTabletDevice
import org.breezyweather.common.extensions.setSystemBarStyle
import org.breezyweather.common.options.appearance.BackgroundAnimationMode
import org.breezyweather.databinding.FragmentHomeBinding
import org.breezyweather.domain.location.model.getPlace
import org.breezyweather.domain.settings.SettingsManager
import org.breezyweather.ui.common.widgets.SwipeSwitchLayout
import org.breezyweather.ui.main.MainActivity
import org.breezyweather.ui.main.MainActivityViewModel
import org.breezyweather.ui.main.adapters.main.MainAdapter
import org.breezyweather.ui.main.adapters.main.ViewType
import org.breezyweather.ui.main.layouts.MainLayoutManager
import org.breezyweather.ui.main.utils.MainModuleUtils
import org.breezyweather.ui.theme.ThemeManager
import org.breezyweather.ui.theme.resource.ResourcesProviderFactory
import org.breezyweather.ui.theme.resource.providers.ResourceProvider
import org.breezyweather.ui.theme.weatherView.WeatherView
import org.breezyweather.ui.theme.weatherView.WeatherViewController

class HomeFragment : MainModuleFragment() {

    private lateinit var binding: FragmentHomeBinding

    // Remote-control focus: the card currently outlined, and the edge to select once a scroll settles.
    private var remoteSelection: View? = null
    private var remotePendingEdge = 0
    private var remoteOnToolbar = false

    private var remoteFocusRingCache: android.graphics.drawable.Drawable? = null
    private lateinit var viewModel: MainActivityViewModel
    private lateinit var weatherView: WeatherView

    private var adapter: MainAdapter? = null
    private var scrollListener: OnScrollListener? = null
    private var recyclerViewAnimator: Animator? = null
    private var resourceProvider: ResourceProvider? = null

    private val previewOffset = EqualtableLiveData(0)
    private var callback: Callback? = null
    private var lastCurrentLocation: Location? = null

    interface Callback {
        fun onEditIconClicked()
        fun onManageIconClicked()
        fun onOpenInOtherAppIconClicked()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentHomeBinding.inflate(layoutInflater, container, false)

        initModel()

        // attach weather view.
        weatherView = ThemeManager
            .getInstance(requireContext())
            .weatherThemeDelegate
            .getWeatherView(requireContext())
        (binding.switchLayout.parent.parent.parent as CoordinatorLayout).addView(
            weatherView as View,
            0,
            CoordinatorLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        initView()
        setCallback(requireActivity() as Callback)

        return binding.root
    }

    private fun isBackgroundAnimationEnabled() =
        when (SettingsManager.getInstance(requireContext()).backgroundAnimationMode) {
            BackgroundAnimationMode.SYSTEM -> !requireContext().isMotionReduced
            BackgroundAnimationMode.ENABLED -> true
            BackgroundAnimationMode.DISABLED -> false
        }

    override fun onResume() {
        super.onResume()
        weatherView.setDrawable(!isHidden)
        // Al volver de otra pantalla (p. ej. satélite) el foco puede haberse perdido: se devuelve
        // a la primera fila visible para que la flecha abajo siga en la lista.
        view?.post {
            if (::binding.isInitialized && binding.recyclerView.findFocus() == null) {
                binding.recyclerView.focusFirstVisibleRow()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        weatherView.setDrawable(false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapter = null
        binding.recyclerView.clearOnScrollListeners()
        scrollListener = null
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        weatherView.setDrawable(!hidden)
    }

    override fun setSystemBarStyle() {
        val delegate = ThemeManager
            .getInstance(requireContext())
            .weatherThemeDelegate
        val location = if (::viewModel.isInitialized) viewModel.currentLocation.value?.location else null
        val isLightBackground = location?.let {
            delegate.isLightBackground(
                requireContext(),
                WeatherViewController.getWeatherKind(it),
                WeatherViewController.isDaylight(it)
            )
        } ?: false

        requireActivity().window.setSystemBarStyle(isLightBackground)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateDayNightColors()
        updateViews()
    }

    // init.

    private fun initModel() {
        viewModel = ViewModelProvider(requireActivity())[MainActivityViewModel::class.java]
    }

    @SuppressLint("ClickableViewAccessibility", "NonConstantResourceId", "NotifyDataSetChanged")
    private fun initView() {
        ensureResourceProvider()

        weatherView.setGravitySensorEnabled(
            SettingsManager.getInstance(requireContext()).isGravitySensorEnabled
        )

        weatherView.setDoAnimate(
            isBackgroundAnimationEnabled()
        )

        binding.appBar.doOnApplyWindowInsets { view, insets ->
            view.updatePadding(
                top = insets.top
            )
        }

        binding.toolbar.setNavigationOnClickListener {
            callback?.onManageIconClicked()
        }
        binding.toolbar.inflateMenu(R.menu.activity_main)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit -> callback?.onEditIconClicked()
                R.id.action_open_in_other_app -> callback?.onOpenInOtherAppIconClicked()
                R.id.action_settings -> activity?.let {
                    org.breezyweather.common.utils.helpers.IntentHelper.startSettingsActivity(it)
                }
                R.id.action_emergency_center -> {
                    startActivity(
                        android.content.Intent(
                            requireContext(),
                            org.breezyweather.ui.emergency.EmergencyCenterActivity::class.java
                        )
                    )
                }
            }
            true
        }
        binding.toolbar.menu.findItem(R.id.action_edit).isVisible = false
        binding.toolbar.menu.findItem(R.id.action_open_in_other_app).isVisible = false
        // Needed to get the icon to show the correct color depending on dark mode
        binding.toolbar.overflowIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_more_vert)
        // Title, menu and action icons sit on the weather background: colorSurfaceInverse is almost black in the
        // light theme and looked grey/dim on the TV, so they are drawn white for contrast (2026-11-20).
        binding.toolbar.overflowIcon?.mutate()?.setTint(android.graphics.Color.WHITE)
        binding.toolbar.menu.forEach { it.icon?.mutate()?.setTint(android.graphics.Color.WHITE) }

        // Con mando de TV: desde los botones de la barra superior, la tecla de abajo
        // lleva el foco a la lista de tarjetas.
        binding.recyclerView.isFocusable = true
        binding.recyclerView.descendantFocusability = android.view.ViewGroup.FOCUS_AFTER_DESCENDANTS
        fun pointDownToList(view: android.view.View) {
            view.nextFocusDownId = binding.recyclerView.id
            if (view is android.view.ViewGroup) {
                for (i in 0 until view.childCount) pointDownToList(view.getChildAt(i))
            }
        }
        binding.toolbar.post { pointDownToList(binding.toolbar) }

        binding.switchLayout.setOnSwitchListener(switchListener)
        binding.switchLayout.reset()
        binding.indicator.setSwitchView(binding.switchLayout)
        binding.indicator.setCurrentIndicatorColor(Color.WHITE)
        binding.indicator.setIndicatorColor(
            ColorUtils.setAlphaComponent(Color.WHITE, (0.5 * 255).toInt())
        )
        binding.indicator.doOnApplyWindowInsets { view, insets ->
            view.updatePadding(
                bottom = insets.bottom
            )
        }

        binding.refreshLayout.setOnRefreshListener {
            viewModel.updateWithUpdatingChecking(
                triggeredByUser = true,
                checkPermissions = true
            )
        }

        val listAnimationEnabled = SettingsManager
            .getInstance(requireContext())
            .isCardsFadeInEnabled
        val itemAnimationEnabled = SettingsManager
            .getInstance(requireContext())
            .isElementsAnimationEnabled
        adapter = MainAdapter(
            (requireActivity() as MainActivity),
            binding.recyclerView,
            weatherView,
            null,
            resourceProvider!!,
            listAnimationEnabled,
            itemAnimationEnabled
        ).apply {
            itemTouchHelper.attachToRecyclerView(binding.recyclerView)
        }

        android.util.Log.i("AlertaEcStartup", "home: adapter attached at ${android.os.SystemClock.uptimeMillis()}")
        binding.recyclerView.adapter = adapter
        // Fresh list: forget any remote selection left from a previous visit.
        remoteSelection?.foreground = null
        remoteSelection = null
        remoteOnToolbar = false
        binding.recyclerView.layoutManager = MainLayoutManager(requireContext(), GRID_SPAN_COUNT).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return if (
                        ViewType.isHalfSizeableBlock(binding.recyclerView.adapter!!.getItemViewType(position)) != true
                    ) {
                        // Spreads on the full width
                        GRID_SPAN_COUNT
                    } else {
                        // This takes into account the actual available width (if drawer is open or not)
                        GRID_SPAN_COUNT.div(
                            requireContext().getBlocksPerRow(width.toFloat().div(requireContext().density))
                        )
                    }
                }
            }
        }
        // Con mando: mantener más bloques en memoria y precargar los de abajo para que
        // no se traben al bajar con DPAD_DOWN.
        binding.recyclerView.setItemViewCacheSize(20)
        (binding.recyclerView.layoutManager as? GridLayoutManager)?.isItemPrefetchEnabled = true
        binding.recyclerView.doOnApplyWindowInsets { view, insets ->
            view.updatePadding(bottom = insets.bottom)
        }
        binding.recyclerView.addOnScrollListener(OnScrollListener().also { scrollListener = it })
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState != RecyclerView.SCROLL_STATE_IDLE) return
                // Drop the outline if its card was recycled for another item, then pick the pending edge.
                val selected = remoteSelection
                if (selected != null && selected !in recyclerView.remoteCandidates()) {
                    selected.foreground = null
                    remoteSelection = null
                }
                onRemoteScrollIdle()
            }
        })
        binding.recyclerView.setOnTouchListener(indicatorStateListener)

        if (isAdded && context != null) {
            updatePreviewSubviews()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentLocation.collect {
                    if (it?.location != null) {
                        binding.toolbar.menu.findItem(R.id.action_edit).isVisible = true
                        binding.toolbar.menu.findItem(R.id.action_open_in_other_app).isVisible = true
                        binding.emptyText.visibility = if (it.location.weather != null) View.GONE else View.VISIBLE
                    } else {
                        binding.toolbar.menu.findItem(R.id.action_edit).isVisible = false
                        binding.toolbar.menu.findItem(R.id.action_open_in_other_app).isVisible = false
                        binding.emptyText.visibility = View.GONE
                    }
                    if (it?.location != lastCurrentLocation) {
                        updateViews(it?.location)
                        lastCurrentLocation = it?.location
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loading.collect {
                    setRefreshing(it)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.indicator.collect {
                    binding.switchLayout.isEnabled = it.total > 1

                    if (binding.switchLayout.totalCount != it.total ||
                        binding.switchLayout.position != it.index
                    ) {
                        binding.switchLayout.setData(it.index, it.total)
                        binding.indicator.setSwitchView(binding.switchLayout)
                    }

                    binding.indicator.visibility = if (it.total > 1) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun updateDayNightColors() {
        binding.refreshLayout.setProgressBackgroundColorSchemeColor(
            requireContext().getThemeColor(com.google.android.material.R.attr.colorSurface)
        )
    }

    // control.
    fun updateViews(location: Location? = viewModel.currentLocation.value?.location) {
        ensureResourceProvider()
        updateDarkMode(location)
        updateKeepScreenOn(location)
        updateContentViews(location = location)
        if (isAdded && context != null) {
            updatePreviewSubviews()
        }
    }

    /**
     * With a warning in force, keep the screen on so the TV screensaver (or screen timeout) never hides it.
     * Uses the Alerta EC events (official alerts + strong forecast threats) at WARNING or above, so a
     * light drizzle does not keep the panel on. Cleared as soon as nothing qualifies.
     */
    private fun updateKeepScreenOn(location: Location?) {
        val window = activity?.window ?: return
        val alertActive = location != null &&
            org.breezyweather.alertaec.AlertaEcWeatherAdapter.fromLocation(location).any {
                it.severity >= org.breezyweather.alertaec.AlertaEcSeverity.WARNING
            }
        if (alertActive) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun updateDarkMode(location: Location?) {
        val expectedLightTheme = ThemeManager.isLightTheme(requireContext(), location)

        (activity as BreezyActivity).updateLocalNightMode(expectedLightTheme)
    }

    @SuppressLint("ClickableViewAccessibility", "NotifyDataSetChanged")
    private fun updateContentViews(location: Location?) {
        recyclerViewAnimator?.let {
            it.cancel()
            recyclerViewAnimator = null
        }

        updateDayNightColors()

        binding.switchLayout.reset()

        if (location?.weather == null) {
            adapter!!.setNullWeather()
            adapter!!.notifyDataSetChanged()
            return
        }

        binding.recyclerView.setOnTouchListener(null)

        val listAnimationEnabled = SettingsManager
            .getInstance(requireContext())
            .isCardsFadeInEnabled
        val itemAnimationEnabled = SettingsManager
            .getInstance(requireContext())
            .isElementsAnimationEnabled
        adapter!!.update(
            (requireActivity() as MainActivity),
            binding.recyclerView,
            weatherView,
            location,
            resourceProvider!!,
            listAnimationEnabled,
            itemAnimationEnabled
        )
        adapter!!.notifyDataSetChanged()

        scrollListener!!.postReset(binding.recyclerView)

        // TODO: What's the purpose of this?
        if (!listAnimationEnabled) {
            binding.recyclerView.alpha = 0f
            recyclerViewAnimator = MainModuleUtils.getEnterAnimator(
                binding.recyclerView,
                0
            ).apply {
                startDelay = 150
            }.also { it.start() }
        }
    }

    private fun ensureResourceProvider() {
        val iconProvider = SettingsManager
            .getInstance(requireContext())
            .iconProvider
        if (resourceProvider == null || resourceProvider!!.packageName != iconProvider) {
            resourceProvider = ResourcesProviderFactory.newInstance
        }
    }

    private fun updatePreviewSubviews() {
        val location = viewModel.getValidLocation(previewOffset.value)
        val daylight = WeatherViewController.isDaylight(location)
        val backgroundWeatherKind = if (SettingsManager.getInstance(requireContext()).mainScreenBackgroundEnabled) {
            WeatherViewController.getWeatherKind(location)
        } else {
            WeatherView.WEATHER_KIND_NULL
        }

        // Show "current position" icon:
        // - On the left on mobile because it might not be visible on small displays otherwise
        // - On the right on tablet because on the left it would be confused with the action icon
        binding.toolbar.title =
            (if (location?.isCurrentPosition == true && !requireContext().isTabletDevice) "⊙ " else "") +
            (location?.getPlace(requireContext()) ?: "") +
            (if (location?.isCurrentPosition == true && requireContext().isTabletDevice) " ⊙" else "")

        weatherView.setWeather(backgroundWeatherKind, daylight, requireContext().isDarkMode)
        binding.refreshLayout.setColorSchemeColors(
            ThemeManager
                .getInstance(requireContext())
                .weatherThemeDelegate
                .getThemeColors(
                    requireContext(),
                    WeatherViewController.getWeatherKind(location),
                    daylight
                )[0]
        )
    }

    private fun setRefreshing(isRefreshing: Boolean) {
        binding.refreshLayout.post {
            if (isFragmentViewCreated) {
                binding.refreshLayout.isRefreshing = isRefreshing
                binding.emptyText.visibility = if (isRefreshing) {
                    View.GONE
                } else {
                    viewModel.currentLocation.value?.location.let { loc ->
                        if (loc == null) {
                            View.GONE
                        } else if (loc.weather == null) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                    }
                }
            }
        }
    }

    // interface.

    private fun setCallback(callback: Callback?) {
        this.callback = callback
    }

    // on touch listener.

    @SuppressLint("ClickableViewAccessibility")
    private val indicatorStateListener = View.OnTouchListener { _, event ->
        when (event.action) {
            MotionEvent.ACTION_MOVE ->
                binding.indicator.setDisplayState(true)

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                binding.indicator.setDisplayState(false)
        }
        false
    }

    // on swipe listener (swipe switch layout).

    private val switchListener = object : SwipeSwitchLayout.OnSwitchListener {

        override fun onSwiped(swipeDirection: Int, progress: Float) {
            binding.indicator.setDisplayState(progress != 0f)

            if (progress >= 1) {
                previewOffset.setValue(
                    if (swipeDirection == SwipeSwitchLayout.SWIPE_DIRECTION_LEFT) 1 else -1
                )
            } else {
                previewOffset.setValue(0)
            }
        }

        override fun onSwitched(swipeDirection: Int) {
            binding.indicator.setDisplayState(false)

            viewModel.offsetLocation(
                if (swipeDirection == SwipeSwitchLayout.SWIPE_DIRECTION_LEFT) 1 else -1
            )
            previewOffset.setValue(0)
        }
    }

    /**
     * Remote-control handling for the home list. D-pad keys always stay inside
     * the list (they never fall through to Android's default focus search).
     * Up/Down move between rows, Left/Right move within a row, OK activates the
     * focused card. Returns false only for keys this screen does not use.
     */
    /** Same action as pull-to-refresh, triggered by the remote's yellow key. */
    fun refreshFromRemote(): Boolean {
        if (!isAdded) return false
        viewModel.updateWithUpdatingChecking(triggeredByUser = true, checkPermissions = true)
        return true
    }

    fun handleRemoteKey(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        if (!isAdded || !::binding.isInitialized) return false
        if (event.action != KeyEvent.ACTION_DOWN) return false
        val name = remoteKeyName(keyCode)
        if (name == null) return false
        val before = describeRemoteSelection()
        val handled = when (keyCode) {
            KeyEvent.KEYCODE_DPAD_DOWN -> moveRemoteSelection(0, 1)
            KeyEvent.KEYCODE_DPAD_UP -> moveRemoteSelection(0, -1)
            KeyEvent.KEYCODE_DPAD_RIGHT -> moveRemoteSelection(1, 0)
            KeyEvent.KEYCODE_DPAD_LEFT -> moveRemoteSelection(-1, 0)
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER -> activateRemoteSelection()
            else -> false
        }
        android.util.Log.i(
            "AlertaEcFocus",
            "key=$name handled=$handled from=$before to=${describeRemoteSelection()}"
        )
        return handled
    }

    /**
     * Remote BACK inside Home: first clear the selection and return to the top; only a BACK at the top with
     * nothing selected leaves the app. Avoids being thrown out of the app from inside an hour or block.
     */
    fun handleRemoteBack(): Boolean {
        if (!isAdded || !::binding.isInitialized) return false
        val recycler = binding.recyclerView
        val scrolled = recycler.canRemoteScroll(-1)
        if (remoteSelection == null && !remoteOnToolbar && !scrolled) return false
        remoteSelection?.foreground = null
        remoteSelection = null
        remoteOnToolbar = false
        remotePendingEdge = 0
        if (scrolled) recycler.smoothScrollToPosition(0)
        return true
    }

    /** OK on the focused card: cards with their own actions open a native action list, others are clicked. */
    private fun activateRemoteSelection(): Boolean {
        val target = remoteSelection ?: run {
            android.util.Log.i("AlertaEcFocus", "ok: no selection")
            return false
        }
        if (target is AbstractComposeView) {
            val tag = target.tag as? String
            if (tag == TvCardActions.TAG_DONATIONS) {
                val holder = runCatching { binding.recyclerView.getChildViewHolder(target) }.getOrNull()
                if (holder is org.breezyweather.ui.main.adapters.main.holder.DonationsViewHolder) {
                    holder.toggleFromRemote()
                    return true
                }
            }
            val shown = tag != null && TvCardActions.show(target.context, tag)
            android.util.Log.i("AlertaEcFocus", "ok: card actions tag=$tag shown=$shown")
            return shown
        }
        // A daily cell is focusable but its click listener lives on an inner view: click that one.
        val clickTarget = if (target.hasOnClickListeners()) {
            target
        } else {
            nestedCellOf(target)?.let { (_, cell) -> firstClickableIn(cell) } ?: target
        }
        val clicked = clickTarget.isAttachedToWindow && clickTarget.performClick()
        android.util.Log.i("AlertaEcFocus", "ok: click ${target.javaClass.simpleName} performed=$clicked")
        return clicked
    }

    private fun remoteKeyName(keyCode: Int): String? = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_UP -> "UP"
        KeyEvent.KEYCODE_DPAD_DOWN -> "DOWN"
        KeyEvent.KEYCODE_DPAD_LEFT -> "LEFT"
        KeyEvent.KEYCODE_DPAD_RIGHT -> "RIGHT"
        KeyEvent.KEYCODE_DPAD_CENTER -> "CENTER"
        KeyEvent.KEYCODE_ENTER -> "ENTER"
        KeyEvent.KEYCODE_NUMPAD_ENTER -> "NUMPAD_ENTER"
        else -> null
    }

    /** Readable position of the focused card for AlertaEcFocus logs: index among candidates and its kind. */
    private fun describeRemoteSelection(): String {
        if (remoteOnToolbar) return "toolbar:${remoteSelection?.javaClass?.simpleName}"
        val view = remoteSelection ?: return "none"
        val index = if (::binding.isInitialized) binding.recyclerView.remoteCandidates().indexOf(view) else -1
        val kind = when (view) {
            is AbstractComposeView -> "compose:${view.tag ?: "untagged"}"
            else -> view.javaClass.simpleName
        }
        return "#$index($kind)"
    }

    private fun moveRemoteSelection(dx: Int, dy: Int): Boolean {
        if (remoteOnToolbar) return moveWithinToolbar(dx, dy)
        val recycler = binding.recyclerView
        val candidates = recycler.remoteCandidates()
        if (candidates.isEmpty()) {
            // Nothing clickable on screen (e.g. empty home): only scroll vertically; otherwise let the system move focus.
            return dy != 0 && scrollByRemote(dy)
        }
        val current = remoteSelection?.takeIf { it in candidates }
        if (current == null) {
            selectRemote(if (dy < 0) candidates.last() else candidates.first())
            return true
        }
        val next = nextRemoteCard(candidates, current, dx, dy)
        if (next != null) {
            selectRemote(next)
            if (dy != 0) revealRemote(next)
            return true
        }
        // Left/Right at the visible edge of an hourly/daily list: scroll it one cell and keep going.
        if (dx != 0 && stepNestedList(current, dx)) return true
        if (dx != 0) {
            // Edge of the row. Never hand the key to Android's focus search: it moved the real focus to some other
            // view and the list scrolled towards it while our selection stayed put (the "up and down" bounce).
            // Both edges do nothing: LEFT used to open Places, which pulled the user out of the hourly/daily rows
            // while scrolling them (TV test 2026-11-20). Places stays one press away on the menu button.
            return true
        }
        // Nothing visible above and the list is already at its top: go straight to the top bar (menu, edit,
        // settings, more). If the list can still scroll up, cards may be hidden above (e.g. the first row of
        // blocks when "Visibility" sits at the top edge): scroll first; onRemoteScrollIdle() goes to the bar
        // only if no new card appeared, so the header is never crept through 40 % per press.
        if (dy < 0 && !recycler.canRemoteScroll(-1) && jumpToToolbar()) return true
        // Reached the edge of what is on screen: scroll, then select the new edge.
        remotePendingEdge = dy
        return scrollByRemote(dy).also { moved -> if (!moved) remotePendingEdge = 0 }
    }

    /** The cell of a nested (hourly/daily) list that contains [view], with that list; null outside such lists. */
    private fun nestedCellOf(view: View): Pair<RecyclerView, View>? {
        var cell = view
        var parent = view.parent
        while (parent is View && parent !is RecyclerView) {
            cell = parent
            parent = parent.parent
        }
        val list = parent as? RecyclerView ?: return null
        return if (list === binding.recyclerView) null else list to cell
    }

    /** What OK should act on inside a list cell: the cell itself if clickable, else its clickable child. */
    private fun remoteTargetIn(cell: View): View {
        if (cell.hasOnClickListeners() || cell.isFocusable) return cell
        return firstClickableIn(cell) ?: cell
    }

    /** First view with a click listener in [view]'s subtree (depth-first), or null. */
    private fun firstClickableIn(view: View): View? {
        if (view.hasOnClickListeners()) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) firstClickableIn(view.getChildAt(i))?.let { return it }
        return null
    }

    /** Moves one hour/day left or right past the visible edge, scrolling the list horizontally. */
    private fun stepNestedList(current: View, dx: Int): Boolean {
        val (list, cell) = nestedCellOf(current) ?: return false
        val position = list.getChildAdapterPosition(cell)
        val target = position + dx
        val count = list.adapter?.itemCount ?: 0
        if (position == RecyclerView.NO_POSITION || target !in 0 until count) return false
        list.scrollToPosition(target)
        list.post {
            if (!isAdded) return@post
            list.findViewHolderForAdapterPosition(target)?.itemView?.let { selectRemote(remoteTargetIn(it)) }
        }
        return true
    }

    private fun moveWithinToolbar(dx: Int, dy: Int): Boolean {
        val bar = toolbarCandidates()
        if (bar.isEmpty()) {
            remoteOnToolbar = false
            return false
        }
        val current = remoteSelection?.takeIf { it in bar } ?: bar.first().also { selectRemote(it) }
        if (dx != 0) {
            val next = bar.getOrNull(bar.indexOf(current) + dx) ?: return false
            selectRemote(next)
            return true
        }
        if (dy > 0) {
            // Leave the top bar: clear its outline. With no visible card, let the system move focus.
            remoteOnToolbar = false
            remoteSelection?.foreground = null
            remoteSelection = null
            val first = binding.recyclerView.remoteCandidates().firstOrNull() ?: return false
            selectRemote(first)
        }
        return true
    }

    /** Clickable views in the top bar (navigation icon, edit, more), left to right. */
    private fun toolbarCandidates(): List<View> {
        val out = ArrayList<View>()
        fun walk(v: View) {
            if (!v.isShown) return
            if (v.isClickable || v.hasOnClickListeners()) {
                out.add(v)
                return
            }
            if (v is ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
        }
        walk(binding.toolbar)
        return out.sortedBy { it.screenLeft() }
    }

    private fun nextRemoteCard(candidates: List<View>, current: View, dx: Int, dy: Int): View? {
        val tolerance = (ROW_TOLERANCE_DP * resources.displayMetrics.density).toInt()
        val currentTop = current.screenTop()
        val currentLeft = current.screenLeft()
        if (dx != 0) {
            val row = candidates
                .filter { kotlin.math.abs(it.screenTop() - currentTop) <= tolerance }
                .sortedBy { it.screenLeft() }
            return row.getOrNull(row.indexOf(current) + dx)
        }
        val others = candidates.filter {
            if (dy > 0) it.screenTop() > currentTop + tolerance else it.screenTop() < currentTop - tolerance
        }
        if (others.isEmpty()) return null
        val targetTop = if (dy > 0) others.minOf { it.screenTop() } else others.maxOf { it.screenTop() }
        // Within the next row, pick the card horizontally closest to the current one.
        return others
            .filter { kotlin.math.abs(it.screenTop() - targetTop) <= tolerance }
            .minByOrNull { kotlin.math.abs(it.screenLeft() - currentLeft) }
    }

    /** Scrolls the list by a fraction of its height. False when it cannot move that way. */
    private fun scrollByRemote(direction: Int): Boolean {
        val recycler = binding.recyclerView
        if (!recycler.canRemoteScroll(direction)) return false
        val step = (recycler.height * REMOTE_SCROLL_FRACTION).toInt().coerceAtLeast(1)
        recycler.smoothScrollBy(0, if (direction > 0) step else -step)
        return true
    }

    /**
     * True while the list still has content beyond its visible area in [direction].
     * Reads the real layout instead of canScrollVertically(), whose estimated range is unreliable
     * for this mixed-span grid and reported "end" before the last cards were reachable.
     */
    private fun RecyclerView.canRemoteScroll(direction: Int): Boolean {
        val manager = layoutManager ?: return false
        val count = adapter?.itemCount ?: 0
        if (count == 0) return false
        return if (direction > 0) {
            val last = manager.findViewByPosition(count - 1) ?: return true
            manager.getDecoratedBottom(last) > height - paddingBottom
        } else {
            val first = manager.findViewByPosition(0) ?: return true
            manager.getDecoratedTop(first) < paddingTop
        }
    }

    /** Smoothly scrolls just enough to show the whole selected card (TV-style focus follow). */
    private fun revealRemote(card: View) {
        val recycler = binding.recyclerView
        val margin = (REMOTE_REVEAL_MARGIN_DP * resources.displayMetrics.density).toInt()
        val listTop = recycler.screenTop() + recycler.paddingTop
        val listBottom = recycler.screenTop() + recycler.height - recycler.paddingBottom
        val top = card.screenTop()
        val bottom = top + card.height
        val delta = when {
            // Taller than the viewport: align its top.
            card.height > listBottom - listTop -> top - listTop - margin
            bottom > listBottom -> bottom - listBottom + margin
            top < listTop -> top - listTop - margin
            else -> 0
        }
        if (delta != 0) recycler.smoothScrollBy(0, delta)
    }

    private fun selectRemote(view: View) {
        remoteSelection?.foreground = null
        remoteSelection = view
        view.foreground = remoteFocusRing()
        // Keep the selected hour/day visible in its horizontal list (automatic horizontal scroll).
        nestedCellOf(view)?.let { (list, cell) ->
            list.getChildAdapterPosition(cell).takeIf { it >= 0 }?.let { list.smoothScrollToPosition(it) }
        }
    }

    /**
     * Two-tone ring: dark navy outside, amber inside. The old plain white ring was invisible on the
     * white cards of the light theme; this one keeps strong contrast on light and dark backgrounds.
     */
    private fun remoteFocusRing(): android.graphics.drawable.Drawable {
        return remoteFocusRingCache ?: run {
            val density = resources.displayMetrics.density
            val outer = GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                setStroke((6 * density).toInt(), REMOTE_RING_OUTER)
                cornerRadius = 12 * density
            }
            val inner = GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                setStroke((3 * density).toInt(), REMOTE_RING_INNER)
                cornerRadius = 10 * density
            }
            val inset = (3 * density).toInt()
            android.graphics.drawable.LayerDrawable(arrayOf(outer, inner)).apply {
                setLayerInset(1, inset, inset, inset, inset)
            }
        }.also { remoteFocusRingCache = it }
    }

    /**
     * Outermost clickable views that are at least partly on screen inside the list,
     * sorted top to bottom, then left to right (screen coordinates).
     */
    private fun RecyclerView.remoteCandidates(): List<View> {
        val listTop = screenTop()
        val out = ArrayList<View>()
        val clip = android.graphics.Rect()
        fun walk(v: View) {
            if (!v.isShown) return
            // A nested horizontal list (hourly / daily trend) is never a single target: it used to be picked as one
            // big focusable view, which swallowed DOWN/RIGHT and made OK unable to open one hour or day.
            // Its visible cells are the targets instead, so OK opens that hour/day detail like a tap on a phone.
            if (v is RecyclerView && v !== this@remoteCandidates) {
                for (i in 0 until v.childCount) {
                    val cell = v.getChildAt(i)
                    if (cell.getGlobalVisibleRect(clip) && clip.width() * 2 >= cell.width) walk(cell)
                }
                return
            }
            // A ComposeView is one selectable card: its own buttons are reached through the Compose mode.
            // Cells of a nested horizontal list (hourly / daily trend) have no id but are focusable on TV.
            val nestedCell = v.isFocusable && v.isEnabled && v.parent.let { it is RecyclerView && it !== this@remoteCandidates }
            // Only the Compose cards that have actions (tagged) are selectable; other ComposeViews are not.
            val actionCard = v is AbstractComposeView && v.tag != null
            if (actionCard || v.hasOnClickListeners() || nestedCell || (v.isFocusable && v.isEnabled && v.id != View.NO_ID)) {
                val top = v.screenTop()
                if (top + v.height > listTop && top < listTop + height) out.add(v)
                return // do not descend into clickable children of a card
            }
            if (v is ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
        }
        for (i in 0 until childCount) walk(getChildAt(i))
        return out.sortedWith(compareBy({ it.screenTop() }, { it.screenLeft() }))
    }

    private fun View.screenTop(): Int = IntArray(2).also { getLocationOnScreen(it) }[1]

    private fun View.screenLeft(): Int = IntArray(2).also { getLocationOnScreen(it) }[0]

    /** Selects the first top-bar control and scrolls the list back to its start; false if the bar is empty. */
    private fun jumpToToolbar(): Boolean {
        val bar = toolbarCandidates()
        if (bar.isEmpty()) return false
        val recycler = binding.recyclerView
        if (recycler.canRemoteScroll(-1)) recycler.smoothScrollToPosition(0)
        remotePendingEdge = 0
        remoteOnToolbar = true
        selectRemote(bar.first())
        return true
    }

    /** After a remote scroll settles, select the card at the edge we scrolled towards. */
    private fun onRemoteScrollIdle() {
        val direction = remotePendingEdge
        if (direction == 0) return
        remotePendingEdge = 0
        val candidates = binding.recyclerView.remoteCandidates()
        // Continue from the current card in the scroll direction; never jump back to the opposite edge.
        val current = remoteSelection?.takeIf { it in candidates }
        val next = current?.let { nextRemoteCard(candidates, it, 0, direction) }
        // Scrolled up and still nothing above: a tall card (the daily trend column) can hide what is above it for
        // more than one 40 % step, so keep scrolling within the same press; only at the very top (header only)
        // finish the press on the top bar.
        if (direction < 0 && current != null && next == null) {
            if (binding.recyclerView.canRemoteScroll(-1)) {
                remotePendingEdge = direction
                if (scrollByRemote(direction)) return
                remotePendingEdge = 0
            }
            if (jumpToToolbar()) return
        }
        val edge = next ?: current ?: if (direction > 0) candidates.firstOrNull() else candidates.lastOrNull()
        if (edge != null && edge !== current) selectRemote(edge)
    }

    // on scroll changed listener.

    private inner class OnScrollListener : RecyclerView.OnScrollListener() {

        private var mScrollY = 0

        fun postReset(recyclerView: RecyclerView) {
            recyclerView.post {
                if (!isFragmentViewCreated) {
                    return@post
                }
                mScrollY = 0
                onScrolled(recyclerView, 0, 0)
            }
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            mScrollY = recyclerView.computeVerticalScrollOffset()
            weatherView.onScroll(mScrollY)
            adapter?.onScroll()
        }
    }

    companion object {
        // 60 is 5 * 4 * 3, which allows us to divide from 1, 2, 3, 4 or 5 and have a whole number
        const val GRID_SPAN_COUNT = 60

        /** Fraction of the visible list height moved per remote-control press. */
        private const val REMOTE_SCROLL_FRACTION = 0.4f

        /** Space (dp) kept between a revealed card and the list edge. */
        private const val REMOTE_REVEAL_MARGIN_DP = 16f

        /** Vertical slack (dp) to treat two cards as being on the same row. */
        private const val ROW_TOLERANCE_DP = 24f

        /** Remote focus ring colors: dark navy outside + amber inside (contrast on white and dark cards). */
        private const val REMOTE_RING_OUTER = 0xFF0B1F3A.toInt()
        private const val REMOTE_RING_INNER = 0xFFFFC400.toInt()
    }
}
