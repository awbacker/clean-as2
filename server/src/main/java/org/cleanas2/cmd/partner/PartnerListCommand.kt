package org.cleanas2.cmd.partner

import org.cleanas2.cmd.*
import org.cleanas2.common.PartnerRecord
import org.cleanas2.common.annotation.Command
import org.cleanas2.service.PartnerService

import javax.inject.Inject

/**
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
@Command(group = "partner", name = "list", description = "Lists all partners")
class PartnerListCommand @Inject
constructor(private val partners: PartnerService) : CommandBase() {

    override fun run(vararg params: String): CommandResult {
        val t = StringTable("Name", "AS2 ID", "Cert File", "MDN", "URL")
        t.setNoDataMessage("No partner records found")
        for (p in partners.allPartners!!) {
            t.add(p.name!!, p.as2id!!, p.certificate ?: "null", p.sendSettings.mdnMode, p.sendSettings.url!!)
        }

        val cr = CommandResult()
        cr.results.addAll(t.toList())
        return cr
    }
}
