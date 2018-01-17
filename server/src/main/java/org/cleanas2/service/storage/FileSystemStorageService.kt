package org.cleanas2.service.storage

import net.engio.mbassy.listener.Handler
import org.apache.commons.io.Charsets
import org.apache.commons.io.FileUtils
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.boon.json.JsonFactory
import org.cleanas2.bus.*
import org.cleanas2.common.PendingMdnInfoFile
import org.cleanas2.message.IncomingFileMessage
import org.cleanas2.message.OutgoingFileMessage
import org.cleanas2.service.ServerConfiguration
import org.cleanas2.service.SystemDir
import org.cleanas2.util.AS2Util
import org.cleanas2.util.JsonUtil

import javax.inject.Inject
import javax.inject.Singleton
import java.io.IOException
import java.nio.file.*

import java.nio.file.StandardOpenOption.*
import org.apache.commons.lang3.StringUtils.isBlank
import java.nio.charset.StandardCharsets

/**
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
@Singleton
class FileSystemStorageService @Inject
constructor(private val config: ServerConfiguration) {
    private val lock = Any()

    @Handler
    fun saveIncomingFile(busMessage: SaveIncomingFileMsg) {
        try {
            synchronized(lock) {
                val msg = busMessage.message
                val fileName = AS2Util.makeFileName(if (isBlank(msg.fileName)) msg.messageId else msg.fileName)
                val dir = config.getDirectory(SystemDir.Inbox).resolve(msg.senderId)
                ensureDirectory(dir)
                val file = AS2Util.getUniqueFileName(dir, fileName)
                Files.copy(busMessage.fileData.inputStream, file)
                logger.info("Saved file : " + fileName)
            }
        } catch (e: Exception) {
            logger.error("Error saving the received file to a file: ", e)
            busMessage.errorCause = e
        }

    }

    @Throws(IOException::class)
    private fun ensureDirectory(dir: Path) {
        if (!Files.isDirectory(dir)) {
            logger.debug("creating directory : " + dir)
            Files.createDirectories(dir)
        }
    }

    @Handler
    fun saveMdn(busMessage: SaveMdnMsg) {
        try {
            synchronized(lock) {
                val dir = config.getDirectory(SystemDir.Mdn)
                val mdnFilePath = dir.resolve(AS2Util.makeFileName(busMessage.mdn.messageId, ".mdn.json"))
                ensureDirectory(dir)
                Files.write(mdnFilePath, JsonUtil.toPrettyJson(busMessage.mdn).toByteArray())
            }
        } catch (e: IOException) {
            logger.error("Error saving the MDN to a file: ", e)
        }

    }

    @Handler
    fun saveIncomingMdn(busMessage: SaveIncomingMdnMsg) {
        try {
            synchronized(lock) {
                // yes, this is almost identical to saveMdn... if not identical.  but they are different
                // classes and the behavior might be different later
                val dir = config.getDirectory(SystemDir.Mdn)
                val mdnFilePath = dir.resolve(AS2Util.makeFileName(busMessage.mdn.attributes.originalMessageId, ".mdn.json"))
                ensureDirectory(dir)
                Files.write(mdnFilePath, JsonUtil.toPrettyJson(busMessage.mdn).toByteArray())
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    @Handler
    fun savePendingMdnInfoToFiles(busMessage: SavePendingMdnMsg) {
        try {
            synchronized(lock) {
                val msg = busMessage.msg
                logger.debug("Storing pending MDN info")

                val fileName = AS2Util.makeFileName(msg.loggingText, ".json")
                val infoFile = config.getDirectory(SystemDir.PendingMdnInfo).resolve(fileName)
                val dataFile = AS2Util.getUniqueFileName(config.getDirectory(SystemDir.PendingMdn), msg.filePath)

                val pm = PendingMdnInfoFile()
                pm.originalFile = msg.filePath.toString()
                pm.pendingFile = dataFile.toString()
                pm.outgoingMic = msg.outgoingMic

                Files.write(infoFile, JsonUtil.toPrettyJson(pm).toByteArray(StandardCharsets.UTF_8), CREATE, TRUNCATE_EXISTING)
                Files.move(msg.filePath, dataFile)

                msg.pendingInfo.infoFile = infoFile.toString()
                msg.pendingInfo.dataFile = dataFile.toString()
                msg.status = "pending"
            }
        } catch (e: Exception) {
            busMessage.errorCause = e
            logger.error("Error saving pending MDN info", e)
        }

    }

    @Handler
    fun loadPendingMdnInfo(busMessage: LoadPendingMdnMsg) {
        try {
            val infoFile = makeMdnFilePath(busMessage.originalMessageId)
            val bytes = Files.readAllBytes(infoFile)
            busMessage.fileData = JsonFactory.create().readValue(bytes, PendingMdnInfoFile::class.java)
        } catch (e: Exception) {
            busMessage.errorCause = e
        }

    }


    @Handler
    fun deletePendingMdnInfo(busMessage: DeletePendingMdnMsg) {
        try {
            val infoFile = makeMdnFilePath(busMessage.originalMessageId)
            val bytes = Files.readAllBytes(infoFile)
            val f = JsonFactory.create().readValue(bytes, PendingMdnInfoFile::class.java)
            val sentFile = Paths.get(f.pendingFile)

            logger.info(String.format("delete pending-info file : '%s' from folder : %s", infoFile.fileName, infoFile.parent))
            if (!FileUtils.deleteQuietly(infoFile.toFile())) {
                busMessage.isError = true
                logger.info("    failed to delete pending-info file : " + infoFile.fileName)
            }

            logger.info(String.format("delete pending file : '%s' from  : %s", sentFile.fileName, sentFile.parent))
            if (!FileUtils.deleteQuietly(sentFile.toFile())) {
                busMessage.isError = true
                logger.info("    failed to delete pending file")
            }
        } catch (e: IOException) {
            busMessage.errorCause = e
        }

    }

    private fun makeMdnFilePath(originalMessageId: String): Path {
        val fileName = AS2Util.makeFileName(originalMessageId, ".json")
        logger.debug("Loading file from : " + fileName)
        return config.getDirectory(SystemDir.PendingMdnInfo).resolve(fileName)
    }

    companion object {

        private val logger = LogFactory.getLog(FileSystemStorageService::class.java.simpleName)
    }


}
