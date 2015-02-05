package org.cleanas2.server;

import com.google.inject.AbstractModule;
import org.boon.json.JsonFactory;
import org.boon.json.ObjectMapper;
import org.cleanas2.config.json.JsonConfigMap;

import java.util.Map;

/**
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class JsonConfiguredServerModule extends AbstractModule {

    private final CommandLineConfig config;

    public JsonConfiguredServerModule(CommandLineConfig config) {
        this.config = config;
    }

    @Override
    protected void configure() {
        ObjectMapper objectMapper = JsonFactory.create();
        JsonConfigMap configMap = new JsonConfigMap("", objectMapper.readValue(config.configFile.toFile(), Map.class));
        bind(JsonConfigMap.class).toInstance(configMap);
        bind(CommandLineConfig.class).toInstance(config);
    }

}
