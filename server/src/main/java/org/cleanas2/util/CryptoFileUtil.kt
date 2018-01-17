package org.cleanas2.util

import org.apache.commons.io.FilenameUtils
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.security.*
import java.security.cert.*

import org.boon.Lists.list
import org.boon.sort.Sorting.sort

/**
 * Contains helper methods for dealing with crypto files such as p12 and cert/crt.
 *
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
object CryptoFileUtil {

    private val logger = LogFactory.getLog(CryptoFileUtil::class.java)

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
    @Throws(Exception::class)
    fun loadFile(alias: String, file: Path, password: String, keystore: KeyStore) {
        logger.debug("reading: (" + alias + ") " + file.fileName)
        when (FilenameUtils.getExtension(file.toString())) {
            "p12" -> mergeP12FileIntoKeystore(file.toFile(), password.toCharArray(), keystore, "test".toCharArray(), alias)
            "cert", "crt", "cer" -> {
                val cert = readCertificateFile(file)
                if (cert != null) {
                    //logger.debug("setting X509 cert for alias: " + alias);
                    keystore.setCertificateEntry(alias, cert)
                }
            }
            else -> logger.error("We don't know how to load files of type :" + FilenameUtils.getExtension(file.toString()))
        }
    }


    /**
     * Merges a P12 file into an existing keystore by copying the entries.  This copying
     * changes the protection password.  A password is required (i think) for p12 files
     *
     *
     * Requires "BC" provider
     *
     * @param p12file          File entry that is a .p12 file
     * @param filePassword     Password for the P12 file _and_ the entries inside
     * @param outKs            #KeyStore object to copy the certificates/keys into
     * @param keystorePassword Password for the keystore
     * @param newAlias         The alias to use as an override for the one in the file (e.g. CycloneAS2 generates a guid style alias, etc)
     */
    @Throws(KeyStoreException::class, IOException::class, NoSuchAlgorithmException::class, CertificateException::class, UnrecoverableEntryException::class, NoSuchProviderException::class)
    private fun mergeP12FileIntoKeystore(p12file: File, filePassword: CharArray, outKs: KeyStore, keystorePassword: CharArray, newAlias: String) {

        val tempKeystore = KeyStore.getInstance("PKCS12", "BC")

        FileInputStream(p12file).use { fs -> tempKeystore.load(fs, filePassword) }

        for (alias in list(tempKeystore.aliases())) {
            val entry: KeyStore.Entry
            if (tempKeystore.entryInstanceOf(alias, KeyStore.TrustedCertificateEntry::class.java)) {
                entry = tempKeystore.getEntry(alias, null)
                outKs.setEntry(newAlias, entry, null)
            } else {
                entry = tempKeystore.getEntry(alias, KeyStore.PasswordProtection(filePassword))
                outKs.setEntry(newAlias, entry, KeyStore.PasswordProtection(keystorePassword))
            }
            //logger.info(String.format("  alias '%s' : %s", alias, DebugLogUtil.describeKeyStoreEntry(tempKeystore, alias.trim(), filePassword)));
        }
    }

    /**
     * Uses the bouncy castle provider to read either a DER or PEM encoded certificate.  There is no need ot use a
     * PEM reader if the file is not PEM, since this handles both cases perfectly well, and we don't need to worry
     * about the output format.
     */
    @Throws(Exception::class)
    private fun readCertificateFile(keyFile: Path): X509Certificate? {
        val factory = CertificateFactory.getInstance("X.509", "BC")
        return factory.generateCertificate(Files.newInputStream(keyFile)) as X509Certificate
    }

    //TODO: Add password usage? If not, then delete the password parameter

    private fun aliasHasCertificate(ks: KeyStore, alias: String): Boolean {
        try {
            val cert = ks.getCertificate(alias)
            return cert.publicKey != null
        } catch (ignored: KeyStoreException) {
            return false
        }

    }

    private fun aliasHasPrivateKey(ks: KeyStore, alias: String): Boolean {
        try {
            val o = ks.getKey(alias, "test".toCharArray())
            return o != null
        } catch (ignored: Exception) {
            return false
        }

    }

    @Throws(KeyStoreException::class)
    fun printAliases(ks: KeyStore) {
        logger.info("  aliases:")
        val aliases = list(ks.aliases())
        sort(aliases)
        for (alias in aliases) {
            logger.info(String.format("    %-15s (%s, %s)",
                    alias,
                    if (CryptoFileUtil.aliasHasPrivateKey(ks, alias)) "PK" else "  ",
                    if (CryptoFileUtil.aliasHasCertificate(ks, alias)) "CERT" else "")
            )
        }
    }

}
