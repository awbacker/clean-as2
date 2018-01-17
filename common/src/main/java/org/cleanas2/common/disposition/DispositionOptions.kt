package org.cleanas2.common.disposition

import org.cleanas2.common.exception.AS2Exception

import java.util.NoSuchElementException
import java.util.StringTokenizer


/**
 * FROM THE SPEC
 * ---------------------------------
 * Finally, the header, Disposition-notification-options, identifies
 * characteristics of message disposition notification as in [5].  The
 * most important of these options is for indicating the signing options
 * for the MDN, as in the following example:
 *
 *
 * Disposition-notification-options:
 * signed-receipt-protocol=optional,pkcs7-signature; signed-receipt-micalg=optional,sha1,md5
 *
 *
 *
 *
 * The "signed-receipt-protocol" parameter is used to request a
 * signed receipt from the recipient trading partner.  The "signed-
 * receipt-protocol" parameter also specifies the format in which the
 * signed receipt SHOULD be returned to the requester.
 *
 *
 * The "signed-receipt-micalg" parameter is a list of MIC algorithms
 * preferred by the requester for use in signing the returned
 * receipt.  The list of MIC algorithms SHOULD be honored by the
 * recipient from left to right.
 *
 *
 * Both the "signed-receipt-protocol" and the "signed- receipt-
 * micalg" option parameters are REQUIRED when requesting a signed
 * receipt.
 */
class DispositionOptions @Throws(AS2Exception::class)
constructor(options: String?) {

    val micAlgorithm: String
    val micAlgorithmImportance: String
    val protocol: String
    val protocolImportance: String

    init {
        if (options == null) throw AS2Exception("Invalid disposition options format: <NULL>")
        val optionTokens = StringTokenizer(options, "=,;", false)
        if (optionTokens.countTokens() <= 5) {
            throw AS2Exception("Invalid disposition options format: " + options)
        }
        try {
            optionTokens.nextToken()
            this.protocolImportance = optionTokens.nextToken().trim { it <= ' ' }
            this.protocol = optionTokens.nextToken().trim { it <= ' ' }
            optionTokens.nextToken()
            this.micAlgorithmImportance = optionTokens.nextToken().trim { it <= ' ' }
            this.micAlgorithm = optionTokens.nextToken().trim { it <= ' ' }
        } catch (e: NoSuchElementException) {
            throw AS2Exception("Invalid disposition options format: " + options)
        }

    }

    override fun toString(): String {
        return String.format("signed-receipt-protocol=%s, %s; signed-receipt-micalg=%s, %s",
                protocolImportance,
                protocol,
                micAlgorithmImportance,
                micAlgorithm)
    }
}
