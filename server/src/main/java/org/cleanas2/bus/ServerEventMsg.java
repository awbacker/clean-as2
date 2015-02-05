package org.cleanas2.bus;

import org.cleanas2.common.serverEvent.EventLevel;
import org.cleanas2.common.serverEvent.Phase;

public class ServerEventMsg extends MessageBase {

    public final Phase phase;
    public final EventLevel eventLevel;
    public final String message;
    public final Throwable exceptionSource;
    public final String as2MessageId;
    public String fileName;

    public ServerEventMsg(Phase phase, EventLevel eventLevel, String as2MessageId, String message) {
        this(phase, eventLevel, as2MessageId, message, null);
    }

    public ServerEventMsg(Phase phase, EventLevel eventLevel, String as2MessageId, String message, Throwable exceptionSource) {
        this.phase = phase;
        this.eventLevel = eventLevel;
        this.as2MessageId = as2MessageId;
        this.message = message;
        this.exceptionSource = exceptionSource;
    }
}
