package org.cleanas2.service.polling;

import net.engio.mbassy.listener.Handler;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.boon.datarepo.Repo;
import org.boon.datarepo.Repos;
import org.cleanas2.server.MessageBus;
import org.cleanas2.bus.*;
import org.cleanas2.common.service.AdminDump;
import org.cleanas2.common.service.ConfigurableService;
import org.cleanas2.common.service.StoppableService;
import org.cleanas2.message.OutgoingFileMessage;
import org.cleanas2.util.DebugUtil;
import org.joda.time.DateTime;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.boon.Lists.list;
import static org.boon.criteria.ObjectFilter.eq;
import static org.cleanas2.util.RepoCriteria.*;

/**
 * Module for watching directories and sending the files that magically show up in them
 *
 * TODO: separate ALL the concerns (?) ... is a SendScheduledFilesMsg blocking the service from
 * TODO: receiving new WatchDirectory or WatchFile messages? should it be on its own thread?
 *
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
@Singleton
public class DirectoryPollingService implements ConfigurableService, StoppableService, AdminDump {

    private static final long TIMER_DELAY = 30 * 1000;  // 30 seconds
    private final Log logger = LogFactory.getLog(DirectoryPollingService.class.getSimpleName());

    private final Repo<Path, WatchedDir> dirRepo = Repos.builder().primaryKey("directory").uniqueSearchIndex("directory").build(Path.class, WatchedDir.class);
    private final Repo<Path, WatchedFile> fileRepo = Repos.builder().primaryKey("file").addLogging(true).build(Path.class, WatchedFile.class);
    private final FileAlterationMonitor monitor = new FileAlterationMonitor(5 * 1000);
    private final Timer timer = new Timer("SendScheduledFilesMsg generator", true);
    private boolean _isSending = false;

    @Inject
    public DirectoryPollingService() throws Exception {
    }

    @Override
    public void initialize() throws Exception {
        monitor.start();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                MessageBus.publishAsync(new SendScheduledFilesMsg());
            }
        }, TIMER_DELAY, TIMER_DELAY);
    }

    @Handler
    public void watchDirectory(WatchDirectoryMsg busMessage) throws Exception {
        logger.debug("watching directory: " + busMessage.directory);

        WatchedDir dir = new WatchedDir(busMessage.directory.normalize(), busMessage.senderId, busMessage.receiverId);
        dirRepo.init(list(dir));

        FileAlterationObserver obs = new FileAlterationObserver(dir.directory.toFile());
        obs.addListener(new FileCreatedListenerAdapter());

        monitor.addObserver(obs);

        // tell the observer to check the directory.  this should pick up all existing files on start
        obs.checkAndNotify();
    }

    @Handler
    public void sendScheduledFiles(SendScheduledFilesMsg busMessage) {
        if (_isSending) {
            logger.debug("already sending files, exiting");
            return;
        }

        try {
            _isSending = true;
            while (true) {
                List<WatchedFile> toSend = fileRepo.query(
                        eq("status", WatchStatus.NEW),
                        dateBeforeNow("sendAt"),
                        pathExists("file")
                );

                if (toSend.isEmpty()) {
                    break;
                }

                WatchedFile f = toSend.get(0);
                logger.debug("Trying to send file: " + f.file.toString());

                f.status = WatchStatus.SEND;
                SendFileMsg message = new SendFileMsg(new OutgoingFileMessage(f.file, f.senderId, f.receiverId));

                // publish sync, so we wait for the send to be processed
                MessageBus.publish(message);
                if (message.isError()) {
                    logger.error("Error detected while sending file", message.getErrorCause());
                    //TODO: detect if host is down, and do a resend later for the other messages?
                }
            }
        } catch (Throwable e) {
            logger.debug("Error creating and sending file-send message", e);
        } finally {
            _isSending = false;
        }
    }

    @Handler
    public void startWatchingFile(WatchFileMsg busMessage) {
        try {
            Path parent = busMessage.file.getParent();
            Path file = busMessage.file;

            if (dirRepo.get(parent) == null) {
                logger.debug("parent not found");
                return;
            }
            if (!Files.exists(file)) {
                logger.debug("file to watch not found");
                return;
            }

            WatchedDir dir = dirRepo.get(parent);
            WatchedFile watchedFile = fileRepo.get(file);

            if (watchedFile == null) {
                watchedFile = new WatchedFile(busMessage.file, dir);
                DebugUtil.debugPrintObject(logger, "added file to watch", watchedFile);
                fileRepo.add(watchedFile);
            } else {
                switch (watchedFile.status) {
                    case NEW:
                        logger.debug("incrementing send-at on " + watchedFile.file.getFileName());
                        watchedFile.sendAt = DateTime.now().plusSeconds(15);
                        break;
                    case SEND:
                        logger.error("file is currently being sent: " + watchedFile.file);
                        break;
                    case RESEND:
                        logger.error("file is already in resend status");
                        break;
                }
            }
        } catch (Exception e) {
            logger.error("Error starting watch file", e);
        }
    }


    @Handler
    public void resendFile(ResendFileMsg busMessage) {
        logger.debug("Trying to resend message");
        WatchedFile watchedFile = fileRepo.get(busMessage.fileToResend);
        if (watchedFile != null) {
            watchedFile.retries += 1;
            watchedFile.status = WatchStatus.RESEND;
            watchedFile.sendAt = DateTime.now().plusMinutes(15 * watchedFile.retries);
            DebugUtil.debugPrintObject(logger, "message to be resent", watchedFile);
        }
    }

    @Handler
    public void stopWatchingFile(WatchFileStopMsg busMessage) {
        if (fileRepo.get(busMessage.filePath) != null) {
            logger.debug("Removing file from watch list: " + busMessage.filePath);
            fileRepo.removeByKey(busMessage.filePath);
        } else {
            logger.debug("File is not currently watched: " + busMessage.filePath);
        }
    }


    @Override
    public List<String> dumpCurrentStatus() {
        List<String> out = new ArrayList<>(10);
        out.add("Watched Directories:");
        for (WatchedDir x : dirRepo) {
            out.add("    " + x.toString());
        }
        return out;
    }


    @Override
    public void stop() {
        try {
            timer.cancel();
            monitor.stop(250); // wait 250 ms for the thread to stop, then call terminate
        } catch (Exception e) {
            logger.error("Error stopping file watcher: " + e.getLocalizedMessage());
        }
    }
}
