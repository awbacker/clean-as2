package org.cleanas2.common

import org.boon.Str

/**
 * This record is deserialized from the JSON config file by the PartnerService.  Not all values
 * in the structure are required, most have defaults.
 *
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
class PartnerRecord {

    /**
     * AS2 ID of the partner. Required.
     */
    var as2id: String? = null

    /**
     * Friendly name for the partner.  Not used internally except for logging/viewing
     */
    var name: String? = null

    /**
     * Description of the partner.  Also not used internally, informational only.
     */
    var description: String? = null

    /**
     * Email address for the partner. This is a left-over from AS1, and is sent as an HTTP header, but not
     * normally used.  When an MDN is requested, this value will be sent
     */
    var email: String? = null

    /**
     * The certificate file to use when signing messages to send to this partner.  Can be DER or PEM encoded
     */
    var certificate: String? = null

    /**
     * The basic settings controlling what and how to send files
     */
    val sendSettings = SendSettings()

    fun shouldSign(): Boolean {
        return !Str.isEmpty(this.sendSettings.signAlgorithm)
    }

    fun shouldEncrypt(): Boolean {
        return !Str.isEmpty(this.sendSettings.encryptAlgorithm)
    }

    class SendSettings {

        /**
         * The URL to send outgoing files to
         */
        var url: String? = null

        /**
         * The encryption algorithm to use.  Valid values are "3DES", "" (for no encryption)
         */
        var encryptAlgorithm: String? = null

        /**
         * The signing algorithm to use, and generate a content MIC from.  "SHA1" or "MD5"
         */
        var signAlgorithm: String? = null

        /**
         * Transfer encoding to use.  This should not normally change, and should always be "binary".  if
         * another value is used, the BC library may not encrypt/sign as expected by the receiver.
         */
        val transferEncoding = "binary"

        /**
         * The content type to send with the file sent.  This is added to the MIME message before signing/encrypting.
         * After signing/encrypting, the content type sent is for the MIME message, so this is not visible in HTTP
         */
        val contentType = "application/EDIFACT"

        /**
         * MDN options to send to the foreign server.  This should match the signing algorithm used.
         * In the future, this should be *calculated* from the other settings, not set manually
         */
        var mdnOptions: String? = null

        /**
         * The type of MDN to send.  Valid values are "none", "standard", "async".  Default is STANDARD (synchronous)
         * Note: Cyclone Server does not handle ASYNC MDN correctly (it ignores the "reply to" URL in the headers)
         */
        val mdnMode = MdnMode.STANDARD
    }
}
