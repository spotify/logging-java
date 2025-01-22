/*-
 * -\-\-
 * logging
 * --
 * Copyright (C) 2016 - 2020 Spotify AB
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -/-/-
 */

/*
 * Copyright (c) 2012-2014 Spotify AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.spotify.logging;

import static ch.qos.logback.classic.Level.OFF;
import static java.lang.System.getenv;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import com.spotify.logging.logback.CustomLogstashEncoder;
import com.spotify.logging.logback.MillisecondPrecisionSyslogAppender;
import io.sentry.SentryOptions;
import io.sentry.logback.SentryAppender;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;
import net.logstash.logback.composite.loggingevent.ArgumentsJsonProvider;
import org.slf4j.LoggerFactory;

/**
 * Base configurator of logback for spotify services/tools. LoggingConfigurator.configureDefaults()
 * should generally be called as soon as possible on start-up. The configured logging backend is
 * logback. If the SPOTIFY_SYSLOG_HOST or SPOTIFY_SYSLOG_PORT environment variable is defined,
 * configureDefaults() will use the syslog appender, otherwise it will use the console appender.
 *
 * <p>One aspect of the logging is that we setup a general uncaught exception handler to log
 * uncaught exceptions at info level, if syslog is chosen as logging backend.
 *
 * @see JewelCliLoggingConfigurator For some integration with JewelCLI
 */
public class LoggingConfigurator {

  /**
   * Defines if new lines should be replaced in the logged message to avoid potential CRLF
   * injection. See https://find-sec-bugs.github.io/bugs.htm#CRLF_INJECTION_LOGS
   */
  public enum ReplaceNewLines {
    OFF,
    ON;

    public static String getMsgPattern(final ReplaceNewLines replaceNewLines) {
      return replaceNewLines == ReplaceNewLines.ON ? "%replace(%msg){'[\\r\\n]', ''}" : "%msg";
    }
  }

  public static final String DEFAULT_IDENT = "java";
  public static final String SPOTIFY_HOSTNAME = "SPOTIFY_HOSTNAME";
  public static final String SPOTIFY_SYSLOG_HOST = "SPOTIFY_SYSLOG_HOST";
  public static final String SPOTIFY_SYSLOG_PORT = "SPOTIFY_SYSLOG_PORT";
  private static final String USE_JSON_LOGGING = "USE_JSON_LOGGING";

  @SuppressWarnings("unused")
  public enum Level {
    OFF(ch.qos.logback.classic.Level.OFF),
    ERROR(ch.qos.logback.classic.Level.ERROR),
    WARN(ch.qos.logback.classic.Level.WARN),
    INFO(ch.qos.logback.classic.Level.INFO),
    DEBUG(ch.qos.logback.classic.Level.DEBUG),
    TRACE(ch.qos.logback.classic.Level.TRACE),
    ALL(ch.qos.logback.classic.Level.ALL);

    final ch.qos.logback.classic.Level logbackLevel;

    Level(ch.qos.logback.classic.Level logbackLevel) {
      this.logbackLevel = logbackLevel;
    }
  }

  /** Mute all logging. */
  public static void configureNoLogging() {
    final Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    final LoggerContext context = rootLogger.getLoggerContext();

    // Clear context, removing all appenders
    context.reset();

    // Set logging level to OFF
    for (final Logger logger : context.getLoggerList()) {
      if (logger != rootLogger) {
        logger.setLevel(null);
      }
    }
    rootLogger.setLevel(OFF);
  }

  /**
   * Configure logging with default behaviour for a service.
   *
   * <p>Uses INFO logging level. If the USE_JSON_LOGGING environment variable is set to {@code
   * true}, the LogstashEncoder will be used. Otherwise, if the SPOTIFY_SYSLOG_HOST or
   * SPOTIFY_SYSLOG_PORT environment variable is defined, the syslog appender will be used.
   * Otherwise console appender will be used.
   *
   * @param serviceName name of the service
   */
  public static void configureService(final String serviceName) {
    if (getenv(USE_JSON_LOGGING) != null && Boolean.parseBoolean(getenv(USE_JSON_LOGGING))) {
      configureLogstashEncoderDefaults(Level.INFO);
    } else {
      configureDefaults(serviceName);
    }
  }

