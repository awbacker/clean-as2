package org.cleanas2.cmd.system;

import org.cleanas2.server.ServerSession;
import org.cleanas2.cmd.CommandBase;
import org.cleanas2.cmd.CommandResult;
import org.cleanas2.common.annotation.Command;
import org.cleanas2.common.service.AdminDump;

import java.util.List;

import static org.cleanas2.util.AS2Util.ofType;

/**
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
@Command(name = "status", description = "Shows status for all running modules")
public class StatusCommand extends CommandBase {

    @Override
    public CommandResult run(String... params) throws Exception {
        CommandResult cr = new CommandResult();
        List<Object> allServices = ServerSession.getSession().getAllServices();
        List<AdminDump> adminDumps = ofType(allServices, AdminDump.class);

        for (AdminDump svc : adminDumps) {
            cr.add(svc.getClass().getSimpleName());
            try {
                for (String line : svc.dumpCurrentStatus()) {
                    cr.add("    " + line);
                }
            } catch (Throwable e) {
                cr.add("    error getting status: " + e.getLocalizedMessage());
            }
        }

        cr.add("== additional modules ==");
        for (Object svc : allServices) {
            if (adminDumps.contains(svc)) continue;
            cr.add("    " + svc.getClass().getSimpleName());
        }

        return cr;
    }
}
