package org.cleanas2.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.boon.json.*;
import org.cleanas2.server.MessageBus;
import org.cleanas2.bus.*;
import org.cleanas2.common.PartnerRecord;
import org.cleanas2.common.service.AdminDump;
import org.cleanas2.common.service.ConfigurableService;
import org.cleanas2.config.json.JsonConfigMap;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains a partner record read from the configuration file
 *
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
@Singleton
public class PartnerService implements ConfigurableService, AdminDump {
    private static final Log logger = LogFactory.getLog(PartnerService.class.getSimpleName());
    private final CompanyService company;
    private final ServerConfiguration config;
    private List<PartnerRecord> partners = null;

    @Inject
    public PartnerService(JsonConfigMap options, CompanyService companies, ServerConfiguration config) throws Exception {
        this.company = companies;
        this.config = config;
        configure(options);
    }

    private void configure(JsonConfigMap options) throws Exception {
        JsonParserFactory parse = new JsonParserFactory();
        JsonSerializerFactory serial = new JsonSerializerFactory();

        parse.setCaseInsensitiveFields(true);
        parse.setRespectIgnore(true);
        parse.setUseAnnotations(true);
        serial.setUseAnnotations(true);

        options.requireList("partners");
        ObjectMapper mapper = JsonFactory.create(parse, serial);

        // convert back to a json string so it can be mapped back into an object again
        String json = mapper.toJson(options.get("partners")); // partners should be an array (e.g. ValueArray
        this.partners = mapper.parser().parseList(PartnerRecord.class, json); //mapper.readValue(json, List.class, PartnerRecord.class);
    }

    @Override
    public void initialize() throws Exception {
        for (PartnerRecord p : partners) {
            MessageBase msg = new LoadCertificateMsg(p.certificate, p.as2id);
            MessageBus.publish(msg);
            if (msg.isError()) {
                throw msg.getErrorCause();
            }

            // create a directory in the outbox of the company for this partner
            Path newFolder = config.getDirectory(SystemDir.Outbox).resolve(p.as2id);
            if (!Files.isDirectory(newFolder)) {
                logger.debug("creating new directory : " + newFolder);
                Files.createDirectory(newFolder);
            }

            WatchDirectoryMsg wd = new WatchDirectoryMsg(newFolder, company.getAs2id(), p.as2id);
            MessageBus.publishAsync(wd);
        }
    }

    public PartnerRecord getPartner(String as2id) {
        for (PartnerRecord p : partners) {
            if (p.as2id.equalsIgnoreCase(as2id)) return p;
        }
        return null;
    }

    public List<PartnerRecord> getAllPartners() {
        return partners;
    }

    @Override
    public List<String> dumpCurrentStatus() {
        List<String> items = new ArrayList<>();
        items.add("partners = " + this.partners.size());
        return items;
    }
}
