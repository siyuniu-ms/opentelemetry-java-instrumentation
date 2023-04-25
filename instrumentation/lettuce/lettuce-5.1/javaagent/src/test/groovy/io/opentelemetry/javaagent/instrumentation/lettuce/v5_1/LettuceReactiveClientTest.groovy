/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.lettuce.v5_1

import io.lettuce.core.RedisClient
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.lettuce.v5_1.AbstractLettuceReactiveClientTest
import io.opentelemetry.instrumentation.test.AgentTestTrait
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes
import reactor.core.scheduler.Schedulers

class LettuceReactiveClientTest extends AbstractLettuceReactiveClientTest implements AgentTestTrait {
  @Override
  RedisClient createClient(String uri) {
    return RedisClient.create(uri)
  }

  // TODO(anuraaga): reactor library instrumentation doesn't seem to handle this case, figure out if
  // it should and if so move back to base class.
  def "async subscriber with specific thread pool"() {
    when:
    runWithSpan("test-parent") {
      reactiveCommands.set("a", "1")
        .then(reactiveCommands.get("a"))
        .subscribeOn(Schedulers.elastic())
        .subscribe()
    }

    then:
    assertTraces(1) {
      trace(0, 3) {
        span(0) {
          name "test-parent"
          attributes {
          }
        }
        span(1) {
          name "SET"
          kind SpanKind.CLIENT
          childOf span(0)
          attributes {
            "$SemanticAttributes.NET_SOCK_PEER_ADDR" "127.0.0.1"
            "$SemanticAttributes.NET_SOCK_PEER_NAME" expectedHostAttributeValue
            "$SemanticAttributes.NET_SOCK_PEER_PORT" port
            "$SemanticAttributes.DB_SYSTEM" "redis"
            "$SemanticAttributes.DB_STATEMENT" "SET a ?"
          }
          event(0) {
            eventName "redis.encode.start"
          }
          event(1) {
            eventName "redis.encode.end"
          }
        }
        span(2) {
          name "GET"
          kind SpanKind.CLIENT
          childOf span(0)
          attributes {
            "$SemanticAttributes.NET_SOCK_PEER_ADDR" "127.0.0.1"
            "$SemanticAttributes.NET_SOCK_PEER_NAME" expectedHostAttributeValue
            "$SemanticAttributes.NET_SOCK_PEER_PORT" port
            "$SemanticAttributes.DB_SYSTEM" "redis"
            "$SemanticAttributes.DB_STATEMENT" "GET a"
          }
          event(0) {
            eventName "redis.encode.start"
          }
          event(1) {
            eventName "redis.encode.end"
          }
        }
      }
    }
  }
}
