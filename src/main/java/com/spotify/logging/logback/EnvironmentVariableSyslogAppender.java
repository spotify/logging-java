package com.spotify.logging.logback;

import com.google.common.base.Preconditions;

import com.spotify.logging.LoggingConfigurator;

import ch.qos.logback.classic.net.SyslogAppender;
import ch.qos.logback.core.CoreConstants;

/**
 * A {@link SyslogAppender} that uses millisecond precision, and that by default configures its
 * values for {@link SyslogAppender#syslogHost} and {@link SyslogAppender#port} based on the
 * environment variables specified in {@link #syslogHostEnvVar} and {@link #syslogPortEnvVar}.
 */
public class EnvironmentVariableSyslogAppender extends MillisecondPrecisionSyslogAppender {

  private String syslogHostEnvVar = LoggingConfigurator.SPOTIFY_SYSLOG_HOST;
  private String syslogPortEnvVar = LoggingConfigurator.SPOTIFY_SYSLOG_PORT;
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
      setSyslogHost(System.getenv(syslogHostEnvVar));
    }
    checkSetPort(System.getenv(syslogPortEnvVar));

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
          "unable to parse value for \"" + syslogPortEnvVar + "\" (" +
          environmentValue + ") as an int", e);
    }
  }

  public String getSyslogHostEnvVar() {
    return syslogHostEnvVar;
  }

  public void setSyslogHostEnvVar(String syslogHostEnvVar) {
    this.syslogHostEnvVar = syslogHostEnvVar;
  }

  public String getSyslogPortEnvVar() {
    return syslogPortEnvVar;
  }

  public void setSyslogPortEnvVar(String syslogPortEnvVar) {
    this.syslogPortEnvVar = syslogPortEnvVar;
  }

  @Override
  public void setPort(int port) {
    portConfigured = true;
    super.setPort(port);
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }
}
