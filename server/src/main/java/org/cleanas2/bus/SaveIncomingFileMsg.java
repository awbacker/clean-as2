package org.cleanas2.bus;

import org.cleanas2.message.IncomingFileMessage;

import javax.mail.internet.MimeBodyPart;
import java.nio.file.Path;

/**
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class SaveIncomingFileMsg extends MessageBase {

    public final IncomingFileMessage message;
    public final MimeBodyPart fileData;
    public Path fileSavedAs;

    public SaveIncomingFileMsg(IncomingFileMessage msg, MimeBodyPart data) {
        this.fileData = data;
        this.message = msg;
    }
}
