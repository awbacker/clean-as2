package org.cleanas2.cmd.partner

import org.apache.commons.lang3.StringUtils
import org.boon.Str
import org.cleanas2.cmd.CommandBase
import org.cleanas2.cmd.CommandResult
import org.cleanas2.common.PartnerRecord
import org.cleanas2.common.annotation.Command
import org.cleanas2.service.PartnerService
import org.cleanas2.util.JsonUtil

import javax.inject.Inject

import org.boon.Str.rpad
import org.boon.primitive.Arry.slc

/**
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
@Command(group = "partner", name = "view", description = "view a specific partner by AS2 ID")
class PartnerViewCommand @Inject
constructor(private val partners: PartnerService) : CommandBase() {

    override fun run(vararg params: String): CommandResult {
        val cr = CommandResult()
        if (params.size == 0) {
            cr.add("  !  No partner name supplied. ")
            cr.add("  !  try 'partner-view <name>'")
            return cr
        }

        val p = partners.getPartner(params[0])
        if (p == null) {
            cr.add("  !  No partner exists with AS2 ID '%s", params[0])
            return cr
        }

        val blah = JsonUtil.toPrettyJson(p).trim { it <= ' ' }
        var lines = Str.splitLines(blah)
        for (i in lines.indices) {
            lines[i] = StringUtils.chomp(lines[i])
            lines[i] = StringUtils.chomp(lines[i], ",")
        }

        lines = slc(lines, 1, -1) // remove the {} from first and last line

        cr.add("PARTNER '%s'", params[0])
        cr.add(rpad("", 60, '-'))
        cr.add(lines)

        return cr
    }
}
