package org.cleanas2.service.net;

import net.engio.mbassy.listener.Handler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cleanas2.bus.SendFileMsg;
import org.cleanas2.common.serverEvent.Phase;
import org.cleanas2.pipeline.FailureTask;
import org.cleanas2.pipeline.ServerEventEmitter;
import org.cleanas2.service.net.pipelines.fileSend.*;
import org.cleanas2.pipeline.Pipeline;

import javax.inject.Inject;

public class FileSenderService {

    private static final Log logger = LogFactory.getLog(FileSenderService.class.getSimpleName());

    @Inject
    public FileSenderService() {
    }

    @Handler
    public void handleSendFile(final SendFileMsg busMessage) throws Exception {
        Context context = new Context(busMessage.as2message);
        ServerEventEmitter<Context> fileSendEvent = new FileSendEmitter(context);

        Pipeline<Context> p = new Pipeline<>();
        p.step(fileSendEvent.Info("Starting to send file"));
        p.step(
                CreateMimeBodyPart.class,
                EncryptMimeBodyPart.class,
                ValidateMessage.class,
                SendFile.class,
                ReceiveMdn.class
        );
        p.done(CleanupAndPostProcess.class);
        p.done(fileSendEvent.Info("File Send Finished"));
        p.fail(fileSendEvent.Error("Error Sending File"));
        p.fail(new FailureTask<Context>() {
            @Override
            public void process(Context ctx, Exception e) {
                // we could also just stick the bus message in the context if we want?
                busMessage.setError(true);
                busMessage.setErrorCause(e);
            }
        });

        p.run(context);
    }

    private class FileSendEmitter extends ServerEventEmitter<Context> {
        public FileSendEmitter(Context context) {
            super(Phase.FILE_SEND, context);
        }

        @Override
        public String GetMessageId() {
            return this.context.message.messageId;
        }
    }
}
