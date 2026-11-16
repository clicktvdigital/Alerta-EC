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
import androidx.core.graphics.ColorUtils
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
    private var remoteFocusRingCache: GradientDrawable? = null
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
        updateContentViews(location = location)
        if (isAdded && context != null) {
            updatePreviewSubviews()
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

    fun handleRemoteKey(keyCode: Int): Boolean {
        android.util.Log.i("AlertaEcKeys", "key=$keyCode onToolbar=$remoteOnToolbar selected=${remoteSelection != null}")
        if (!isAdded || !::binding.isInitialized) return false
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_DOWN -> moveRemoteSelection(0, 1)
            KeyEvent.KEYCODE_DPAD_UP -> moveRemoteSelection(0, -1)
            KeyEvent.KEYCODE_DPAD_RIGHT -> moveRemoteSelection(1, 0)
            KeyEvent.KEYCODE_DPAD_LEFT -> moveRemoteSelection(-1, 0)
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                val target = remoteSelection ?: return false
                target.isAttachedToWindow && target.performClick()
            }
            else -> false
        }
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
            return true
        }
        // Left/Right at the edge of the row are not consumed, so the side menu and other focus targets still work.
        if (dx != 0) return false
        // Top row and the list cannot scroll up: move focus to the top bar (menu, edit, more).
        if (dy < 0 && !recycler.canScrollVertically(-1)) {
            val bar = toolbarCandidates()
            if (bar.isNotEmpty()) {
                remoteOnToolbar = true
                selectRemote(bar.first())
                return true
            }
        }
        // Reached the edge of what is on screen: scroll, then select the new edge.
        remotePendingEdge = dy
        return scrollByRemote(dy).also { moved -> if (!moved) remotePendingEdge = 0 }
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
        val canMove = if (direction > 0) recycler.canScrollVertically(1) else recycler.canScrollVertically(-1)
        if (!canMove) return false
        val step = (recycler.height * REMOTE_SCROLL_FRACTION).toInt().coerceAtLeast(1)
        recycler.smoothScrollBy(0, if (direction > 0) step else -step)
        return true
    }

    private fun selectRemote(view: View) {
        remoteSelection?.foreground = null
        remoteSelection = view
        view.foreground = remoteFocusRing()
    }

    private fun remoteFocusRing(): GradientDrawable {
        return remoteFocusRingCache ?: GradientDrawable().apply {
            val density = resources.displayMetrics.density
            setColor(Color.TRANSPARENT)
            setStroke((3 * density).toInt(), Color.WHITE)
            cornerRadius = 12 * density
        }.also { remoteFocusRingCache = it }
    }

    /**
     * Outermost clickable views that are at least partly on screen inside the list,
     * sorted top to bottom, then left to right (screen coordinates).
     */
    private fun RecyclerView.remoteCandidates(): List<View> {
        val listTop = screenTop()
        val out = ArrayList<View>()
        fun walk(v: View) {
            if (!v.isShown) return
            if (v.hasOnClickListeners() || (v.isFocusable && v.isEnabled && v.id != View.NO_ID)) {
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

    /** After a remote scroll settles, select the card at the edge we scrolled towards. */
    private fun onRemoteScrollIdle() {
        val direction = remotePendingEdge
        if (direction == 0) return
        remotePendingEdge = 0
        val candidates = binding.recyclerView.remoteCandidates()
        val edge = if (direction > 0) candidates.firstOrNull() else candidates.lastOrNull()
        if (edge != null) selectRemote(edge)
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

        /** Vertical slack (dp) to treat two cards as being on the same row. */
        private const val ROW_TOLERANCE_DP = 24f
    }
}
