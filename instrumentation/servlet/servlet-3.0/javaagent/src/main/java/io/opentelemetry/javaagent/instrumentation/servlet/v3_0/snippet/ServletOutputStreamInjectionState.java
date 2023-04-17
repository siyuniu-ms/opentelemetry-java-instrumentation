/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.servlet.v3_0.snippet;

import io.opentelemetry.instrumentation.api.util.VirtualField;
import io.opentelemetry.javaagent.bootstrap.servlet.InjectionState;
import javax.annotation.Nullable;
import javax.servlet.ServletOutputStream;

public class ServletOutputStreamInjectionState {

  private static final VirtualField<ServletOutputStream, InjectionState> virtualField =
      VirtualField.find(ServletOutputStream.class, InjectionState.class);

  public static void initializeInjectionStateIfNeeded(
      ServletOutputStream servletOutputStream, SnippetInjectingResponseWrapper wrapper) {
    InjectionState state = virtualField.get(servletOutputStream);
    System.out.println("initializeInjectionStateIfNeeded state         " + state + wrapper.isContentTypeTextHtml());
    if (!wrapper.isContentTypeTextHtml()) {
      virtualField.set(servletOutputStream, null);
      System.out.println("state null set to be       "+state);
      return;
    }
    if (state == null || state.getWrapper() != wrapper) {
      state = new InjectionState(wrapper);
      virtualField.set(servletOutputStream, state);
      System.out.println("state set to be     "+state + "for servlet "+servletOutputStream);
    }
  }

  @Nullable
  public static InjectionState getInjectionState(ServletOutputStream servletOutputStream) {
    System.out.println("getInjectionState      " + servletOutputStream);
    InjectionState state = virtualField.get(servletOutputStream);
//    System.out.println("virtualField get        "+ state);
//    System.out.println("InjectionState line 36-------\n");
    return state;
  }

  private ServletOutputStreamInjectionState() {}
}
