package org.cleanas2.common.exception

class AS2Exception : Exception {

    private val sources: Map<String, Any>? = null

    constructor(msg: String) : super(msg) {}

    constructor(msg: String, cause: Throwable) : super(msg, cause) {}

}