  /**
   * Configure logging with default behaviour and log to stderr. Uses the {@link #DEFAULT_IDENT}
   * logging identity. Uses INFO logging level. If the SPOTIFY_SYSLOG_HOST or SPOTIFY_SYSLOG_PORT
   * environment variable is defined, the syslog appender will be used, otherwise console appender
   * will be.
   */
  public static void configureDefaults() {
    configureDefaults(DEFAULT_IDENT);
  }

  /**
   * Configure logging with default behaviour and log to stderr using INFO logging level. If the
   * SPOTIFY_SYSLOG_HOST or SPOTIFY_SYSLOG_PORT environment variable is defined, the syslog appender
   * will be used, otherwise console appender will be.
   *
   * @param ident The logging identity.
   */
  public static void configureDefaults(final String ident) {
    configureDefaults(ident, Level.INFO);
  }

  /**
   * Configure logging with default behaviour and log to stderr. If the SPOTIFY_SYSLOG_HOST or
   * SPOTIFY_SYSLOG_PORT environment variable is defined, the syslog appender will be used,
   * otherwise console appender will be.
   *
   * @param ident The logging identity.
   * @param level logging level to use.
   */
  public static void configureDefaults(final String ident, final Level level) {
    configureDefaults(ident, level, ReplaceNewLines.OFF);
  }

  /**
   * Configure logging with default behaviour and log to stderr. If the SPOTIFY_SYSLOG_HOST or
   * SPOTIFY_SYSLOG_PORT environment variable is defined, the syslog appender will be used,
   * otherwise console appender will be.
   *
   * @param ident The logging identity.
   * @param level logging level to use.
   * @param replaceNewLines configures new lines replacement in the messages
   */
  public static void configureDefaults(
      final String ident, final Level level, final ReplaceNewLines replaceNewLines) {
    configureDefaults(ident, level, replaceNewLines, false);
  }

  /**
   * Configure logging with default behaviour and log to stderr. If the SPOTIFY_SYSLOG_HOST or
   * SPOTIFY_SYSLOG_PORT environment variable is defined, the syslog appender will be used,
   * otherwise console appender will be.
   *
   * @param ident The logging identity.
   * @param level logging level to use.
   * @param replaceNewLines configures new lines replacement in the messages
   * @param appendMarkers append markers to the log message
   */
  public static void configureDefaults(
      final String ident,
      final Level level,
      final ReplaceNewLines replaceNewLines,
      final boolean appendMarkers) {
    // Call configureSyslogDefaults if the SPOTIFY_SYSLOG_HOST or SPOTIFY_SYSLOG_PORT env var is
    // set. If this causes a problem, we could introduce a configureConsoleDefaults method which
    // users could call instead to avoid this behavior.
    final @Nullable String syslogHost = getSyslogHost();
    final int syslogPort = getSyslogPort();
    if (syslogHost != null || syslogPort != -1) {
      configureSyslogDefaults(ident, level, syslogHost, syslogPort, replaceNewLines);
      return;
    }

    final Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    // Setup context
    final LoggerContext context = setupLoggerContext(rootLogger, ident);

    // Setup stderr output
    rootLogger.addAppender(getStdErrAppender(context, replaceNewLines, appendMarkers));

    // Setup logging level
    rootLogger.setLevel(level.logbackLevel);

    // Log uncaught exceptions
    UncaughtExceptionLogger.setDefaultUncaughtExceptionHandler();
  }

  /**
   * Configure logging with the LogstashEncoder library.
   * (https://github.com/logstash/logstash-logback-encoder)
   *
   * <p>An appender is configured to send the log messages to stdout. It is expected something like
   * a Docker container will capture these logs for further processing.
   */
  public static void configureLogstashEncoderDefaults(final Level level) {
    final Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    final LoggerContext context = rootLogger.getLoggerContext();

    context.reset();

    final CustomLogstashEncoder encoder = new CustomLogstashEncoder().setupStackdriver();
    encoder.setContext(context);
    encoder.addProvider(new ArgumentsJsonProvider());
    encoder.start();

    final ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
    appender.setTarget("System.out");
    appender.setName("stdout");
    appender.setEncoder(encoder);
    appender.setContext(context);
    appender.start();

    rootLogger.addAppender(appender);
    rootLogger.setLevel(level.logbackLevel);

    UncaughtExceptionLogger.setDefaultUncaughtExceptionHandler();
  }

