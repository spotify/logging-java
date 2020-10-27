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
 * Filters events within the levels levelMin and levelMax, inclusive.
 *
 * <p>Events with a level above levelMax (if specified) or a level below levelMin (if specified)
 * will be denied. Events that pass these criteria will get a FilterReply.NEUTRAL result to allow
 * the rest of the filter chain process the event.
 */
public class LevelRangeFilter extends Filter<ILoggingEvent> {

  private @Nullable Level levelMax;
  private @Nullable Level levelMin;

  @Override
  public FilterReply decide(final ILoggingEvent event) {
    if (!isStarted()) {
      return FilterReply.NEUTRAL;
    }

    if (levelMin != null && event.getLevel().levelInt < levelMin.levelInt) {
      return FilterReply.DENY;
    }

    if (levelMax != null && event.getLevel().levelInt > levelMax.levelInt) {
      return FilterReply.DENY;
    }

    return FilterReply.NEUTRAL;
  }

  public Level getLevelMax() {
    return levelMax;
  }

  public void setLevelMax(final Level levelMax) {
    this.levelMax = levelMax;
  }

  public Level getLevelMin() {
    return levelMin;
  }

  public void setLevelMin(final Level levelMin) {
    this.levelMin = levelMin;
  }

  public void start() {
    super.start();
  }
}
