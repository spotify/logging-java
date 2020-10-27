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

import static ch.qos.logback.classic.Level.ALL;
import static ch.qos.logback.classic.Level.DEBUG;
import static ch.qos.logback.classic.Level.ERROR;
import static ch.qos.logback.classic.Level.INFO;
import static ch.qos.logback.classic.Level.OFF;
import static ch.qos.logback.classic.Level.TRACE;
import static ch.qos.logback.classic.Level.WARN;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.spi.FilterReply;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

/** LevelRangeFilterTest */
public class LevelRangeFilterTest {

  @Test
  public void verifyNoFilteringPassesAllEvents() {
    final List<Level> allLevels = asList(OFF, ERROR, WARN, INFO, DEBUG, TRACE, ALL);
    final LevelRangeFilter filter = new LevelRangeFilter();
    filter.start();

    for (final Level level : allLevels) {
      final LoggingEvent event = new LoggingEvent();
      event.setLevel(level);
      assertEquals(FilterReply.NEUTRAL, filter.decide(event));
    }
  }

  @Test
  public void verifyMinFilter() {

    final Set<Level> passLevels = new HashSet<Level>(asList(OFF, ERROR, WARN, INFO));
    final Set<Level> denyLevels = new HashSet<Level>(asList(DEBUG, TRACE, ALL));

    final LevelRangeFilter filter = new LevelRangeFilter();
    filter.setLevelMin(INFO);
    filter.start();

    verifyFilter(denyLevels, passLevels, filter);
  }

  @Test
  public void verifyMaxFilter() {

    final Set<Level> denyLevels = new HashSet<Level>(asList(OFF, ERROR, WARN));
    final Set<Level> passLevels = new HashSet<Level>(asList(INFO, DEBUG, TRACE, ALL));

    final LevelRangeFilter filter = new LevelRangeFilter();
    filter.setLevelMax(INFO);
    filter.start();

    verifyFilter(denyLevels, passLevels, filter);
  }

  @Test
  public void verifyMinAndMaxFilter() {

    final Set<Level> denyLevels = new HashSet<Level>(asList(OFF, ERROR, WARN, TRACE, ALL));
    final Set<Level> passLevels = new HashSet<Level>(asList(INFO, DEBUG));

    final LevelRangeFilter filter = new LevelRangeFilter();
    filter.setLevelMax(INFO);
    filter.setLevelMin(DEBUG);
    filter.start();

    verifyFilter(denyLevels, passLevels, filter);
  }

  private void verifyFilter(
      final Set<Level> denyLevels, final Set<Level> passLevels, final LevelRangeFilter filter) {
    for (final Level level : passLevels) {
      final LoggingEvent event = new LoggingEvent();
      event.setLevel(level);
      assertEquals(FilterReply.NEUTRAL, filter.decide(event));
    }

    for (final Level level : denyLevels) {
      final LoggingEvent event = new LoggingEvent();
      event.setLevel(level);
      assertEquals(FilterReply.DENY, filter.decide(event));
    }
  }
}
