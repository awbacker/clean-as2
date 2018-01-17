package org.cleanas2.common.exception


import org.cleanas2.common.disposition.DispositionType

class DispositionException : Exception {
    var disposition: DispositionType? = null
    var text: String? = null

    constructor(disposition: DispositionType, text: String, cause: Throwable) : super(disposition.toString()) {
        initCause(cause)
        this.disposition = disposition
        this.text = text
    }

    constructor(disposition: DispositionType, text: String) : super(disposition.toString()) {
        this.disposition = disposition
        this.text = text
    }

    companion object {

        fun error(statusDescription: String, statusMessage: String): DispositionException {
            val typ = DispositionType.error(statusDescription)
            return DispositionException(typ, statusMessage)
        }

        fun error(statusDescription: String, statusMessage: String, e: Throwable): DispositionException {
            val typ = DispositionType.error(statusDescription)
            return DispositionException(typ, statusMessage, e)
        }
    }

}
