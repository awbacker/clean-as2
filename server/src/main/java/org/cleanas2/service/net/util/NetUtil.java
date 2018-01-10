package org.cleanas2.service.net.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.*;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.apache.http.message.BasicHeader;
import org.cleanas2.common.MdnMode;

import javax.mail.internet.InternetHeaders;
import java.net.Socket;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.http.HttpStatus.*;
import static org.boon.Lists.list;
import static org.cleanas2.util.AS2Util.getOrDefault;
import static org.cleanas2.util.AS2Util.newCaseInsensitiveMap;

/**
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class NetUtil {

    private static final Log logger = LogFactory.getLog(NetUtil.class.getSimpleName());

    private static final List<Integer> validResponseCodes = list(
            SC_OK, SC_CREATED, SC_ACCEPTED, SC_PARTIAL_CONTENT, SC_NO_CONTENT
    );

    public static Boolean isPost(HttpRequest request) {
        return "POST".equals(request.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH));
    }


    public static boolean isInvalidResponseCode(HttpResponse response) {
        return !validResponseCodes.contains(response.getStatusLine().getStatusCode());
    }

    /**
     * Get debugging info for logging
     */
    private static String getRemoteInfo(DefaultBHttpServerConnection c) {
        return String.format("%s:%d", c.getRemoteAddress(), c.getRemotePort());
    }

    /**
     * Get debugging info for logging
     */
    private static String getLocalInfo(DefaultBHttpServerConnection c) {
        return String.format("%s:%d", c.getLocalAddress(), c.getLocalPort());
    }

    /**
     * Get debugging info for logging
     */
    public static String getClientInfo(Socket s) {
        return String.format("%s:%s", s.getInetAddress().getHostAddress(), Integer.toString(s.getPort()));
    }

    /**
     * Get debugging info for logging
     */
    public static String getServerInfo(Socket s) {
        return String.format("%s:%s", s.getLocalAddress().getHostAddress(), Integer.toString(s.getLocalPort()));
    }

    public static String getEndpointInfo(DefaultBHttpServerConnection conn) {
        return getRemoteInfo(conn) + " ==> " + getLocalInfo(conn);
    }

    public static Map<String, String> httpHeadersToMap(HttpMessage http) {
        // make lookup by key be case insensitive (but a little slower).  N is wo low in this case, it doesn't matter
        Map<String, String> strMap = newCaseInsensitiveMap();
        for (Header h : http.getAllHeaders()) {
            strMap.put(h.getName(), h.getValue());
        }
        return strMap;
    }

    public static Header[] mapToHttpHeaders(Map<String, String> inputMap) {
        List<Header> h = new ArrayList<>(inputMap.size());
        for (String key : inputMap.keySet()) {
            h.add(new BasicHeader(key, inputMap.get(key)));
        }
        return h.toArray(new Header[h.size()]);
    }

    public static InternetHeaders mapToInternetHeaders(Map<String, String> inputMap) {
        InternetHeaders h = new InternetHeaders();
        if (inputMap != null && inputMap.size() > 0) {
            for (String key : inputMap.keySet()) {
                h.addHeader(key, inputMap.get(key));
            }
        }
        return h;
    }

    public static MdnMode getMdnMode(HttpRequest request) {
        Map<String, String> headers = httpHeadersToMap(request);
        String notifyOpts = getOrDefault(headers, "Disposition-Notification-Options", null); // if sending mdn this should have format, signed, etc
        String notifyTo = getOrDefault(headers, "Disposition-Notification-To", null);   // this is required, and should be email
        String deliveryOpts = getOrDefault(headers, "Receipt-Delivery-Option", null);   // this is the async mdn url

        if (isEmpty(notifyOpts) && isEmpty(notifyTo)) {
            return MdnMode.NONE;
        }

        return isEmpty(deliveryOpts)
                ? MdnMode.STANDARD
                : MdnMode.ASYNC;
    }


}
