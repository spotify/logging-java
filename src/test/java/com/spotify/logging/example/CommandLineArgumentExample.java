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

import com.spotify.logging.JewelCliLoggingConfigurator;
import com.spotify.logging.JewelCliLoggingOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.flamingpenguin.jewel.cli.ArgumentValidationException;
import uk.co.flamingpenguin.jewel.cli.CliFactory;

/** CommandLineArgumentExample */
public class CommandLineArgumentExample {

  private static final Logger logger = LoggerFactory.getLogger(CommandLineArgumentExample.class);

  private static interface Options extends JewelCliLoggingOptions {}

  /** Run with -info or use the example runner below. */
  public static void main(final String... args) {

    final Options options;
    try {
      options = CliFactory.parseArguments(Options.class, args);
    } catch (ArgumentValidationException e) {
      e.printStackTrace();
      System.exit(1);
      return;
    }

    JewelCliLoggingConfigurator.configure(options);

    while (true) {
      // Should be logged once per iteration.
      logger.info("info!");
      logger.warn("warn!");
      logger.error("error!");

      // Should not be logged.
      logger.trace("trace!");
      logger.debug("debug!");

      try {
        Thread.sleep(1000);
      } catch (InterruptedException ignore) {
      }
    }
  }

  /** Helpers for running the example above with the test logging config file. */
  public static class InfoExampleRunner {

    public static void main(final String... args) {
      CommandLineArgumentExample.main("--info", "--ident=example");
    }
  }

  public static class DefaultExampleRunner {

    public static void main(final String... args) {
      CommandLineArgumentExample.main();
    }
  }
}
