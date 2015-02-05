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

import net.kencochrane.raven.log4j2.SentryAppender;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.SyslogAppender;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.File;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static java.lang.System.getenv;
import static org.apache.logging.log4j.Level.ALL;
import static org.apache.logging.log4j.core.appender.ConsoleAppender.Target.SYSTEM_ERR;

/**
 * Base configurator of logback for spotify services/tools. LoggingConfigurator.configureDefaults()
 * should generally be called as soon as possible on start-up. The configured logging backend is
 * logback. If the SPOTIFY_SYSLOG_HOST or SPOTIFY_SYSLOG_PORT environment variable is defined,
 * configureDefaults() will use the syslog appender, otherwise it will use the console appender.
 *
 * <p> One aspect of the logging is that we setup a general uncaught exception handler to log
 * uncaught exceptions at info level, if syslog is chosen as logging backend.
 *
 * @see JewelCliLoggingConfigurator For some integration with JewelCLI
 */
public class LoggingConfigurator {

  public static final String DEFAULT_IDENT = "java";

  public enum Level {
    OFF(org.apache.logging.log4j.Level.OFF),
    ERROR(org.apache.logging.log4j.Level.ERROR),
    WARN(org.apache.logging.log4j.Level.WARN),
    INFO(org.apache.logging.log4j.Level.INFO),
    DEBUG(org.apache.logging.log4j.Level.DEBUG),
    TRACE(org.apache.logging.log4j.Level.TRACE),
    ALL(org.apache.logging.log4j.Level.ALL);

    final org.apache.logging.log4j.Level level;

    Level(org.apache.logging.log4j.Level level) {
      this.level = level;
    }
  }

  /**
   * Mute all logging.
   */
  public static void configureNoLogging() {
    reconfigure(Level.OFF, null);
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
   * SPOTIFY_SYSLOG_HOST or SPOTIFY_SYSLOG_PORT environment variable is defined, the syslog
   * appender will be used, otherwise console appender will be.
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
    // Call configureSyslogDefaults if the SPOTIFY_SYSLOG_HOST or SPOTIFY_SYSLOG_PORT env var is
    // set. If this causes a problem, we could introduce a configureConsoleDefaults method which
    // users could call instead to avoid this behavior.
    final String syslogHost = syslogHost();
    final int syslogPort = syslogPort();
    if (syslogHost != null || syslogPort != -1) {
      configureSyslogDefaults(ident, level, syslogHost, syslogPort);
      return;
    }

    reconfigure(level, stdErrAppender(ident));
  }

  /**
   * Configure logging with default behavior and log to syslog using INFO logging level.
   *
   * @param ident Syslog ident to use.
   */
  public static void configureSyslogDefaults(final String ident) {
    configureSyslogDefaults(ident, Level.INFO);
  }

  /**
   * Configure logging with default behavior and log to syslog.
   *
   * @param ident Syslog ident to use.
   * @param level logging level to use.
   */
  public static void configureSyslogDefaults(final String ident, final Level level) {
    final String syslogHost = getenv("SPOTIFY_SYSLOG_HOST");
    final String port = getenv("SPOTIFY_SYSLOG_PORT");
    final int syslogPort = port == null ? -1 : Integer.valueOf(port);
    configureSyslogDefaults(ident, level, syslogHost, syslogPort);
  }

