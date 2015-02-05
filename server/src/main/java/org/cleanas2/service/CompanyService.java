package org.cleanas2.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cleanas2.server.MessageBus;
import org.cleanas2.bus.LoadCertificateMsg;
import org.cleanas2.common.service.ConfigurableService;
import org.cleanas2.config.json.JsonConfigMap;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
@Singleton
public class CompanyService implements ConfigurableService {
    private static final Log logger = LogFactory.getLog(CompanyService.class.getSimpleName());

    private String as2id;
    private String name;
    private String email;
    private JsonConfigMap cert; // turn this into a real class

    @Inject
    public CompanyService(JsonConfigMap options) throws Exception {
        configure(options);
    }

    private void configure(JsonConfigMap options) throws Exception {
        JsonConfigMap opts = options.getSection("company", true);
        opts.requireValues("as2id", "email", "name");
        cert = opts.getSectionOrCreate("certificate", "file");
        this.as2id = opts.getString("as2id");
        this.email = opts.getString("email");
        this.name = opts.getString("name");
    }

    @Override
    public void initialize() throws Exception {
        LoadCertificateMsg loadCert = new LoadCertificateMsg(cert.getString("file"), this.as2id, cert.getString("password", ""));
        MessageBus.publish(loadCert);
        if (loadCert.isError()) {
            throw loadCert.getErrorCause();
        }
    }

    public String getAs2id() {
        return as2id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }
}
