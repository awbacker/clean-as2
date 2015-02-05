package org.cleanas2.service.net.pipelines.fileSend;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cleanas2.common.PartnerRecord;
import org.cleanas2.service.PartnerService;
import org.cleanas2.service.net.FileSenderService;
import org.cleanas2.service.net.util.MimeUtil;
import org.cleanas2.pipeline.PipelineTask;

import javax.inject.Inject;
import javax.mail.internet.MimeBodyPart;

/**
 * Creates the MIME body part to send to the client, reading from the local file.
 */
public class CreateMimeBodyPart implements PipelineTask<Context> {
    private static final Log logger = LogFactory.getLog(FileSenderService.class.getSimpleName());
    private final PartnerService partners;

    @Inject
    public CreateMimeBodyPart(PartnerService partners) {
        this.partners = partners;
    }

    @Override
    public void process(Context ctx) throws Exception {
        PartnerRecord p = partners.getPartner(ctx.message.receiverId);
        String filename = ctx.message.filePath.getFileName().toString();

        // keep this here (not in a function) so it is clear what is going on with transfer encoding & content type
        MimeBodyPart data = MimeUtil.fromFile(ctx.message.filePath, p.sendSettings.contentType);
        data.setHeader("Content-Transfer-Encoding", p.sendSettings.transferEncoding); // this must be binary or everything screws up
        data.setHeader("Content-Type", p.sendSettings.contentType);
        // set this here ? also in outgoing headers?
        data.setHeader("Content-Disposition", String.format("Attachment; filename=\"%s\"", filename));

        ctx.mimeData = data;
    }
}
