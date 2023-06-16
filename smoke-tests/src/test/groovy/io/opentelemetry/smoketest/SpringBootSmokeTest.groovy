/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.smoketest

import io.opentelemetry.api.trace.TraceId
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest
import spock.lang.IgnoreIf
import spock.lang.Unroll

import java.time.Duration
import java.util.jar.Attributes
import java.util.jar.JarFile

import static io.opentelemetry.smoketest.TestContainerManager.useWindowsContainers
import static java.util.stream.Collectors.toSet

@IgnoreIf({ useWindowsContainers() })
class SpringBootSmokeTest extends SmokeTest {

  protected String getTargetImage(String jdk) {
    "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-spring-boot:jdk$jdk-20230616.152272"
  }

  @Override
  protected boolean getSetServiceName() {
    return false
  }

  @Override
  protected Map<String, String> getExtraEnv() {
    return Collections.singletonMap("OTEL_METRICS_EXPORTER", "otlp")
  }

  @Override
  protected TargetWaitStrategy getWaitStrategy() {
    return new TargetWaitStrategy.Log(Duration.ofMinutes(1), ".*Started SpringbootApplication in.*")
  }

  @Unroll
  def "snippet test"(int jdk) {
    setup:
    def output = startTarget(jdk)
    def currentAgentVersion = new JarFile(agentPath).getManifest().getMainAttributes().get(Attributes.Name.IMPLEMENTATION_VERSION).toString()

    when:
    def response = client().get("/snippetTest").aggregate().join()
    Collection<ExportTraceServiceRequest> traces = waitForTraces()
    String responseBody = response.contentUtf8()
    then: "spans are exported"
    response.status().isSuccess()
    println(responseBody)

    responseBody.contains("<script>console.log(hi)</script>")

    cleanup:
    stopTarget()

    where:
    jdk << [11]
  }
}
