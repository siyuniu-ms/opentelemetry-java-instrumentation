/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.servlet.v3_0.snippet;

import static io.opentelemetry.javaagent.instrumentation.servlet.v3_0.snippet.TestUtil.readFileAsBytes;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import io.opentelemetry.javaagent.bootstrap.servlet.InjectionState;
import org.junit.jupiter.api.Test;

class SnippetServletOutputStreamTest {

  @Test
  void testInjectionForStringContainHeadTag() throws IOException {
    String snippet = "\n  <script type=\"text/javascript\"> Test </script>";
    byte[] html = readFileAsBytes("beforeSnippetInjection.html");

    InjectionState obj = createInjectionStateForTesting(snippet, UTF_8);
    InMemoryServletOutputStream out = new InMemoryServletOutputStream();

    OutputStreamSnippetInjectionHelper helper = new OutputStreamSnippetInjectionHelper(snippet);
    boolean injected = helper.handleWrite(obj, out, html, 0, html.length);
    assertThat(obj.getHeadTagBytesSeen()).isEqualTo(-1);
    assertThat(injected).isEqualTo(true);

    byte[] expectedHtml = readFileAsBytes("afterSnippetInjection.html");
    assertThat(out.getBytes()).isEqualTo(expectedHtml);
  }

  @Test
  void testInjectionForChinese() throws IOException {
    String snippet = "\n  <script type=\"text/javascript\"> Test </script>";
    byte[] html = readFileAsBytes("beforeSnippetInjectionChinese.html");

    InjectionState obj = createInjectionStateForTesting(snippet, UTF_8);
    InMemoryServletOutputStream out = new InMemoryServletOutputStream();

    OutputStreamSnippetInjectionHelper helper = new OutputStreamSnippetInjectionHelper(snippet);
    boolean injected = helper.handleWrite(obj, out, html, 0, html.length);

    byte[] expectedHtml = readFileAsBytes("afterSnippetInjectionChinese.html");
    assertThat(injected).isTrue();
    assertThat(obj.getHeadTagBytesSeen()).isEqualTo(-1);
    assertThat(out.getBytes()).isEqualTo(expectedHtml);
  }

  @Test
  void testInjectionForStringWithoutHeadTag() throws IOException {
    String snippet = "\n  <script type=\"text/javascript\"> Test </script>";
    byte[] html = readFileAsBytes("htmlWithoutHeadTag.html");

    InjectionState obj = createInjectionStateForTesting(snippet, UTF_8);
    InMemoryServletOutputStream out = new InMemoryServletOutputStream();

    OutputStreamSnippetInjectionHelper helper = new OutputStreamSnippetInjectionHelper(snippet);
    boolean injected = helper.handleWrite(obj, out, html, 0, html.length);

    assertThat(injected).isFalse();
    assertThat(obj.getHeadTagBytesSeen()).isEqualTo(0);
    assertThat(out.getBytes()).isEmpty();
  }

  @Test
  void testHeadTagSplitAcrossTwoWrites() throws IOException {
    String snippet = "\n  <script type=\"text/javascript\"> Test </script>";
    String htmlFirstPart = "<!DOCTYPE html>\n<html lang=\"en\">\n<he";
    byte[] htmlFirstPartBytes = htmlFirstPart.getBytes(UTF_8);

    InjectionState obj = createInjectionStateForTesting(snippet, UTF_8);
    InMemoryServletOutputStream out = new InMemoryServletOutputStream();

    OutputStreamSnippetInjectionHelper helper = new OutputStreamSnippetInjectionHelper(snippet);
    boolean injected =
        helper.handleWrite(obj, out, htmlFirstPartBytes, 0, htmlFirstPartBytes.length);

    assertThat(injected).isFalse();
    assertThat(obj.getHeadTagBytesSeen()).isEqualTo(3);
    assertThat(out.getBytes()).isEmpty();

    String htmlSecondPart =
        "ad>\n"
            + "  <meta charset=\"UTF-8\">\n"
            + "  <title>Title</title>\n"
            + "</head>\n"
            + "<body>\n"
            + "\n"
            + "</body>\n"
            + "</html>";
    byte[] htmlSecondPartBytes = htmlSecondPart.getBytes(UTF_8);
    injected = helper.handleWrite(obj, out, htmlSecondPartBytes, 0, htmlSecondPartBytes.length);

    assertThat(injected).isTrue();
    assertThat(obj.getHeadTagBytesSeen()).isEqualTo(-1);

    String expectedSecondPart =
        "ad>\n"
            + "  <script type=\"text/javascript\"> Test </script>\n"
            + "  <meta charset=\"UTF-8\">\n"
            + "  <title>Title</title>\n"
            + "</head>\n"
            + "<body>\n"
            + "\n"
            + "</body>\n"
            + "</html>";
    assertThat(out.getBytes()).isEqualTo(expectedSecondPart.getBytes(UTF_8));
  }

  private static InjectionState createInjectionStateForTesting(String snippet, Charset charset) {
    HttpServletResponse response = mock(HttpServletResponse.class);
    when(response.isCommitted()).thenReturn(false);
    when(response.getCharacterEncoding()).thenReturn(charset.name());

    return new InjectionState(new SnippetInjectingResponseWrapper(response, snippet));
  }

  private static class InMemoryServletOutputStream extends ServletOutputStream {

    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

    @Override
    public void write(int b) {
      baos.write(b);
    }

    public byte[] getBytes() {
      return baos.toByteArray();
    }
  }
}
