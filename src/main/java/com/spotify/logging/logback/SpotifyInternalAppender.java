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

import ch.qos.logback.classic.net.SyslogAppender;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.net.SyslogAppenderBase;
import com.spotify.logging.LoggingConfigurator;
import java.lang.management.ManagementFactory;
import javax.annotation.Nullable;

/**
 * A {@link SyslogAppender} that uses millisecond precision, and that by default configures its
 * values for {@link SyslogAppender#syslogHost} and {@link SyslogAppender#port} based on the
 * environment variables specified in {@link LoggingConfigurator#SPOTIFY_SYSLOG_HOST} and {@link
 * LoggingConfigurator#SPOTIFY_SYSLOG_PORT}. If the configuration explicitly sets the {@link
 * SyslogAppenderBase#syslogHost} or {@link SyslogAppenderBase#port} values, the environment
 * variables will not be used. Note that logback's configuration support allows you to use
 * environment variables in your logback.xml file as well (see
 * http://logback.qos.ch/manual/configuration.html#scopes).
 */
@SuppressWarnings("WeakerAccess")
public class SpotifyInternalAppender extends MillisecondPrecisionSyslogAppender {

  private @Nullable String serviceName;

  private boolean portConfigured = false;

  private LoggingConfigurator.ReplaceNewLines replaceNewLines =
      LoggingConfigurator.ReplaceNewLines.OFF;

  @Override
  public void start() {
    if (serviceName == null) {
      throw new IllegalStateException("serviceName must be configured");
    }

    // set up some defaults
    setFacility("LOCAL0");

    // our internal syslog-ng configuration splits logs up based on service name, and expects the
    // format below.
    String serviceAndPid = String.format("%s[%s]", serviceName, getMyPid());
    setSuffixPattern(
        serviceAndPid
            + ": "
            + LoggingConfigurator.ReplaceNewLines.getMsgPattern(this.replaceNewLines));
    setStackTracePattern(serviceAndPid + ": " + CoreConstants.TAB);

    if (getSyslogHost() == null) {
      setSyslogHost(System.getenv(LoggingConfigurator.SPOTIFY_SYSLOG_HOST));
    }
    checkSetPort(System.getenv(LoggingConfigurator.SPOTIFY_SYSLOG_PORT));

    super.start();
  }

  private void checkSetPort(@Nullable String environmentValue) {
    if (environmentValue == null || portConfigured) {
      return;
    }

    try {
      setPort(Integer.parseInt(environmentValue));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "unable to parse value for \""
              + LoggingConfigurator.SPOTIFY_SYSLOG_PORT
              + "\" ("
              + environmentValue
              + ") as an int",
          e);
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

  public void setReplaceNewLines(LoggingConfigurator.ReplaceNewLines replaceNewLines) {
    this.replaceNewLines = replaceNewLines;
  }

  // copied from LoggingConfigurator to avoid making public and exposing externally.
  // TODO (bjorn): We probably want to move this to the utilities project.
  // Also, the portability of this function is not guaranteed.
  static String getMyPid() {
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
}
