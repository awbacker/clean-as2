package org.cleanas2.message;

import org.apache.http.HttpResponse;
import org.cleanas2.service.net.util.NetUtil;

import java.nio.file.Path;
import java.util.Map;

import static org.cleanas2.util.AS2Util.newCaseInsensitiveMap;

/**
 * This is used by the AS2FileSender to hold the MDN reply that comes from the client IF the client is
 * sending messages over the same connection.  If the message is an ASYNC mdn, that is handled by the
 * IncomingFileService and IncomingAsyncMdn message type.
 *
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class IncomingSyncMdn extends IncomingMdnBase {

    public final String messageId;
    public final Path originalFile;
    public final String originalMic;
    public Map<String, String> responseHeaders = newCaseInsensitiveMap();

    public IncomingSyncMdn(OutgoingFileMessage as2msg, HttpResponse response) {
        messageId = as2msg.messageId;
        originalFile = as2msg.filePath;
        originalMic = as2msg.outgoingMic;
        responseHeaders = NetUtil.httpHeadersToMap(response);
    }
}
