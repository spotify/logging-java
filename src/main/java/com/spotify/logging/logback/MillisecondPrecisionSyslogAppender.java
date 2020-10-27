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

package com.spotify.logging.logback;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.net.SyslogAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.net.SyslogAppenderBase;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;

/** A {@link SyslogAppender} with millisecond timestamp precision. */
public class MillisecondPrecisionSyslogAppender extends SyslogAppender {
  private Charset charset = StandardCharsets.UTF_8;
  private final PatternLayout stackTraceLayout = new PatternLayout();
  private @Nullable OutputStream sos;

  @Override
  public void start() {
    super.start();
    sos = getSyslogOutputStream();
    setupStackTraceLayout();
  }

  String getPrefixPattern() {
    return "%syslogStart{" + getFacility() + "}%nopex{}";
  }

  @Override
  protected void append(ILoggingEvent eventObject) {
    // code based on ch.qos.logback.core.net.SyslogAppenderBase.append()
    if (!isStarted()) {
      return;
    }

    try {
      String msg = getLayout().doLayout(eventObject);
      if (msg == null) {
        return;
      }
      if (msg.length() > getMaxMessageSize()) {
        msg = msg.substring(0, getMaxMessageSize());
      }
      assert sos != null;
      sos.write(msg.getBytes(charset));
      sos.flush();
      postProcess(eventObject, sos);
    } catch (IOException ioe) {
      addError("Failed to send diagram to " + getSyslogHost(), ioe);
    }
  }

  @Override
  protected void postProcess(final Object eventObject, final OutputStream sw) {
    if (isThrowableExcluded()) {
      return;
    }

    final ILoggingEvent event = (ILoggingEvent) eventObject;
    final IThrowableProxy tp = event.getThrowableProxy();

    if (tp == null) {
      return;
    }

    final String stackTracePrefix = stackTraceLayout.doLayout(event);
    recursiveWrite(sw, stackTracePrefix, tp, 0, null);
  }

  private void recursiveWrite(
      final OutputStream sw,
      final String stackTracePrefix,
      final IThrowableProxy tp,
      final int indent,
      final @Nullable String firstLinePrefix) {
    final StackTraceElementProxy[] stepArray = tp.getStackTraceElementProxyArray();
    try {
      handleThrowableFirstLine(sw, tp, stackTracePrefix, indent, firstLinePrefix);
      for (final StackTraceElementProxy step : stepArray) {
        final StringBuilder sb = new StringBuilder();
        sb.append(stackTracePrefix);
        addIndent(sb, indent);
        sb.append(step);
        sw.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        sw.flush();
      }
    } catch (IOException e) {
      return;
    }

    final IThrowableProxy[] suppressed = tp.getSuppressed();
    if (suppressed != null) {
      for (final IThrowableProxy current : suppressed) {
        recursiveWrite(sw, stackTracePrefix, current, indent + 1, CoreConstants.SUPPRESSED);
      }
    }

    final IThrowableProxy cause = tp.getCause();
    if (cause != null) {
      recursiveWrite(sw, stackTracePrefix, cause, indent, CoreConstants.CAUSED_BY);
    }
  }

  private void addIndent(final StringBuilder sb, final int indent) {
    for (int i = 0; i < indent; i++) {
      sb.append(CoreConstants.TAB);
    }
  }

  // LOGBACK-411 and LOGBACK-750
  private void handleThrowableFirstLine(
      final OutputStream sw,
      final IThrowableProxy tp,
      final String stackTracePrefix,
      final int indent,
      final @Nullable String prefix)
      throws IOException {
    StringBuilder sb = new StringBuilder().append(stackTracePrefix);
    addIndent(sb, indent);
    if (prefix != null) {
      sb.append(prefix);
    }
    sb.append(tp.getClassName()).append(": ").append(tp.getMessage());
    sw.write(sb.toString().getBytes(StandardCharsets.UTF_8));
    sw.flush();
  }

  @Override
  public Layout<ILoggingEvent> buildLayout() {
    final PatternLayout layout = new PatternLayout();
    layout
        .getInstanceConverterMap()
        .put("syslogStart", MillisecondPrecisionSyslogStartConverter.class.getName());
    if (suffixPattern == null) {
      suffixPattern = DEFAULT_SUFFIX_PATTERN;
    }
    layout.setPattern(getPrefixPattern() + suffixPattern);
    layout.setContext(getContext());
    layout.start();
    return layout;
  }

  private void setupStackTraceLayout() {
    stackTraceLayout
        .getInstanceConverterMap()
        .put("syslogStart", MillisecondPrecisionSyslogStartConverter.class.getName());

    stackTraceLayout.setPattern(getPrefixPattern() + getStackTracePattern());
    stackTraceLayout.setContext(getContext());
    stackTraceLayout.start();
  }

  // Horrible hack to access the syslog stream through reflection
  private OutputStream getSyslogOutputStream() {
    Field f;
    try {
      f = SyslogAppenderBase.class.getDeclaredField("sos");
      f.setAccessible(true);
      return (OutputStream) f.get(this);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  /** @return the charset used for encoding the output */
  public Charset getCharset() {
    return charset;
  }

  /** @param charset the charset to use for encoding the output */
  public void setCharset(Charset charset) {
    this.charset = charset;
  }
}
