package io.err0.log4j2;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * With thanks to https://www.baeldung.com/log4j2-plugins
 */
@Plugin(name = "Err0Appender",
        category = Core.CATEGORY_NAME,
        elementType = Appender.ELEMENT_TYPE)
public class Err0Appender extends AbstractAppender {

    private static ConcurrentLinkedQueue<LogEvent> queue = new ConcurrentLinkedQueue<>();

    protected Err0Appender(String name, Filter filter) {
        super(name, filter, null);
    }

    @PluginFactory
    public static Err0Appender createAppender(@PluginAttribute("name") String name, @PluginElement("Filter") final Filter filter) {
        // TODO: pickup configuration for appender, or throw an exception
        return new Err0Appender(name, filter);
    }

    @Override
    public void append(LogEvent event) {
        queue.add(event);
        // TODO: run queue, pushing logs as a batch to error 0.
        System.err.println("ERR0\t" + event.getMessage().getFormattedMessage());
    }
}
