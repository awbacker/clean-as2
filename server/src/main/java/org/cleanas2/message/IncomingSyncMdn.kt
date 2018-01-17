package org.cleanas2.message

import org.apache.http.HttpResponse
import org.cleanas2.service.net.util.NetUtil

import java.nio.file.Path

import org.cleanas2.util.AS2Util.newCaseInsensitiveMap

/**
 * This is used by the AS2FileSender to hold the MDN reply that comes from the client IF the client is
 * sending messages over the same connection.  If the message is an ASYNC mdn, that is handled by the
 * IncomingFileService and IncomingAsyncMdn message type.
 *
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
class IncomingSyncMdn(as2msg: OutgoingFileMessage, response: HttpResponse) : IncomingMdnBase() {

    val messageId: String
    val originalFile: Path
    val originalMic: String?
    var responseHeaders = newCaseInsensitiveMap<String>()

    init {
        messageId = as2msg.loggingText
        originalFile = as2msg.filePath
        originalMic = as2msg.outgoingMic
        responseHeaders = NetUtil.httpHeadersToMap(response)
    }
}
