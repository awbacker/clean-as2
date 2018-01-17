package org.cleanas2.cmd

import org.cleanas2.cmd.CommandResult

/**
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
abstract class CommandBase {
    @Throws(Exception::class)
    abstract fun run(vararg params: String): CommandResult
}
