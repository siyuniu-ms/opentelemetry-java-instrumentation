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

import static java.util.stream.Collectors.toSet

@IgnoreIf({ TestContainerManager.useWindowsContainers() })
class SpringBootSmokeTest extends SmokeTest {

  protected String getTargetImage(String jdk) {
    "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-spring-boot:jdk$jdk-20230321.4484174638"
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
  def "spring boot smoke test on JDK #jdk"(int jdk) {
    setup:
    def output = startTarget(jdk)
    def currentAgentVersion = new JarFile(agentPath).getManifest().getMainAttributes().get(Attributes.Name.IMPLEMENTATION_VERSION).toString()

    when:
    def response = client().get("/greeting").aggregate().join()
    Collection<ExportTraceServiceRequest> traces = waitForTraces()

    then: "spans are exported"
    response.contentUtf8() == "Hi!"
    countSpansByName(traces, 'GET /greeting') == 1
    countSpansByName(traces, 'WebController.greeting') == 1
    countSpansByName(traces, 'WebController.withSpan') == 1

    then: "thread details are recorded"
    getSpanStream(traces)
      .allMatch { it.attributesList.stream().map { it.key }.collect(toSet()).containsAll(["thread.id", "thread.name"]) }

    then: "correct agent version is captured in the resource"
    [currentAgentVersion] as Set == findResourceAttribute(traces, "telemetry.auto.version")
      .map { it.stringValue }
      .collect(toSet())

    then: "OS is captured in the resource"
    findResourceAttribute(traces, "os.type")
      .map { it.stringValue }
      .findAny()
      .isPresent()

    then: "javaagent logs its version on startup"
    isVersionLogged(output, currentAgentVersion)

    then: "correct traceIds are logged via MDC instrumentation"
    def loggedTraceIds = getLoggedTraceIds(output)
    def spanTraceIds = getSpanStream(traces)
      .map({ TraceId.fromBytes(it.getTraceId().toByteArray()) })
      .collect(toSet())
    loggedTraceIds == spanTraceIds

    then: "JVM metrics are exported"
    def metrics = new MetricsInspector(waitForMetrics())
    metrics.hasMetricsNamed("process.runtime.jvm.memory.init")
    metrics.hasMetricsNamed("process.runtime.jvm.memory.usage")
    metrics.hasMetricsNamed("process.runtime.jvm.memory.committed")
    metrics.hasMetricsNamed("process.runtime.jvm.memory.limit")

    then: "service name is autodetected"
    def serviceName = findResourceAttribute(traces, "service.name")
      .map { it.stringValue }
      .findAny()
    serviceName.isPresent()
    serviceName.get() == "otel-spring-test-app"

    cleanup:
    stopTarget()

    where:
    jdk << [8, 11, 17, 19]
  }
}
