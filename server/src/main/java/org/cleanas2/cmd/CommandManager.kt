package org.cleanas2.cmd

import org.cleanas2.server.ServerSession
import org.cleanas2.common.annotation.Command

import javax.inject.Singleton
import java.util.*

import org.apache.commons.lang3.StringUtils.isBlank
import org.cleanas2.util.AS2Util.newCaseInsensitiveMap

/**
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
@Singleton
class CommandManager {
    private val groups = newCaseInsensitiveMap<CommandMap>()

    val allGroups: Map<String, CommandMap>
        get() = groups

    fun createCommandInstance(groupName: String, commandName: String): CommandBase? {
        val grp = if (isBlank(groupName)) DEFAULT_GROUP else groupName
        if (hasNoCommand(grp, commandName)) return null

        val x = groups[grp]!![commandName]
        return ServerSession.session!!.getInstance(x as Class<CommandBase>)
    }

    fun hasNoCommand(groupName: String, commandName: String): Boolean {
        val grp = if (isBlank(groupName)) DEFAULT_GROUP else groupName
        return !hasGroup(grp) || !getGroup(grp).containsKey(commandName)
    }

    /**
     * Adds a command to the proper command group, creating the group if it is not found
     */
    fun <T : CommandBase> registerCommand(cmd: Command, klass: Class<T>) {
        val group = if (isBlank(cmd.group)) DEFAULT_GROUP else cmd.group
        if (!groups.containsKey(group)) {
            groups.put(group, CommandMap())
        }
        groups[group]!!.put(cmd.name, klass)
    }

    fun <T : CommandBase> registerCommand(klass: Class<T>) {
        val a = klass.getAnnotation(Command::class.java)
        if (a != null) {
            registerCommand(a, klass)
        }
    }

    fun hasGroup(groupName: String): Boolean {
        return groups.containsKey(groupName)
    }

    fun getGroup(groupName: String): CommandMap {
        return groups[groupName]!!
    }

    /**
     * Internal class for holding a map of commands.  Used to avoid retyping all the java verbosity associated with
     * generic definitions.
     */
    class CommandMap : TreeMap<String, Class<out CommandBase>>(String.CASE_INSENSITIVE_ORDER) {

        val commandAttributes: List<Command>
            get() {
                val commands = ArrayList<Command>(this.size)
                this.values.mapTo(commands) { it.getAnnotation(Command::class.java) as Command }
                return commands
            }
    }

    companion object {

        val DEFAULT_GROUP = "##default##"
    }
}
