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
