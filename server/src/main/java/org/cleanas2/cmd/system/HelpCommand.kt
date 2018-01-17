package org.cleanas2.cmd.system

import org.boon.Lists
import org.boon.core.Function
import org.cleanas2.cmd.*
import org.cleanas2.common.annotation.Command

import javax.inject.Inject

import org.boon.Str.joinCollection

/**
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
@Command(name = "help", description = "Prints this help message")
class HelpCommand @Inject
constructor(private val mgr: CommandManager) : CommandBase() {

    override fun run(vararg params: String): CommandResult {
        val cr = CommandResult()
        cr.results.add("Available Commands: ")
        cr.results.add("----------------------------------------------------------")
        val st = StringTable("Command", "Description")

        // add the default groups
        if (mgr.hasGroup(CommandManager.DEFAULT_GROUP)) {
            for (cmd in mgr.getGroup(CommandManager.DEFAULT_GROUP).commandAttributes) {
                st.add(cmd.name, cmd.description)
            }
        }

        // add the non-default groups, like partner
        for (groupName in mgr.allGroups.keys) {
            if (groupName == CommandManager.DEFAULT_GROUP) continue

            val map = mgr.getGroup(groupName)
            val names = Lists.mapBy(map.commandAttributes, Function<Command, String> { command -> command.name })
            st.add(groupName, "Sub-commands: " + joinCollection(',', names))
        }

        cr.add(st.toList())
        return cr
    }
}
