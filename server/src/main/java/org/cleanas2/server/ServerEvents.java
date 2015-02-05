package org.cleanas2.server;

//import com.powere2e.platform.log.enumeration.EventLevel;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cleanas2.bus.ServerEventMsg;
import org.cleanas2.common.serverEvent.EventLevel;
import org.cleanas2.common.serverEvent.Phase;
import org.cleanas2.message.OutgoingFileMessage;
import org.cleanas2.service.ServerConfiguration;


/**
 * Helper class for writing PowerE2E logs using the PowerE2E log connector.  These log statements are
 * quite long, with many parameters, and tend to dirty up the real code and need LOTS of helpers
 */
@SuppressWarnings("UnusedDeclaration")
public class ServerEvents {

    private static final boolean printException = false;
    private static final Log logger = LogFactory.getLog(ServerEvents.class.getSimpleName());
    private static ServerConfiguration configuration;

    public static void heartbeat(boolean isBusy) {

//        CustomerType ct = CustomerType.valueOf(getConfiguration().getCompanyType());

//        HeartBeatTool.getInstance().appendInfo(
//                getConfiguration().getCompanyId(),
//                getConfiguration().getCompanyName(),
//                ct,
//                "LOC",
//                Plugable.PluginType.SD,
//                "",
//                "传输端发送",
//                "AS2",
//                isBusy);
    }

    public static void Info(Phase phase, String messageId, String message) {
        ServerEventMsg msg = new ServerEventMsg(phase, EventLevel.Info, message, message);
        MessageBus.publishAsync(msg);
    }

    public static void Error(Phase phase, String messageId, String message) {
        ServerEventMsg msg = new ServerEventMsg(phase, EventLevel.Info, message, message);
        MessageBus.publishAsync(msg);
    }

    public static void Error(Phase phase, String messageId, String message, Throwable error) {
        ServerEventMsg msg = new ServerEventMsg(phase, EventLevel.Error, message, message, error);
        MessageBus.publishAsync(msg);
    }


    /* MDN RECEIVE LOGS */
    public static void mdnReceiveStart(OutgoingFileMessage msg) {
        ServerEvents.doLogMessageInfo(EventLevel.Info, Phase.MDN_RECEIVE, msg, null, "MDN Receive - Start");
    }

    public static void mdnReceiveEnd(OutgoingFileMessage msg) {
        ServerEvents.doLogMessageInfo(EventLevel.Info, Phase.MDN_RECEIVE, msg, null, "MDN Receive - End");
    }

    public static void mdnReceiveInfo(OutgoingFileMessage msg, String message) {
        ServerEvents.doLogMessageInfo(EventLevel.Info, Phase.MDN_RECEIVE, msg, null, "MDN Receive - " + message);
    }

    public static void mdnReceiveError(OutgoingFileMessage msg, Exception exception) {
        ServerEvents.mdnReceiveError(msg, exception, "No custom message provided");
    }

    public static void mdnReceiveError(OutgoingFileMessage msg, Exception exception, String message) {
        ServerEvents.doLogMessageInfo(EventLevel.Error, Phase.MDN_RECEIVE, msg, exception, "MDN Receive - Error - " + message);
    }

    /* ASYNC MDN RECEIVE LOGS */

    public static void mdnAsyncReceiveStart(OutgoingFileMessage msg) {
        ServerEvents.doLogMessageInfo(EventLevel.Info, Phase.ASYNC_MDN_RECEIVE, msg, null, "ASYNC MDN Receive - Start");
    }

    public static void mdnAsyncReceiveEnd(OutgoingFileMessage msg) {
        doLogMessageInfo(EventLevel.Info, Phase.ASYNC_MDN_RECEIVE, msg, null, "ASYNC MDN Receive - End");
    }

    public static void mdnAsyncReceiveInfo(OutgoingFileMessage msg, String message) {
        doLogMessageInfo(EventLevel.Info, Phase.ASYNC_MDN_RECEIVE, msg, null, "ASYNC MDN Receive - " + message);
    }

    public static void mdnAsyncReceiveError(OutgoingFileMessage msg, Exception exception) {
        mdnReceiveError(msg, exception, "No custom message provided");
    }

    public static void mdnAsyncReceiveError(OutgoingFileMessage msg, Exception exception, String message) {
        doLogMessageInfo(EventLevel.Error, Phase.ASYNC_MDN_RECEIVE, msg, exception, message);
    }

