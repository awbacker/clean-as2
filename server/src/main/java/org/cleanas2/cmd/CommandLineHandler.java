package org.cleanas2.cmd;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.boon.Str;
import org.cleanas2.server.ServerSession;
import org.cleanas2.common.annotation.Command;
import org.cleanas2.util.Constants;
import org.cleanas2.util.FastClasspathScanner;

import javax.inject.Inject;
import java.io.*;
import java.util.LinkedList;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.boon.Lists.list;
import static org.boon.primitive.Arry.array;

/**
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class CommandLineHandler extends Thread {

    private static final Log logger = LogFactory.getLog(CommandLineHandler.class.getSimpleName());
    private final BufferedReader reader;
    private final BufferedWriter writer;
    private final CommandManager cmdManager;

    @Inject
    public CommandLineHandler(CommandManager manager) {
        cmdManager = manager;
        reader = new BufferedReader(new InputStreamReader(System.in));
        writer = new BufferedWriter(new OutputStreamWriter(System.out));

        // add all the commands! yay!
        FastClasspathScanner fcps = new FastClasspathScanner(array("org.cleanas2.cmd"));
        fcps.matchSubclassesOf(CommandBase.class, new FastClasspathScanner.SubclassMatchProcessor<CommandBase>() {
            @Override
            public void processMatch(Class<? extends CommandBase> matchingClass) {
                cmdManager.registerCommand(matchingClass);
            }
        });
        fcps.scan();
    }

    private class ParsedCommandLine {
        public String group;
        public String command;
        public String[] params;
        public boolean parseError;

        public boolean isDefaultGroup() {
            return group.equals(CommandManager.DEFAULT_GROUP);
        }
    }

    @Override
    public void run() {
        logger.debug("starting command line processor thread");
        writeLn("---------------");
        writeLn("Please enter a a command, or 'help'");
        writeLn("");

        while (!this.isInterrupted()) {
            try {
                write(Constants.APP_NAME + " => ");
                String line = readLn();

                if (isBlank(line)) {
                    continue;
                }

                ParsedCommandLine cl = parseCommandLine(line);
                if (cl.parseError) {
                    continue;
                }

                CommandBase cmd = cmdManager.createCommandInstance(cl.group, cl.command);

                // if we got here, we should have a valid command and set of params, so just run it
                CommandResult r = cmd.run(cl.params);
                for (String s : r.results) {
                    writeLn(StringUtils.chomp(s));
                }

                if (r.terminateService) {
                    ServerSession.getSession().shutdown();
                    break;
                }
                writeLn("");
            } catch (Exception e) {
                writeLn("Error running command: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void helpPrintGroupSubCommands(String groupName) {
        writeLn("Command '%s' requires a sub-command.  Available sub-commands:", groupName);
        // there didn't type the command name, only group name, so show the help
        StringTable st = new StringTable("Name", "Description");
        st.setNoDataMessage("There are no commands registered");
        for (Command c : cmdManager.getGroup(groupName).getCommandAttributes()) {
            st.add(c.name(), c.description());
        }
        writeLn(st.toString());
    }


    private void write(final String line, final Object... args) {
        try {
            writer.write(String.format(line, args));
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeLn(final String line, final Object... args) {
        try {
            writer.write(String.format(line, args));
            writer.write("\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String readLn() throws IOException {
        String line = reader.readLine();
        if (line == null) return null;
        return line.trim();
    }

    public ParsedCommandLine parseCommandLine(String line) {
        ParsedCommandLine cl = new ParsedCommandLine();

        LinkedList<String> parts2 = new LinkedList<>(list(Str.splitBySpace(line)));
        cl.group = parts2.pop();

        // check if the command is part of a group, or the first parameter was a group
        if (cmdManager.hasGroup(cl.group)) {
            if (parts2.size() == 0) {
                helpPrintGroupSubCommands(cl.group);
                cl.parseError = true;
                return cl;
            }
            cl.command = parts2.pop();
        } else {
            cl.command = cl.group;
            cl.group = CommandManager.DEFAULT_GROUP;
        }

        if (cmdManager.hasNoCommand(cl.group, cl.command)) {
            if (cl.isDefaultGroup()) {
                writeLn("'%s' is not a valid command or sub-command.  Type 'help' for a list", cl.command);
            } else {
                writeLn("'%s' is not a valid sub-command for '%s'", cl.command, cl.group);
            }
            cl.parseError = true;
            return cl;
        }

        cl.params = parts2.toArray(new String[parts2.size()]);
        return cl;
    }
}
