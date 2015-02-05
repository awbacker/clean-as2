package org.cleanas2.service.polling;

import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cleanas2.server.MessageBus;
import org.cleanas2.bus.WatchFileMsg;
import org.cleanas2.bus.WatchFileStopMsg;

import java.io.File;
import java.io.IOException;

/**
 * Handles the file created/etc events and sends out the correct bus message for the type
 * of event that happened.
 *
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
class FileCreatedListenerAdapter extends FileAlterationListenerAdaptor {
    private static final Log logger = LogFactory.getLog(FileCreatedListenerAdapter.class.getSimpleName());

    // Is triggered when a file is created in the monitored folder
    @Override
    public void onFileCreate(File file) {
        try {
            logger.debug("file created: " + file.getCanonicalPath());
            MessageBus.publish(new WatchFileMsg(file.toPath()));
        } catch (IOException e) {
            logger.debug("onFileCreate error : " + e.getMessage());
        }
    }

    @Override
    public void onFileChange(File file) {
        try {
            logger.debug("File changed: " + file.getCanonicalPath());
            MessageBus.publish(new WatchFileMsg(file.toPath()));
        } catch (IOException e) {
            logger.debug("onFileChange error : " + e.getMessage());
        }
    }

    @Override
    public void onFileDelete(File file) {
        try {
            logger.debug("file deleted: " + file.getCanonicalPath());
            MessageBus.publish(new WatchFileStopMsg(file.toPath()));
        } catch (IOException e) {
            logger.debug("onFileDelete error : " + e.getMessage());
        }
    }
}
