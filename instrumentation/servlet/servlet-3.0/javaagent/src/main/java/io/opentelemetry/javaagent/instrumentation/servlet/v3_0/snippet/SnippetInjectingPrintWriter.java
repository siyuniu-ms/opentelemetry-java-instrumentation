/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.servlet.v3_0.snippet;

import io.opentelemetry.javaagent.bootstrap.servlet.InjectionState;
import java.io.PrintWriter;
import java.util.Arrays;

public class SnippetInjectingPrintWriter extends PrintWriter {
  private final String snippet;
  private final InjectionState state;

  public SnippetInjectingPrintWriter(
      PrintWriter writer, String snippet, SnippetInjectingResponseWrapper wrapper) {
    super(writer);
    System.out.println("SnippetInjectingPrintWriter init");
    state = new InjectionState(wrapper);
    this.snippet = snippet;
  }

  @Override
  public void write(String s, int off, int len) {
    System.out.println("SnippetInjectingPrintWriter s " + s);

    if (state.isHeadTagWritten()) {
      super.write(s, off, len);
      return;
    }
    for (int i = off; i < s.length() && i - off < len; i++) {
      write(s.charAt(i));
    }
  }

  @Override
  public void write(int b) {
    System.out.println("SnippetInjectingPrintWriter b " + b);
    super.write(b);
    if (state.isHeadTagWritten()) {
      return;
    }
    boolean endOfHeadTagFound = state.processByte(b);
    if (!endOfHeadTagFound) {
      return;
    }

    if (((SnippetInjectingResponseWrapper)state.getWrapper()).isNotSafeToInject()) {
      return;
    }
    ((SnippetInjectingResponseWrapper)state.getWrapper()).updateContentLengthIfPreviouslySet();
    System.out.println("SnippetInjectingPrintWriter write snippet");

    super.write(snippet);
    System.out.println("SnippetInjectingPrintWriter done");

  }

  @Override
  public void write(char[] buf, int off, int len) {
    System.out.println("SnippetInjectingPrintWriter buf " + Arrays.toString(buf));

    if (state.isHeadTagWritten()) {
      super.write(buf, off, len);
      return;
    }
    for (int i = off; i < buf.length && i - off < len; i++) {
      write(buf[i]);
    }
  }
}
