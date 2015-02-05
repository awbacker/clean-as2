package org.cleanas2.bus;

import java.nio.file.Path;

/**
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class ResendFileMsg {
    public final Path fileToResend;

    public ResendFileMsg(Path filepath) {
        fileToResend = filepath;
    }
}
