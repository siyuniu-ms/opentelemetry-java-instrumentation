/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.redisson;

import io.opentelemetry.instrumentation.api.instrumenter.net.InetSocketAddressNetClientAttributesGetter;
import java.net.InetSocketAddress;
import javax.annotation.Nullable;

final class RedissonNetAttributesGetter
    extends InetSocketAddressNetClientAttributesGetter<RedissonRequest, Void> {

  @Nullable
  @Override
  public String getPeerName(RedissonRequest redissonRequest) {
    return null;
  }

  @Nullable
  @Override
  public Integer getPeerPort(RedissonRequest redissonRequest) {
    return null;
  }

  @Override
  protected InetSocketAddress getPeerSocketAddress(RedissonRequest request, @Nullable Void unused) {
    return request.getAddress();
  }
}
