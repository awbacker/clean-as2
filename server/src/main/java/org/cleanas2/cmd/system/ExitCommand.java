package org.cleanas2.cmd.system;

import org.cleanas2.cmd.CommandBase;
import org.cleanas2.cmd.CommandResult;
import org.cleanas2.common.annotation.Command;

/**
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
@Command(name = "exit", description = "Exits the AS2 Server")
public class ExitCommand extends CommandBase {
    @Override
    public CommandResult run(String... params) {
        CommandResult cr = new CommandResult();
        cr.terminateService = true;
        return cr;
    }
}
