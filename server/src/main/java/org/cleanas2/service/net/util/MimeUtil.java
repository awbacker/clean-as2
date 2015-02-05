package org.cleanas2.service.net.util;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.*;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.*;
import org.bouncycastle.mail.smime.*;
import org.bouncycastle.openssl.EncryptionException;
import org.bouncycastle.operator.OperatorCreationException;
import org.cleanas2.common.disposition.DispositionOptions;
import org.cleanas2.util.CryptoHelper;

import javax.activation.DataHandler;
import javax.mail.MessagingException;
import javax.mail.internet.*;
import javax.mail.util.ByteArrayDataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.*;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.cleanas2.util.Constants.CRLF;

/**
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class MimeUtil {

    public static final String BC = "BC";
    private static final Log logger = LogFactory.getLog(MimeUtil.class.getSimpleName());

    /**
     * Determines if the MIME message is encrypted based on the content type
     *
     * @param part MIME body part to check
     * @return true if message is encrypted
     * @throws javax.mail.MessagingException
     */
    public static boolean isEncrypted(MimeBodyPart part) throws MessagingException {
        ContentType contentType = new ContentType(part.getContentType());
        String baseType = contentType.getBaseType().toLowerCase();
        String mimeType = contentType.getParameter("smime-type");
        return "application/pkcs7-mime".equalsIgnoreCase(baseType) &&
                mimeType != null &&
                "enveloped-data".equalsIgnoreCase(mimeType);
    }

    /**
     * Determines if the body part is signed (not necessarily encrypted)
     *
     * @param part MIME body part to check
     * @return true if body part is signed (multipart/signed)
     * @throws javax.mail.MessagingException
     */
    public static boolean isSigned(MimeBodyPart part) throws MessagingException {
        ContentType contentType = new ContentType(part.getContentType());
        String baseType = contentType.getBaseType().toLowerCase();
        return "multipart/signed".equalsIgnoreCase(baseType);
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
    public static MimeBodyPart signBodyPart(MimeBodyPart part, X509Certificate senderCert, PrivateKey senderKey, String signingAlgorithm) throws EncryptionException {
        try {
            // make a new mime part from the CONTENT, ignoring the headers that were added
            // something is fishy here...
            SMIMESignedGenerator gen = new SMIMESignedGenerator();
            SignerInfoGenerator sigGen = new JcaSimpleSignerInfoGeneratorBuilder()
                    .setProvider(BC)
                    .build(CryptoHelper.translateSigningAlgorithmName(signingAlgorithm), senderKey, senderCert);
            gen.addSignerInfoGenerator(sigGen);
            MimeMultipart smime = gen.generate(part);
            MimeBodyPart tmpBody = new MimeBodyPart();
            tmpBody.setContent(smime);
            tmpBody.setHeader("Content-Type", smime.getContentType());
            return tmpBody;
        } catch (Exception e) {
            throw new EncryptionException("Error signing body part : " + e.getMessage(), e);
        }
    }

    /**
     * Encrypts a MIME message using the receiver's certificate
     *
     * @param part      MIME body part to encrypt.  This could have previously been signed
     * @param cert      X509 Certificate to encrypt with.  The receiver will use their private key to decrypt
     * @param algorithm Encryption algorithm to use (des3/aes256/idea/etc)
     */
    public static MimeBodyPart encryptBodyPart(MimeBodyPart part, X509Certificate cert, String algorithm) throws EncryptionException {
        try {
            ASN1ObjectIdentifier algo = CryptoHelper.translateEncryptionAlgorithmName(algorithm);
            SMIMEEnvelopedGenerator gen = new SMIMEEnvelopedGenerator();
            gen.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(cert).setProvider(BC));
            return gen.generate(part, new JceCMSContentEncryptorBuilder(algo).setProvider(BC).build());
        } catch (Exception e) {
            throw new EncryptionException("Error encrypting body part: " + e.getMessage(), e);
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
    public static MimeBodyPart decryptBodyPart(MimeBodyPart part, X509Certificate cert, PrivateKey key)
            throws GeneralSecurityException, MessagingException, CMSException, IOException,
            SMIMEException {

        // Make sure the data is encrypted
        if (!isEncrypted(part)) {
            throw new GeneralSecurityException("Content-Type indicates data isn't encrypted");
        }

        SMIMEEnveloped envelope = new SMIMEEnveloped(part);

        RecipientId recId = new JceKeyTransRecipientId(cert);
        RecipientInformationStore recipients = envelope.getRecipientInfos();
        RecipientInformation recipient = recipients.get(recId);

        if (recipient == null) {
            throw new GeneralSecurityException("Certificate does not match part signature");
        }

        byte[] data = recipient.getContent(new JceKeyTransEnvelopedRecipient(key).setProvider(BC));
        return SMIMEUtil.toMimeBodyPart(data);
    }

    /**
     * Verifies the signature on a MIME body part, and removes the signature.  Returns just the inner content
     *
     * @param part The MIME body part to process.  Must contain a MimeMultipart as it's content
     * @param cert Certificate the part should have been signed with on the sending side
     */
    public static MimeBodyPart verifyAndRemoveSignature(MimeBodyPart part, X509Certificate cert) throws GeneralSecurityException, IOException, MessagingException {
        try {
            if (!isSigned(part)) {
                throw new GeneralSecurityException("Content-Type indicates data isn't signed");
            }

            SMIMESigned smime = new SMIMESigned((MimeMultipart) part.getContent());
            //Store certs = smime.getCertificates();
            SignerInformationStore signers = smime.getSignerInfos();
            Collection c = signers.getSigners();

            for (Object aC : c) {
                SignerInformation signer = (SignerInformation) aC;

                //Collection certCollection = certs.getMatches(signer.getSID());
                //Iterator certIt = certCollection.iterator();
                //X509Certificate abc = new JcaX509CertificateConverter().setProvider(BC).getCertificate((X509CertificateHolder) certIt.next());

                try {
                    if (signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider(BC).build(cert))) {
                        logger.debug("signature verified");
                        break;
                    } else {
                        logger.debug("signature verification failed");
                    }
                } catch (OperatorCreationException e) {
                    logger.error("signature verification failed", e);
                }
            }
            return smime.getContent();
        } catch (CMSException e) {
            throw new GeneralSecurityException("An encryption related error occurred when checking/removing the digital signature", e);
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
    public static String calculateMicString(MimeBodyPart data, String contentDisposition) throws Exception {
        String mic = "";
        DispositionOptions dispOptions = new DispositionOptions(contentDisposition);
        if (!StringUtils.isBlank(dispOptions.micAlgorithm)) {
            mic = CryptoHelper.calculateMIC(data, dispOptions.micAlgorithm);
        }
        return mic;
    }

    /**
     * Converts a collection of Http.Core Headers into a InternetHeader to an array suitable for a message or Message MDN
     *
     * @param in Collection of headers, usually from a MimeBodyPart/Message
     */
    public static InternetHeaders responseHeadersToMimeHeaders(Header[] in) {
        InternetHeaders out = new InternetHeaders();
        for (Header h : in) {
            out.setHeader(h.getName(), h.getValue());
        }
        return out;
    }

    /**
     * Creates a MimeBodyPart from the HttpEntity in the response (used when receiving a reply to a sent file)
     *
     * @throws MessagingException When there is an error creating the body part
     * @throws IOException        When there is an error reading from the stream
     */
    public static MimeBodyPart fromHttpResponse(HttpResponse response) throws MessagingException, IOException {
        // it looks like the only header we care about is the content type, actually, but copy them all anyway
        InternetHeaders headers = responseHeadersToMimeHeaders(response.getAllHeaders());
        byte[] content = EntityUtils.toByteArray(response.getEntity());
        return new MimeBodyPart(headers, content);
    }

    /**
     * Creates a MimeBodyPart from the HttpEntity in the request
     *
     * @throws MessagingException When there is an error creating the body part
     * @throws IOException        When there is an error reading from the stream
     */
    public static MimeBodyPart fromHttpRequest(HttpEntityEnclosingRequest request) throws IOException, MessagingException {
        String ct = getSingleHeader(request, "Content-Type");
        byte[] entityContent = EntityUtils.toByteArray(request.getEntity());
        ByteArrayDataSource dataSource = new ByteArrayDataSource(entityContent, ct);
        MimeBodyPart receivedPart = new MimeBodyPart();
        receivedPart.setDataHandler(new DataHandler(dataSource));
        receivedPart.setHeader("Content-Type", ct);
        return receivedPart;
    }

    private static String getSingleHeader(HttpEntityEnclosingRequest request, String name) throws IOException {
        Header header = request.getFirstHeader(name);
        if (header == null) {
            throw new IOException("Request did not contain a " + name + " header");
        }
        return header.getValue();
    }

    /**
     * Reads the contents of a file into a MimeBodyPart.  Does not set the content type
     */
    public static MimeBodyPart fromFile(Path filePath, String contentType) throws IOException, MessagingException {
        byte[] data = Files.readAllBytes(filePath);
        ByteArrayDataSource dataSource = new ByteArrayDataSource(data, contentType);
        MimeBodyPart outBodyPart = new MimeBodyPart();
        outBodyPart.setDataHandler(new DataHandler(dataSource));
        return outBodyPart;
    }

    /**
     * Creates a body part from the string & content type.  Strips the string of whitespace and adds 2 \r\n at end.
     */
    public static MimeBodyPart textBodyPart(String bodyText) throws MessagingException {
        return textBodyPart(bodyText, "text/plain", null);
    }

    /**
     * Creates a body part from the string & content type.  Strips the string of whitespace and adds 2 \r\n at end.
     */
    public static MimeBodyPart textBodyPart(String bodyText, String contentType, Map<String, String> headers) throws MessagingException {
        MimeBodyPart textPart = new MimeBodyPart();
        StringBuilder bodyContent = new StringBuilder();

        // use the built-in javax.mail way of doing this.  the headers need to be added to the body content
        // directly.. the mimeBodyPart.addHeader() doesn't do it... grr... fucking confusing.  now, it appears
        // that, when you call getInputStream(), it returns the content.  this is then read by input stream which
        // extracts the headers from the body text.  maybe it works when reading, not 100% sure.
        if (headers != null && headers.size() > 0) {
            InternetHeaders x = NetUtil.mapToInternetHeaders(headers);
            Enumeration en = x.getAllHeaderLines();
            while (en.hasMoreElements()) {
                bodyContent.append((String) en.nextElement());
                bodyContent.append(CRLF);
            }
        }

        bodyContent.append(CRLF);
        if (!isBlank(bodyText)) {
            bodyContent.append(StringUtils.stripToEmpty(bodyText));
        }
        textPart.setContent(bodyContent.toString(), contentType);
        textPart.setHeader("Content-Type", contentType);
        return textPart;
    }

    /**
     * Converts a multipart by wrapping it in a new MimeBodyPart.  Copies the content-type from the multipart
     */
    public static MimeBodyPart multiPartToBodyPart(MimeMultipart multiPart) throws MessagingException {
        // Convert report parts to MimeBodyPart
        MimeBodyPart report = new MimeBodyPart();
        report.setContent(multiPart);
        report.setHeader("Content-Type", multiPart.getContentType());
        return report;
    }

}
