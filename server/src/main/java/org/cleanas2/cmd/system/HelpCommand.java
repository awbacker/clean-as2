package org.cleanas2.cmd.system;

import org.boon.Lists;
import org.boon.core.Function;
import org.cleanas2.cmd.*;
import org.cleanas2.common.annotation.Command;

import javax.inject.Inject;
import java.util.List;

import static org.boon.Str.joinCollection;

/**
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
@Command(name = "help", description = "Prints this help message")
public class HelpCommand extends CommandBase {

    private final CommandManager mgr;

    @Inject
    public HelpCommand(CommandManager mgr) {
        this.mgr = mgr;
    }

    @Override
    public CommandResult run(String... params) {
        CommandResult cr = new CommandResult();
        cr.results.add("Available Commands: ");
        cr.results.add("----------------------------------------------------------");
        StringTable st = new StringTable("Command", "Description");

        // add the default groups
        if (mgr.hasGroup(CommandManager.DEFAULT_GROUP)) {
            for (Command cmd : mgr.getGroup(CommandManager.DEFAULT_GROUP).getCommandAttributes()) {
                st.add(cmd.name(), cmd.description());
            }
        }

        // add the non-default groups, like partner
        for (String groupName : mgr.getAllGroups().keySet()) {
            if (groupName.equals(CommandManager.DEFAULT_GROUP)) continue;

            CommandManager.CommandMap map = mgr.getGroup(groupName);
            List<String> names = Lists.mapBy(map.getCommandAttributes(), new Function<Command, String>() {
                @Override
                public String apply(Command command) {
                    return command.name();
                }
            });
            st.add(groupName, "Sub-commands: " + joinCollection(',', names));
        }

        cr.add(st.toList());
        return cr;
    }
}
