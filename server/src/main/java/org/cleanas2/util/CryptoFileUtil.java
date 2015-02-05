package org.cleanas2.util;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.*;
import java.util.List;

import static org.boon.Lists.list;
import static org.boon.sort.Sorting.sort;

/**
 * Contains helper methods for dealing with crypto files such as p12 and cert/crt.
 *
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class CryptoFileUtil {

    private static final Log logger = LogFactory.getLog(CryptoFileUtil.class);

    /**
     * Attempts to load a file based on the file extension.  Password is required, but may not be used
     * if there is no need for it.  In those cases, just pass "".  A password is not needed for reading
     * a private key out of the keystore once it has been loaded from the file.
     *
     * @param alias    The alias to assign to the cert/key/etc read in from the file
     * @param file     Path the the file to read
     * @param password Password for the file (optional)
     * @param keystore Keystore to save the crypto object to.  Stored private keys will have a password of "test"
     * @throws Exception
     */
    public static void loadFile(String alias, Path file, String password, KeyStore keystore) throws Exception {
        logger.debug("reading: (" + alias + ") " + file.getFileName());
        switch (FilenameUtils.getExtension(file.toString())) {
            case "p12":
                mergeP12FileIntoKeystore(file.toFile(), password.toCharArray(), keystore, "test".toCharArray(), alias);
                break;
            case "cert":
            case "crt":
            case "cer":
                X509Certificate cert = readCertificateFile(file);
                if (cert != null) {
                    //logger.debug("setting X509 cert for alias: " + alias);
                    keystore.setCertificateEntry(alias, cert);
                }
                break;
            default:
                logger.error("We don't know how to load files of type :" + FilenameUtils.getExtension(file.toString()));
        }
    }


    /**
     * Merges a P12 file into an existing keystore by copying the entries.  This copying
     * changes the protection password.  A password is required (i think) for p12 files
     * <p/>
     * Requires "BC" provider
     *
     * @param p12file          File entry that is a .p12 file
     * @param filePassword     Password for the P12 file _and_ the entries inside
     * @param outKs            #KeyStore object to copy the certificates/keys into
     * @param keystorePassword Password for the keystore
     * @param newAlias         The alias to use as an override for the one in the file (e.g. CycloneAS2 generates a guid style alias, etc)
     */
    private static void mergeP12FileIntoKeystore(File p12file, char[] filePassword, KeyStore outKs, char[] keystorePassword, String newAlias)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableEntryException, NoSuchProviderException {

        KeyStore tempKeystore = KeyStore.getInstance("PKCS12", "BC");

        try (InputStream fs = new FileInputStream(p12file)) {
            tempKeystore.load(fs, filePassword);
        }

        for (String alias : list(tempKeystore.aliases())) {
            KeyStore.Entry entry;
            if (tempKeystore.entryInstanceOf(alias, KeyStore.TrustedCertificateEntry.class)) {
                entry = tempKeystore.getEntry(alias, null);
                outKs.setEntry(newAlias, entry, null);
            } else {
                entry = tempKeystore.getEntry(alias, new KeyStore.PasswordProtection(filePassword));
                outKs.setEntry(newAlias, entry, new KeyStore.PasswordProtection(keystorePassword));
            }
            //logger.info(String.format("  alias '%s' : %s", alias, DebugLogUtil.describeKeyStoreEntry(tempKeystore, alias.trim(), filePassword)));
        }
    }

    /**
     * Uses the bouncy castle provider to read either a DER or PEM encoded certificate.  There is no need ot use a
     * PEM reader if the file is not PEM, since this handles both cases perfectly well, and we don't need to worry
     * about the output format.
     */
    private static X509Certificate readCertificateFile(Path keyFile) throws Exception {
        CertificateFactory factory = CertificateFactory.getInstance("X.509", "BC");
        return (X509Certificate) factory.generateCertificate(Files.newInputStream(keyFile));
    }

    //TODO: Add password usage? If not, then delete the password parameter

    private static boolean aliasHasCertificate(KeyStore ks, String alias) {
        try {
            java.security.cert.Certificate cert = ks.getCertificate(alias);
            return cert.getPublicKey() != null;
        } catch (KeyStoreException ignored) {
            return false;
        }
    }

    private static boolean aliasHasPrivateKey(KeyStore ks, String alias) {
        try {
            Object o = ks.getKey(alias, "test".toCharArray());
            return o != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public static void printAliases(KeyStore ks) throws KeyStoreException {
        logger.info("  aliases:");
        List<String> aliases = list(ks.aliases());
        sort(aliases);
        for (String alias : aliases) {
            logger.info(String.format("    %-15s (%s, %s)",
                            alias,
                            CryptoFileUtil.aliasHasPrivateKey(ks, alias) ? "PK" : "  ",
                            CryptoFileUtil.aliasHasCertificate(ks, alias) ? "CERT" : "")
            );
        }
    }

}
