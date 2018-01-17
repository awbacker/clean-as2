@file:Suppress("unused")

package org.cleanas2.server

//import com.powere2e.platform.log.enumeration.EventLevel;

import org.apache.commons.io.FilenameUtils
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.cleanas2.bus.ServerEventMsg
import org.cleanas2.common.serverEvent.EventLevel
import org.cleanas2.common.serverEvent.Phase
import org.cleanas2.message.OutgoingFileMessage
import org.cleanas2.service.ServerConfiguration


/**
 * Helper class for writing PowerE2E logs using the PowerE2E log connector.  These log statements are
 * quite long, with many parameters, and tend to dirty up the real code and need LOTS of helpers
 */
object ServerEvents {

    private val printException = false
    private val logger = LogFactory.getLog(ServerEvents::class.java.simpleName)
    var configuration: ServerConfiguration? = null

    /**
     * Gets the name of this process, for reporting to the LOG service.  The heartbeat service uses
     * a different value
     */
    private val processName: String
        get() = "AS2"

    fun heartbeat(isBusy: Boolean) {

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

    fun Info(phase: Phase, messageId: String, message: String) {
        val msg = ServerEventMsg(phase, EventLevel.Info, message, message)
        MessageBus.publishAsync(msg)
    }

    fun Error(phase: Phase, messageId: String, message: String) {
        val msg = ServerEventMsg(phase, EventLevel.Info, message, message)
        MessageBus.publishAsync(msg)
    }

    fun Error(phase: Phase, messageId: String, message: String, error: Throwable?) {
        val msg = ServerEventMsg(phase, EventLevel.Error, message, message, error)
        MessageBus.publishAsync(msg)
    }


    /* MDN RECEIVE LOGS */
    fun mdnReceiveStart(msg: OutgoingFileMessage) {
        ServerEvents.doLogMessageInfo(EventLevel.Info, Phase.MDN_RECEIVE, msg, null, "MDN Receive - Start")
    }

    fun mdnReceiveEnd(msg: OutgoingFileMessage) {
        ServerEvents.doLogMessageInfo(EventLevel.Info, Phase.MDN_RECEIVE, msg, null, "MDN Receive - End")
    }

    fun mdnReceiveInfo(msg: OutgoingFileMessage, message: String) {
        ServerEvents.doLogMessageInfo(EventLevel.Info, Phase.MDN_RECEIVE, msg, null, "MDN Receive - " + message)
    }

    fun mdnReceiveError(msg: OutgoingFileMessage, exception: Exception) {
        ServerEvents.mdnReceiveError(msg, exception, "No custom message provided")
    }

    fun mdnReceiveError(msg: OutgoingFileMessage, exception: Exception?, message: String) {
        ServerEvents.doLogMessageInfo(EventLevel.Error, Phase.MDN_RECEIVE, msg, exception, "MDN Receive - Error - " + message)
    }

    /* ASYNC MDN RECEIVE LOGS */

    fun mdnAsyncReceiveStart(msg: OutgoingFileMessage) {
        ServerEvents.doLogMessageInfo(EventLevel.Info, Phase.ASYNC_MDN_RECEIVE, msg, null, "ASYNC MDN Receive - Start")
    }

    fun mdnAsyncReceiveEnd(msg: OutgoingFileMessage) {
        doLogMessageInfo(EventLevel.Info, Phase.ASYNC_MDN_RECEIVE, msg, null, "ASYNC MDN Receive - End")
    }

    fun mdnAsyncReceiveInfo(msg: OutgoingFileMessage, message: String) {
        doLogMessageInfo(EventLevel.Info, Phase.ASYNC_MDN_RECEIVE, msg, null, "ASYNC MDN Receive - " + message)
    }

    fun mdnAsyncReceiveError(msg: OutgoingFileMessage, exception: Exception) {
        mdnReceiveError(msg, exception, "No custom message provided")
    }

    fun mdnAsyncReceiveError(msg: OutgoingFileMessage, exception: Exception, message: String) {
        doLogMessageInfo(EventLevel.Error, Phase.ASYNC_MDN_RECEIVE, msg, exception, message)
    }

    /* MDN SEND LOGS */

    fun mdnSendStart(msg: OutgoingFileMessage) {
        ServerEvents.doLogMessageInfo(EventLevel.Info, Phase.MDN_SEND, msg, null, "MDN Send - Start")
    }

    fun mdnSendEnd(msg: OutgoingFileMessage) {
        ServerEvents.doLogMessageInfo(EventLevel.Info, Phase.MDN_SEND, msg, null, "MDN Send - End")
    }

    fun mdnSendInfo(msg: OutgoingFileMessage, message: String) {
        ServerEvents.doLogMessageInfo(EventLevel.Info, Phase.MDN_SEND, msg, null, "MDN Send - " + message)
    }

    fun mdnSendError(msg: OutgoingFileMessage, exception: Exception, message: String) {
        ServerEvents.doLogMessageInfo(EventLevel.Error, Phase.MDN_SEND, msg, exception, "MDN Send - Error - " + message)
    }

    /* ASYNC MDN SEND LOGS */

    fun mdnAsyncSendStart(msg: OutgoingFileMessage) {
        ServerEvents.doLogMessageInfo(EventLevel.Info, Phase.ASYNC_MDN_SEND, msg, null, "ASYNC MDN Send - Start")
    }

    fun mdnAsyncSendEnd(msg: OutgoingFileMessage) {
        ServerEvents.doLogMessageInfo(EventLevel.Info, Phase.ASYNC_MDN_SEND, msg, null, "ASYNC MDN Send - End")
    }

    fun mdnAsyncSendInfo(msg: OutgoingFileMessage, message: String) {
        ServerEvents.doLogMessageInfo(EventLevel.Info, Phase.ASYNC_MDN_SEND, msg, null, "ASYNC MDN Send - " + message)
    }

    fun mdnAsyncSendError(msg: OutgoingFileMessage, exception: Exception, message: String) {
        ServerEvents.doLogMessageInfo(EventLevel.Error, Phase.ASYNC_MDN_SEND, msg, exception, "ASYNC MDN Send - Error - " + message)
    }


    /* UTILITY FUNCTIONS */

    private fun doLogMessageInfo(level: EventLevel, phase: Phase, as2message: OutgoingFileMessage, message: String) {
        ServerEvents.doLogMessageInfo(level, phase, as2message, null, message)
    }

    private fun doLogMessageInfo(level: EventLevel, phase: Phase, as2message: OutgoingFileMessage, exception: Exception?, message: String) {
        if (exception != null && printException) {
            logger.error(message + "[" + getFilenameFromMessage(as2message) + "] - " + as2message.loggingText, exception)
        } else {
            logger.info(message + "[" + getFilenameFromMessage(as2message) + "] - " + as2message.loggingText)
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
    private fun getFilenameFromMessage(msg: OutgoingFileMessage): String? {
        return msg.filePath.fileName.toString()
    }

    /**
     * Gets the full path without the file name
     */
    private fun getFilePathFromMessage(msg: OutgoingFileMessage): String {
        return msg.filePath.parent.toString()
    }

    /**
     * Gets only the filename, removing the extension.  This is used as the "key" in
     */
    private fun getFilenameWithoutExtension(msg: OutgoingFileMessage): String? {
        if (getFilenameFromMessage(msg) != null) return FilenameUtils.removeExtension(getFilenameFromMessage(msg))
        return msg.messageId
    }

}
