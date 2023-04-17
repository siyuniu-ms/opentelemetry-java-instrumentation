/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.bootstrap.servlet;

public class ExperimentalSnippetHolder {

  private static volatile String snippet = "";

  private static boolean isSet = false;

  public static void setSnippet(String snippet) {
    if (isSet) {
      return;
    }
    ExperimentalSnippetHolder.snippet = snippet;
    System.out.println("otel snippet set to be "+snippet.length() + " content"+snippet);
    isSet = true;
  }

  public static String getSnippet() {
    System.out.println("otel getSnippet "+snippet.length());
    return snippet;
  }

  private ExperimentalSnippetHolder() {}
}
