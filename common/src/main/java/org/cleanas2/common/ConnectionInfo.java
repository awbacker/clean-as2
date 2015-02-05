package org.cleanas2.common;

import org.apache.http.HttpRequest;
import org.apache.http.impl.BHttpConnectionBase;

/**
 * Contains information about an incoming connection.  used in handling incoming messages, and in debugging.
 *
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class ConnectionInfo {
    public String sourceIp = "";
    public int sourcePort = 0;
    public String destinationIp = "";
    public int destinationPort = 0;
    public String requestMethod = "";
    public String requestUri = "";

    public ConnectionInfo() {
    }

    public ConnectionInfo(BHttpConnectionBase connection, HttpRequest request) {
        populateFrom(connection, request);
    }

    void populateFrom(BHttpConnectionBase connection, HttpRequest request) {
        sourceIp = connection.getRemoteAddress().toString();
        sourcePort = connection.getLocalPort();
        destinationIp = connection.getLocalAddress().toString();
        destinationPort = connection.getLocalPort();
        requestMethod = request.getRequestLine().getMethod();
        requestUri = request.getRequestLine().getUri();
    }
}
