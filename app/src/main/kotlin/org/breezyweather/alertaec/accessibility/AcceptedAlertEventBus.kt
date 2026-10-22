package org.breezyweather.alertaec.accessibility

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Ephemeral process transport with best-effort delivery: no replay or persistence.
 * An offer without an active collector is dropped and cannot become backlog for a future subscriber.
 * The one-slot buffer may discard the previous event when a collector is slower than the producer.
 * [tryEmit] is non-suspending and true means only that the bus accepted the offer; it does not mean
 * that any collector received or reproduced it. SharedFlow broadcasts to every active collector,
 * so this does not provide exactly-once delivery.
 */
class AcceptedAlertEventBus {
    private val mutableEvents = MutableSharedFlow<AcceptedAlertEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<AcceptedAlertEvent> = mutableEvents

    /** Best-effort offer: true means accepted by the bus, not necessarily received or reproduced. */
    fun tryEmit(event: AcceptedAlertEvent): Boolean = mutableEvents.tryEmit(event)
}
