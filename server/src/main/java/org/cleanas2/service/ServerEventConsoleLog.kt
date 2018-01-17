package org.cleanas2.service

import javax.inject.Inject
import javax.inject.Singleton

import net.engio.mbassy.listener.Handler
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.cleanas2.bus.ServerEventMsg
import org.cleanas2.common.serverEvent.EventLevel

@Singleton
class ServerEventConsoleLog @Inject
constructor() {

    @Handler
    fun handleStatusMessage(msg: ServerEventMsg) {
        if (msg.eventLevel == EventLevel.Info) {
            logger.info(
                    String.format("[%s] %s", msg.phase, msg.message)
            )
        }
        if (msg.eventLevel == EventLevel.Error) {
            if (msg.exceptionSource != null) {
                logger.error(msg.message, msg.exceptionSource)
            }
        }
    }

    companion object {

        private val logger = LogFactory.getLog(ServerEventConsoleLog::class.java.simpleName)
    }

}
