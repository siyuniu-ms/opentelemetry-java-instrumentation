/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.servlet.v5_0.snippet;

import static io.opentelemetry.javaagent.instrumentation.servlet.v5_0.snippet.ServletOutputStreamInjectionState.initializeInjectionStateIfNeeded;
import static java.util.logging.Level.FINE;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

public class SnippetInjectingResponseWrapper extends HttpServletResponseWrapper {

  private static final Logger logger = Logger.getLogger(HttpServletResponseWrapper.class.getName());

  public static final String FAKE_SNIPPET_HEADER = "FAKE_SNIPPET_HEADER";

  private static final int UNSET = -1;
  private final String snippet;
  private final int snippetLength;

  private long contentLength = UNSET;

  private SnippetInjectingPrintWriter snippetInjectingPrintWriter = null;

  public SnippetInjectingResponseWrapper(HttpServletResponse response, String snippet) {
    super(response);
    this.snippet = snippet;
    snippetLength = snippet.length();
  }

  @Override
  public boolean containsHeader(String name) {
    // this function is overridden in order to make sure the response is wrapped
    // but not wrapped twice
    // we don't use req.setAttribute
    // because async requests pass down their attributes, but don't pass down our wrapped response
    // and so we would see the presence of the attribute and think the response was already wrapped
    // when it really is not
    // see also https://docs.oracle.com/javaee/7/api/javax/servlet/AsyncContext.html
    if (name.equals(FAKE_SNIPPET_HEADER)) {
      return true;
    }
    return super.containsHeader(name);
  }

  @Override
  public void setHeader(String name, String value) {
    // checking content-type is just an optimization to avoid unnecessary parsing
    if ("Content-Length".equalsIgnoreCase(name) && isContentTypeTextHtml()) {
      try {
        contentLength = Long.parseLong(value);
      } catch (NumberFormatException ex) {
        logger.log(FINE, "NumberFormatException", ex);
      }
    }
    super.setHeader(name, value);
  }

  @Override
  public void addHeader(String name, String value) {
    // checking content-type is just an optimization to avoid unnecessary parsing
    if ("Content-Length".equalsIgnoreCase(name) && isContentTypeTextHtml()) {
      try {
        contentLength = Long.parseLong(value);
      } catch (NumberFormatException ex) {
        logger.log(FINE, "NumberFormatException", ex);
      }
    }
    super.addHeader(name, value);
  }

  @Override
  public void setIntHeader(String name, int value) {
    // checking content-type is just an optimization to avoid unnecessary parsing
    if ("Content-Length".equalsIgnoreCase(name) && isContentTypeTextHtml()) {
      contentLength = value;
    }
    super.setIntHeader(name, value);
  }

  @Override
  public void addIntHeader(String name, int value) {
    // checking content-type is just an optimization to avoid unnecessary parsing
    if ("Content-Length".equalsIgnoreCase(name) && isContentTypeTextHtml()) {
      contentLength = value;
    }
    super.addIntHeader(name, value);
  }

  @Override
  public void setContentLength(int len) {
    contentLength = len;
    super.setContentLength(len);
  }

  @Override
  public void setContentLengthLong(long length) {
    contentLength = length;
    super.setContentLengthLong(length);
  }

  boolean isContentTypeTextHtml() {
    String contentType = super.getContentType();
    if (contentType == null) {
      contentType = super.getHeader("content-type");
    }
    return contentType != null && contentType.startsWith("text/html");
  }

  @Override
  public ServletOutputStream getOutputStream() throws IOException {
    ServletOutputStream output = super.getOutputStream();
    initializeInjectionStateIfNeeded(output, this);
    return output;
  }

  @Override
  public PrintWriter getWriter() throws IOException {
    if (!isContentTypeTextHtml()) {
      return super.getWriter();
    }
    if (snippetInjectingPrintWriter == null) {
      snippetInjectingPrintWriter =
          new SnippetInjectingPrintWriter(super.getWriter(), snippet, this);
    }
    return snippetInjectingPrintWriter;
  }

  void updateContentLengthIfPreviouslySet() {
    if (contentLength != UNSET) {
      setContentLength((int) contentLength + snippetLength);
    }
  }

  boolean isNotSafeToInject() {
    // if content-length was set and response was already committed (headers sent to the client),
    // then it is not safe to inject because the content-length header cannot be updated to account
    // for the snippet length
    return contentLength != UNSET && isCommitted();
  }
}
