package org.cleanas2.bus

import org.cleanas2.common.serverEvent.EventLevel
import org.cleanas2.common.serverEvent.Phase

class ServerEventMsg @JvmOverloads constructor(val phase: Phase, val eventLevel: EventLevel, val as2MessageId: String, val message: String, val exceptionSource: Throwable? = null) : MessageBase() {
    var fileName: String? = null
}
