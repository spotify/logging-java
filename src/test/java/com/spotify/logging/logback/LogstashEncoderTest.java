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

import static net.logstash.logback.argument.StructuredArguments.value;
import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.spotify.logging.LoggingConfigurator;
import com.spotify.logging.LoggingConfigurator.Level;
import net.logstash.logback.encoder.LogstashEncoder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogstashEncoderTest {

  private static final Logger log = LoggerFactory.getLogger(LogstashEncoder.class);

  private final ObjectMapper mapper = new ObjectMapper();

  @Rule public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

  @Test
  public void shouldIncludeStructuredArguments() throws JsonProcessingException {
    LoggingConfigurator.configureLogstashEncoderDefaults(Level.INFO);
    log.info(
        "foo={} bar={} list={} map={} thing={}",
        value("foo", 17),
        value("bar", "quux"),
        value("list", ImmutableList.of(1, 2)),
        value("map", ImmutableMap.of("a", 3, "b", 4)),
        value("thing", new Thing(5, "6")));
    final String log = systemOutRule.getLog();
    final JsonNode parsedMessage = mapper.readTree(log);
    assertEquals(
        "foo=17 bar=quux list=[1, 2] map={a=3, b=4} thing=Thing{v1=5, v2='6'}",
        parsedMessage.get("message").asText());
    assertEquals(17, parsedMessage.get("foo").asInt());
    assertEquals("quux", parsedMessage.get("bar").asText());
    assertEquals(mapper.createArrayNode().add(1).add(2), parsedMessage.get("list"));
    assertEquals(mapper.createObjectNode().put("a", 3).put("b", 4), parsedMessage.get("map"));
    assertEquals(mapper.createObjectNode().put("v1", 5).put("v2", "6"), parsedMessage.get("thing"));
  }

  public static class Thing {

    private final int v1;
    private final String v2;

    public Thing(int v1, String v2) {
      this.v1 = v1;
      this.v2 = v2;
    }

    public int getV1() {
      return v1;
    }

    public String getV2() {
      return v2;
    }

    @Override
    public String toString() {
      return "Thing{" + "v1=" + v1 + ", v2='" + v2 + '\'' + '}';
    }
  }
}
