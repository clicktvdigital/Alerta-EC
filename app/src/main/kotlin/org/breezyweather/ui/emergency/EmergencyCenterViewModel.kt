package org.breezyweather.ui.emergency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.breezyweather.alertaec.info.OfficialInfoCachedItem
import org.breezyweather.alertaec.info.OfficialInfoSelectionReadState
import org.breezyweather.alertaec.info.OfficialInfoSelector
import javax.inject.Inject
import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class EmergencyCenterReadDispatcher

/** Provides the offline emergency-center state from the app-private official-info cache. */
@HiltViewModel
class EmergencyCenterViewModel @Inject constructor(
    private val selector: OfficialInfoSelector,
    private val clock: EmergencyCenterClock,
    @EmergencyCenterReadDispatcher private val readDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _uiState = MutableStateFlow(EmergencyCenterUiState())
    val uiState: StateFlow<EmergencyCenterUiState> = _uiState.asStateFlow()
    val state: StateFlow<EmergencyCenterUiState> = uiState

    private var reloadJob: Job? = null
    private var reloadGeneration = 0L

    init {
        reload()
    }

    /** Reloads only the local cache; this does not perform network or location work. */
    fun reload() {
        reloadJob?.cancel()
        val generation = ++reloadGeneration
        _uiState.value = _uiState.value.copy(isLoading = true)
        reloadJob = viewModelScope.launch {
            val selection = try {
                withContext(readDispatcher) {
                    selector.select(clock.nowMillis())
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                null
            }
            currentCoroutineContext().ensureActive()
            if (generation != reloadGeneration) return@launch
            _uiState.value = selection?.let { result ->
                EmergencyCenterUiState(
                    current = result.current,
                    history = result.history,
                    readState = result.readState,
                    isLoading = false,
                )
            } ?: EmergencyCenterUiState(
                readState = OfficialInfoSelectionReadState.READ_ERROR,
                isLoading = false,
            )
        }
    }
}

fun interface EmergencyCenterClock {
    fun nowMillis(): Long
}

data class EmergencyCenterUiState(
    val current: List<OfficialInfoCachedItem> = emptyList(),
    val history: List<OfficialInfoCachedItem> = emptyList(),
    val readState: OfficialInfoSelectionReadState = OfficialInfoSelectionReadState.EMPTY,
    val isLoading: Boolean = true,
)
