package org.cleanas2.server

import com.google.inject.AbstractModule
import org.boon.json.JsonFactory
import org.cleanas2.config.json.JsonConfigMap

/**
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
class JsonConfiguredServerModule(private val config: CommandLineConfig) : AbstractModule() {

    override fun configure() {
        val objectMapper = JsonFactory.create()
        val configMap = JsonConfigMap("", objectMapper.readValue(config.configFile.toFile(), Map::class.java) as Map<*, *>)
        bind(JsonConfigMap::class.java).toInstance(configMap)
        bind(CommandLineConfig::class.java).toInstance(config)
    }
}