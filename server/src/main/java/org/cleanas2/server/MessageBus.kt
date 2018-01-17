package org.cleanas2.server

import net.engio.mbassy.bus.MBassador
import net.engio.mbassy.bus.MessagePublication
import net.engio.mbassy.bus.config.BusConfiguration
import net.engio.mbassy.bus.config.Feature
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.cleanas2.bus.MessageBase

/**
 * Wrapper around the MBassy event bus, our very own singleton.  Use this class to publish
 * events to the bus.
 *
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
object MessageBus {

    private val logger = LogFactory.getLog(MessageBus::class.java.simpleName)
    val bus = mainBus

    // gets the standard bus object
    private val mainBus: MBassador<MessageBase>
        get() = MBassador(
                BusConfiguration()
                        .addFeature(Feature.SyncPubSub.Default())
                        .addFeature(Feature.AsynchronousHandlerInvocation.Default(2, 2))
                        .addFeature(Feature.AsynchronousMessageDispatch.Default().setNumberOfMessageDispatchers(2))
        )

    fun publishAsync(message: MessageBase): MessagePublication {
        return bus.publishAsync(message)
    }

    fun publish(message: MessageBase) {
        bus.publish(message)
    }

    fun subscribe(o: Any) {
        bus.subscribe(o)
    }

}
