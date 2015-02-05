package org.cleanas2.message;

import org.apache.http.HttpRequest;
import org.apache.http.impl.BHttpConnectionBase;
import org.cleanas2.common.ConnectionInfo;
import org.cleanas2.service.net.util.NetUtil;

import java.util.Map;

import static org.cleanas2.util.AS2Util.*;

/**
 * Creating from an incoming HTTP connection.  This just contains the HTTP headers, some
 * connection information, and the sender & receiver ID.
 * <p/>
 * This is used as the base for making other messages, like IncomingAsyncMdn and IncomingFileMessage.
 * It is a generic message holder, so code does not have to use the request object.
 *
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class IncomingMessage {

    public final Map<String, String> requestHeaders = newCaseInsensitiveMap();
    public final ConnectionInfo connectionInfo;
    public final String receiverId;
    public final String senderId;

    public IncomingMessage(BHttpConnectionBase connection, HttpRequest request) {
        this.connectionInfo = new ConnectionInfo(connection, request);
        this.requestHeaders.putAll(NetUtil.httpHeadersToMap(request));
        this.senderId = getOrDefault(requestHeaders, "AS2-From", "");
        this.receiverId = getOrDefault(requestHeaders, "AS2-To", "");
    }

}
