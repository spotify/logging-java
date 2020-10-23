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

// XXX (bjorn): The portability of this code is unknown.

import java.io.File;

/**
 * Configures logging according to logging related options given at the jewel cli command line. The
 * application's command line options should extend JewelCliLoggingOptions and then call:
 *
 * <p>
 *
 * <pre>
 *   JewelCliLoggingConfigurator.configure(opts);
 * </pre>
 *
 * <p>Prior to command line parsing having been completed, the application is suggested to
 * initialize logging as soon as possible using LoggingConfigurator.
 *
 * @see LoggingConfigurator
 */
public class JewelCliLoggingConfigurator {

  @SuppressWarnings("deprecation")
  public static void configure(final JewelCliLoggingOptions opts) {
    LoggingConfigurator.configure(opts);
  }

  public static void configure(final File file) {
    LoggingConfigurator.configure(file);
  }
}
