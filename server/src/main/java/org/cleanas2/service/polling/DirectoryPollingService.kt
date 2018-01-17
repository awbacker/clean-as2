package org.cleanas2.service.polling

import net.engio.mbassy.listener.Handler
import org.apache.commons.io.monitor.FileAlterationMonitor
import org.apache.commons.io.monitor.FileAlterationObserver
import org.apache.commons.logging.LogFactory
import org.boon.Lists.list
import org.boon.criteria.ObjectFilter.eq
import org.boon.datarepo.Repos
import org.cleanas2.bus.*
import org.cleanas2.common.service.AdminDump
import org.cleanas2.common.service.ConfigurableService
import org.cleanas2.common.service.StoppableService
import org.cleanas2.message.OutgoingFileMessage
import org.cleanas2.server.MessageBus
import org.cleanas2.util.DebugUtil
import org.cleanas2.util.RepoCriteria.dateBeforeNow
import org.cleanas2.util.RepoCriteria.pathExists
import org.joda.time.DateTime
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Module for watching directories and sending the files that magically show up in them
 *
 * TODO: separate ALL the concerns (?) ... is a SendScheduledFilesMsg blocking the service from
 * TODO: receiving new WatchDirectory or WatchFile messages? should it be on its own thread?
 *
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
@Singleton
class DirectoryPollingService @Inject
@Throws(Exception::class)
constructor() : ConfigurableService, StoppableService, AdminDump {
    private val logger = LogFactory.getLog(DirectoryPollingService::class.java.simpleName)

    private val dirRepo = Repos.builder().primaryKey("directory").uniqueSearchIndex("directory").build(Path::class.java, WatchedDir::class.java)
    private val fileRepo = Repos.builder().primaryKey("file").addLogging(true).build(Path::class.java, WatchedFile::class.java)
    private val monitor = FileAlterationMonitor((5 * 1000).toLong())
    private val timer = Timer("SendScheduledFilesMsg generator", true)
    private var _isSending = false

    @Throws(Exception::class)
    override fun initialize() {
        monitor.start()
        timer.schedule(object : TimerTask() {
            override fun run() {
                MessageBus.publishAsync(SendScheduledFilesMsg())
            }
        }, TIMER_DELAY, TIMER_DELAY)
    }

    @Handler
    @Throws(Exception::class)
    fun watchDirectory(busMessage: WatchDirectoryMsg) {
        logger.debug("watching directory: " + busMessage.directory)

        val dir = WatchedDir(busMessage.directory.normalize(), busMessage.senderId, busMessage.receiverId)
        dirRepo.init(list(dir))

        val obs = FileAlterationObserver(dir.directory.toFile())
        obs.addListener(FileCreatedListenerAdapter())

        monitor.addObserver(obs)

        // tell the observer to check the directory.  this should pick up all existing files on start
        obs.checkAndNotify()
    }

    @Handler
    fun sendScheduledFiles(busMessage: SendScheduledFilesMsg) {
        if (_isSending) {
            logger.debug("already sending files, exiting")
            return
        }

        try {
            _isSending = true
            while (true) {
                val toSend = fileRepo.query(
                        eq("status", WatchStatus.NEW),
                        dateBeforeNow("sendAt"),
                        pathExists("file")
                )

                if (toSend.isEmpty()) {
                    break
                }

                val f = toSend[0]
                logger.debug("Trying to send file: " + f.file.toString())

                f.status = WatchStatus.SEND
                val message = SendFileMsg(OutgoingFileMessage(f.file, f.senderId, f.receiverId))

                // publish sync, so we wait for the send to be processed
                MessageBus.publish(message)
                if (message.isError) {
                    logger.error("Error detected while sending file", message.errorCause)
                    //TODO: detect if host is down, and do a resend later for the other messages?
                }
            }
        } catch (e: Throwable) {
            logger.debug("Error creating and sending file-send message", e)
        } finally {
            _isSending = false
        }
    }

    @Handler
    fun startWatchingFile(busMessage: WatchFileMsg) {
        try {
            val parent = busMessage.file.parent
            val file = busMessage.file

            if (dirRepo.get(parent) == null) {
                logger.debug("parent not found")
                return
            }
            if (!Files.exists(file)) {
                logger.debug("file to watch not found")
                return
            }

            val dir = dirRepo.get(parent)
            var watchedFile: WatchedFile? = fileRepo.get(file)

            if (watchedFile == null) {
                watchedFile = WatchedFile(busMessage.file, dir)
                DebugUtil.debugPrintObject(logger, "added file to watch", watchedFile)
                fileRepo.add(watchedFile)
            } else {
                when (watchedFile.status) {
                    WatchStatus.NEW -> {
                        logger.debug("incrementing send-at on " + watchedFile.file.fileName)
                        watchedFile.sendAt = DateTime.now().plusSeconds(15)
                    }
                    WatchStatus.SEND -> logger.error("file is currently being sent: " + watchedFile.file)
                    WatchStatus.RESEND -> logger.error("file is already in resend status")
                }
            }
        } catch (e: Exception) {
            logger.error("Error starting watch file", e)
        }

    }


    @Handler
    fun resendFile(busMessage: ResendFileMsg) {
        logger.debug("Trying to resend message")
        val watchedFile = fileRepo.get(busMessage.fileToResend)
        if (watchedFile != null) {
            watchedFile.retries += 1
            watchedFile.status = WatchStatus.RESEND
            watchedFile.sendAt = DateTime.now().plusMinutes(15 * watchedFile.retries)
            DebugUtil.debugPrintObject(logger, "message to be resent", watchedFile)
        }
    }

    @Handler
    fun stopWatchingFile(busMessage: WatchFileStopMsg) {
        if (fileRepo.get(busMessage.filePath) != null) {
            logger.debug("Removing file from watch list: " + busMessage.filePath)
            fileRepo.removeByKey(busMessage.filePath)
        } else {
            logger.debug("File is not currently watched: " + busMessage.filePath)
        }
    }


    override fun dumpCurrentStatus(): List<String> {
        val out = ArrayList<String>(10)
        out.add("Watched Directories:")
        for (x in dirRepo) {
            out.add("    " + x.toString())
        }
        return out
    }


    override fun stop() {
        try {
            timer.cancel()
            monitor.stop(250) // wait 250 ms for the thread to stop, then call terminate
        } catch (e: Exception) {
            logger.error("Error stopping file watcher: " + e.localizedMessage)
        }

    }

    companion object {

        private val TIMER_DELAY = (30 * 1000).toLong()  // 30 seconds
    }
}
