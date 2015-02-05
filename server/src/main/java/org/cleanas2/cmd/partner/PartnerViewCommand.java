package org.cleanas2.cmd.partner;

import org.apache.commons.lang.StringUtils;
import org.boon.Str;
import org.cleanas2.cmd.CommandBase;
import org.cleanas2.cmd.CommandResult;
import org.cleanas2.common.PartnerRecord;
import org.cleanas2.common.annotation.Command;
import org.cleanas2.service.PartnerService;
import org.cleanas2.util.JsonUtil;

import javax.inject.Inject;

import static org.boon.Str.rpad;
import static org.boon.primitive.Arry.slc;

/**
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
@Command(group = "partner", name = "view", description = "view a specific partner by AS2 ID")
public class PartnerViewCommand extends CommandBase {

    private final PartnerService partners;

    @Inject
    public PartnerViewCommand(PartnerService partners) {
        this.partners = partners;
    }

    @Override
    public CommandResult run(String... params) {
        CommandResult cr = new CommandResult();
        if (params.length == 0) {
            cr.add("  !  No partner name supplied. ");
            cr.add("  !  try 'partner-view <name>'");
            return cr;
        }

        PartnerRecord p = partners.getPartner(params[0]);
        if (p == null) {
            cr.add("  !  No partner exists with AS2 ID '%s", params[0]);
            return cr;
        }

        String blah = JsonUtil.toPrettyJson(p).trim();
        String[] lines = Str.splitLines(blah);
        for (int i = 0; i < lines.length; i++) {
            lines[i] = StringUtils.chomp(lines[i]);
            lines[i] = StringUtils.chomp(lines[i], ",");
        }

        lines = slc(lines, 1, -1); // remove the {} from first and last line

        cr.add("PARTNER '%s'", params[0]);
        cr.add(rpad("", 60, '-'));
        cr.add(lines);

        return cr;
    }
}
