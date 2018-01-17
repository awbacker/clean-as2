package org.cleanas2.service.net.util

import org.apache.commons.lang3.StringUtils
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.http.*
import org.apache.http.util.EntityUtils
import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.cms.*
import org.bouncycastle.cms.jcajce.*
import org.bouncycastle.mail.smime.*
import org.bouncycastle.openssl.EncryptionException
import org.bouncycastle.operator.OperatorCreationException
import org.cleanas2.common.disposition.DispositionOptions
import org.cleanas2.util.CryptoHelper

import javax.activation.DataHandler
import javax.mail.MessagingException
import javax.mail.internet.*
import javax.mail.util.ByteArrayDataSource
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.security.GeneralSecurityException
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.*

import org.apache.commons.lang3.StringUtils.isBlank
import org.cleanas2.util.Constants.CRLF

/**
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
object MimeUtil {

 val BC = "BC"
private val logger = LogFactory.getLog(MimeUtil::class.java.simpleName)

/**
 * Determines if the MIME message is encrypted based on the content type
 *
 * @param part MIME body part to check
 * @return true if message is encrypted
 * @throws javax.mail.MessagingException
 */
    @Throws(MessagingException::class)
 fun isEncrypted(part:MimeBodyPart):Boolean {
val contentType = ContentType(part.contentType)
val baseType = contentType.baseType.toLowerCase()
val mimeType = contentType.getParameter("smime-type")
return "application/pkcs7-mime".equals(baseType, ignoreCase = true) &&
mimeType != null &&
"enveloped-data".equals(mimeType, ignoreCase = true)
}

/**
 * Determines if the body part is signed (not necessarily encrypted)
 *
 * @param part MIME body part to check
 * @return true if body part is signed (multipart/signed)
 * @throws javax.mail.MessagingException
 */
    @Throws(MessagingException::class)
 fun isSigned(part:MimeBodyPart):Boolean {
val contentType = ContentType(part.contentType)
val baseType = contentType.baseType.toLowerCase()
return "multipart/signed".equals(baseType, ignoreCase = true)
}

/**
 * Digitally signs a message by generating a hash, and encrypting that with the sender's private key, and
 * attaching
 *
 * @param part             The mime body part to sign
 * @param senderCert       Cert
 * @param senderKey        Key
 * @param signingAlgorithm The algorithm to use, usually SHA1 or MD5
 */
    @Throws(EncryptionException::class)
 fun signBodyPart(part:MimeBodyPart, senderCert:X509Certificate, senderKey:PrivateKey, signingAlgorithm:String):MimeBodyPart {
try
{
 // make a new mime part from the CONTENT, ignoring the headers that were added
            // something is fishy here...
            val gen = SMIMESignedGenerator()
val sigGen = JcaSimpleSignerInfoGeneratorBuilder()
.setProvider(BC)
.build(CryptoHelper.translateSigningAlgorithmName(signingAlgorithm), senderKey, senderCert)
gen.addSignerInfoGenerator(sigGen)
val smime = gen.generate(part)
val tmpBody = MimeBodyPart()
tmpBody.setContent(smime)
tmpBody.setHeader("Content-Type", smime.contentType)
return tmpBody
}
catch (e:Exception) {
throw EncryptionException("Error signing body part : " + e.message, e)
}

}

/**
 * Encrypts a MIME message using the receiver's certificate
 *
 * @param part      MIME body part to encrypt.  This could have previously been signed
 * @param cert      X509 Certificate to encrypt with.  The receiver will use their private key to decrypt
 * @param algorithm Encryption algorithm to use (des3/aes256/idea/etc)
 */
    @Throws(EncryptionException::class)
 fun encryptBodyPart(part:MimeBodyPart, cert:X509Certificate, algorithm:String):MimeBodyPart {
try
{
val algo = CryptoHelper.translateEncryptionAlgorithmName(algorithm)
val gen = SMIMEEnvelopedGenerator()
gen.addRecipientInfoGenerator(JceKeyTransRecipientInfoGenerator(cert).setProvider(BC))
return gen.generate(part, JceCMSContentEncryptorBuilder(algo).setProvider(BC).build())
}
catch (e:Exception) {
throw EncryptionException("Error encrypting body part: " + e.message, e)
}

}

