package org.cleanas2.message

import org.apache.http.HttpRequest
import org.apache.http.impl.BHttpConnectionBase
import org.cleanas2.common.MdnMode
import org.cleanas2.service.net.util.NetUtil

/**
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
class IncomingFileMessage(connection: BHttpConnectionBase, request: HttpRequest) : IncomingMessage(connection, request) {
    val messageId: String = requestHeaders["Message-ID"]!!
    var mdnMode = MdnMode.NONE
    var fileName: String

    init {
        mdnMode = NetUtil.getMdnMode(request)
        fileName = messageId
    }

}
