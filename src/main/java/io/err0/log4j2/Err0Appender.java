package io.err0.log4j2;

import com.google.gson.JsonObject;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.*;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * With thanks to https://www.baeldung.com/log4j2-plugins
 */
@Plugin(name = "Err0Appender",
        category = Core.CATEGORY_NAME)
public class Err0Appender extends AbstractAppender {

    public static class Err0Log {
        public Err0Log(final String error_code, final long ts, final String message, final JsonObject metadata) {
            this.error_code = error_code;
            this.ts = ts;
            this.message = message;
            this.metadata = metadata;
        }
        public final String error_code;
        public final long ts;
        public final String message;
        public final JsonObject metadata;
    }

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

        @PluginBuilderAttribute("realm_uuid")
        @Required
        private String realm_uuid;

        @PluginBuilderAttribute("prj_uuid")
        @Required
        private String prj_uuid;

        @PluginBuilderAttribute("pattern")
        @Required
        private String pattern;

        @Override
        public Err0Appender build() {
            return new Err0Appender(name, null, url, token, realm_uuid, prj_uuid, pattern);
        }
    }

    private static ConcurrentLinkedQueue<Err0Log> queue = new ConcurrentLinkedQueue<>();

    protected Err0Appender(String name, Filter filter, String url, String token, String realm_uuid, String prj_uuid, String pattern) {
        super(name, filter, null);
        this.url = url;
        this.token = token;
        this.realm_uuid = realm_uuid;
        this.prj_uuid = prj_uuid;
        this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
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
    private final String realm_uuid;
    private final String prj_uuid;
    private final Pattern pattern;
    private boolean stopped = false;

    private boolean pollQueue() {
        try {
            ArrayList<Err0Log> list = new ArrayList<>();
            Err0Log logEvent = null;
            do {
                logEvent = queue.poll();
                if (null != logEvent) {
                    list.add(logEvent);
                }
            } while (null != logEvent);
            if (list.size() > 0) {
                // TODO: send to ERR0
                for (Err0Log log : list) {
                    // TODO: run queue, pushing logs as a batch to error 0.
                    System.err.println("ERR0\t" + log.error_code + "\t" + log.ts + "\t" + log.message + "\t" + log.metadata.toString());
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
        final String formattedMessage = event.getMessage().getFormattedMessage();
        final Matcher matcher = pattern.matcher(formattedMessage);
        if (matcher.find()) {
            final String error_code = matcher.group(1);
            final long ts = event.getTimeMillis();
            final JsonObject metadata = new JsonObject();
            final JsonObject log4j2Metadata = new JsonObject();
            final Level level = event.getLevel();
            final StackTraceElement source = event.getSource();
            log4j2Metadata.addProperty("level", level.name());
            log4j2Metadata.addProperty("source_class", source.getClassName());
            log4j2Metadata.addProperty("source_file", source.getFileName());
            log4j2Metadata.addProperty("source_line", source.getLineNumber());
            metadata.add("log4j2", log4j2Metadata);
            queue.add(new Err0Log(error_code, ts, formattedMessage, metadata));
        }
    }
}
