/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.okhttp.v3_0.internal;

import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesGetter;
import java.util.List;
import javax.annotation.Nullable;
import okhttp3.Request;
import okhttp3.Response;

enum OkHttpAttributesGetter implements HttpClientAttributesGetter<Request, Response> {
  INSTANCE;

  @Override
  public String getHttpRequestMethod(Request request) {
    return request.method();
  }

  @Override
  public String getUrlFull(Request request) {
    return request.url().toString();
  }

  @Override
  public List<String> getHttpRequestHeader(Request request, String name) {
    return request.headers(name);
  }

  @Override
  public Integer getHttpResponseStatusCode(
      Request request, Response response, @Nullable Throwable error) {
    return response.code();
  }

  @Override
  public List<String> getHttpResponseHeader(Request request, Response response, String name) {
    return response.headers(name);
  }
}
