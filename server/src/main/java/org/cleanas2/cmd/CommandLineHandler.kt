package org.cleanas2.cmd

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.StringUtils.isBlank
import org.apache.commons.logging.LogFactory
import org.boon.Lists.list
import org.boon.Str
import org.boon.primitive.Arry.array
import org.cleanas2.server.ServerSession
import org.cleanas2.util.Constants
import org.cleanas2.util.FastClasspathScanner
import java.io.*
import java.util.*
import javax.inject.Inject

/**
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
class CommandLineHandler @Inject
constructor(private val cmdManager: CommandManager) : Thread() {
    private val reader: BufferedReader = BufferedReader(InputStreamReader(System.`in`))
    private val writer: BufferedWriter = BufferedWriter(OutputStreamWriter(System.out))

    init {
        // add all the commands! yay!
        val fcps = FastClasspathScanner(array("org.cleanas2.cmd"))

        val onMatch = object : FastClasspathScanner.SubclassMatchProcessor<CommandBase> {
            override fun processMatch(matchingClass: Class<out CommandBase>) {
                cmdManager.registerCommand(matchingClass)
            }
        }

        fcps.matchSubclassesOf(CommandBase::class.java, onMatch)
        fcps.scan()
    }

    private inner class ParsedCommandLine {
        var group: String = ""
        var command: String = ""
        var params: Array<String> = arrayOf()
        var parseError: Boolean = false

        val isDefaultGroup: Boolean
            get() = group == CommandManager.DEFAULT_GROUP
    }

    override fun run() {
        logger.debug("starting command line processor thread")
        writeLn("---------------")
        writeLn("Please enter a a command, or 'help'")
        writeLn("")

        while (!this.isInterrupted) {
            try {
                write(Constants.APP_NAME + " => ")
                val line = readLn()

                if (isBlank(line)) {
                    continue
                }

                val cl = parseCommandLine(line)
                if (cl.parseError) {
                    continue
                }

                val cmd = cmdManager.createCommandInstance(cl.group, cl.command)

                // if we got here, we should have a valid command and set of params, so just run it
                val r = cmd!!.run(*cl.params)
                for (s in r.results) {
                    writeLn(StringUtils.chomp(s))
                }

                if (r.terminateService) {
                    ServerSession.session!!.shutdown()
                    break
                }
                writeLn("")
            } catch (e: Exception) {
                writeLn("Error running command: " + e.message)
                e.printStackTrace()
            }

        }
    }

    private fun helpPrintGroupSubCommands(groupName: String) {
        writeLn("Command '%s' requires a sub-command.  Available sub-commands:", groupName)
        // there didn't type the command name, only group name, so show the help
        val st = StringTable("Name", "Description")
        st.setNoDataMessage("There are no commands registered")
        for (c in cmdManager.getGroup(groupName).commandAttributes) {
            st.add(c.name, c.description)
        }
        writeLn(st.toString())
    }


    private fun write(line: String, vararg args: Any) {
        try {
            writer.write(String.format(line, *args))
            writer.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    private fun writeLn(line: String, vararg args: Any) {
        try {
            writer.write(String.format(line, *args))
            writer.write("\n")
            writer.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    @Throws(IOException::class)
    private fun readLn(): String {
        val line = reader.readLine() ?: return ""
        return line.trim { it <= ' ' }
    }

    private fun parseCommandLine(line: String): ParsedCommandLine {
        val cl = ParsedCommandLine()

        val parts2 = LinkedList(list(*Str.splitBySpace(line)))
        cl.group = parts2.pop()

        // check if the command is part of a group, or the first parameter was a group
        if (cmdManager.hasGroup(cl.group)) {
            if (parts2.size == 0) {
                helpPrintGroupSubCommands(cl.group)
                cl.parseError = true
                return cl
            }
            cl.command = parts2.pop()
        } else {
            cl.command = cl.group
            cl.group = CommandManager.DEFAULT_GROUP
        }

        if (cmdManager.hasNoCommand(cl.group, cl.command)) {
            if (cl.isDefaultGroup) {
                writeLn("'%s' is not a valid command or sub-command.  Type 'help' for a list", cl.command)
            } else {
                writeLn("'%s' is not a valid sub-command for '%s'", cl.command, cl.group)
            }
            cl.parseError = true
            return cl
        }

        cl.params = parts2.toTypedArray()
        return cl
    }

    companion object {

        private val logger = LogFactory.getLog(CommandLineHandler::class.java.simpleName)
    }
}
