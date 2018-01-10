package org.cleanas2.cmd;

import org.cleanas2.server.ServerSession;
import org.cleanas2.common.annotation.Command;

import javax.inject.Singleton;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.cleanas2.util.AS2Util.newCaseInsensitiveMap;

/**
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
@Singleton
public class CommandManager {

    public static final String DEFAULT_GROUP = "##default##";
    private final Map<String, CommandMap> groups = newCaseInsensitiveMap();

    public CommandBase createCommandInstance(String groupName, String commandName) {
        groupName = isBlank(groupName) ? DEFAULT_GROUP : groupName;
        if (hasNoCommand(groupName, commandName)) return null;

        Class<? extends CommandBase> x = groups.get(groupName).get(commandName);
        return ServerSession.getSession().getInstance(x);
    }

    public boolean hasNoCommand(String groupName, String commandName) {
        groupName = isBlank(groupName) ? DEFAULT_GROUP : groupName;
        return !hasGroup(groupName) || !getGroup(groupName).containsKey(commandName);
    }

    /**
     * Adds a command to the proper command group, creating the group if it is not found
     */
    public <T extends CommandBase> void registerCommand(Command cmd, Class<T> klass) {
        String group = (isBlank(cmd.group())) ? DEFAULT_GROUP : cmd.group();
        if (!groups.containsKey(group)) {
            groups.put(group, new CommandMap());
        }
        groups.get(group).put(cmd.name(), klass);
    }

    public <T extends CommandBase> void registerCommand(Class<T> klass) {
        Command a = klass.getAnnotation(Command.class);
        if (a != null) {
            registerCommand(a, klass);
        }
    }

    public Map<String, CommandMap> getAllGroups() {
        return groups;
    }

    public boolean hasGroup(String groupName) {
        return groups.containsKey(groupName);
    }

    public CommandMap getGroup(String groupName) {
        return groups.get(groupName);
    }

    /**
     * Internal class for holding a map of commands.  Used to avoid retyping all the java verbosity associated with
     * generic definitions.
     */
    public static class CommandMap extends TreeMap<String, Class<? extends CommandBase>> {
        public CommandMap() {
            super(String.CASE_INSENSITIVE_ORDER);
        }

        public List<Command> getCommandAttributes() {
            List<Command> commands = new ArrayList<>(this.size());
            for (Class cls : this.values()) {
                commands.add((Command) cls.getAnnotation(Command.class));
            }
            return commands;
        }
    }
}
