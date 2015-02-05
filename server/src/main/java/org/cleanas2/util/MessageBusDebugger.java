package org.cleanas2.util;

import net.engio.mbassy.listener.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Class that can be added to the message bus to show each message that is submitted.  This
 * will run before the other handlers (priority is higher)
 *
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class MessageBusDebugger {

    private static final Log logger = LogFactory.getLog(MessageBusDebugger.class.getSimpleName());

    @Handler(priority = 1, delivery = Invoke.Synchronously)
    public void MessageReceived(Object o) {
        logger.debug("[message] " + o.getClass().getName());
    }
}
