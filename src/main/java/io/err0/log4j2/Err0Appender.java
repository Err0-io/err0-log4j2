package io.err0.log4j2;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.*;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
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

        @Override
        public Err0Appender build() {
            return new Err0Appender(name, null, url, token);
        }
    }

    private static ConcurrentLinkedQueue<Err0Log> queue = new ConcurrentLinkedQueue<>();

    protected Err0Appender(String name, Filter filter, String url, String token) {
        super(name, filter, null);
        this.baseUrl = url;
        try {
            this.url = new URL(url + "~/api/bulk-log");
        }
        catch (MalformedURLException e) {
            System.err.println(e.getMessage());
            this.url = null;
        }
        this.token = token;
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                //System.err.println("shutdown hook");
                stopped = true;
                while (pollQueue()) {}
                Err0Http.shutdown();
            }
        }));
        this.thread.setDaemon(true);
        this.thread.start();
    }

    private final String baseUrl;
    private URL url;
    private final String token;
    private final static Pattern pattern = Pattern.compile("\\[([A-Z][A-Z0-9]*-[0-9]+)\\]");
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
                JsonObject bulkLog = new JsonObject();
                JsonArray logs = new JsonArray();
                bulkLog.add("logs", logs);
                for (Err0Log log : list) {
                    //System.err.println("ERR0\t" + log.error_code + "\t" + log.ts + "\t" + log.message + "\t" + log.metadata.toString());

                    JsonObject o = new JsonObject();
                    o.addProperty("error_code", log.error_code);
                    o.addProperty("ts", Long.toString(log.ts));
                    o.addProperty("msg", log.message);
                    o.add("metadata", log.metadata);

                    logs.add(o);
                }
                Err0Http.call(url, token, bulkLog);
                return true;
            }
        }
        catch (Throwable t) {
            // ignore
        }
        return false;
    }

    private final Thread thread = new Thread() {
        @Override
        public void run() {
            for (;!stopped;) {
                if (!Err0Http.canCall()) {
                    Thread.yield();
                } else {
                    boolean wasEmpty = pollQueue();
                    if (wasEmpty) {
                        Thread.yield();
                    }
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
