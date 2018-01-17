package org.cleanas2.bus

/**
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
open class MessageBase {
    var isError = false
    var errorCause: Exception? = null
        set(errorCause) {
            this.isError = true
            field = errorCause
        }
}
