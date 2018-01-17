package org.cleanas2.message

import org.cleanas2.common.MdnAttributes
import org.cleanas2.common.MdnMode

import org.cleanas2.util.AS2Util.newCaseInsensitiveMap

/**
 * This MDN class is used by the AS2FileReceiver & AsyncMdnSender to SEND Async MDN or regular MDN in
 * RESPONSE/REPLY to a file being received.  If the partner is requesting an ASYNC mdn for the file
 * we are receiving, then the MDN will be sent by the AsyncMdnSender.  If they are requesting a SYNC (standard)
 * mdn, then the FileReceiverService/Handler will send the MDN directly.
 *
 *
 * The file receiver can send either an Async MDN or a Sync MDN
 * The file receiver can receive either a file or an ASYNC MDN
 *
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
class ReplyMdn {

    /**
     * Mdn mode from the incoming message
     */
    var mdnMode: MdnMode? = null
    var asyncReplyToUrl: String? = null

    /**
     * If the reply should be signed or not
     */
    var isSignedReply: Boolean = false
    var signedReceiptMicAlgorithm: String = ""

    var bodyText: String = ""

    val responseHeaders = newCaseInsensitiveMap<String>()
    val attributes = MdnAttributes()

    var partnerId: String = ""
    /**
     * The company that received the message (e.g. the receiverId).  We don't technically
     * need to keep track of this, but if we have >1 company on a server in the future we may need it
     */
    var companyId: String = ""
}
