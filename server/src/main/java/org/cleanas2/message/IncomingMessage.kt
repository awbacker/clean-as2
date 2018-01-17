package org.cleanas2.message

import org.apache.http.HttpRequest
import org.apache.http.impl.BHttpConnectionBase
import org.cleanas2.common.ConnectionInfo
import org.cleanas2.service.net.util.NetUtil

import org.cleanas2.util.AS2Util.getOrDefault
import org.cleanas2.util.AS2Util.newCaseInsensitiveMap

/**
 * Creating from an incoming HTTP connection.  This just contains the HTTP headers, some
 * connection information, and the sender & receiver ID.
 *
 *
 * This is used as the base for making other messages, like IncomingAsyncMdn and IncomingFileMessage.
 * It is a generic message holder, so code does not have to use the request object.
 *
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
open class IncomingMessage(connection: BHttpConnectionBase, request: HttpRequest) {

    val requestHeaders: MutableMap<String, String> = newCaseInsensitiveMap()
    val connectionInfo: ConnectionInfo
    val receiverId: String
    val senderId: String

    init {
        this.connectionInfo = ConnectionInfo(connection, request)
        this.requestHeaders.putAll(NetUtil.httpHeadersToMap(request))
        this.senderId = getOrDefault(requestHeaders, "AS2-From", "")
        this.receiverId = getOrDefault(requestHeaders, "AS2-To", "")
    }

}
