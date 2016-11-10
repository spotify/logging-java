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

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.net.SyslogAppender;

import static com.spotify.logging.LoggingConfigurator.getSyslogAppender;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LoggingConfiguratorTest {

  @Rule
  public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Test
  public void testGetSyslogAppender() {
    final LoggerContext context = new LoggerContext();

    SyslogAppender appender = (SyslogAppender) getSyslogAppender(context, "", -1);
    assertEquals("wrong host", "localhost", appender.getSyslogHost());
    assertEquals("wrong port", 514, appender.getPort());

    appender = (SyslogAppender) getSyslogAppender(context, null, -1);
    assertEquals("wrong host", "localhost", appender.getSyslogHost());
    assertEquals("wrong port", 514, appender.getPort());

    appender = (SyslogAppender) getSyslogAppender(context, "host", -1);
    assertEquals("wrong host", "host", appender.getSyslogHost());
    assertEquals("wrong port", 514, appender.getPort());

    appender = (SyslogAppender) getSyslogAppender(context, null, 999);
    assertEquals("wrong host", "localhost", appender.getSyslogHost());
    assertEquals("wrong port", 999, appender.getPort());

  }

  private String getLoggingContextHostnameProperty() {
    final Logger accessPointLogger = (Logger) LoggerFactory.getLogger("logger");
    final LoggerContext loggerContext = accessPointLogger.getLoggerContext();
    return loggerContext.getProperty("hostname");
  }

  @Test
  public void shouldReturnHeliosHostnameWithNoHostname() {
    LoggingConfigurator.configureDefaults();
    assertNull(getLoggingContextHostnameProperty());
  }

  @Test
  public void shouldReturnHeliosHostname() {
    environmentVariables.set(LoggingConfigurator.SPOTIFY_HOSTNAME, "hostname");
    LoggingConfigurator.configureDefaults();
    assertEquals("hostname", getLoggingContextHostnameProperty());
  }

  @Test
  public void shouldReturnHeliosHostnameWithNoHostnameForSyslogAppender() {
    LoggingConfigurator.configureSyslogDefaults("idnet");
    assertNull(getLoggingContextHostnameProperty());
  }

  @Test
  public void shouldReturnHeliosHostnameWithNoDomainForSyslogAppender() {
    environmentVariables.set(LoggingConfigurator.SPOTIFY_HOSTNAME, "hostname");
    LoggingConfigurator.configureSyslogDefaults("idnet");
    assertEquals("hostname", getLoggingContextHostnameProperty());
  }

}