  /**
   * Configure logging with default behavior and log to syslog using INFO logging level.
   *
   * @param ident Syslog ident to use.
   */
  public static void configureSyslogDefaults(final String ident) {
    configureSyslogDefaults(ident, Level.INFO, ReplaceNewLines.OFF);
  }

  /**
   * Configure logging with default behavior and log to syslog.
   *
   * @param ident Syslog ident to use.
   * @param level logging level to use.
   * @param replaceNewLines configures new lines replacement in the messages
   */
  public static void configureSyslogDefaults(
      final String ident, final Level level, final ReplaceNewLines replaceNewLines) {
    final String syslogHost = getenv(SPOTIFY_SYSLOG_HOST);
    final String port = getenv(SPOTIFY_SYSLOG_PORT);
    final int syslogPort = port == null ? -1 : Integer.parseInt(port);
    configureSyslogDefaults(ident, level, syslogHost, syslogPort, replaceNewLines);
  }

  /**
   * Configure logging with default behavior and log to syslog.
   *
   * @param ident Syslog ident to use.
   * @param level logging level to use.
   * @param host Hostname or IP address of syslog host.
   * @param port Port to connect to syslog on.
   * @param replaceNewLines configures new lines replacement in the messages
   */
  public static void configureSyslogDefaults(
      final String ident,
      final Level level,
      final @Nullable String host,
      final int port,
      final ReplaceNewLines replaceNewLines) {
    configureSyslogDefaults(ident, level, host, port, Logger.ROOT_LOGGER_NAME, replaceNewLines);
  }

  /**
   * Configure logging with default behavior and log to syslog.
   *
   * @param ident Syslog ident to use.
   * @param level logging level to use.
   * @param host Hostname or IP address of syslog host.
   * @param port Port to connect to syslog on.
   * @param loggerName Name of the logger to which the syslog appender will be added
   * @param replaceNewLines configures new lines replacement in the messages
   */
  public static void configureSyslogDefaults(
      final String ident,
      final Level level,
      final @Nullable String host,
      final int port,
      final String loggerName,
      final ReplaceNewLines replaceNewLines) {
    final Logger logger = (Logger) LoggerFactory.getLogger(loggerName);

    // Setup context
    final LoggerContext context = setupLoggerContext(logger, ident);

    // Setup syslog output
    logger.addAppender(getSyslogAppender(context, host, port, replaceNewLines));

    // Setup logging level
    logger.setLevel(level.logbackLevel);

    // Log uncaught exceptions
    UncaughtExceptionLogger.setDefaultUncaughtExceptionHandler();
  }

  /**
   * Add a sentry appender for error log event.
   *
   * @param dsn the sentry dsn to use (as produced by the sentry webinterface).
   * @return the configured sentry appender.
   */
  public static SentryAppender addSentryAppender(final String dsn) {
    return addSentryAppender(dsn, Level.ERROR);
  }

  /**
   * Add a sentry appender. This method is deprecated in favor of {@link #addSentryAppender(String,
   * Level, SentryOptions)}.
   *
   * @param dsn the sentry dsn to use (as produced by the sentry webinterface).
   * @param logLevelThreshold the threshold for log events to be sent to sentry.
   * @return the configured sentry appender.
   * @deprecated Use {@link #addSentryAppender(String, Level, SentryOptions)}.
   */
  @Deprecated
  public static SentryAppender addSentryAppender(final String dsn, Level logLevelThreshold) {
    return addSentryAppender(dsn, logLevelThreshold, new SentryOptions());
  }

