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

package com.spotify.logging.logback;

import static ch.qos.logback.classic.Level.*;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import java.util.*;
import org.junit.Test;

public class LoggerThresholdFilterTest {

  private String spotifyLog = "com.spotify";
  private String appLog = "com.spotify.app";
  private String appPkgLog = "com.spotify.app.subpackage";
  private String appClassLog = "com.spotify.app.Subclass";
  private String spotifyLibraryLog = "com.spotify.library";
  private String orgLog = "org.eclipse";
  private String extPkgLog = "org.eclipse.jetty";
  private String extClassLog = "org.eclipse.jetty.server.Server";
  private String allOrg = "org";
  private String allCom = "com";

  private List<String> variousLoggers =
      asList(
          spotifyLog,
          appLog,
          appPkgLog,
          appClassLog,
          orgLog,
          extPkgLog,
          extClassLog,
          allOrg,
          allCom);

  private List<Level> allLevels = asList(OFF, ERROR, WARN, INFO, DEBUG, TRACE, ALL);

  private Set<String> spotifyLoggers;
  private Set<String> spotifyAppLoggers;
  private Set<String> externalLoggers;

  public LoggerThresholdFilterTest() {
    spotifyLoggers =
        new HashSet<String>(asList(spotifyLog, appLog, appPkgLog, appClassLog, spotifyLibraryLog));
    spotifyAppLoggers = new HashSet<String>(asList(appLog, appPkgLog, appClassLog));
    externalLoggers = new HashSet<String>(asList(orgLog, extPkgLog, extClassLog, allOrg, allCom));
  }

  @Test
  public void verifyNoFilteringPassesAllEvents() {
    final LoggerThresholdFilter filter = new LoggerThresholdFilter();
    filter.start();

    for (Level level : allLevels) {
      final LoggingEvent evt = new LoggingEvent();
      evt.setLevel(level);
      for (String logger : variousLoggers) {
        evt.setLoggerName(logger);
        assertEquals(FilterReply.NEUTRAL, filter.decide(evt));
      }
    }
  }

  @Test
  public void verifyFilteringAllAtWarn() {
    final LoggerThresholdFilter filter = new LoggerThresholdFilter();
    filter.setLevel(WARN);
    filter.start();

    for (Level level : allLevels) {
      final LoggingEvent evt = new LoggingEvent();
      evt.setLevel(level);
      for (String logger : variousLoggers) {
        evt.setLoggerName(logger);
        if (level.isGreaterOrEqual(WARN)) assertEquals(FilterReply.NEUTRAL, filter.decide(evt));
        else assertEquals(FilterReply.DENY, filter.decide(evt));
      }
    }
  }

  @Test
  public void filterSpotifyAtInfoOthersAtWarn() {
    List<Filter<ILoggingEvent>> filters = new ArrayList<Filter<ILoggingEvent>>();

    LoggerThresholdFilter filter = new LoggerThresholdFilter();
    filter.setLogger(spotifyLog);
    filter.setLevel(INFO);
    filter.start();
    filters.add(filter);

    filter = new LoggerThresholdFilter();
    filter.setExceptLogger(spotifyLog);
    filter.setLevel(WARN);
    filter.start();
    filters.add(filter);

    for (Level level : allLevels) {
      final LoggingEvent evt = new LoggingEvent();
      evt.setLevel(level);

      for (String logger : variousLoggers) {
        evt.setLoggerName(logger);

        FilterReply expected;
        FilterReply actual = FilterReply.NEUTRAL;

        if (spotifyLoggers.contains(logger)) {
          if (level.isGreaterOrEqual(INFO)) expected = FilterReply.NEUTRAL;
          else expected = FilterReply.DENY;
        } else {
          if (level.isGreaterOrEqual(WARN)) expected = FilterReply.NEUTRAL;
          else expected = FilterReply.DENY;
        }

        for (Filter<ILoggingEvent> logFilter : filters) {
          FilterReply nextReply = logFilter.decide(evt);
          actual = andFilterReplies(actual, nextReply);
        }

        assertEquals(
            String.format("Logger: %s, Level: %s", logger, level.toString()), expected, actual);
      }
    }
  }

  // this isn't a real AND, but since this is only DENY or NEUTRAL it's ok
  public FilterReply andFilterReplies(FilterReply first, FilterReply second) {
    if (first == FilterReply.DENY || second == FilterReply.DENY) return FilterReply.DENY;
    else return first;
  }
}
