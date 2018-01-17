package org.cleanas2.cmd.system

import org.cleanas2.server.ServerSession
import org.cleanas2.cmd.CommandBase
import org.cleanas2.cmd.CommandResult
import org.cleanas2.common.annotation.Command
import org.cleanas2.common.service.AdminDump

import org.cleanas2.util.AS2Util.ofType

/**
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
@Command(name = "status", description = "Shows status for all running modules")
class StatusCommand : CommandBase() {

    @Throws(Exception::class)
    override fun run(vararg params: String): CommandResult {
        val cr = CommandResult()
        val allServices = ServerSession.session!!.getAllServices()
        val adminDumps = ofType(allServices, AdminDump::class.java)

        for (svc in adminDumps) {
            cr.add(svc.javaClass.simpleName)
            try {
                for (line in svc.dumpCurrentStatus()) {
                    cr.add("    " + line)
                }
            } catch (e: Throwable) {
                cr.add("    error getting status: " + e.localizedMessage)
            }

        }

        cr.add("== additional modules ==")
        for (svc in allServices) {
            if (adminDumps.contains(svc)) continue
            cr.add("    " + svc.javaClass.getSimpleName())
        }

        return cr
    }
}
