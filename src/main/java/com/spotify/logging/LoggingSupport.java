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

import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Java analogue of spotify.util.logging_support module from spotify-common. Does not currently
 * exactly mirror the API of Python logging_support, but tries to be similar for basic
 * functionality.
 *
 * See <a href="https://wiki.spotify.net/wiki/Backend_architecture/Logging">the wiki</a> for more
 * information.
 */
public class LoggingSupport {

  protected static final AtomicLong rid = new AtomicLong();

  /**
   * The ident portion of the log line (comes between the record ID and the log message type).
   * Format of log idents are defined in identities.py in the log-parser git project.
   */
  public static class Ident {
    public static final Ident EMPTY_IDENT = new Ident(0);

    private final String ident;

    /**
     * Create a new ident.
     *
     * @param version   Ident version number. Will only be prepended to ident if not equal to 0.
     * @param identData Objects that will be converted to strings, joined with tab characters, and
     *                  placed inside brackets to make up the ident string.
     */
    public Ident(final int version, final Object... identData) {
      final StringBuilder buf = new StringBuilder();

      if (version != 0) {
        buf.append(version).append(':');
      }
      buf.append('[');

      boolean first = true;
      for (final Object part : identData) {
        if (!first) {
          buf.append('\t');
        } else {
          first = false;
        }
        buf.append(escape(part));
      }

      buf.append(']');

      this.ident = buf.toString();
    }

    public String toString() {
      return ident;
    }
  }

  /**
   * Generate a new debug log message according to the Spotify log format.
   *
   * @param logger  Which Logger to use for writing the log messages. It is assumed that this Logger
   *                is already set up via com.spotify.logging.LoggingConfigurator and a
   *                [service]-log4j.xml configuration so that the time stamp, hostname, service, and
   *                process ID portions of the log message are automatically prepended. See for
   *                example search's <a href="https://git.spotify.net/cgit.cgi/search.git/tree/src/main/java/com/spotify/search/Main.java">Main.java</a>
   *                and <a href="https://git.spotify.net/cgit.cgi/search.git/tree/search-log4j.xml">search-log4j.xml</a>.
   * @param type    Log message type. Log messages are defined in the messages.py module in the
   *                log-parser git project. When a new message type is added or changed in a
   *                service, it should also be changed/added in messages.py.
   * @param version Version of the log message. This is incremented by callers and in messages.py if
   *                the format of a given log message type is changed.
   * @param ident   Ident object to give information generally about a client who made the request
   *                that resulted in this message.
   * @param args    Additional arguments that will be converted to strings, escaped, and appended
   *                (tab-separated) after the log message type and version number.
   */
  public static void debug(final Logger logger, final String type, final int version,
                           final Ident ident, final Object... args) {

    logger.debug(buildLogLine(type, version, ident, args));
  }

  /**
   * Generate a new info log message according to the Spotify log format.
   *
   * @see LoggingSupport#debug for parameter descriptions.
   */
  public static void info(final Logger logger, final String type, final int version,
                          final Ident ident, final Object... args) {

    logger.info(buildLogLine(type, version, ident, args));
  }

  /**
   * Generate a new warn log message according to the Spotify log format.
   *
   * @see LoggingSupport#debug for parameter descriptions.
   */
  public static void warn(final Logger logger, final String type, final int version,
                          final Ident ident, final Object... args) {

    logger.warn(buildLogLine(type, version, ident, args));
  }

  /**
   * Generate a new error log message according to the Spotify log format.
   *
   * @see LoggingSupport#debug for parameter descriptions.
   */
  public static void error(final Logger logger, final String type, final int version,
                           final Ident ident, final Object... args) {

    logger.error(buildLogLine(type, version, ident, args));
  }

  protected static String buildLogLine(final String type, final int version, final Ident ident,
                                       final Object... args) {
    final StringBuilder line = new StringBuilder();
    line.append(LoggingSupport.rid.getAndIncrement()).append(' ');

    if (ident == null) {
      line.append(Ident.EMPTY_IDENT);
    } else {
      line.append(ident);
    }
    line.append(' ');

    line.append(type).append('\t').append(version);

    for (final Object arg : args) {
      line.append('\t').append(escape(arg));
    }

    return line.toString();
  }

  protected static String escape(final Object o) {
    if (o == null) {
      return "";
    }
    return o.toString().replaceAll("[\t\n]", " ");
  }
}
