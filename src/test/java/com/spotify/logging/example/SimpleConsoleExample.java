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

package com.spotify.logging.example;

import static com.spotify.logging.LoggingConfigurator.Level.INFO;

import com.spotify.logging.LoggingConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** SimpleConsoleExample */
public class SimpleConsoleExample {

  private static final Logger logger = LoggerFactory.getLogger(SimpleConsoleExample.class);

  public static void main(final String... args) {

    LoggingConfigurator.configureDefaults("example", INFO, LoggingConfigurator.ReplaceNewLines.ON);

    while (true) {
      // Should not be logged
      logger.trace("trace!");

      // Should show up in console
      logger.debug("debug!");
      logger.info("info!");
      logger.warn("warn!");
      logger.error("\nerror!\n", new Exception("failure"));

      try {
        Thread.sleep(1000);
      } catch (InterruptedException ignore) {
      }
    }
  }
}
