package com.spotify.logging.logback;

import com.spotify.logging.LoggingConfigurator;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.ExpectedException;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class EnvironmentVariableSyslogAppenderTest {

  private EnvironmentVariableSyslogAppender appender;

  @Rule
  public final EnvironmentVariables environmentVariables = new EnvironmentVariables();
  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    appender = new EnvironmentVariableSyslogAppender();
    appender.setContext(((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).getLoggerContext());
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
  public void shouldSupportCustomisingEnvironmentVariableForHost() throws Exception {
    environmentVariables.set("MY_HOST_ENVVAR", "www.spotify.com");

    appender.setSyslogHostEnvVar("MY_HOST_ENVVAR");

    appender.start();

    assertThat(appender.getSyslogHost(), is("www.spotify.com"));
  }

  @Test
  public void shouldSupportCustomisingEnvironmentVariableForPort() throws Exception {
    environmentVariables.set("MY_PORT_ENVVAR", "5242");

    appender.setSyslogPortEnvVar("MY_PORT_ENVVAR");

    appender.start();

    assertThat(appender.getPort(), is(5242));
  }

  @Test
  public void shouldFailIfServiceNameMissing() throws Exception {
    appender = new EnvironmentVariableSyslogAppender();
    appender.setContext(((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).getLoggerContext());

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("serviceName must be configured");

    appender.start();
  }

  @Test
  public void shouldAddIdentToSuffixPattern() throws Exception {
    appender.start();

    assertThat(appender.getSuffixPattern(), containsString("myservice"));
  }

  @Test
  public void shouldAddPidToSuffixPattern() throws Exception {
    appender.start();

    assertThat(appender.getSuffixPattern(), containsString(LoggingConfigurator.getMyPid()));
  }

  @Test
  public void shouldAddIdentToStackTracePattern() throws Exception {
    appender.start();

    assertThat(appender.getStackTracePattern(), containsString("myservice"));
  }

  @Test
  public void shouldAddPidToStackTracePattern() throws Exception {
    appender.start();

    assertThat(appender.getStackTracePattern(), containsString(LoggingConfigurator.getMyPid()));
  }

  private void setSyslogHostEnvVar() {
    // this must be a valid host name that can be looked up anywhere
    environmentVariables.set(LoggingConfigurator.SPOTIFY_SYSLOG_HOST, "www.spotify.com");
  }
}