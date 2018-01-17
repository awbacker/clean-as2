package org.cleanas2.util

import net.engio.mbassy.listener.*
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * Class that can be added to the message bus to show each message that is submitted.  This
 * will run before the other handlers (priority is higher)
 *
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
class MessageBusDebugger {

    @Handler(priority = 1, delivery = Invoke.Synchronously)
    fun MessageReceived(o: Any) {
        logger.debug("[message] " + o.javaClass.name)
    }

    companion object {

        private val logger = LogFactory.getLog(MessageBusDebugger::class.java.simpleName)
    }
}
