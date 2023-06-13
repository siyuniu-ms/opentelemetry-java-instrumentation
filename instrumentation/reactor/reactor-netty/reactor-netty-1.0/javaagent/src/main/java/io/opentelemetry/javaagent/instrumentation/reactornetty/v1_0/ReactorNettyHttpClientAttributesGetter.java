/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.reactornetty.v1_0;

import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesGetter;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import javax.annotation.Nullable;
import reactor.netty.http.client.HttpClientConfig;
import reactor.netty.http.client.HttpClientResponse;

final class ReactorNettyHttpClientAttributesGetter
    implements HttpClientAttributesGetter<HttpClientConfig, HttpClientResponse> {

  @Override
  public String getUrlFull(HttpClientConfig request) {
    String uri = request.uri();
    if (isAbsolute(uri)) {
      return uri;
    }

    // use the baseUrl if it was configured
    String baseUrl = request.baseUrl();

    if (uri == null) {
      // internally reactor netty appends "/" to the baseUrl
      return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

    if (baseUrl != null) {
      if (baseUrl.endsWith("/") && uri.startsWith("/")) {
        baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
      }
      return baseUrl + uri;
    }

    // otherwise, use the host+port config to construct the full url
    SocketAddress hostAddress = request.remoteAddress().get();
    if (hostAddress instanceof InetSocketAddress) {
      InetSocketAddress inetHostAddress = (InetSocketAddress) hostAddress;
      return (request.isSecure() ? "https://" : "http://")
          + inetHostAddress.getHostString()
          + ":"
          + inetHostAddress.getPort()
          + (uri.startsWith("/") ? "" : "/")
          + uri;
    }

    return uri;
  }

  private static boolean isAbsolute(String uri) {
    return uri != null && !uri.isEmpty() && !uri.startsWith("/");
  }

  @Override
  public String getHttpRequestMethod(HttpClientConfig request) {
    return request.method().name();
  }

  @Override
  public List<String> getHttpRequestHeader(HttpClientConfig request, String name) {
    return request.headers().getAll(name);
  }

  @Override
  public Integer getHttpResponseStatusCode(
      HttpClientConfig request, HttpClientResponse response, @Nullable Throwable error) {
    return response.status().code();
  }

  @Override
  public List<String> getHttpResponseHeader(
      HttpClientConfig request, HttpClientResponse response, String name) {
    return response.responseHeaders().getAll(name);
  }
}
