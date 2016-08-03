package com.spotify.logging.logback;

import com.google.common.base.Preconditions;

import com.spotify.logging.LoggingConfigurator;

import ch.qos.logback.classic.net.SyslogAppender;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.net.SyslogAppenderBase;

/**
 * A {@link SyslogAppender} that uses millisecond precision, and that by default configures its
 * values for {@link SyslogAppender#syslogHost} and {@link SyslogAppender#port} based on the
 * environment variables specified in {@link LoggingConfigurator#SPOTIFY_SYSLOG_HOST} and
 * {@link LoggingConfigurator#SPOTIFY_SYSLOG_PORT}. If the configuration explicitly sets the
 * {@link SyslogAppenderBase#syslogHost} or {@link SyslogAppenderBase#port} values, the environment
 * variables will not be used. Note that logback's configuration support allows you to use
 * environment variables in your logback.xml file as well (see
 * http://logback.qos.ch/manual/configuration.html#scopes).
 */
@SuppressWarnings("WeakerAccess")
public class SpotifyInternalAppender extends MillisecondPrecisionSyslogAppender {

  private String serviceName;

  private boolean portConfigured = false;

  @Override
  public void start() {
    Preconditions.checkState(serviceName != null, "serviceName must be configured");

    // set up some defaults
    setFacility("LOCAL0");

    // our internal syslog-ng configuration splits logs up based on service name, and expects the
    // format below.
    String serviceAndPid = String.format("%s[%s]", serviceName, LoggingConfigurator.getMyPid());
    setSuffixPattern(serviceAndPid + ": %msg");
    setStackTracePattern(serviceAndPid + ": " + CoreConstants.TAB);

    if (getSyslogHost() == null) {
      setSyslogHost(System.getenv(LoggingConfigurator.SPOTIFY_SYSLOG_HOST));
    }
    checkSetPort(System.getenv(LoggingConfigurator.SPOTIFY_SYSLOG_PORT));

    super.start();
  }

  private void checkSetPort(String environmentValue) {
    if (environmentValue == null || portConfigured) {
      return;
    }

    try {
      setPort(Integer.parseInt(environmentValue));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "unable to parse value for \"" + LoggingConfigurator.SPOTIFY_SYSLOG_PORT + "\" (" +
          environmentValue + ") as an int", e);
    }
  }

  @Override
  public void setPort(int port) {
    portConfigured = true;
    super.setPort(port);
  }

  /**
   * The service name you want to include in the logs - this is a mandatory setting, and determines
   * where syslog-ng will send the log output (/spotify/log/${serviceName}).
   *
   * @param serviceName the service name
   */
  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }
}
