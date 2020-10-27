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

import uk.co.flamingpenguin.jewel.cli.Option;

/**
 * This is the interface to have your options interface extend when using
 * JewelCliLoggingConfigurator to configure logging based on the resulting options.
 */
public interface JewelCliLoggingOptions {

  @Option(longName = "syslog", description = "Log to syslog in addition to stderr")
  boolean syslog();

  @Option(
      longName = "syslogHost",
      defaultValue = "",
      description = "Log to syslog at specified host in addition to stderr")
  String syslogHost();

  @Option(
      longName = "syslogPort",
      defaultValue = "-1",
      description = "Log to syslog at specified port in addition to stderr")
  int syslogPort();

  @Option(longName = "trace", description = "Set log level to TRACE")
  boolean trace();

  @Option(longName = "debug", description = "Set log level to DEBUG")
  boolean debug();

  @Option(longName = "info", description = "Set log level to INFO")
  boolean info();

  @Option(longName = "warn", description = "Set log level to WARN")
  boolean warn();

  @Option(longName = "error", description = "Set log level to ERROR")
  boolean error();

  @Option(
      longName = "ident",
      description = "Set ident",
      defaultValue = LoggingConfigurator.DEFAULT_IDENT)
  String ident();

  @Option(
      longName = "logconfig",
      description = "Set log configuration according to a logback configuration file",
      defaultValue = "")
  String logFileName();
}
