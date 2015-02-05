package org.cleanas2.cmd.cert;

import org.cleanas2.cmd.*;
import org.cleanas2.common.annotation.Command;
import org.cleanas2.service.CertificateService;

import javax.inject.Inject;
import java.security.cert.X509Certificate;

/**
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
@Command(group = "cert", name = "list", description = "List all certificates in the system")
public class CertListCommand extends CommandBase {

    private final CertificateService certs;

    @Inject
    public CertListCommand(CertificateService certs) {
        this.certs = certs;
    }

    @Override
    public CommandResult run(String... params) throws Exception {
        CommandResult cr = new CommandResult();
        StringTable st = new StringTable("Alias", "Has PK", "Cert Type", "Serial", "X509 Version");
        st.setNoDataMessage("No certificates were found");
        for (String alias : certs.getAllAliases()) {
            X509Certificate cert = certs.getCertificate(alias);
            st.add(
                    alias,
                    certs.hasPrivateKey(alias) ? "Yes" : "",
                    cert.getClass().getSimpleName(),
                    cert.getSerialNumber(),
                    cert.getVersion()
            );
        }
        cr.results.addAll(st.toList());
        return cr;
    }
}
