/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.smoketest

import io.opentelemetry.proto.trace.v1.Span
import spock.lang.Shared
import spock.lang.Unroll

import static org.junit.Assume.assumeFalse

abstract class AppServerTest extends SmokeTest {
  @Shared
  String jdk
  @Shared
  String serverVersion
  @Shared
  boolean isWindows

  def setupSpec() {
    (serverVersion, jdk) = getAppServer()
//    isWindows = System.getProperty("os.name").toLowerCase().contains("windows") &&
//      "1" != System.getenv("USE_LINUX_CONTAINERS")
    isWindows = false;

    // ibm-semeru-runtimes doesn't publish windows images
    // adoptopenjdk is deprecated and doesn't publish Windows 2022 images
    assumeFalse(isWindows && jdk.endsWith("-openj9"))

    startTarget(jdk, serverVersion, isWindows)
  }

  protected Tuple<String> getAppServer() {
    def appServer = getClass().getAnnotation(AppServer)
    if (appServer == null) {
      throw new IllegalStateException("Server not specified, either add @AppServer annotation or override getAppServer method")
    }
    return new Tuple(appServer.version(), appServer.jdk())
  }

  @Override
  protected String getTargetImage(String jdk) {
    throw new UnsupportedOperationException("App servers tests should use getTargetImagePrefix")
  }

  @Override
  protected String getTargetImage(String jdk, String serverVersion, boolean windows) {
    String platformSuffix = windows ? "-windows" : ""
    String extraTag = "20230418.4737426282"
    String fullSuffix = "${serverVersion}-jdk$jdk$platformSuffix-$extraTag"
    return getTargetImagePrefix() + ":" + fullSuffix
  }

  protected abstract String getTargetImagePrefix()

  def cleanupSpec() {
    stopTarget()
  }

  boolean testSmoke() {
    true
  }

  boolean testAsyncSmoke() {
    true
  }

  boolean testException() {
    true
  }

  boolean testRequestWebInfWebXml() {
    true
  }

  boolean testRequestOutsideDeployedApp() {
    true
  }


  @Unroll
  def "JSP smoke test on WildFly"() {
    when:
    def response = client().get("/app/jsp").aggregate().join()
    TraceInspector traces = new TraceInspector(waitForTraces())
    String responseBody = response.contentUtf8()

    println("=========================")
    println("=========================")
    println(response.contentType())
    println("=========================")
    println("=========================")
    println(responseBody)
    println("=========================")
    println("=========================")

    then:
    response.status().isSuccess()
    responseBody.contains("Successful JSP test")
    responseBody.contains("<script>console.log(hi)</script>")

    traces.countSpansByKind(Span.SpanKind.SPAN_KIND_SERVER) == 1

    traces.countSpansByName('GET /app/jsp') == 1

    where:
    [appServer, jdk] << getTestParams()
  }

  protected String getSpanName(String path) {
    switch (path) {
      case "/app/greeting":
      case "/app/headers":
      case "/app/exception":
      case "/app/asyncgreeting":
        return "GET " + path
      case "/app/hello.txt":
      case "/app/file-that-does-not-exist":
        return "GET /app/*"
    }
    return "GET"
  }

  protected List<List<Object>> getTestParams() {
    return [
      [serverVersion, jdk, isWindows]
    ]
  }
}
