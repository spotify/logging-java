package com.spotify.logging;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.SyslogAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.net.AbstractSocketManager;
import org.apache.logging.log4j.core.net.Protocol;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.util.EnglishEnums;

import java.io.Serializable;

import static org.apache.logging.log4j.core.net.Facility.LOCAL0;

@Plugin(name = "MillisecondPrecisionSyslog", category = "Core", elementType = "appender", printObject = true)
public class MillisecondPrecisionSyslogAppender extends SyslogAppender {

  private static final boolean IGNORE_LOGGING_EXCEPTIONS = true;
  private static final boolean IMMEDIATE_FLUSH = true;
  private static final Filter NO_FILTER = null;
  private static final int NO_RECONNECTION_DELAY = 0;
  private static final boolean NO_APPEND_NEWLINE = false;
  private static final String NO_ESCAPE_NEWLINE = null;

  private static final int CONNECT_TIMEOUT_5000_MILLIS = 5000;
  private static final boolean IMMEDIATE_FAIL = true;
  private static final SslConfiguration NO_SSL_CONFIG = null;

  private final String host;
  private final int port;

  private MillisecondPrecisionSyslogAppender(final String name, final String host,
                                             final int port,
                                             final Layout<? extends Serializable> layout,
                                             final Filter filter, final boolean ignoreExceptions,
                                             final boolean immediateFlush,
                                             final AbstractSocketManager manager) {
    super(name, layout, filter, ignoreExceptions, immediateFlush, manager, null);
    this.host = host;
    this.port = port;
  }

  public String host() {
    return host;
  }

  public int port() {
    return port;
  }

  static MillisecondPrecisionSyslogAppender create(final String name, final String host,
                                                   final int port, final String ident) {
    return create(
        name, host, port, Protocol.UDP, ident, NO_FILTER, IGNORE_LOGGING_EXCEPTIONS,
        IMMEDIATE_FLUSH, NO_APPEND_NEWLINE, NO_ESCAPE_NEWLINE);
  }

  static MillisecondPrecisionSyslogAppender create(final String name, final String host,
                                                   final int port, final Protocol protocol,
                                                   final String ident,
                                                   final Filter filter,
                                                   final boolean ignoreExceptions,
                                                   final boolean immediateFlush,
                                                   final boolean appendNewline,
                                                   final String escapeNewline) {

    final Layout<? extends Serializable> layout = new MillisecondPrecisionSyslogLayout(
        LOCAL0, ident, Util.pid(), appendNewline, escapeNewline);

    final AbstractSocketManager manager = createSocketManager(
        name, protocol, host, port, CONNECT_TIMEOUT_5000_MILLIS, NO_SSL_CONFIG,
        NO_RECONNECTION_DELAY, IMMEDIATE_FAIL, layout);

    return new MillisecondPrecisionSyslogAppender(
        name, host, port, layout, filter, ignoreExceptions, immediateFlush, manager);
  }

  @PluginFactory
  public static MillisecondPrecisionSyslogAppender createAppender(
      @PluginAttribute("name") final String name,
      @PluginAttribute(value = "host", defaultString = "127.0.0.1") final String host,
      @PluginAttribute(value = "port", defaultInt = 514) final int port,
      @PluginAttribute(value = "protocol", defaultString = "UDP") final String protocolStr,
      @PluginAttribute("ident") final String ident,
      @PluginElement("Filter") final Filter filter,
      @PluginAttribute("immediateFlush") final String immediateFlushStr,
      @PluginAttribute(value = "ignoreExceptions", defaultBoolean = true) final boolean ignoreExceptions) {
    if (name == null) {
      LOGGER.error("No name provided for MillisecondPrecisionSyslogAppender");
      return null;
    }
    if (ident == null) {
      LOGGER.error("No ident provided for MillisecondPrecisionSyslogAppender");
      return null;
    }
    final Protocol protocol = EnglishEnums.valueOf(Protocol.class, protocolStr);
    final boolean immediateFlush;
    if (immediateFlushStr != null) {
      immediateFlush = Boolean.parseBoolean(immediateFlushStr);
    } else {
      // When using UDP, send one packet per log message
      immediateFlush = (protocol == Protocol.UDP);
    }
    final boolean appendNewline = (protocol == Protocol.TCP);
    final String escapeNewline = (protocol == Protocol.TCP) ? "\t" : null;
    return MillisecondPrecisionSyslogAppender.create(
        name, host, port, protocol, ident, filter, ignoreExceptions, immediateFlush, appendNewline,
        escapeNewline);
  }
}