  /**
   * Configure logging with default behavior and log to syslog.
   *
   * @param ident Syslog ident to use.
   * @param level logging level to use.
   * @param host  Hostname or IP address of syslog host.
   * @param port  Port to connect to syslog on.
   */
  public static void configureSyslogDefaults(final String ident, final Level level,
                                             final String host, final int port) {
    reconfigure(level, syslogAppender(host, port, ident));
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
   * Add a sentry appender.
   *
   * @param dsn       the sentry dsn to use (as produced by the sentry webinterface).
   * @param threshold the threshold for log events to be sent to sentry.
   * @return the configured sentry appender.
   */
  public static SentryAppender addSentryAppender(final String dsn, Level threshold) {
    SentryAppender appender = new SentryAppender();
    appender.setDsn(dsn);
    appender.start();

    final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
    final AbstractConfiguration cfg = (AbstractConfiguration) ctx.getConfiguration();

    cfg.addAppender(appender);
    cfg.getRootLogger().addAppender(appender, threshold.level, null);

    return appender;
  }

  /**
   * Create a stderr appender.
   */
  private static Appender stdErrAppender(final String ident) {

    // Setup format
    final Layout<String> layout = PatternLayout.newBuilder()
        .withCharset(Charset.forName("UTF-8"))
        .withPattern("%d{HH:mm:ss.SSS} " + ident + "[" + Util.pid() + "]: " +
                     "%-5level [%thread] %logger{0}: %msg%n")
        .build();

    final Appender appender = ConsoleAppender.newBuilder()
        .setLayout(layout)
        .setName("stderr")
        .setTarget(SYSTEM_ERR)
        .build();

    appender.start();

    return appender;
  }

  /**
   * Create a syslog appender. The appender will use the facility local0. If host is null or an
   * empty string, default to "localhost". If port is less than 0, default to 514.
   */
  static Appender syslogAppender(final String host, final int port, final String ident) {
    final String h = isNullOrEmpty(host) ? "localhost" : host;
    final int p = port <= 0 ? 514 : port;

    final SyslogAppender appender = MillisecondPrecisionSyslogAppender.create(
        "syslog", h, p, ident);

    appender.start();

    return appender;
  }

  /**
   * This is not a public interface and only here to be called from JewelCliLoggingConfigurator.
   * All JewelCli specific functionality should be moved to JewelCliLoggingConfigurator, but that
   * requires some work in defining an interface for *it* to use to configure this cmdline/config
   * format agnostic class.
   *
   * The implementation was moved here from JewelCliLoggingConfigurator as part of creating this
   * class for use in a non_jewelCli (argot, scala) project and so essentially constitutes an
   * improvement rather than a regression, but at some point the work should be put in to define a
   * nice programatical interface to configure spotify logging options.
   *
   * @deprecated Don't use, see docs.
   */
  static void configure(final JewelCliLoggingOptions opts) {
    // Use config file to setup logging if specified, discarding any other logging options.
    if (!opts.logFileName().isEmpty()) {
      configure(new File(opts.logFileName()), opts.ident());
      return;
    }

    // See if syslog host was specified via command line or environment variable.
    // The command line value takes precedence, which defaults to an empty string.
    String syslogHost = opts.syslogHost();
    if (isNullOrEmpty(syslogHost)) {
      syslogHost = syslogHost();
    }

    // See if syslog port was specified via command line or environment variable.
    // The command line value takes precedence, which defaults to -1.
    int syslogPort = opts.syslogPort();
    if (syslogPort < 0) {
      syslogPort = syslogPort();
    }

    final Appender appender;
    if (opts.syslog() || syslogHost != null || syslogPort > 0) {
      appender = syslogAppender(syslogHost, syslogPort, opts.ident());
    } else {
      appender = stdErrAppender(opts.ident());
    }

    final Level level;

    // Setup logging levels
    if (opts.error()) {
      level = Level.ERROR;
    } else if (opts.warn()) {
      level = Level.WARN;
    } else if (opts.info()) {
      level = Level.INFO;
    } else if (opts.debug()) {
      level = Level.DEBUG;
    } else if (opts.trace()) {
      level = Level.TRACE;
    } else {
      level = Level.INFO;
    }

    reconfigure(level, appender);
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
   * @param file         A logback configuration file.
   * @param defaultIdent Fallback logging identity, used if not specified in config file.
   */
  public static void configure(final File file, final String defaultIdent) {
    final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
    final Configuration cfg = ctx.getConfiguration();

    // Log uncaught exceptions
    UncaughtExceptionLogger.setDefaultUncaughtExceptionHandler();

    // Load logging configuration from file
    ctx.setConfigLocation(file.toURI());
    ctx.reconfigure();

    cfg.getProperties().put("pid", Util.pid());

    final String ident = cfg.getProperties().get("ident");
    if (ident == null) {
      cfg.getProperties().put("ident", defaultIdent);
    }
  }

  /**
   * Reconfigure logging to use specified level and appender, clearing out other appenders.
   */
  private static void reconfigure(final Level level, final Appender appender) {
    final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
    final AbstractConfiguration cfg = (AbstractConfiguration) ctx.getConfiguration();

    // Remove and stop all appenders
    clearAppenders(cfg.getRootLogger());
    for (final LoggerConfig loggerConfig : cfg.getLoggers().values()) {
      clearAppenders(loggerConfig);
    }
    stopAppenders(cfg.getAppenders());
    cfg.getAppenders().clear();

    // Setup new appender
    if (appender != null) {
      cfg.addAppender(appender);
      cfg.getRootLogger().addAppender(appender, ALL, null);
    }

    // Setup new logging level
    cfg.getRootLogger().setLevel(level.level);
    for (final LoggerConfig logger : cfg.getLoggers().values()) {
      logger.setLevel(level.level);
    }

    // Take all configuration changes into effect
    ctx.updateLoggers();

    // Log uncaught exceptions
    UncaughtExceptionLogger.setDefaultUncaughtExceptionHandler();
  }

  /**
   * Remove and stop all appenders of a logger.
   */
  private static void clearAppenders(final LoggerConfig config) {
    final Map<String, Appender> appenders = new HashMap<String, Appender>(config.getAppenders());
    for (final String appender : appenders.keySet()) {
      config.removeAppender(appender);
    }
    stopAppenders(appenders);
  }

  /**
   * Stop a set of appenders.
   */
  private static void stopAppenders(final Map<String, Appender> appenders) {
    for (final Appender appender : appenders.values()) {
      appender.stop();
    }
  }

  private static String syslogHost() {
    return emptyToNull(getenv("SPOTIFY_SYSLOG_HOST"));
  }

  private static int syslogPort() {
    final String port = getenv("SPOTIFY_SYSLOG_PORT");
    return isNullOrEmpty(port) ? -1 : Integer.valueOf(port);
  }

  private static String emptyToNull(final String s) {
    return isNullOrEmpty(s) ? null : s;
  }

  private static boolean isNullOrEmpty(final String s) {
    return s == null || s.isEmpty();
  }
}
