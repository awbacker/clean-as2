package org.cleanas2.cmd.cert

import org.cleanas2.cmd.*
import org.cleanas2.common.annotation.Command
import org.cleanas2.service.CertificateService

import javax.inject.Inject
import java.security.cert.X509Certificate

/**
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
@Command(group = "cert", name = "list", description = "List all certificates in the system")
class CertListCommand @Inject
constructor(private val certs: CertificateService) : CommandBase() {

    @Throws(Exception::class)
    override fun run(vararg params: String): CommandResult {
        val cr = CommandResult()
        val st = StringTable("Alias", "Has PK", "Cert Type", "Serial", "X509 Version")
        st.setNoDataMessage("No certificates were found")
        for (alias in certs.allAliases) {
            val cert = certs.getCertificate(alias)
            st.add(
                    alias,
                    if (certs.hasPrivateKey(alias)) "Yes" else "",
                    cert.javaClass.simpleName,
                    cert.serialNumber,
                    cert.version
            )
        }
        cr.results.addAll(st.toList())
        return cr
    }
}
