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

import static com.spotify.logging.LoggingConfigurator.addSentryAppender;
import static com.spotify.logging.LoggingConfigurator.getSyslogAppender;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.net.SyslogAppender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.status.Status;
import com.spotify.logging.LoggingConfigurator.ReplaceNewLines;
import com.spotify.logging.logback.CustomLogstashEncoder;
import io.sentry.logback.SentryAppender;
import net.logstash.logback.composite.loggingevent.ArgumentsJsonProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.slf4j.LoggerFactory;

public class LoggingConfiguratorTest {

  @Rule public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Test
  public void testGetSyslogAppender() {
    final LoggerContext context = new LoggerContext();

    SyslogAppender appender =
        (SyslogAppender)
            getSyslogAppender(context, "", -1, LoggingConfigurator.ReplaceNewLines.OFF);
    assertEquals("wrong host", "localhost", appender.getSyslogHost());
    assertEquals("wrong port", 514, appender.getPort());

    appender =
        (SyslogAppender)
            getSyslogAppender(context, null, -1, LoggingConfigurator.ReplaceNewLines.OFF);
    assertEquals("wrong host", "localhost", appender.getSyslogHost());
    assertEquals("wrong port", 514, appender.getPort());

    appender =
        (SyslogAppender)
            getSyslogAppender(context, "host", -1, LoggingConfigurator.ReplaceNewLines.OFF);
    assertEquals("wrong host", "host", appender.getSyslogHost());
    assertEquals("wrong port", 514, appender.getPort());

    appender =
        (SyslogAppender)
            getSyslogAppender(context, null, 999, LoggingConfigurator.ReplaceNewLines.OFF);
    assertEquals("wrong host", "localhost", appender.getSyslogHost());
    assertEquals("wrong port", 999, appender.getPort());
  }

  @Test
  public void testGetSyslogAppenderRespectsNewLineReplacement() {
    final LoggerContext context = new LoggerContext();

    SyslogAppender appender =
        (SyslogAppender)
            getSyslogAppender(context, "", -1, LoggingConfigurator.ReplaceNewLines.OFF);
    assertEquals("%property{ident}[%property{pid}]: %msg", appender.getSuffixPattern());

    appender = (SyslogAppender) getSyslogAppender(context, "", -1, null);
    assertEquals("%property{ident}[%property{pid}]: %msg", appender.getSuffixPattern());

    appender =
        (SyslogAppender) getSyslogAppender(context, "", -1, LoggingConfigurator.ReplaceNewLines.ON);
    assertEquals(
        "%property{ident}[%property{pid}]: %replace(%msg){'[\\r\\n]', ''}",
        appender.getSuffixPattern());
  }

  @Test
  public void testGetSentryAppenderNoErrorsWithDsnOption() {
    SentryAppender sentryAppender =
        addSentryAppender(
            "https://examplePublicKey@o0.ingest.sentry.io/0", LoggingConfigurator.Level.ERROR);

    assertEquals(
        0,
        sentryAppender.getContext().getStatusManager().getCopyOfStatusList().stream()
            .filter(
                status -> {
                  if (status.getLevel() == Status.WARN
                      && status
                          .getMessage()
                          .contains(
                              "Failed to init Sentry during appender initialization: DSN is "
                                  + "required. Use empty string to disable SDK.")) {
                    return true;
                  }

                  return false;
                })
            .count());
  }

  private String getLoggingContextHostnameProperty() {
    final Logger accessPointLogger = (Logger) LoggerFactory.getLogger("logger");
    final LoggerContext loggerContext = accessPointLogger.getLoggerContext();
    return loggerContext.getProperty("hostname");
  }

  @Test
  public void shouldReturnHeliosNonEmptyHostnameWithNoHostname() {
    LoggingConfigurator.configureDefaults();
    assertNotNull(getLoggingContextHostnameProperty());
  }

  @Test
  public void shouldReturnHeliosHostname() {
    environmentVariables.set(LoggingConfigurator.SPOTIFY_HOSTNAME, "hostname");
    LoggingConfigurator.configureDefaults();
    assertEquals("hostname", getLoggingContextHostnameProperty());
  }

  @Test
  public void shouldReturnHeliosNonEmptyHostnameWithNoHostnameForSyslogAppender() {
    LoggingConfigurator.configureSyslogDefaults("idnet");
    assertNotNull(getLoggingContextHostnameProperty());
  }

