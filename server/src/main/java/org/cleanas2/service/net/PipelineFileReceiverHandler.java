package org.cleanas2.service.net;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.cleanas2.common.serverEvent.Phase;
import org.cleanas2.message.IncomingMessage;
import org.cleanas2.pipeline.Pipeline;
import org.cleanas2.pipeline.ServerEventEmitter;
import org.cleanas2.service.net.pipelines.fileReceive.*;

import java.io.IOException;


public class PipelineFileReceiverHandler implements HttpRequestHandler {

    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
        try {
            DefaultBHttpServerConnection connection = (DefaultBHttpServerConnection) context.getAttribute("connection");
            IncomingMessage msg = new IncomingMessage(connection, request);
            RunPipeline(request, response, context, connection, msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void RunPipeline(HttpRequest request, HttpResponse response, HttpContext context, DefaultBHttpServerConnection connection, IncomingMessage msg) throws Exception {
        Context ctx = new Context(msg, request, response, context, connection);
        FileSendEvent evt = new FileSendEvent(ctx);
        Pipeline<Context> p = new Pipeline<>();
        p.step(evt.Info("Starting file receive"));
        p.step(ExtractMimeDataFromRequest.class);
        p.step(HandleAsyncMdn.class);
        p.step(HandleFile.class);
        p.step(SendMdn.class);
        p.done(evt.Info("File receive finished"));
        p.fail(SetResponseError.class);
        p.fail(evt.Error("Error sending file"));
        p.run(ctx);
    }

    public static class FileSendEvent extends ServerEventEmitter<Context> {

        public FileSendEvent(Context context) {
            super(Phase.FILE_RECEIVE, context);
        }

        @Override
        public String GetMessageId() {
            return context.message.receiverId;
        }
    }
}
