package org.cleanas2.service.net.util

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.http.*
import org.apache.http.impl.DefaultBHttpServerConnection
import org.apache.http.message.BasicHeader
import org.cleanas2.common.MdnMode

import javax.mail.internet.InternetHeaders
import java.net.Socket
import java.util.*

import org.apache.commons.lang3.StringUtils.isEmpty
import org.apache.http.HttpStatus.*
import org.boon.Lists.list
import org.cleanas2.util.AS2Util.getOrDefault
import org.cleanas2.util.AS2Util.newCaseInsensitiveMap

/**
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
object NetUtil {

    private val logger = LogFactory.getLog(NetUtil::class.java.simpleName)

    private val validResponseCodes = list(
            SC_OK, SC_CREATED, SC_ACCEPTED, SC_PARTIAL_CONTENT, SC_NO_CONTENT
    )

    fun isPost(request: HttpRequest): Boolean {
        return "POST" == request.requestLine.method.toUpperCase(Locale.ENGLISH)
    }


    fun isInvalidResponseCode(response: HttpResponse): Boolean {
        return !validResponseCodes.contains(response.statusLine.statusCode)
    }

    /**
     * Get debugging info for logging
     */
    private fun getRemoteInfo(c: DefaultBHttpServerConnection): String {
        return String.format("%s:%d", c.remoteAddress, c.remotePort)
    }

    /**
     * Get debugging info for logging
     */
    private fun getLocalInfo(c: DefaultBHttpServerConnection): String {
        return String.format("%s:%d", c.localAddress, c.localPort)
    }

    /**
     * Get debugging info for logging
     */
    fun getClientInfo(s: Socket): String {
        return String.format("%s:%s", s.inetAddress.hostAddress, Integer.toString(s.port))
    }

    /**
     * Get debugging info for logging
     */
    fun getServerInfo(s: Socket): String {
        return String.format("%s:%s", s.localAddress.hostAddress, Integer.toString(s.localPort))
    }

    fun getEndpointInfo(conn: DefaultBHttpServerConnection): String {
        return getRemoteInfo(conn) + " ==> " + getLocalInfo(conn)
    }

    fun httpHeadersToMap(http: HttpMessage): AbstractMap<String, String> {
        // make lookup by key be case insensitive (but a little slower).  N is wo low in this case, it doesn't matter
        val strMap = newCaseInsensitiveMap<String>()
        for (h in http.allHeaders) {
            strMap.put(h.name, h.value)
        }
        return strMap
    }

    fun mapToHttpHeaders(inputMap: Map<String, String>): Array<Header> {
        val h = ArrayList<Header>(inputMap.size)
        for (key in inputMap.keys) {
            h.add(BasicHeader(key, inputMap[key]))
        }
        return h.toTypedArray()
    }

    fun mapToInternetHeaders(inputMap: Map<String, String>?): InternetHeaders {
        val h = InternetHeaders()
        if (inputMap != null && inputMap.isNotEmpty()) {
            for (key in inputMap.keys) {
                h.addHeader(key, inputMap[key])
            }
        }
        return h
    }

    fun getMdnMode(request: HttpRequest): MdnMode {
        val headers = httpHeadersToMap(request)
        val notifyOpts = getOrDefault<String, String>(headers, "Disposition-Notification-Options", "") // if sending mdn this should have format, signed, etc
        val notifyTo = getOrDefault<String, String>(headers, "Disposition-Notification-To", "")   // this is required, and should be email
        val deliveryOpts = getOrDefault<String, String>(headers, "Receipt-Delivery-Option", "")   // this is the async mdn url

        if (isEmpty(notifyOpts) && isEmpty(notifyTo)) {
            return MdnMode.NONE
        }

        return if (isEmpty(deliveryOpts))
            MdnMode.STANDARD
        else
            MdnMode.ASYNC
    }


}
