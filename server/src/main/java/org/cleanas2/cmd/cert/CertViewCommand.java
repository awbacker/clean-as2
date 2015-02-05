package org.cleanas2.cmd.cert;

import org.boon.Str;
import org.cleanas2.cmd.CommandBase;
import org.cleanas2.cmd.CommandResult;
import org.cleanas2.common.annotation.Command;
import org.cleanas2.service.CertificateService;

import javax.inject.Inject;
import java.security.cert.X509Certificate;

/**
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
@Command(group = "cert", name = "view", description = "View a single certificate by passing in the AS2 ID")
public class CertViewCommand extends CommandBase {

    private final CertificateService certs;

    @Inject
    public CertViewCommand(CertificateService certs) {
        this.certs = certs;
    }

    @Override
    public CommandResult run(String... params) throws Exception {
        CommandResult cr = new CommandResult();
        if (params.length == 0) {
            cr.add("  !  No AS2 ID supplied supplied. ");
            cr.add("  !  try 'cert view <as2-id>'");
            return cr;
        }

        X509Certificate cert = certs.getCertificate(params[0]);
        if (cert == null) {
            cr.add("  !  No certificate found for AS2 ID: " + params[0]);
            return cr;
        }

        cr.add(Str.splitLines(cert.toString()));
        return cr;
    }

}
