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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import javax.annotation.Nullable;

/**
 * Comparable to ch.qos.logback.classic.filter.ThresholdFilter, but for a specific logger. Filters
 * out events for the given logger (and its children) below the threshold level; events for all
 * other loggers are neutral.
 */
public class LoggerThresholdFilter extends Filter<ILoggingEvent> {

  private @Nullable String logger;
  private @Nullable Level level;
  private @Nullable String exceptLogger;

  @Override
  public FilterReply decide(ILoggingEvent event) {
    if (!isStarted()) {
      return FilterReply.NEUTRAL;
    }

    if (logger != null && !event.getLoggerName().startsWith(logger)) {
      return FilterReply.NEUTRAL;
    }

    if (exceptLogger != null && event.getLoggerName().startsWith(exceptLogger)) {
      return FilterReply.NEUTRAL;
    }

    if (level != null && !event.getLevel().isGreaterOrEqual(level)) {
      return FilterReply.DENY;
    }

    return FilterReply.NEUTRAL;
  }

  public void setLogger(String logger) {
    this.logger = logger;
  }

  public void setLevel(Level level) {
    this.level = level;
  }

  public void setExceptLogger(String exceptLogger) {
    this.exceptLogger = exceptLogger;
  }
}
