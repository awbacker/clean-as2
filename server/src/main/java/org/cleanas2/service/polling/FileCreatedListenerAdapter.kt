package org.cleanas2.service.polling

import org.apache.commons.io.monitor.FileAlterationListenerAdaptor
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.cleanas2.server.MessageBus
import org.cleanas2.bus.WatchFileMsg
import org.cleanas2.bus.WatchFileStopMsg

import java.io.File
import java.io.IOException

/**
 * Handles the file created/etc events and sends out the correct bus message for the type
 * of event that happened.
 *
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
internal class FileCreatedListenerAdapter : FileAlterationListenerAdaptor() {

    // Is triggered when a file is created in the monitored folder
    override fun onFileCreate(file: File?) {
        try {
            logger.debug("file created: " + file!!.canonicalPath)
            MessageBus.publish(WatchFileMsg(file.toPath()))
        } catch (e: IOException) {
            logger.debug("onFileCreate error : " + e.message)
        }

    }

    override fun onFileChange(file: File?) {
        try {
            logger.debug("File changed: " + file!!.canonicalPath)
            MessageBus.publish(WatchFileMsg(file.toPath()))
        } catch (e: IOException) {
            logger.debug("onFileChange error : " + e.message)
        }

    }

    override fun onFileDelete(file: File?) {
        try {
            logger.debug("file deleted: " + file!!.canonicalPath)
            MessageBus.publish(WatchFileStopMsg(file.toPath()))
        } catch (e: IOException) {
            logger.debug("onFileDelete error : " + e.message)
        }

    }

    companion object {
        private val logger = LogFactory.getLog(FileCreatedListenerAdapter::class.java.simpleName)
    }
}
