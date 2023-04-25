/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.jedis.v4_0;

import io.opentelemetry.instrumentation.api.instrumenter.net.InetSocketAddressNetClientAttributesGetter;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import javax.annotation.Nullable;

final class JedisNetAttributesGetter
    extends InetSocketAddressNetClientAttributesGetter<JedisRequest, Void> {

  @Nullable
  @Override
  public String getPeerName(JedisRequest jedisRequest) {
    return null;
  }

  @Nullable
  @Override
  public Integer getPeerPort(JedisRequest jedisRequest) {
    return null;
  }

  @Override
  @Nullable
  protected InetSocketAddress getPeerSocketAddress(
      JedisRequest jedisRequest, @Nullable Void unused) {
    SocketAddress socketAddress = jedisRequest.getRemoteSocketAddress();
    if (socketAddress instanceof InetSocketAddress) {
      return (InetSocketAddress) socketAddress;
    }
    return null;
  }
}
