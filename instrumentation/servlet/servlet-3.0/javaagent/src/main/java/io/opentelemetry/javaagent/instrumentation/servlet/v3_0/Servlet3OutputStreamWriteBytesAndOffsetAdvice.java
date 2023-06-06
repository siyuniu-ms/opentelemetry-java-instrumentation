/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.servlet.v3_0;

import static io.opentelemetry.javaagent.instrumentation.servlet.v3_0.Servlet3Singletons.getSnippetInjectionHelper;

import io.opentelemetry.javaagent.instrumentation.servlet.snippet.InjectionState;
import io.opentelemetry.javaagent.instrumentation.servlet.v3_0.snippet.ServletOutputStreamInjectionState;
import java.io.IOException;
import javax.servlet.ServletOutputStream;
import net.bytebuddy.asm.Advice;

public class Servlet3OutputStreamWriteBytesAndOffsetAdvice {
  @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class, suppress = Throwable.class)
  public static boolean methodEnter(
      @Advice.This ServletOutputStream servletOutputStream,
      @Advice.Argument(value = 0) byte[] write,
      @Advice.Argument(value = 1) int off,
      @Advice.Argument(value = 2) int len)
      {
    System.out.println("------ Servlet3OutputStreamWriteBytesAndOffsetAdvice");

    try {
      InjectionState state = ServletOutputStreamInjectionState.getInjectionState(
          servletOutputStream);
      if (state == null) {
        return true;
      }

      return !getSnippetInjectionHelper().handleWrite(state, servletOutputStream, write, off, len);
    }catch(Throwable t){
      t.printStackTrace();

    }
    return true;
  }
}
