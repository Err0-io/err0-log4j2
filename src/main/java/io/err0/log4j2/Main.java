package io.err0.log4j2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.FileInputStream;

public class Main {

    public static void main(String args[]) {

        try {

            String log4jConfigFile = "log4j2.xml";
            ConfigurationSource source = new ConfigurationSource(new FileInputStream(log4jConfigFile));
            Configurator.initialize(null, source);

            Logger logger = LogManager.getLogger(Main.class);

            logger.debug("[EG-2] debug");
            logger.info("[EG-3] info");
            logger.warn("[EG-4] warn");
            logger.error("[EG-5] error");
            logger.fatal("[EG-6] fatal");

        }
        catch (Throwable t) {
            System.err.println(t.getMessage());
        }
    }
}
