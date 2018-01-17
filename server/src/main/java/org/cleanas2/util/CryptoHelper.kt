package org.cleanas2.util

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.boon.Str
import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.cms.CMSAlgorithm
import org.bouncycastle.mail.smime.SMIMEEnvelopedGenerator
import org.bouncycastle.mail.smime.SMIMESignedGenerator
import org.bouncycastle.operator.*
import org.bouncycastle.operator.bc.BcDigestCalculatorProvider
import org.bouncycastle.util.encoders.Base64

import javax.mail.MessagingException
import javax.mail.internet.MimeBodyPart
import java.io.IOException
import java.security.GeneralSecurityException

import org.boon.Maps.map


/**
 * Helper methods for dealing with the Crypto libraries.  This includes converting between AS2 friendly
 * names for encryption/digest algorithms, calculating message digests, and any other crypto library
 * related functionality that isn't specific to the end result
 *
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
object CryptoHelper {

    private val logger = LogFactory.getLog(CryptoHelper::class.java.simpleName)

    /**
     * Supported signing algorithms for the "signAlgorithm" property of a partnership
     */
    private val signAlgoMap = map(
            "MD5", "MD5withRSA",
            "SHA1", "SHA1withRSA",
            "SHA-1", "SHA1withRSA"
    )

    /**
     * Used when we calculate the message digest.  this happens based on the
     * content-disposition string, which says something like micalg="sha1"
     */
    private val micAlgoMap = map(
            "MD5", SMIMESignedGenerator.DIGEST_MD5,
            "SHA1", SMIMESignedGenerator.DIGEST_SHA1,
            "SHA-1", SMIMESignedGenerator.DIGEST_SHA1,
            "3DES", SMIMEEnvelopedGenerator.DES_EDE3_CBC,
            "CAST5", SMIMEEnvelopedGenerator.CAST5_CBC,
            "IDEA", SMIMEEnvelopedGenerator.IDEA_CBC,
            "RC2", SMIMEEnvelopedGenerator.RC2_CBC
    )

    /**
     * Supported encryption values for the "encryptAlgorithm" property of a partnership
     */
    private val encryptAlgoMap = map(
            "3DES", CMSAlgorithm.DES_EDE3_CBC,
            "DES3", CMSAlgorithm.DES_EDE3_CBC,
            "RC2", CMSAlgorithm.RC2_CBC
    )

    private val digestAlgoNameMap = map(
            "MD5", "MD5",
            "SHA1", "SHA-1",
            "SHA-1", "SHA-1"
    )

    /**
     * Converts an algorithm name to the bouncyCastle equivalent specific algorithm identifier.  For
     * example, sha1 => SHA1withRSA.  This value can then be used by the SigningInfoGenerator
     */
    fun translateSigningAlgorithmName(as2AlgorithmName: String): String {
        return mapGet(as2AlgorithmName, signAlgoMap, "SHA1", "signing algorithm")
    }

    fun translateEncryptionAlgorithmName(as2AlgorithmName: String): ASN1ObjectIdentifier {
        return mapGet(as2AlgorithmName, encryptAlgoMap, "3DES", "encryption")
    }

    private fun translateDigestAlgoName(digestAlgorithm: String): String {
        return mapGet(digestAlgorithm, digestAlgoNameMap, "SHA1", "digest name for mic")
    }

    /**
     * Gets the value from the map, or returns the default.  Map keys are required to be in UPPER case
     */
    private fun <T> mapGet(keyName: String, mapIn: Map<String, T>, def: String, typeName: String): T {
        val keyNameUpper = Str.upper(keyName)
        if (mapIn.containsKey(keyNameUpper)) {
            return mapIn[keyNameUpper]!!
        } else {
            logger.error("Incorrect $typeName algorithm '$keyNameUpper'.  Using '$def'")
            return mapIn[def]!!
        }
    }

    /**
     * Calculates the Message Integrity Code header for a MIME part.  The intent of this function
     * is to calculate the MIC code the same way that the smime SIGNING code does it, so that we can
     * compare it when it comes back.  This saves us from writing a more complicated MimeUtil.sign() function
     * that returns the signed part PLUS the digest, but we can do that if we need to later.  We probably should.
     *
     * @param part            MIME part to use
     * @param digestAlgorithm algorithm to use to generate the MIC (usually 'SHA1' or 'MD5')
     * @return A string containing the MIC followed by the algorithm (e.g. 988a8e6ee0b5b0fa234fd1d68d213953, MD5)
     * @throws GeneralSecurityException Thrown when the requested algorithm is not found
     * @throws MessagingException       There was an error in the MimeBodyPart
     * @throws IOException              There was an error writing to the digest output stream
     */
    @Throws(GeneralSecurityException::class, IOException::class, MessagingException::class)
    fun calculateMIC(part: MimeBodyPart, digestAlgorithm: String): String {
        val dc = getDigestCalculator(digestAlgorithm)
        part.writeTo(dc.outputStream) // stream the whole part, same as it will be sent over the wire
        val micString = Base64.toBase64String(dc.digest) + ", " + digestAlgorithm

        logger.debug("Calculated MIC: " + micString)
        return micString
    }

    /**
     * Gets the digest provider based on the algorithm passed in.  This converts the AS2 accepted algorithm
     * to a bouncy castle specific one if needed ("e.g. SHA1=> SHA-1") and does the upper casing, etc
     */
    @Throws(GeneralSecurityException::class)
    private fun getDigestCalculator(digestAlgorithm: String): DigestCalculator {
        val algo = translateDigestAlgoName(digestAlgorithm)
        val id = DefaultDigestAlgorithmIdentifierFinder().find(algo)
        try {
            return BcDigestCalculatorProvider().get(id)
        } catch (e: OperatorCreationException) {
            throw GeneralSecurityException("Could not find digest algorithm for input " + algo, e)
        }

    }


}
