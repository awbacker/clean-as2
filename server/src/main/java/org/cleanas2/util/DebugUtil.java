package org.cleanas2.util;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.boon.Str;

import javax.crypto.SecretKey;
import java.security.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import static org.boon.Lists.list;

/**
 * Class that contains only classes for writing debug information to the log.  These
 * are aids during development, such as dumping a whole object out as javascript, or
 * pretty-printing a map, or writing a temp file (to the CleanAS2 system/temp directory)
 *
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
@SuppressWarnings("unchecked")
public class DebugUtil {

    private static final Log _internalLogger = LogFactory.getLog(DebugUtil.class.getSimpleName());

    public static void debugPrintObject(Log log, String headerText, Object obj) {
        try {
            String allStuff = ((Str.isEmpty(headerText) ? "" : headerText + " : ") + JsonUtil.toPrettyJson(obj));
            for (String line : Str.splitLines(allStuff)) {
                log.debug(StringUtils.stripEnd(line, "\r\n,"));
            }
        } catch (Throwable e) {
            _internalLogger.error("Error printing object to debug", e);
        }
    }

    public static void debugPrintHeaders(Log log, String headerText, Iterable<Header> headers) {
        log.debug(headerText + ": {");
        for (Header h : headers) {
            log.debug(String.format("    %s: %s", h.getName(), h.getValue()));
        }
        log.debug("}");
    }


    public static void debugPrintHeaders(Log log, String headerText, Header[] headers) {
        debugPrintHeaders(log, headerText, list(headers));
    }

    public static void debugPrintHeaders(Log log, String headerText, Enumeration headers) {
        List<Header> newHeaders = new ArrayList<>();
        while (headers.hasMoreElements()) {
            javax.mail.Header badHeader = (javax.mail.Header) headers.nextElement();
            newHeaders.add(new BasicHeader(badHeader.getName(), badHeader.getValue()));
        }
        debugPrintHeaders(log, headerText, newHeaders);
    }

    /**
     * Prints debugging information for a keystore entry associated with an alias.
     * Each alias can have only ONE keystore entry
     */
    public static String describeKeyStoreEntry(KeyStore keyStore, String alias, char[] password) throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableEntryException {
        String entryDescription = "";
        if (keyStore.entryInstanceOf(alias, KeyStore.TrustedCertificateEntry.class)) {
            // check if it is this kind (just a public key, basically).  if you try to get it out with a
            // password, it will fail saying you shouldn't send a password.  but if you try to get the other one
            // out without a password, it will fail.  so no one code branch will work for both cases.
            KeyStore.TrustedCertificateEntry certEntry = (KeyStore.TrustedCertificateEntry) keyStore.getEntry(alias, null);
            java.security.cert.Certificate certificate = certEntry.getTrustedCertificate();
            entryDescription = "TrustedCertificateEntry (type=" + certificate.getType() + ")";
        } else {
            KeyStore.Entry entry = keyStore.getEntry(alias, new KeyStore.PasswordProtection(password));
            if (entry instanceof KeyStore.SecretKeyEntry) {
                KeyStore.SecretKeyEntry skEntry = (KeyStore.SecretKeyEntry) entry;
                SecretKey key = skEntry.getSecretKey();
                entryDescription = "SecretKeyEntry (" + key.getAlgorithm() + ") - alias '" + alias + "'";
            } else if (entry instanceof KeyStore.PrivateKeyEntry) {
                KeyStore.PrivateKeyEntry pkEntry = (KeyStore.PrivateKeyEntry) entry;
                PrivateKey key = pkEntry.getPrivateKey();
                java.security.cert.Certificate certificate = pkEntry.getCertificate();
                entryDescription = String.format("PrivateKeyEntry (alg=%s, type=%s, pub key=%s)",
                        key.getAlgorithm(),
                        certificate.getType(),
                        certificate.getPublicKey().getAlgorithm());
            }
        }
        return entryDescription;
    }
}
