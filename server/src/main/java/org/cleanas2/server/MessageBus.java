package org.cleanas2.server;

import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.bus.MessagePublication;
import net.engio.mbassy.bus.config.BusConfiguration;
import net.engio.mbassy.bus.config.Feature;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cleanas2.bus.MessageBase;

/**
 * Wrapper around the MBassy event bus, our very own singleton.  Use this class to publish
 * events to the bus.
 *
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class MessageBus {

    private static final Log logger = LogFactory.getLog(MessageBus.class.getSimpleName());
    private static final MBassador<MessageBase> bus = getMainBus();

    // gets the standard bus object
    private static MBassador<MessageBase> getMainBus() {
        return new MBassador<>(
                new BusConfiguration()
                        .addFeature(Feature.SyncPubSub.Default())
                        .addFeature(Feature.AsynchronousHandlerInvocation.Default(2, 2))
                        .addFeature(Feature.AsynchronousMessageDispatch.Default().setNumberOfMessageDispatchers(2))
        );
    }

    public static MessagePublication publishAsync(MessageBase message) {
        return bus.publishAsync(message);
    }

    public static void publish(MessageBase message) {
        bus.publish(message);
    }

    public static void subscribe(Object o) {
        bus.subscribe(o);
    }

    public static MBassador<MessageBase> getBus() {
        return bus;
    }

}
