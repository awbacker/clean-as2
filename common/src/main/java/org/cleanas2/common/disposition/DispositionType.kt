package org.cleanas2.common.disposition

import org.apache.commons.lang3.StringUtils
import org.boon.Lists
import org.boon.Str
import java.util.StringTokenizer

import java.text.MessageFormat.format


/*
   AS2-disposition-field =
       "Disposition" ":" disposition-mode ";"
       AS2-disposition-type [ '/' AS2-disposition-modifier ]

   disposition-mode =
       action-mode "/" sending-mode

   action-mode =
       "manual-action" | "automatic-action"

   sending-mode =
       "MDN-sent-manually" | "MDN-sent-automatically"

   AS2-disposition-type =
       "processed" | "failed"

   AS2-disposition-modifier =
       ( "error" | "warning" ) | AS2-disposition-modifier-extension

   AS2-disposition-modifier-extension =
       "error: authentication-failed" |
       "error: decompression-failed" |
       "error: decryption-failed" |
       "error: insufficient-message-security" |
       "error: integrity-check-failed" |
       "error: unexpected-processing-error" |
       "warning: " AS2-MDN-warning-description |
       "failure: " AS2-MDN-failure-description

   AS2-MDN-warning-description = *( TEXT )

   AS2-MDN-failure-description = *( TEXT )

   AS2-received-content-MIC-field =
       "Received-content-MIC" ":" encoded-message-digest ","
       digest-alg-id CRLF

   encoded-message-digest =
       1*( 'A'-Z' | 'a'-'z' | '0'-'9' | '/' | '+' | '=' )  (
       i.e. base64( message-digest ) )

   digest-alg-id = "sha1" | "md5"
 */

class DispositionType protected constructor(
        val actionMode: String,
        val sendingMode: String,
        val dispositionType: String,
        val dispositionModifier: String,
        val dispositionDescription: String
) {

    /**
     * If the disposition type indicated processing was successful
     */
    val isSuccess: Boolean
        get() = TYPE_PROCESSED == dispositionType && StringUtils.isBlank(dispositionModifier)

    val isWarning: Boolean
        get() = MOD_WARNING.equals(dispositionModifier, ignoreCase = true)

    /**
     * If the format of the disposition type is valid or not (e.g. does not violate any internal rules)
     */
    // we don't care about "manual-action" mode right now
    // only certain values are allowed for "error"
    // warning & failure can have any description, but must have one (?)
    val isFormatValid: Boolean
        get() {
            if (StringUtils.isBlank(actionMode) || !actionMode.equals(AUTOMATIC_ACTION, ignoreCase = true)) return false
            if (StringUtils.isBlank(dispositionType) || !_types.contains(dispositionType)) return false
            if (TYPE_PROCESSED == dispositionType) {
                if (StringUtils.isBlank(dispositionModifier)) return true
                if (!_modifiers.contains(dispositionModifier)) return false
                if (MOD_ERROR == dispositionModifier) {
                    if (!_errorDescriptions.contains(dispositionDescription)) return false
                } else {
                    if (StringUtils.isBlank(dispositionDescription)) return false
                }
            }
            return true
        }

    override fun toString(): String {
        var x = format("{0}/{1}; {2}", actionMode, sendingMode, dispositionType)
        if (!Str.isEmpty(dispositionModifier)) {
            x += format("/{0}:{1}", dispositionModifier, if (Str.isEmpty(dispositionDescription)) "" else dispositionDescription)
        }
        return x
    }

    companion object {

        val ERR_AUTH = "authentication-failed"
        val ERR_DECOMPRESSION = "decompression-failed"
        val ERR_DECRYPTION = "decryption-failed"
        val ERR_INTEGRITY_CHECK = "integrity-check-failed"
        val ERR_NOT_ENOUGH_SECURITY = "insufficient-message-security"
        val ERR_UNEXPECTED = "unexpected-processing-error"

        val AUTOMATIC_ACTION = "automatic-action"
        val MDN_SENT_AUTOMATICALLY = "mdn-sent-automatically"

        val TYPE_PROCESSED = "processed"
        val TYPE_FAILED = "failed"
        val MOD_ERROR = "error"
        val MOD_WARNING = "warning"


        private val _types = Lists.list(TYPE_PROCESSED, TYPE_FAILED)
        private val _modifiers = Lists.list(MOD_ERROR, MOD_WARNING, "failure")
        private val _errorDescriptions = Lists.list(
                ERR_AUTH, ERR_DECOMPRESSION, ERR_DECRYPTION, ERR_INTEGRITY_CHECK, ERR_UNEXPECTED, ERR_NOT_ENOUGH_SECURITY
        )

        fun error(dispositionDescription: String): DispositionType {
            assert(_errorDescriptions.contains(dispositionDescription))
            return DispositionType(AUTOMATIC_ACTION, MDN_SENT_AUTOMATICALLY,
                    TYPE_PROCESSED,
                    MOD_ERROR,
                    dispositionDescription // should be something like integrity-check-failed
            )
        }

        fun success(): DispositionType {
            return DispositionType(
                    AUTOMATIC_ACTION,
                    MDN_SENT_AUTOMATICALLY,
                    TYPE_PROCESSED,
                    // kotlin-update: these were change to blank strings.  they were null before
                    "", ""
            )
        }

        fun failure(failureDescription: String): DispositionType {
            return DispositionType(AUTOMATIC_ACTION, MDN_SENT_AUTOMATICALLY,
                    TYPE_FAILED,
                    "failure",
                    failureDescription
            )
        }

        fun warning(warningDescription: String): DispositionType {
            return DispositionType(
                    AUTOMATIC_ACTION,
                    MDN_SENT_AUTOMATICALLY,
                    TYPE_FAILED,
                    "failure",
                    warningDescription
            )
        }

        fun fromString(disposition: String): DispositionType {
            var i = 0
            val items = arrayOf("", "", "", "", "")
            val tok = StringTokenizer(disposition + "", "/;:", false)
            while (tok.hasMoreTokens()) {
                items[i] = tok.nextToken().trim { it <= ' ' }.toLowerCase()
                i += 1
                if (i > 4) break
            }
            return DispositionType(items[0], items[1], items[2], items[3], items[4])
        }

        fun isFormatValid(dispositionType: String): Boolean {
            return fromString(dispositionType).isFormatValid
        }
    }

}
