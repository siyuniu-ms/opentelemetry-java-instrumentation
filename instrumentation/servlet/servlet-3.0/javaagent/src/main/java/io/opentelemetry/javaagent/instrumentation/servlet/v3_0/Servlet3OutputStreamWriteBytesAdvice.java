/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.servlet.v3_0;

import static io.opentelemetry.javaagent.instrumentation.servlet.v3_0.Servlet3Singletons.getSnippetInjectionHelper;

import io.opentelemetry.javaagent.instrumentation.servlet.snippet.InjectionState;
import io.opentelemetry.javaagent.instrumentation.servlet.v3_0.snippet.ServletOutputStreamInjectionState;
import javax.servlet.ServletOutputStream;
import net.bytebuddy.asm.Advice;


public class Servlet3OutputStreamWriteBytesAdvice {

  @SuppressWarnings("CatchAndPrintStackTrace")
  @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class, suppress = Throwable.class)
  public static boolean methodEnter(
      @Advice.This ServletOutputStream servletOutputStream, @Advice.Argument(0) byte[] write)
       {
    System.out.println("------ Servlet3OutputStreamWriteBytesAdvice");

    try {
      InjectionState state = ServletOutputStreamInjectionState.getInjectionState(
          servletOutputStream);
      if (state == null) {
        return true;
      }
      return !getSnippetInjectionHelper()
          .handleWrite(state, servletOutputStream, write, 0, write.length);
    } catch(Throwable t){
      t.printStackTrace();
//      System.out.println("warning"+t.p);

    }
    return true;
  }
}
