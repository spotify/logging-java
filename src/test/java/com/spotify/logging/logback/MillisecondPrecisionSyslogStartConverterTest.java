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
 * Copyright (c) 2012-2016 Spotify AB
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

import static java.util.Arrays.asList;
import static org.junit.Assert.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.Test;

public class MillisecondPrecisionSyslogStartConverterTest {

  final String HOSTNAME = "contextPropertyHostname";

  @Test
  public void shouldLogContextPropertyHostname() {
    LoggerContext context = new LoggerContext();
    context.putProperty("hostname", HOSTNAME);

    MillisecondPrecisionSyslogStartConverter millisecondPrecisionSyslogStartConverter =
        new MillisecondPrecisionSyslogStartConverter();
    millisecondPrecisionSyslogStartConverter.setContext(context);
    millisecondPrecisionSyslogStartConverter.setOptionList(asList("LOCAL0"));
    millisecondPrecisionSyslogStartConverter.start();

    LoggingEvent event = new LoggingEvent();
    event.setLevel(Level.INFO);
    String message = millisecondPrecisionSyslogStartConverter.convert(event);

    assertTrue(message.contains(HOSTNAME));
  }
}
