/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.servlet.v3_0.snippet;

import io.opentelemetry.javaagent.bootstrap.servlet.InjectionState;

import static java.util.logging.Level.FINE;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.logging.Logger;

public class OutputStreamSnippetInjectionHelper {

  private static final Logger logger =
      Logger.getLogger(OutputStreamSnippetInjectionHelper.class.getName());

  private final String snippet;

  public OutputStreamSnippetInjectionHelper(String snippet) {
    this.snippet = snippet;
  }

  /**
   * return true means this method performed the injection, return false means it didn't inject
   * anything Servlet3OutputStreamWriteAdvice would skip the write method when the return value is
   * true, and would write the original bytes when the return value is false.
   */
  public boolean handleWrite(
      InjectionState state, OutputStream out, byte[] original, int off, int length)
      throws IOException {
    System.out.println("OutputStream byte[] " + Arrays.toString(original));
    if (state.isHeadTagWritten()) {
      return false;
    }
    int endOfHeadTagPosition;
    boolean endOfHeadTagFound = false;
    for (endOfHeadTagPosition = off;
        endOfHeadTagPosition < length && endOfHeadTagPosition - off < length;
        endOfHeadTagPosition++) {
      if (state.processByte(original[endOfHeadTagPosition])) {
        endOfHeadTagFound = true;
        break;
      }
    }
    if (!endOfHeadTagFound) {
      return false;
    }

    if (((SnippetInjectingResponseWrapper)state.getWrapper()).isNotSafeToInject()) {
      return false;
    }
    byte[] snippetBytes;
    try {
      snippetBytes = snippet.getBytes(((SnippetInjectingResponseWrapper) state.getWrapper()).getCharacterEncoding());
    } catch (UnsupportedEncodingException e) {
      logger.log(FINE, "UnsupportedEncodingException", e);
      return false;
    }
    // updating Content-Length before any further writing in case that writing triggers a flush
    ((SnippetInjectingResponseWrapper)state.getWrapper()).updateContentLengthIfPreviouslySet();
    out.write(original, off, endOfHeadTagPosition + 1);
    System.out.println("OutputStream bytes b write snippet");

    out.write(snippetBytes);
    System.out.println("OutputStream bytes b done");

    out.write(original, endOfHeadTagPosition + 1, length - endOfHeadTagPosition - 1);
    return true;
  }

  public boolean handleWrite(InjectionState state, OutputStream out, int b) throws IOException {
    System.out.println("OutputStream b " + b);
    if (state.isHeadTagWritten()) {
      return false;
    }
    if (!state.processByte(b)) {
      return false;
    }

    if (((SnippetInjectingResponseWrapper)state.getWrapper()).isNotSafeToInject()) {
      return false;
    }
    byte[] snippetBytes;
    try {
      snippetBytes = snippet.getBytes(((SnippetInjectingResponseWrapper) state.getWrapper()).getCharacterEncoding());
    } catch (UnsupportedEncodingException e) {
      logger.log(FINE, "UnsupportedEncodingException", e);
      return false;
    }
    ((SnippetInjectingResponseWrapper)state.getWrapper()).updateContentLengthIfPreviouslySet();
    out.write(b);
    System.out.println("OutputStream int b write snippet");
    out.write(snippetBytes);
    System.out.println("OutputStream int b done");

    return true;
  }
}