    /* MDN SEND LOGS */

    public static void mdnSendStart(OutgoingFileMessage msg) {
        ServerEvents.doLogMessageInfo(EventLevel.Info, Phase.MDN_SEND, msg, null, "MDN Send - Start");
    }

    public static void mdnSendEnd(OutgoingFileMessage msg) {
        ServerEvents.doLogMessageInfo(EventLevel.Info, Phase.MDN_SEND, msg, null, "MDN Send - End");
    }

    public static void mdnSendInfo(OutgoingFileMessage msg, String message) {
        ServerEvents.doLogMessageInfo(EventLevel.Info, Phase.MDN_SEND, msg, null, "MDN Send - " + message);
    }

    public static void mdnSendError(OutgoingFileMessage msg, Exception exception, String message) {
        ServerEvents.doLogMessageInfo(EventLevel.Error, Phase.MDN_SEND, msg, exception, "MDN Send - Error - " + message);
    }

    /* ASYNC MDN SEND LOGS */

    public static void mdnAsyncSendStart(OutgoingFileMessage msg) {
        ServerEvents.doLogMessageInfo(EventLevel.Info, Phase.ASYNC_MDN_SEND, msg, null, "ASYNC MDN Send - Start");
    }

    public static void mdnAsyncSendEnd(OutgoingFileMessage msg) {
        ServerEvents.doLogMessageInfo(EventLevel.Info, Phase.ASYNC_MDN_SEND, msg, null, "ASYNC MDN Send - End");
    }

    public static void mdnAsyncSendInfo(OutgoingFileMessage msg, String message) {
        ServerEvents.doLogMessageInfo(EventLevel.Info, Phase.ASYNC_MDN_SEND, msg, null, "ASYNC MDN Send - " + message);
    }

    public static void mdnAsyncSendError(OutgoingFileMessage msg, Exception exception, String message) {
        ServerEvents.doLogMessageInfo(EventLevel.Error, Phase.ASYNC_MDN_SEND, msg, exception, "ASYNC MDN Send - Error - " + message);
    }


    /* UTILITY FUNCTIONS */

    private static void doLogMessageInfo(EventLevel level, Phase phase, OutgoingFileMessage as2message, String message) {
        ServerEvents.doLogMessageInfo(level, phase, as2message, null, message);
    }

    private static void doLogMessageInfo(EventLevel level, Phase phase, OutgoingFileMessage as2message, Exception exception, String message) {
        if (exception != null && printException) {
            logger.error(message + "[" + getFilenameFromMessage(as2message) + "] - " + as2message.getLoggingText(), exception);
        } else {
            logger.info(message + "[" + getFilenameFromMessage(as2message) + "] - " + as2message.getLoggingText());
        }

//        CustomerType ct = CustomerType.valueOf(getConfiguration().getCompanyType());

//        LogUtil.getInstance().appendTransLog(
//                getConfiguration().getCompanyId(),
//                getConfiguration().getCompanyName(),
//                ct,
//                eventLevel,
//                getFilePathFromMessage(as2message),
//                "",
//                phase.name(),
//                exception,
//                message
//        );
    }

    /**
     * Get the message attribute MA_FILENAME (not http header) containing the file name
     *
     * @return Filename, or null if not present
     */
    private static String getFilenameFromMessage(OutgoingFileMessage msg) {
        return msg.filePath.getFileName().toString();
    }

    /**
     * Gets the full path without the file name
     */
    private static String getFilePathFromMessage(OutgoingFileMessage msg) {
        return msg.filePath.getParent().toString();
    }

    /**
     * Gets only the filename, removing the extension.  This is used as the "key" in
     */
    private static String getFilenameWithoutExtension(OutgoingFileMessage msg) {
        // prefer the real filename from the message.  this is present when we are _sending_
        if (getFilenameFromMessage(msg) != null) return FilenameUtils.removeExtension(getFilenameFromMessage(msg));
        //String httpMessageId = msg.getHeader("Message-ID");

        if (msg.messageId != null) return msg.messageId;
        return "no-filename-found";
    }

    /**
     * Gets the name of this process, for reporting to the LOG service.  The heartbeat service uses
     * a different value
     */
    private static String getProcessName() {
        return "AS2";
    }

    public static ServerConfiguration getConfiguration() {
        return configuration;
    }

    public static void setConfiguration(ServerConfiguration configuration) {
        ServerEvents.configuration = configuration;
    }

}