  /**
   * Add a sentry appender.
   *
   * @param dsn the sentry dsn to use (as produced by the sentry webinterface).
   * @param logLevelThreshold the threshold for log events to be sent to sentry.
   * @param sentryOptions Options to pass to the sentry appender
   * @return the configured sentry appender.
   */
  public static SentryAppender addSentryAppender(
      final String dsn, Level logLevelThreshold, SentryOptions sentryOptions) {

    final Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    final LoggerContext context = rootLogger.getLoggerContext();

    SentryAppender appender = new SentryAppender();
    sentryOptions.setDsn(dsn);
    appender.setOptions(sentryOptions);

    appender.setContext(context);
    ThresholdFilter levelFilter = new ThresholdFilter();
    levelFilter.setLevel(logLevelThreshold.logbackLevel.toString());
    levelFilter.start();
    appender.setMinimumEventLevel(logLevelThreshold.logbackLevel);
    appender.addFilter(levelFilter);

    appender.start();

    rootLogger.addAppender(appender);

    return appender;
  }

  /**
   * Create a stderr appender.
   *
   * @param context The logger context to use.
   * @param appendMarkers append markers to the log message
   * @return An appender writing to stderr.
   */
  private static Appender<ILoggingEvent> getStdErrAppender(
      final LoggerContext context,
      final ReplaceNewLines replaceNewLines,
      final boolean appendMarkers) {

    // Setup format
    final PatternLayoutEncoder encoder = new PatternLayoutEncoder();
    encoder.setContext(context);
    encoder.setPattern(
        "%date{HH:mm:ss.SSS} %property{ident}[%property{pid}]: %-5level [%thread] %logger{0}: "
            + (appendMarkers ? "[%marker] " : "")
            + ReplaceNewLines.getMsgPattern(replaceNewLines)
            + "%n");
    encoder.setCharset(StandardCharsets.UTF_8);
    encoder.start();

    // Setup stderr appender
    final ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
    appender.setTarget("System.err");
    appender.setName("stderr");
    appender.setEncoder(encoder);
    appender.setContext(context);
    appender.start();

    return appender;
  }

  /**
   * Create a syslog appender. The appender will use the facility local0. If host is null or an
   * empty string, default to "localhost". If port is less than 0, default to 514.
   *
   * @param context The logger context to use.
   * @param host The host running the syslog daemon.
   * @param port The port to connect to.
   * @return An appender that writes to syslog.
   */
  static Appender<ILoggingEvent> getSyslogAppender(
      final LoggerContext context,
      final @Nullable String host,
      final int port,
      final ReplaceNewLines replaceNewLines) {
    final String h = (host == null || host.isEmpty()) ? "localhost" : host;
    final int p = port < 0 ? 514 : port;

    final MillisecondPrecisionSyslogAppender appender = new MillisecondPrecisionSyslogAppender();

    appender.setFacility("LOCAL0");
    appender.setSyslogHost(h);
    appender.setPort(p);
    appender.setName("syslog");
    appender.setCharset(StandardCharsets.UTF_8);
    appender.setContext(context);
    appender.setSuffixPattern(
        "%property{ident}[%property{pid}]: " + ReplaceNewLines.getMsgPattern(replaceNewLines));
    appender.setStackTracePattern("%property{ident}[%property{pid}]: " + CoreConstants.TAB);
    appender.start();

    return appender;
  }

