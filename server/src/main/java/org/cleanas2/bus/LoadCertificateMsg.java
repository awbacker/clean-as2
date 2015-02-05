package org.cleanas2.bus;

/**
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class LoadCertificateMsg extends MessageBase {
    public final String fileName;
    public final String alias;
    public final String password;

    public LoadCertificateMsg(String fileName, String alias) {
        this(fileName, alias, "");
    }

    public LoadCertificateMsg(String fileName, String alias, String password) {
        this.fileName = fileName;
        this.alias = alias;
        this.password = password;
    }
}

