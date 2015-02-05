package org.cleanas2.cmd.partner;

import org.cleanas2.cmd.*;
import org.cleanas2.common.PartnerRecord;
import org.cleanas2.common.annotation.Command;
import org.cleanas2.service.PartnerService;

import javax.inject.Inject;

/**
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
@Command(group = "partner", name = "list", description = "Lists all partners")
public class PartnerListCommand extends CommandBase {

    private final PartnerService partners;

    @Inject
    public PartnerListCommand(PartnerService partners) {
        this.partners = partners;
    }

    @Override
    public CommandResult run(String... params) {
        StringTable t = new StringTable("Name", "AS2 ID", "Cert File", "MDN", "URL");
        t.setNoDataMessage("No partner records found");
        for (PartnerRecord p : partners.getAllPartners()) {
            t.add(p.name, p.as2id, p.certificate, p.sendSettings.mdnMode, p.sendSettings.url);
        }

        CommandResult cr = new CommandResult();
        cr.results.addAll(t.toList());
        return cr;
    }
}
