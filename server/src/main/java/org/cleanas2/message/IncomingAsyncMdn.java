package org.cleanas2.message;

import org.apache.commons.lang3.StringUtils;
import org.boon.core.reflection.BeanUtils;
import org.cleanas2.common.ConnectionInfo;

import java.util.Map;

import static org.cleanas2.util.AS2Util.newCaseInsensitiveMap;

/**
 * This is used by the AsyncMdnProcessor ONLY (via the AsyncMdnReceiver module and AS2FileReceiver) to
 * handle an MDN that is returned to us ASYNC.  Perhaps this can be combined with another MDN at some
 * time, but for now, all three MDN use cases (incoming async, incoming sync, rely) are modeled separately.
 *
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class IncomingAsyncMdn extends IncomingMdnBase {

    public final String senderId;
    public final String receiverId;
    public final ConnectionInfo connectionInfo;
    public final Map<String, String> requestHeaders = newCaseInsensitiveMap();

    public IncomingAsyncMdn(IncomingMessage msg) {
        this.requestHeaders.putAll(msg.requestHeaders);
        this.connectionInfo = BeanUtils.copy(msg.connectionInfo);
        this.senderId = msg.senderId;
        this.receiverId = msg.receiverId;
    }

    public String getLoggingText() {
        if (StringUtils.isBlank(attributes.originalMessageId)) return "<no message-id found in attributes>";
        return attributes.originalMessageId;
    }
}
