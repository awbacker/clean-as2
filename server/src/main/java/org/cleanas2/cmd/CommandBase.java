package org.cleanas2.cmd;

import org.cleanas2.cmd.CommandResult;

/**
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public abstract class CommandBase {
    public abstract CommandResult run(String... params) throws Exception;
}