  @Test
  public void shouldReturnHeliosHostnameWithNoDomainForSyslogAppender() {
    environmentVariables.set(LoggingConfigurator.SPOTIFY_HOSTNAME, "hostname");
    LoggingConfigurator.configureSyslogDefaults("idnet");
    assertEquals("hostname", getLoggingContextHostnameProperty());
  }

  @Test
  public void shouldConfigureLogstashEncoderWithLevel() {
    LoggingConfigurator.configureLogstashEncoderDefaults(LoggingConfigurator.Level.DEBUG);
    assertLogstashEncoder(Level.DEBUG);
  }

  @Test
  public void shouldConfigureDefaultWithIdentAndLevelWhenSyslogEnvVarIsNotSet() {
    LoggingConfigurator.configureDefaults("some-ident", LoggingConfigurator.Level.DEBUG);
    assertDefault("some-ident", Level.DEBUG);
  }

  @Test
  public void shouldConfigureDefaultWithMarkerSupport() {
    LoggingConfigurator.configureDefaults(
        "some-ident", LoggingConfigurator.Level.DEBUG, ReplaceNewLines.OFF, true);
    final Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    final ConsoleAppender<?> stderr = (ConsoleAppender<?>) rootLogger.getAppender("stderr");
    assertTrue(stderr.getEncoder() instanceof PatternLayoutEncoder);
    assertEquals(
        "%date{HH:mm:ss.SSS} %property{ident}[%property{pid}]: %-5level [%thread] %logger{0}: [%marker] %msg%n",
        ((PatternLayoutEncoder) stderr.getEncoder()).getPattern());
  }

  @Test
  public void shouldConfigureDefaultWithoutMarkerSupport() {
    LoggingConfigurator.configureDefaults(
        "some-ident", LoggingConfigurator.Level.DEBUG, ReplaceNewLines.OFF, false);
    final Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    final ConsoleAppender<?> stderr = (ConsoleAppender<?>) rootLogger.getAppender("stderr");
    assertTrue(stderr.getEncoder() instanceof PatternLayoutEncoder);
    assertEquals(
        "%date{HH:mm:ss.SSS} %property{ident}[%property{pid}]: %-5level [%thread] %logger{0}: %msg%n",
        ((PatternLayoutEncoder) stderr.getEncoder()).getPattern());
  }

  @Test
  public void shouldConfigureLogstashEncoderWhenEnvVarIsSetToTrue() {
    environmentVariables.set("USE_JSON_LOGGING", "true");
    LoggingConfigurator.configureService("MyService");
    assertLogstashEncoder(Level.INFO);
  }

  @Test
  public void shouldConfigureDefaultWithServiceNameWhenEnvVarIsNotSet() {
    LoggingConfigurator.configureService("MyService");
    assertDefault("MyService", Level.INFO);
  }

  @Test
  public void shouldConfigureDefaultWithServiceNameWhenEnvVarIsSetToFalse() {
    environmentVariables.set("USE_JSON_LOGGING", "false");
    LoggingConfigurator.configureService("MyService");
    assertDefault("MyService", Level.INFO);
  }

  @Test
  public void shouldConfigureDefaultWithServiceNameWhenEnvVarIsSetToYes() {
    environmentVariables.set("USE_JSON_LOGGING", "yes");
    LoggingConfigurator.configureService("MyService");
    assertDefault("MyService", Level.INFO);
  }

  private void assertLogstashEncoder(final Level level) {
    final Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    final ConsoleAppender<?> stdout = (ConsoleAppender<?>) rootLogger.getAppender("stdout");
    assertTrue(stdout.getEncoder() instanceof CustomLogstashEncoder);
    assertEquals(level, rootLogger.getLevel());
    final CustomLogstashEncoder encoder = (CustomLogstashEncoder) stdout.getEncoder();
    assertEquals(
        1,
        encoder.getProviders().getProviders().stream()
            .filter(ArgumentsJsonProvider.class::isInstance)
            .count());
  }

  private void assertDefault(final String ident, final Level level) {
    final Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    final ConsoleAppender<?> stderr = (ConsoleAppender<?>) rootLogger.getAppender("stderr");
    assertTrue(stderr.getEncoder() instanceof PatternLayoutEncoder);
    assertEquals(level, rootLogger.getLevel());
    final LoggerContext context = rootLogger.getLoggerContext();
    assertEquals(ident, context.getProperty("ident"));
  }
}
