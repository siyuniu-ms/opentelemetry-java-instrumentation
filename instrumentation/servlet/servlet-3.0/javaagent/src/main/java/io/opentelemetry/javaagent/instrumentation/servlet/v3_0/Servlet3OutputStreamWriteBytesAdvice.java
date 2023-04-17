/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.servlet.v3_0;


import io.opentelemetry.javaagent.bootstrap.servlet.InjectionState;
import io.opentelemetry.javaagent.instrumentation.servlet.v3_0.snippet.ServletOutputStreamInjectionState;
import java.io.IOException;
import java.util.Arrays;
import javax.servlet.ServletOutputStream;
import net.bytebuddy.asm.Advice;

import static io.opentelemetry.javaagent.instrumentation.servlet.v3_0.Servlet3Singletons.getSnippetInjectionHelper;

public class Servlet3OutputStreamWriteBytesAdvice {

  @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class, suppress = Throwable.class)
  public static boolean methodEnter(
      @Advice.This ServletOutputStream servletOutputStream, @Advice.Argument(0) byte[] write)
      throws IOException {

    InjectionState state = ServletOutputStreamInjectionState.getInjectionState(servletOutputStream);
    System.out.println("Servlet3OutputStreamWriteBytesAdvice " + state );
    System.out.write(write);
    System.out.println("Advice"  + Arrays.toString(write) );
    if (state == null) {
      return true;
    }
    // if handleWrite returns true, then it means the original bytes + the snippet were written
    // to the servletOutputStream, and so we no longer need to execute the original method
    // call (see skipOn above)
    // if it returns false, then it means nothing was written to the servletOutputStream and the
    // original method call should be executed
    return !getSnippetInjectionHelper()
        .handleWrite(state, servletOutputStream, write, 0, write.length);
  }
}
