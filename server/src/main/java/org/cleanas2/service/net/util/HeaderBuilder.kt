package org.cleanas2.service.net.util

import org.apache.http.Header
import org.apache.http.message.BasicHeader

import java.util.ArrayList

/**
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
class HeaderBuilder {
    private val headers = ArrayList<BasicHeader>(10)

    fun add(name: String, value: String?) {
        if (value != null) {
            headers.add(BasicHeader(name, value))
        }
    }

    fun toArray(): Array<Header> {
        return headers.toTypedArray()
    }
}
