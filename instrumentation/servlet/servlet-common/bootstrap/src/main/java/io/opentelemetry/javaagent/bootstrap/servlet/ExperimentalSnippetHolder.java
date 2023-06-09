/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.bootstrap.servlet;

import io.opentelemetry.instrumentation.api.internal.ConfigPropertiesUtil;

public class ExperimentalSnippetHolder {

  private static String snippet = getSnippetSetting();

  private static String getSnippetSetting() {
    String result = ConfigPropertiesUtil.getString("otel.experimental.javascript-snippet");
    return result == null ? "" : result;
  }

  public static void setSnippet(String newValue) {
    snippet = newValue;
  }

  public static String getSnippet() {
    return snippet;
  }

  private ExperimentalSnippetHolder() {}
}