  /**
   * This is not a public interface and only here to be called from JewelCliLoggingConfigurator. All
   * JewelCli specific functionality should be moved to JewelCliLoggingConfigurator, but that
   * requires some work in defining an interface for *it* to use to configure this cmdline/config
   * format agnostic class.
   *
   * <p>The implementation was moved here from JewelCliLoggingConfigurator as part of creating this
   * class for use in a non_jewelCli (argot, scala) project and so essentially constitutes an
   * improvement rather than a regression, but at some point the work should be put in to define a
   * nice programatical interface to configure spotify logging options.
   *
   * @deprecated Don't use, see docs.
   */
  @Deprecated
  static void configure(final JewelCliLoggingOptions opts) {
    // Use logback config file to setup logging if specified, discarding any other logging options.
    if (!opts.logFileName().isEmpty()) {
      configure(new File(opts.logFileName()), opts.ident());
      return;
    }

    final Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    // Log uncaught exceptions
    UncaughtExceptionLogger.setDefaultUncaughtExceptionHandler();

    // Setup context
    final LoggerContext context = setupLoggerContext(rootLogger, opts.ident());

    // See if syslog host was specified via command line or environment variable.
    // The command line value takes precedence, which defaults to an empty string.
    String syslogHost = opts.syslogHost();
    if (syslogHost.isEmpty()) {
      syslogHost = getSyslogHost();
    }

    // See if syslog port was specified via command line or environment variable.
    // The command line value takes precedence, which defaults to -1.
    int syslogPort = opts.syslogPort();
    if (syslogPort < 0) {
      syslogPort = getSyslogPort();
    }

    // Setup syslog logging
    if (opts.syslog() || syslogHost != null || syslogPort > 0) {
      rootLogger.addAppender(
          getSyslogAppender(context, syslogHost, syslogPort, ReplaceNewLines.OFF));
    } else {
      rootLogger.addAppender(getStdErrAppender(context, ReplaceNewLines.OFF, false));
    }

    // Setup default logging level
    rootLogger.setLevel(Level.INFO.logbackLevel);

    // Setup logging levels
    if (opts.error()) {
      rootLogger.setLevel(Level.ERROR.logbackLevel);
    }
    if (opts.warn()) {
      rootLogger.setLevel(Level.WARN.logbackLevel);
    }
    if (opts.info()) {
      rootLogger.setLevel(Level.INFO.logbackLevel);
    }
    if (opts.debug()) {
      rootLogger.setLevel(Level.DEBUG.logbackLevel);
    }
    if (opts.trace()) {
      rootLogger.setLevel(Level.TRACE.logbackLevel);
    }
  }

  /**
   * Configure logging using a logback configuration file.
   *
   * @param file A logback configuration file.
   */
  public static void configure(final File file) {
    configure(file, DEFAULT_IDENT);
  }

  /**
   * Configure logging using a logback configuration file.
   *
   * @param file A logback configuration file.
   * @param defaultIdent Fallback logging identity, used if not specified in config file.
   */
  public static void configure(final File file, final String defaultIdent) {
    final Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    // Setup context
    final LoggerContext context = rootLogger.getLoggerContext();
    context.reset();

    // Log uncaught exceptions
    UncaughtExceptionLogger.setDefaultUncaughtExceptionHandler();

    // Load logging configuration from file
    try {
      final JoranConfigurator configurator = new JoranConfigurator();
      configurator.setContext(context);
      configurator.doConfigure(file);
    } catch (JoranException je) {
      // StatusPrinter will handle this
    }

    context.putProperty("pid", getMyPid());
    final String hostname = getSpotifyHostname();
    if (hostname != null) {
      context.putProperty("hostname", hostname);
    }

    final String ident = context.getProperty("ident");
    if (ident == null) {
      context.putProperty("ident", defaultIdent);
    }

    StatusPrinter.printInCaseOfErrorsOrWarnings(context);
  }

  private static LoggerContext setupLoggerContext(Logger rootLogger, String ident) {
    final LoggerContext context = rootLogger.getLoggerContext();
    context.reset();
    context.putProperty("ident", ident);
    context.putProperty("pid", getMyPid());
    context.putProperty("hostname", getSpotifyHostname());
    return context;
  }

  // TODO (bjorn): We probably want to move this to the utilities project.
  // Also, the portability of this function is not guaranteed.
  private static String getMyPid() {
    String pid = "0";
    try {
      final String nameStr = ManagementFactory.getRuntimeMXBean().getName();

      // XXX (bjorn): Really stupid parsing assuming that nameStr will be of the form
      // "pid@hostname", which is probably not guaranteed.
      pid = nameStr.split("@")[0];
    } catch (RuntimeException e) {
      // Fall through.
    }
    return pid;
  }

  private static @Nullable String getSyslogHost() {
    final String host = System.getenv().getOrDefault(SPOTIFY_SYSLOG_HOST, "");
    return host.isEmpty() ? null : host;
  }

  private static int getSyslogPort() {
    final String port = System.getenv().getOrDefault(SPOTIFY_SYSLOG_PORT, "");
    return port.isEmpty() ? -1 : Integer.parseInt(port);
  }

  private static @Nullable String getSpotifyHostname() {
    final String hostname = System.getenv().getOrDefault(SPOTIFY_HOSTNAME, "");
    return hostname.isEmpty() ? null : hostname;
  }
}
