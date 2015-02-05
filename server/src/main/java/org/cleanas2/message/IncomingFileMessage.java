package org.cleanas2.message;

import org.apache.http.HttpRequest;
import org.apache.http.impl.BHttpConnectionBase;
import org.cleanas2.common.MdnMode;
import org.cleanas2.service.net.util.NetUtil;

/**
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class IncomingFileMessage extends IncomingMessage {
    public final String messageId;
    public MdnMode mdnMode = MdnMode.NONE;
    public String fileName;

    public IncomingFileMessage(BHttpConnectionBase connection, HttpRequest request) {
        super(connection, request);
        messageId = requestHeaders.get("Message-ID");
        mdnMode = NetUtil.getMdnMode(request);
        fileName = messageId;
    }

}
