package org.cleanas2.util

import org.apache.commons.lang3.StringUtils
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.http.Header
import org.apache.http.message.BasicHeader
import org.boon.Str

import javax.crypto.SecretKey
import java.security.*
import java.util.ArrayList
import java.util.Enumeration

import org.boon.Lists.list

/**
 * Class that contains only classes for writing debug information to the log.  These
 * are aids during development, such as dumping a whole object out as javascript, or
 * pretty-printing a map, or writing a temp file (to the CleanAS2 system/temp directory)
 *
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
object DebugUtil {

    private val _internalLogger = LogFactory.getLog(DebugUtil::class.java.simpleName)

    fun debugPrintObject(log: Log, headerText: String, obj: Any) {
        try {
            val allStuff = (if (Str.isEmpty(headerText)) "" else headerText + " : ") + JsonUtil.toPrettyJson(obj)
            for (line in Str.splitLines(allStuff)) {
                log.debug(StringUtils.stripEnd(line, "\r\n,"))
            }
        } catch (e: Throwable) {
            _internalLogger.error("Error printing object to debug", e)
        }

    }

    fun debugPrintHeaders(log: Log, headerText: String, headers: Iterable<Header>) {
        log.debug(headerText + ": {")
        for (h in headers) {
            log.debug(String.format("    %s: %s", h.name, h.value))
        }
        log.debug("}")
    }


    fun debugPrintHeaders(log: Log, headerText: String, headers: Array<Header>) {
        debugPrintHeaders(log, headerText, list(*headers))
    }

    fun debugPrintHeaders(log: Log, headerText: String, headers: Enumeration<*>) {
        val newHeaders = ArrayList<Header>()
        while (headers.hasMoreElements()) {
            val badHeader = headers.nextElement() as javax.mail.Header
            newHeaders.add(BasicHeader(badHeader.name, badHeader.value))
        }
        debugPrintHeaders(log, headerText, newHeaders)
    }

    /**
     * Prints debugging information for a keystore entry associated with an alias.
     * Each alias can have only ONE keystore entry
     */
    @Throws(KeyStoreException::class, NoSuchAlgorithmException::class, UnrecoverableEntryException::class)
    fun describeKeyStoreEntry(keyStore: KeyStore, alias: String, password: CharArray): String {
        var entryDescription = ""
        if (keyStore.entryInstanceOf(alias, KeyStore.TrustedCertificateEntry::class.java)) {
            // check if it is this kind (just a public key, basically).  if you try to get it out with a
            // password, it will fail saying you shouldn't send a password.  but if you try to get the other one
            // out without a password, it will fail.  so no one code branch will work for both cases.
            val certEntry = keyStore.getEntry(alias, null) as KeyStore.TrustedCertificateEntry
            val certificate = certEntry.trustedCertificate
            entryDescription = "TrustedCertificateEntry (type=" + certificate.type + ")"
        } else {
            val entry = keyStore.getEntry(alias, KeyStore.PasswordProtection(password))
            if (entry is KeyStore.SecretKeyEntry) {
                val key = entry.secretKey
                entryDescription = "SecretKeyEntry (" + key.algorithm + ") - alias '" + alias + "'"
            } else if (entry is KeyStore.PrivateKeyEntry) {
                val key = entry.privateKey
                val certificate = entry.certificate
                entryDescription = String.format("PrivateKeyEntry (alg=%s, type=%s, pub key=%s)",
                        key.algorithm,
                        certificate.type,
                        certificate.publicKey.algorithm)
            }
        }
        return entryDescription
    }
}
