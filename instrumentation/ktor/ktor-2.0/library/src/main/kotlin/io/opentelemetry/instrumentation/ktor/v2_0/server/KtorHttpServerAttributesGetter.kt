/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.ktor.v2_0.server

import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpServerAttributesGetter

internal enum class KtorHttpServerAttributesGetter :
  HttpServerAttributesGetter<ApplicationRequest, ApplicationResponse> {
  INSTANCE, ;

  override fun getHttpRequestMethod(request: ApplicationRequest): String {
    return request.httpMethod.value
  }

  override fun getHttpRequestHeader(request: ApplicationRequest, name: String): List<String> {
    return request.headers.getAll(name) ?: emptyList()
  }

  override fun getHttpResponseStatusCode(request: ApplicationRequest, response: ApplicationResponse, error: Throwable?): Int? {
    return response.status()?.value
  }

  override fun getHttpResponseHeader(request: ApplicationRequest, response: ApplicationResponse, name: String): List<String> {
    return response.headers.allValues().getAll(name) ?: emptyList()
  }

  override fun getUrlScheme(request: ApplicationRequest): String {
    return request.origin.scheme
  }

  override fun getUrlPath(request: ApplicationRequest): String {
    return request.path()
  }

  override fun getUrlQuery(request: ApplicationRequest): String {
    return request.queryString()
  }
}
