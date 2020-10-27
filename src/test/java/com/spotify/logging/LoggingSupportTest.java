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

package com.spotify.logging;

import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

public class LoggingSupportTest {

  private Logger logger;

  @Before
  public void setUp() {
    LoggingSupport.rid.set(0);
    this.logger = Mockito.mock(Logger.class);
  }

  @Test
  public void doLogBasic() {
    LoggingSupport.info(logger, "FooLog", 2, LoggingSupport.Ident.EMPTY_IDENT);
    verify(logger).info("0 [] FooLog\t2");
  }

  @Test
  public void doLogRidIncrements() {
    LoggingSupport.info(logger, "FooLog", 2, LoggingSupport.Ident.EMPTY_IDENT);
    verify(logger).info("0 [] FooLog\t2");
    LoggingSupport.info(logger, "FooLog", 2, LoggingSupport.Ident.EMPTY_IDENT);
    verify(logger).info("1 [] FooLog\t2");
  }

  @Test
  public void doLogEscape() {
    LoggingSupport.info(logger, "FooLog", 2, null, "foo\t", "ba\nr");
    verify(logger).info("0 [] FooLog\t2\tfoo \tba r");
  }

  @Test
  public void doLogIdent() {
    final LoggingSupport.Ident ident = new LoggingSupport.Ident(1, "hello", 5);
    LoggingSupport.info(logger, "FooLog", 2, ident, "somefield");
    verify(logger).info("0 1:[hello\t5] FooLog\t2\tsomefield");
  }

  @Test
  public void doLogIdentVersionZero() {
    final LoggingSupport.Ident ident = new LoggingSupport.Ident(0, "hello", 5);
    LoggingSupport.info(logger, "FooLog", 2, ident, "somefield");
    verify(logger).info("0 [hello\t5] FooLog\t2\tsomefield");
  }

  @Test
  public void doLogNullField() {
    LoggingSupport.info(logger, "FooLog", 2, null, null, "bar");
    verify(logger).info("0 [] FooLog\t2\t\tbar");
  }

  @Test
  public void doLogNullIdentField() {
    final LoggingSupport.Ident ident = new LoggingSupport.Ident(1, "hello", null);
    LoggingSupport.info(logger, "FooLog", 2, ident, "somefield");
    verify(logger).info("0 1:[hello\t] FooLog\t2\tsomefield");
  }

  @Test
  public void doLogEscapedIdent() {
    final LoggingSupport.Ident ident = new LoggingSupport.Ident(1, "hello", "foo\tbar\n", "baz");
    LoggingSupport.info(logger, "FooLog", 2, ident, "somefield");
    verify(logger).info("0 1:[hello\tfoo bar \tbaz] FooLog\t2\tsomefield");
  }
}
