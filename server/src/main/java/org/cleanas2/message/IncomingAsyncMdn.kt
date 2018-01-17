package org.cleanas2.message

import org.apache.commons.lang3.StringUtils
import org.boon.core.reflection.BeanUtils
import org.cleanas2.common.ConnectionInfo

import org.cleanas2.util.AS2Util.newCaseInsensitiveMap

/**
 * This is used by the AsyncMdnProcessor ONLY (via the AsyncMdnReceiver module and AS2FileReceiver) to
 * handle an MDN that is returned to us ASYNC.  Perhaps this can be combined with another MDN at some
 * time, but for now, all three MDN use cases (incoming async, incoming sync, rely) are modeled separately.
 *
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
class IncomingAsyncMdn(msg: IncomingMessage) : IncomingMdnBase() {

    val senderId: String
    val receiverId: String
    val connectionInfo: ConnectionInfo
    val requestHeaders: MutableMap<String, String> = newCaseInsensitiveMap()

    val loggingText: String
        get() = if (StringUtils.isBlank(attributes.originalMessageId)) "<no message-id found in attributes>" else attributes.originalMessageId

    init {
        this.requestHeaders.putAll(msg.requestHeaders)
        this.connectionInfo = BeanUtils.copy(msg.connectionInfo)
        this.senderId = msg.senderId
        this.receiverId = msg.receiverId
    }
}
