package org.cleanas2.bus

import org.cleanas2.message.IncomingFileMessage

import javax.mail.internet.MimeBodyPart
import java.nio.file.Path

/**
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
class SaveIncomingFileMsg(val message: IncomingFileMessage, val fileData: MimeBodyPart) : MessageBase() {
    var fileSavedAs: Path? = null
}
