/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package com.spotify.logging;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.net.Facility;
import org.apache.logging.log4j.core.net.Priority;
import org.apache.logging.log4j.core.pattern.ExtendedThrowablePatternConverter;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.util.NetUtils;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * Formats a log event as a BSD Log record extended with millisecond precision.
 */
final class MillisecondPrecisionSyslogLayout extends AbstractStringLayout {

    private static final long serialVersionUID = 1L;

    private final Facility facility;

    private static final String OS = System.getProperty("os.name");

    // ASL doesn't handle milliseconds.
    private static final boolean MILLISECOND_PRECISION = !OS.equals("Mac OS X");

    private static final String DATE_FORMAT_PATTERN = MILLISECOND_PRECISION
                                                      ? "MMM dd HH:mm:ss.SSS"
                                                      : "MMM dd HH:mm:ss";

    private final SimpleDateFormat dateFormat = new SimpleDateFormat(
        DATE_FORMAT_PATTERN, Locale.ENGLISH);

    /**
     * Host name used to identify messages from this appender.
     */
    private final String localHostname = NetUtils.getLocalHostname();

    private final String ident;
    private final String pid;

    private final boolean appendNewline;
    private final String escapeNewline;

    private final LogEventPatternConverter exceptionConverter;
    private final LogEventPatternConverter singleLineExceptionConverter;

    MillisecondPrecisionSyslogLayout(final Facility facility, final String ident, final String pid,
                                     final boolean appendNewline, final String escapeNewline) {
        super(Charset.forName("UTF-8"));
        this.facility = facility;
        this.ident = ident;
        this.pid = pid;
        this.appendNewline = appendNewline;
        this.escapeNewline = escapeNewline;

        final List<String> exceptionConverterOptions = new ArrayList<String>();
        if (escapeNewline != null) {
            exceptionConverterOptions.add("separator(" + escapeNewline + ")");
        }

        this.exceptionConverter = ExtendedThrowablePatternConverter.newInstance(
            exceptionConverterOptions.toArray(new String[exceptionConverterOptions.size()]));

        exceptionConverterOptions.add("1");
        this.singleLineExceptionConverter = ExtendedThrowablePatternConverter.newInstance(
            exceptionConverterOptions.toArray(new String[exceptionConverterOptions.size()]));
    }

    /**
     * Formats a {@link org.apache.logging.log4j.core.LogEvent} in conformance with the BSD Log record format.
     *
     * @param event The LogEvent
     * @return the event formatted as a String.
     */
    @Override
    public String toSerializable(final LogEvent event) {
        final StringBuilder buf = new StringBuilder();

        buf.append('<');
        buf.append(Priority.getPriority(facility, event.getLevel()));
        buf.append('>');
        addDate(event.getTimeMillis(), buf);
        buf.append(' ');
        buf.append(localHostname);
        buf.append(' ');
        buf.append(ident);
        buf.append('[');
        buf.append(pid);
        buf.append("]");
        buf.append(':');
        buf.append(' ');

        final String message = event.getMessage().getFormattedMessage();
        if (!message.isEmpty()) {
            if (escapeNewline == null) {
                buf.append(message);
            } else {
                for (int i = 0; i < message.length(); i++) {
                    final char c = message.charAt(i);
                    final int next = i + 1;
                    if (c == '\n') {
                        if (next < message.length() && message.charAt(next) == '\r') {
                            i++;
                        }
                        buf.append(escapeNewline);
                    } else if (c == '\r') {
                        if (next < message.length() && message.charAt(next) == '\n') {
                            i++;
                        }
                        buf.append(escapeNewline);
                    } else {
                        buf.append(c);
                    }
                }
            }
        }

        if (event.getThrown() instanceof SingleLineStackTrace) {
            singleLineExceptionConverter.format(event, buf);
        } else {
            exceptionConverter.format(event, buf);
        }

        if (appendNewline) {
            buf.append('\n');
        }

        return buf.toString();
    }

    private void addDate(final long timestamp, final StringBuilder buf) {
        final int index = buf.length() + 4;
        final Date date = new Date(timestamp);
        final String formatted;
        // SimpleDateFormat is not thread safe.
        synchronized (this) {
            formatted = dateFormat.format(date);
        }
        buf.append(formatted);
        //  RFC 3164 says leading space, not leading zero on days 1-9
        if (buf.charAt(index) == '0') {
            buf.setCharAt(index, ' ');
        }
    }

    /**
     * Gets this SyslogLayout's content format. Specified by:
     * <ul>
     * <li>Key: "structured" Value: "false"</li>
     * <li>Key: "dateFormat" Value: "MMM dd HH:mm:ss[.SSS]"</li>
     * <li>Key: "format" Value: "&lt;LEVEL&gt;TIMESTAMP PROP(HOSTNAME) IDENT[PID]: MESSAGE"</li>
     * <li>Key: "formatType" Value: "logfilepatternreceiver" (format uses the keywords supported by
     * LogFilePatternReceiver)</li>
     * </ul>
     * 
     * @return Map of content format keys supporting SyslogLayout
     */
    @Override
    public Map<String, String> getContentFormat() {
        final Map<String, String> result = new HashMap<String, String>();
        result.put("structured", "false");
        result.put("formatType", "logfilepatternreceiver");
        result.put("dateFormat", DATE_FORMAT_PATTERN);
        result.put("format", "<LEVEL>TIMESTAMP PROP(HOSTNAME) IDENT[PID]: MESSAGE");
        return result;
    }
}
