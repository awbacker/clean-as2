package org.cleanas2.message;

import org.cleanas2.common.MdnAttributes;
import org.cleanas2.common.MdnMode;

import java.util.Map;

import static org.cleanas2.util.AS2Util.newCaseInsensitiveMap;

/**
 * This MDN class is used by the AS2FileReceiver & AsyncMdnSender to SEND Async MDN or regular MDN in
 * RESPONSE/REPLY to a file being received.  If the partner is requesting an ASYNC mdn for the file
 * we are receiving, then the MDN will be sent by the AsyncMdnSender.  If they are requesting a SYNC (standard)
 * mdn, then the FileReceiverService/Handler will send the MDN directly.
 * <p/>
 * The file receiver can send either an Async MDN or a Sync MDN
 * The file receiver can receive either a file or an ASYNC MDN
 *
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class ReplyMdn {

    /**
     * Mdn mode from the incoming message
     */
    public MdnMode mdnMode;
    public String asyncReplyToUrl;

    /**
     * If the reply should be signed or not
     */
    public boolean isSignedReply;
    public String signedReceiptMicAlgorithm;

    public String bodyText;

    public final Map<String, String> responseHeaders = newCaseInsensitiveMap();
    public final MdnAttributes attributes = new MdnAttributes();

    public String partnerId;
    /**
     * The company that received the message (e.g. the receiverId).  We don't technically
     * need to keep track of this, but if we have >1 company on a server in the future we may need it
     */
    public String companyId;
}
