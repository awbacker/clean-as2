package org.cleanas2.cmd;

import java.util.*;

import static org.boon.Lists.list;

/**
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class CommandResult {
    public final List<String> results = new ArrayList<>(5);
    public boolean terminateService = false;

    public void add(String formatString, Object... params) {
        results.add(String.format(formatString, params));
    }

    public void add(String[] lines) {
        results.addAll(list(lines));
    }

    public void add(Collection<? extends String> items) {
        results.addAll(items);
    }
}
