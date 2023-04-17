/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.servlet.v3_0;

import static io.opentelemetry.javaagent.instrumentation.servlet.v3_0.Servlet3Singletons.getSnippetInjectionHelper;

//import com.google.common.base.VerifyException;
import io.opentelemetry.javaagent.bootstrap.servlet.InjectionState;
import io.opentelemetry.javaagent.instrumentation.servlet.v3_0.snippet.ServletOutputStreamInjectionState;

import javax.servlet.ServletOutputStream;
import net.bytebuddy.asm.Advice;

public class Servlet3OutputStreamWriteBytesAndOffsetAdvice {
  @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
  public static boolean methodEnter(
      @Advice.This ServletOutputStream servletOutputStream,
      @Advice.Argument(value = 0) byte[] write,
      @Advice.Argument(value = 1) int off,
      @Advice.Argument(value = 2) int len)
       {
    System.out.println("Advice Servlet3OutputStreamWriteBytesAndOffsetAdvice");
    try{
      InjectionState state = ServletOutputStreamInjectionState.getInjectionState(servletOutputStream);


//      System.out.println("Advice line 27-------\n");
      System.out.println("Servlet3OutputStreamWriteBytesAndOffsetAdvice " + state );
//      System.out.write(write);

      if (state == null) {
        return true;
      }
      // if handleWrite returns true, then it means the original bytes + the snippet were written
      // to the servletOutputStream, and so we no longer need to execute the original method
      // call (see skipOn above)
      // if it returns false, then it means nothing was written to the servletOutputStream and the
      // original method call should be executed
      return !getSnippetInjectionHelper().handleWrite(state, servletOutputStream, write, off, len);
    }
    catch(Throwable t) {
      System.out.println("Servlet3Advice");

      t.printStackTrace();
      return true;
//      throw new VerifyException(t);
    }
  }
}
