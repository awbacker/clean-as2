package org.cleanas2.cmd.system

import org.cleanas2.cmd.CommandBase
import org.cleanas2.cmd.CommandResult
import org.cleanas2.common.annotation.Command

/**
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
@Command(name = "exit", description = "Exits the AS2 Server")
class ExitCommand : CommandBase() {
    override fun run(vararg params: String): CommandResult {
        val cr = CommandResult()
        cr.terminateService = true
        return cr
    }
}
