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

package com.spotify.logging.logback;

import net.logstash.logback.encoder.LogstashEncoder;
import net.logstash.logback.fieldnames.LogstashFieldNames;

public class CustomLogstashEncoder extends LogstashEncoder {
  private final LogstashFieldNames logstashFieldNames = new LogstashFieldNames();

  {
    // These are fields we want ignored for all LogstashEncoders
    logstashFieldNames.setLevelValue("[ignore]");
    logstashFieldNames.setVersion("[ignore]");
  }

  public CustomLogstashEncoder() {
    super();
  }

  public CustomLogstashEncoder setupStackdriver() {
    // Setup fields according to https://cloud.google.com/logging/docs/reference/v2/rest/v2/LogEntry
    logstashFieldNames.setTimestamp("time");
    logstashFieldNames.setLevel("severity");
    this.setFieldNames(logstashFieldNames);
    return this;
  }
}
