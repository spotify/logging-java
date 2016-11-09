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

package com.spotify.logging.logback;

import com.google.common.base.Optional;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ch.qos.logback.classic.pattern.SyslogStartConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.util.LevelToSyslogSeverity;
import ch.qos.logback.core.net.SyslogAppenderBase;

/**
 * A {@link SyslogStartConverter} with millisecond timestamp
 * precision.
 */
public class MillisecondPrecisionSyslogStartConverter extends SyslogStartConverter {

  private long lastTimestamp = -1;
  private String timestampStr = null;
  private SimpleDateFormat simpleFormat;
  private String localHostName;
  private int facility;

  private static final String os = System.getProperty("os.name");

  public void start() {
    int errorCount = 0;

    final String facilityStr = getFirstOption();
    if (facilityStr == null) {
      addError("was expecting a facility string as an option");
      return;
    }

    facility = SyslogAppenderBase.facilityStringToint(facilityStr);

    localHostName = Optional.fromNullable(getContext().getProperty("hostname"))
        .or(getLocalHostname());

    try {
      // ASL doesn't handle milliseconds.
      if (os.equals("Mac OS X")) {
        simpleFormat = new SimpleDateFormat("MMM dd HH:mm:ss", new DateFormatSymbols(Locale.US));
      } else {
        simpleFormat = new SimpleDateFormat("MMM dd HH:mm:ss.SSS", new DateFormatSymbols(Locale.US));
      }
    } catch (IllegalArgumentException e) {
      addError("Could not instantiate SimpleDateFormat", e);
      errorCount++;
    }

    if (errorCount == 0) {
      super.start();
    }
  }

  public String convert(final ILoggingEvent event) {
    final StringBuilder sb = new StringBuilder();

    final int pri = facility + LevelToSyslogSeverity.convert(event);

    sb.append("<");
    sb.append(pri);
    sb.append(">");
    sb.append(computeTimeStampString(event.getTimeStamp()));
    sb.append(' ');
    sb.append(localHostName);
    sb.append(' ');

    return sb.toString();
  }

  String computeTimeStampString(final long now) {
    synchronized (this) {
      if (now != lastTimestamp) {
        lastTimestamp = now;
        timestampStr = simpleFormat.format(new Date(now));
      }
      return timestampStr;
    }
  }
}


