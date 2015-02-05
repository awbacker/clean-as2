package org.cleanas2.service.net.pipelines.fileSend;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.cleanas2.message.OutgoingFileMessage;
import org.cleanas2.pipeline.PipelineContext;

import javax.mail.internet.MimeBodyPart;

/**
 * The context object used when sending a file
 */
public class Context extends PipelineContext {

    public final OutgoingFileMessage message;
    public MimeBodyPart mimeData;
    public CloseableHttpResponse response;
    public CloseableHttpClient client;

    public Context(OutgoingFileMessage msg) {
        message = msg;
    }
}
