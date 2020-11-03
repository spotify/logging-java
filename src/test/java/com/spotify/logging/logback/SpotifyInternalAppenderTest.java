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

package com.spotify.logging.logback;

import static com.spotify.logging.logback.SpotifyInternalAppender.getMyPid;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import ch.qos.logback.classic.Logger;
import com.spotify.logging.LoggingConfigurator;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.ExpectedException;
import org.slf4j.LoggerFactory;

public class SpotifyInternalAppenderTest {

  private SpotifyInternalAppender appender;

  @Rule public final EnvironmentVariables environmentVariables = new EnvironmentVariables();
  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    appender = new SpotifyInternalAppender();
    appender.setContext(
        ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).getLoggerContext());
    appender.setServiceName("myservice");
  }

  @Test
  public void shouldUseEnvironmentVariableForHostByDefault() throws Exception {
    setSyslogHostEnvVar();

    appender.start();

    assertThat(appender.getSyslogHost(), is("www.spotify.com"));
  }

  @Test
  public void shouldUseEnvironmentVariableForPortByDefault() throws Exception {
    environmentVariables.set(LoggingConfigurator.SPOTIFY_SYSLOG_PORT, "7642");

    appender.start();

    assertThat(appender.getPort(), is(7642));
  }

  @Test
  public void shouldFailForNonIntPort() throws Exception {
    environmentVariables.set(LoggingConfigurator.SPOTIFY_SYSLOG_PORT, "76424356436234623462345");

    thrown.expect(IllegalArgumentException.class);
    appender.start();
  }

  @Test
  public void shouldSupportOverridingHost() throws Exception {
    setSyslogHostEnvVar();

    appender.setSyslogHost("www.dn.se");
    appender.start();

    assertThat(appender.getSyslogHost(), is("www.dn.se"));
  }

  @Test
  public void shouldSupportOverridingPort() throws Exception {
    environmentVariables.set(LoggingConfigurator.SPOTIFY_SYSLOG_PORT, "7642");
    appender.setPort(9878);

    appender.start();

    assertThat(appender.getPort(), is(9878));
  }

  @Test
  public void shouldSupportOverridingPortTo514() throws Exception {
    environmentVariables.set(LoggingConfigurator.SPOTIFY_SYSLOG_PORT, "7642");
    appender.setPort(514);

    appender.start();

    assertThat(appender.getPort(), is(514));
  }

  @Test
  public void shouldFailIfServiceNameMissing() throws Exception {
    appender = new SpotifyInternalAppender();
    appender.setContext(
        ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).getLoggerContext());

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("serviceName must be configured");

    appender.start();
  }

  @Test
  public void shouldAddServiceNameToSuffixPattern() throws Exception {
    appender.start();

    assertThat(appender.getSuffixPattern(), containsString("myservice"));
  }

  @Test
  public void shouldAddPidToSuffixPattern() throws Exception {
    appender.start();

    assertThat(appender.getSuffixPattern(), containsString(getMyPid()));
  }

  @Test
  public void shouldAddServiceNameToStackTracePattern() throws Exception {
    appender.start();

    assertThat(appender.getStackTracePattern(), containsString("myservice"));
  }

  @Test
  public void shouldAddPidToStackTracePattern() throws Exception {
    appender.start();

    assertThat(appender.getStackTracePattern(), containsString(getMyPid()));
  }

  private void setSyslogHostEnvVar() {
    // this must be a valid host name that can be looked up anywhere
    environmentVariables.set(LoggingConfigurator.SPOTIFY_SYSLOG_HOST, "www.spotify.com");
  }
}