/**
 * Decrypts a MIME part using the given key & certificate.  Throws an error if the body part isn't actually
 * encrypted according to the Content-Type in the header
 *
 * @param part Part to decrypt
 * @param cert Certificate to use
 * @param key  Key to use
 * @throws GeneralSecurityException          Thrown when the content is not actually encrypted, or when signatures do not match
 * @throws javax.mail.MessagingException
 * @throws org.bouncycastle.cms.CMSException
 * @throws java.io.IOException
 * @throws SMIMEException
 */
    @Throws(GeneralSecurityException::class, MessagingException::class, CMSException::class, IOException::class, SMIMEException::class)
 fun decryptBodyPart(part:MimeBodyPart, cert:X509Certificate, key:PrivateKey):MimeBodyPart {

 // Make sure the data is encrypted
        if (!isEncrypted(part))
{
throw GeneralSecurityException("Content-Type indicates data isn't encrypted")
}

val envelope = SMIMEEnveloped(part)

val recId = JceKeyTransRecipientId(cert)
val recipients = envelope.recipientInfos
val recipient = recipients.get(recId) ?: throw GeneralSecurityException("Certificate does not match part signature")

val data = recipient.getContent(JceKeyTransEnvelopedRecipient(key).setProvider(BC))
return SMIMEUtil.toMimeBodyPart(data)
}

/**
 * Verifies the signature on a MIME body part, and removes the signature.  Returns just the inner content
 *
 * @param part The MIME body part to process.  Must contain a MimeMultipart as it's content
 * @param cert Certificate the part should have been signed with on the sending side
 */
    @Throws(GeneralSecurityException::class, IOException::class, MessagingException::class)
 fun verifyAndRemoveSignature(part:MimeBodyPart, cert:X509Certificate):MimeBodyPart {
try
{
if (!isSigned(part))
{
throw GeneralSecurityException("Content-Type indicates data isn't signed")
}

val smime = SMIMESigned(part.content as MimeMultipart)
 //Store certs = smime.getCertificates();
            val signers = smime.signerInfos
val c = signers.signers

for (aC in c)
{
val signer = aC as SignerInformation

 //Collection certCollection = certs.getMatches(signer.getSID());
                //Iterator certIt = certCollection.iterator();
                //X509Certificate abc = new JcaX509CertificateConverter().setProvider(BC).getCertificate((X509CertificateHolder) certIt.next());

                try
{
if (signer.verify(JcaSimpleSignerInfoVerifierBuilder().setProvider(BC).build(cert)))
{
logger.debug("signature verified")
break
}
else
{
logger.debug("signature verification failed")
}
}
catch (e:OperatorCreationException) {
logger.error("signature verification failed", e)
}

}
return smime.content
}
catch (e:CMSException) {
throw GeneralSecurityException("An encryption related error occurred when checking/removing the digital signature", e)
}

}

/**
 * Calculates the MIC value for the given MimeBodyPart.  This normally happens before encryption,
 * and is verified after decryption.  It relies on the partnership settings to tell us which algorithm,
 * if any, to use
 *
 * @param data Mime body part to calculate the mic (hash/digest/etc) of
 * @return Calculated MIC, or empty string if no mice algorithm specified
 * @throws Exception
 */
    @Throws(Exception::class)
 fun calculateMicString(data:MimeBodyPart, contentDisposition:String):String {
var mic = ""
val dispOptions = DispositionOptions(contentDisposition)
if (!StringUtils.isBlank(dispOptions.micAlgorithm))
{
mic = CryptoHelper.calculateMIC(data, dispOptions.micAlgorithm)
}
return mic
}

/**
 * Converts a collection of Http.Core Headers into a InternetHeader to an array suitable for a message or Message MDN
 *
 * @param in Collection of headers, usually from a MimeBodyPart/Message
 */
     fun responseHeadersToMimeHeaders(`in`:Array<Header>):InternetHeaders {
val out = InternetHeaders()
for (h in `in`)
{
out.setHeader(h.name, h.value)
}
return out
}

