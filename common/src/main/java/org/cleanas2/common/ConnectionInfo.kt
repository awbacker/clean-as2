package org.cleanas2.common

import org.apache.http.HttpRequest
import org.apache.http.impl.BHttpConnectionBase

/**
 * Contains information about an incoming connection.  used in handling incoming messages, and in debugging.
 *
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
class ConnectionInfo {
    var sourceIp = ""
    var sourcePort = 0
    var destinationIp = ""
    var destinationPort = 0
    var requestMethod = ""
    var requestUri = ""

    constructor() {}

    constructor(connection: BHttpConnectionBase, request: HttpRequest) {
        populateFrom(connection, request)
    }

    internal fun populateFrom(connection: BHttpConnectionBase, request: HttpRequest) {
        sourceIp = connection.remoteAddress.toString()
        sourcePort = connection.localPort
        destinationIp = connection.localAddress.toString()
        destinationPort = connection.localPort
        requestMethod = request.requestLine.method
        requestUri = request.requestLine.uri
    }
}
