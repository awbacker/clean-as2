package org.cleanas2.service

import net.engio.mbassy.listener.Handler
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.cleanas2.bus.LoadCertificateMsg
import org.cleanas2.common.service.AdminDump
import org.cleanas2.common.service.ConfigurableService
import org.cleanas2.config.json.JsonConfigMap
import org.cleanas2.util.CryptoFileUtil

import javax.activation.CommandMap
import javax.activation.MailcapCommandMap
import javax.inject.Inject
import javax.inject.Singleton
import java.nio.file.Path
import java.security.*
import java.security.cert.X509Certificate

import org.boon.Lists.list

/**
 * Service for managing and locating certificates and private keys.
 *
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
@Singleton
class CertificateService @Inject
@Throws(Exception::class)
protected constructor(private val config: ServerConfiguration) : ConfigurableService, AdminDump {
    private val keystore: KeyStore

    val allAliases: List<String>
        @Throws(KeyStoreException::class)
        get() = list(keystore.aliases())

    init {
        this.configure()
        keystore = KeyStore.getInstance("PKCS12", "BC")
        keystore.load(null) // init keystore, but with no data
    }

    @Throws(Exception::class)
    private fun configure() {
        if (Security.getProvider("BC") == null) {
            logger.debug("Initializing BC library, adding provider and mail-capabilities")
            Security.addProvider(BouncyCastleProvider())
            val mc = CommandMap.getDefaultCommandMap() as MailcapCommandMap
            mc.addMailcap("application/pkcs7-signature;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.pkcs7_signature")
            mc.addMailcap("application/pkcs7-mime;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.pkcs7_mime")
            mc.addMailcap("application/x-pkcs7-signature;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.x_pkcs7_signature")
            mc.addMailcap("application/x-pkcs7-mime;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.x_pkcs7_mime")
            mc.addMailcap("multipart/signed;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.multipart_signed")

            AccessController.doPrivileged(PrivilegedAction<Any> {
                CommandMap.setDefaultCommandMap(mc)
                null
            })
        }
    }

    @Throws(Exception::class)
    override fun initialize() {
    }

    @Handler
    fun loadCertificate(cert: LoadCertificateMsg) {
        try {
            val fileName = config.getDirectory(SystemDir.Certs).resolve(cert.fileName)
            CryptoFileUtil.loadFile(cert.alias, fileName, cert.password, keystore)
            //CryptoFileUtil.printAliases(keystore);
        } catch (e: Exception) {
            cert.errorCause = e
        }

    }


    @Throws(GeneralSecurityException::class)
    fun getCertificate(as2id: String): X509Certificate {
        try {
            return keystore.getCertificate(as2id) as X509Certificate
        } catch (e: KeyStoreException) {
            throw GeneralSecurityException("No certificate found for '$as2id'", e)
        }

    }

    @Throws(GeneralSecurityException::class)
    fun getPrivateKey(as2id: String): PrivateKey {
        try {
            return keystore.getKey(as2id, "".toCharArray()) as PrivateKey
        } catch (e: KeyStoreException) {
            throw GeneralSecurityException("No private key found for '$as2id'", e)
        } catch (e: NoSuchAlgorithmException) {
            throw GeneralSecurityException("No private key found for '$as2id'", e)
        } catch (e: UnrecoverableKeyException) {
            throw GeneralSecurityException("No private key found for '$as2id'", e)
        }

    }

    fun hasPrivateKey(as2id: String): Boolean {
        try {
            return keystore.getKey(as2id, "".toCharArray()) != null
        } catch (e: Exception) {
            return false
        }

    }

    override fun dumpCurrentStatus(): List<String> {
        try {
            return list(
                    "keystore type = " + keystore.type + " " + keystore.provider.name,
                    "certificates = " + keystore.size()
            )
        } catch (e: Exception) {
            return listOf()
        }

    }

    companion object {

        private val logger = LogFactory.getLog(CertificateService::class.java.simpleName)
    }
}
