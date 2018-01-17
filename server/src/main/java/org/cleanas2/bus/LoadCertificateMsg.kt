package org.cleanas2.bus

/**
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
class LoadCertificateMsg @JvmOverloads constructor(val fileName: String, val alias: String, val password: String = "") : MessageBase()

