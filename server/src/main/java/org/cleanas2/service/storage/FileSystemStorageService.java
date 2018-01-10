package org.cleanas2.service.storage;

import net.engio.mbassy.listener.Handler;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.boon.json.JsonFactory;
import org.cleanas2.bus.*;
import org.cleanas2.common.PendingMdnInfoFile;
import org.cleanas2.message.IncomingFileMessage;
import org.cleanas2.message.OutgoingFileMessage;
import org.cleanas2.service.ServerConfiguration;
import org.cleanas2.service.SystemDir;
import org.cleanas2.util.AS2Util;
import org.cleanas2.util.JsonUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.*;

import static java.nio.file.StandardOpenOption.*;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
@Singleton
public class FileSystemStorageService {

    private static final Log logger = LogFactory.getLog(FileSystemStorageService.class.getSimpleName());
    private final ServerConfiguration config;
    private final Object lock = new Object();

    @Inject
    public FileSystemStorageService(ServerConfiguration config) {
        this.config = config;
    }

    @Handler
    public void saveIncomingFile(SaveIncomingFileMsg busMessage) {
        try {
            synchronized (lock) {
                IncomingFileMessage msg = busMessage.message;
                String fileName = AS2Util.makeFileName(isBlank(msg.fileName) ? msg.messageId : msg.fileName);
                Path dir = config.getDirectory(SystemDir.Inbox).resolve(msg.senderId);
                ensureDirectory(dir);
                Path file = AS2Util.getUniqueFileName(dir, fileName);
                Files.copy(busMessage.fileData.getInputStream(), file);
                logger.info("Saved file : " + fileName);
            }
        } catch (Exception e) {
            logger.error("Error saving the received file to a file: ", e);
            busMessage.setErrorCause(e);
        }
    }

    private void ensureDirectory(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            logger.debug("creating directory : " + dir);
            Files.createDirectories(dir);
        }
    }

    @Handler
    public void saveMdn(SaveMdnMsg busMessage) {
        try {
            synchronized (lock) {
                Path dir = config.getDirectory(SystemDir.Mdn);
                Path mdnFilePath = dir.resolve(AS2Util.makeFileName(busMessage.mdn.messageId, ".mdn.json"));
                ensureDirectory(dir);
                Files.write(mdnFilePath, JsonUtil.toPrettyJson(busMessage.mdn).getBytes());
            }
        } catch (IOException e) {
            logger.error("Error saving the MDN to a file: ", e);
        }
    }

    @Handler
    public void saveIncomingMdn(SaveIncomingMdnMsg busMessage) {
        try {
            synchronized (lock) {
                // yes, this is almost identical to saveMdn... if not identical.  but they are different
                // classes and the behavior might be different later
                Path dir = config.getDirectory(SystemDir.Mdn);
                Path mdnFilePath = dir.resolve(AS2Util.makeFileName(busMessage.mdn.attributes.originalMessageId, ".mdn.json"));
                ensureDirectory(dir);
                Files.write(mdnFilePath, JsonUtil.toPrettyJson(busMessage.mdn).getBytes());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Handler
    public void savePendingMdnInfoToFiles(SavePendingMdnMsg busMessage) {
        try {
            synchronized (lock) {
                OutgoingFileMessage msg = busMessage.msg;
                logger.debug("Storing pending MDN info");

                String fileName = AS2Util.makeFileName(msg.messageId, ".json");
                Path infoFile = config.getDirectory(SystemDir.PendingMdnInfo).resolve(fileName);
                Path dataFile = AS2Util.getUniqueFileName(config.getDirectory(SystemDir.PendingMdn), msg.filePath);

                PendingMdnInfoFile pm = new PendingMdnInfoFile();
                pm.originalFile = msg.filePath.toString();
                pm.pendingFile = dataFile.toString();
                pm.outgoingMic = msg.outgoingMic;

                Files.write(infoFile, JsonUtil.toPrettyJson(pm).getBytes(Charsets.UTF_8), CREATE, TRUNCATE_EXISTING);
                Files.move(msg.filePath, dataFile);

                msg.pendingInfo.infoFile = infoFile.toString();
                msg.pendingInfo.dataFile = dataFile.toString();
                msg.status = "pending";
            }
        } catch (Exception e) {
            busMessage.setErrorCause(e);
            logger.error("Error saving pending MDN info", e);
        }
    }

    @Handler
    public void loadPendingMdnInfo(LoadPendingMdnMsg busMessage) {
        try {
            Path infoFile = makeMdnFilePath(busMessage.originalMessageId);
            byte[] bytes = Files.readAllBytes(infoFile);
            busMessage.fileData = JsonFactory.create().readValue(bytes, PendingMdnInfoFile.class);
        } catch (Exception e) {
            busMessage.setErrorCause(e);
        }
    }


    @Handler
    public void deletePendingMdnInfo(DeletePendingMdnMsg busMessage) {
        try {
            Path infoFile = makeMdnFilePath(busMessage.originalMessageId);
            byte[] bytes = Files.readAllBytes(infoFile);
            PendingMdnInfoFile f = JsonFactory.create().readValue(bytes, PendingMdnInfoFile.class);
            Path sentFile = Paths.get(f.pendingFile);

            logger.info(String.format("delete pending-info file : '%s' from folder : %s", infoFile.getFileName(), infoFile.getParent()));
            if (!FileUtils.deleteQuietly(infoFile.toFile())) {
                busMessage.setError(true);
                logger.info("    failed to delete pending-info file : " + infoFile.getFileName());
            }

            logger.info(String.format("delete pending file : '%s' from  : %s", sentFile.getFileName(), sentFile.getParent()));
            if (!FileUtils.deleteQuietly(sentFile.toFile())) {
                busMessage.setError(true);
                logger.info("    failed to delete pending file");
            }
        } catch (IOException e) {
            busMessage.setErrorCause(e);
        }
    }

    private Path makeMdnFilePath(String originalMessageId) {
        String fileName = AS2Util.makeFileName(originalMessageId, ".json");
        logger.debug("Loading file from : " + fileName);
        return config.getDirectory(SystemDir.PendingMdnInfo).resolve(fileName);
    }


}