/**
 * Creates a MimeBodyPart from the HttpEntity in the response (used when receiving a reply to a sent file)
 *
 * @throws MessagingException When there is an error creating the body part
 * @throws IOException        When there is an error reading from the stream
 */
    @Throws(MessagingException::class, IOException::class)
 fun fromHttpResponse(response:HttpResponse):MimeBodyPart {
 // it looks like the only header we care about is the content type, actually, but copy them all anyway
        val headers = responseHeadersToMimeHeaders(response.allHeaders)
val content = EntityUtils.toByteArray(response.entity)
return MimeBodyPart(headers, content)
}

/**
 * Creates a MimeBodyPart from the HttpEntity in the request
 *
 * @throws MessagingException When there is an error creating the body part
 * @throws IOException        When there is an error reading from the stream
 */
    @Throws(IOException::class, MessagingException::class)
 fun fromHttpRequest(request:HttpEntityEnclosingRequest):MimeBodyPart {
val ct = getSingleHeader(request, "Content-Type")
val entityContent = EntityUtils.toByteArray(request.entity)
val dataSource = ByteArrayDataSource(entityContent, ct)
val receivedPart = MimeBodyPart()
receivedPart.dataHandler = DataHandler(dataSource)
receivedPart.setHeader("Content-Type", ct)
return receivedPart
}

@Throws(IOException::class)
private fun getSingleHeader(request:HttpEntityEnclosingRequest, name:String):String {
val header = request.getFirstHeader(name) ?: throw IOException("Request did not contain a $name header")
return header.value
}

/**
 * Reads the contents of a file into a MimeBodyPart.  Does not set the content type
 */
    @Throws(IOException::class, MessagingException::class)
 fun fromFile(filePath:Path, contentType:String):MimeBodyPart {
val data = Files.readAllBytes(filePath)
val dataSource = ByteArrayDataSource(data, contentType)
val outBodyPart = MimeBodyPart()
outBodyPart.dataHandler = DataHandler(dataSource)
return outBodyPart
}

/**
 * Creates a body part from the string & content type.  Strips the string of whitespace and adds 2 \r\n at end.
 */
    @Throws(MessagingException::class)
@JvmOverloads  fun textBodyPart(bodyText:String, contentType:String = "text/plain", headers:Map<String, String>? = null):MimeBodyPart {
val textPart = MimeBodyPart()
val bodyContent = StringBuilder()

 // use the built-in javax.mail way of doing this.  the headers need to be added to the body content
        // directly.. the mimeBodyPart.addHeader() doesn't do it... grr... fucking confusing.  now, it appears
        // that, when you call getInputStream(), it returns the content.  this is then read by input stream which
        // extracts the headers from the body text.  maybe it works when reading, not 100% sure.
        if (headers != null && headers.size > 0)
{
val x = NetUtil.mapToInternetHeaders(headers)
val en = x.allHeaderLines
while (en.hasMoreElements())
{
bodyContent.append(en.nextElement() as String)
bodyContent.append(CRLF)
}
}

bodyContent.append(CRLF)
if (!isBlank(bodyText))
{
bodyContent.append(StringUtils.stripToEmpty(bodyText))
}
textPart.setContent(bodyContent.toString(), contentType)
textPart.setHeader("Content-Type", contentType)
return textPart
}

/**
 * Converts a multipart by wrapping it in a new MimeBodyPart.  Copies the content-type from the multipart
 */
    @Throws(MessagingException::class)
 fun multiPartToBodyPart(multiPart:MimeMultipart):MimeBodyPart {
 // Convert report parts to MimeBodyPart
        val report = MimeBodyPart()
report.setContent(multiPart)
report.setHeader("Content-Type", multiPart.contentType)
return report
}

}/**
 * Creates a body part from the string & content type.  Strips the string of whitespace and adds 2 \r\n at end.
 */
