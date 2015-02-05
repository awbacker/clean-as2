package org.cleanas2.service;

import net.engio.mbassy.listener.Handler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.cleanas2.bus.LoadCertificateMsg;
import org.cleanas2.common.service.AdminDump;
import org.cleanas2.common.service.ConfigurableService;
import org.cleanas2.config.json.JsonConfigMap;
import org.cleanas2.util.CryptoFileUtil;

import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.List;

import static org.boon.Lists.list;

/**
 * Service for managing and locating certificates and private keys.
 *
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
@Singleton
public class CertificateService implements ConfigurableService, AdminDump {

    private static final Log logger = LogFactory.getLog(CertificateService.class.getSimpleName());
    private final ServerConfiguration config;
    private final KeyStore keystore;

    @Inject
    protected CertificateService(ServerConfiguration config) throws Exception {
        this.config = config;
        this.configure();
        keystore = KeyStore.getInstance("PKCS12", "BC");
        keystore.load(null); // init keystore, but with no data
    }

    private void configure() throws Exception {
        if (Security.getProvider("BC") == null) {
            logger.debug("Initializing BC library, adding provider and mail-capabilities");
            Security.addProvider(new BouncyCastleProvider());
            final MailcapCommandMap mc = (MailcapCommandMap) CommandMap.getDefaultCommandMap();
            mc.addMailcap("application/pkcs7-signature;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.pkcs7_signature");
            mc.addMailcap("application/pkcs7-mime;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.pkcs7_mime");
            mc.addMailcap("application/x-pkcs7-signature;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.x_pkcs7_signature");
            mc.addMailcap("application/x-pkcs7-mime;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.x_pkcs7_mime");
            mc.addMailcap("multipart/signed;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.multipart_signed");

            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    CommandMap.setDefaultCommandMap(mc);
                    return null;
                }
            });
        }
    }

    @Override
    public void initialize() throws Exception {
    }

    @Handler
    public void loadCertificate(LoadCertificateMsg cert) {
        try {
            Path fileName = config.getDirectory(SystemDir.Certs).resolve(cert.fileName);
            CryptoFileUtil.loadFile(cert.alias, fileName, cert.password, keystore);
            //CryptoFileUtil.printAliases(keystore);
        } catch (Exception e) {
            cert.setErrorCause(e);
        }
    }


    public X509Certificate getCertificate(String as2id) throws GeneralSecurityException {
        try {
            return (X509Certificate) keystore.getCertificate(as2id);
        } catch (KeyStoreException e) {
            throw new GeneralSecurityException("No certificate found for '" + as2id + "'", e);
        }
    }

    public PrivateKey getPrivateKey(String as2id) throws GeneralSecurityException {
        try {
            return (PrivateKey) keystore.getKey(as2id, "".toCharArray());
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            throw new GeneralSecurityException("No private key found for '" + as2id + "'", e);
        }
    }

    public boolean hasPrivateKey(String as2id) {
        try {
            return keystore.getKey(as2id, "".toCharArray()) != null;
        } catch (Exception e) {
            return false;
        }
    }

    public List<String> getAllAliases() throws KeyStoreException {
        return list(keystore.aliases());
    }

    @Override
    public List<String> dumpCurrentStatus() {
        try {
            return list(
                    "keystore type = " + keystore.getType() + " " + keystore.getProvider().getName(),
                    "certificates = " + keystore.size()
            );
        } catch (Exception e) {
            return null;
        }
    }
}
