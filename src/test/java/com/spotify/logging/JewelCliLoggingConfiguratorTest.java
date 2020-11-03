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

import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class JewelCliLoggingConfiguratorTest {

  @Mock private JewelCliLoggingOptions mockOptions;

  @Before
  public void setUp() {
    when(mockOptions.ident()).thenReturn("test");
    when(mockOptions.syslogHost()).thenReturn("");
  }

  @Test
  public void syslog() {
    when(mockOptions.syslog()).thenReturn(true);
    when(mockOptions.logFileName()).thenReturn("");
    JewelCliLoggingConfigurator.configure(mockOptions);

    final org.slf4j.Logger logger = LoggerFactory.getLogger(JewelCliLoggingConfiguratorTest.class);
    logger.error("this is an error message", new Exception());
  }

  @Test
  public void threadlog() throws InterruptedException {
    when(mockOptions.syslog()).thenReturn(false);
    when(mockOptions.logFileName()).thenReturn("logback_test.xml");
    JewelCliLoggingConfigurator.configure(mockOptions);

    final org.slf4j.Logger logger = LoggerFactory.getLogger(JewelCliLoggingConfiguratorTest.class);
    logger.trace("this is a trace message - visible once");
    logger.debug("this is a debug message - visible once");
    logger.info("this is a info message - visible twice");
    logger.error("this is a error message - visible once");
  }

  @Test
  public void nestedExceptionTest() throws Exception {
    when(mockOptions.syslog()).thenReturn(true);
    when(mockOptions.logFileName()).thenReturn("");
    JewelCliLoggingConfigurator.configure(mockOptions);

    final org.slf4j.Logger logger = LoggerFactory.getLogger(JewelCliLoggingConfiguratorTest.class);
    logger.error(
        "this is an error message",
        new Exception("first error", new MyException("inner", new IllegalAccessError())));
  }

  private static class MyException extends Exception implements SingleLineStackTrace {

    private MyException(final String s, final Throwable throwable) {
      super(s, throwable);
    }
  }
}
