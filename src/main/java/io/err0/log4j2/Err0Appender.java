package io.err0.log4j2;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.*;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * With thanks to https://www.baeldung.com/log4j2-plugins
 */
@Plugin(name = "Err0Appender",
        category = Core.CATEGORY_NAME)
public class Err0Appender extends AbstractAppender {

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<Err0Appender> {

        @PluginBuilderAttribute("name")
        @Required
        private String name;

        @PluginBuilderAttribute("token")
        @Required
        private String token;

        @PluginBuilderAttribute("url")
        @Required
        private String url;

        // ... additional properties

        // ... getters and setters

        @Override
        public Err0Appender build() {
            return new Err0Appender(name, null, url, token);
        }
    }

    private static ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();

    protected Err0Appender(String name, Filter filter, String url, String token) {
        super(name, filter, null);
        this.url = url;
        this.token = token;
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                System.err.println("shutdown hook");
                stopped = true;
                while (pollQueue()) {}
            }
        }));
        this.thread.setDaemon(true);
        this.thread.start();
    }

    private final String url;
    private final String token;
    private boolean stopped = false;

    private boolean pollQueue() {
        try {
            ArrayList<String> list = new ArrayList<>();
            String logEvent = null;
            do {
                logEvent = queue.poll();
                if (null != logEvent) {
                    list.add(logEvent);
                }
            } while (null != logEvent);
            if (list.size() > 0) {
                // TODO: send to ERR0
                for (String event : list) {
                    // TODO: run queue, pushing logs as a batch to error 0.
                    System.err.println("ERR0\t" + event);
                }
                return true;
            }
        }
        catch (Throwable t) {
            System.err.println("ERR0\t" + t.getMessage());
        }
        return false;
    }

    private final Thread thread = new Thread() {
        @Override
        public void run() {
            for (;!stopped;) {
                boolean wasEmpty = pollQueue();
                if (wasEmpty) {
                    Thread.yield();
                }
            }
        }
    };

    @Override
    public void append(LogEvent event) {
        queue.add(event.getMessage().getFormattedMessage());
    }
}
