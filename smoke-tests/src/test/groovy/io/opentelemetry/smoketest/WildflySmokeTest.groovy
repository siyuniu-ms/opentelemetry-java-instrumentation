/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.smoketest

import io.opentelemetry.proto.trace.v1.Span
import spock.lang.Unroll

import java.time.Duration

abstract class WildflySmokeTest extends AppServerTest {

  protected String getTargetImagePrefix() {
    "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-servlet-wildfly"
  }

  @Override
  protected TargetWaitStrategy getWaitStrategy() {
    return new TargetWaitStrategy.Log(Duration.ofMinutes(1), ".*started in.*")
  }

  @Unroll
  def "JSP smoke test on WildFly"() {
    when:
    def response = client().get("/app/jsp").aggregate().join()
    TraceInspector traces = new TraceInspector(waitForTraces())
    String responseBody = response.contentUtf8()

    then:
    response.status().isSuccess()
    responseBody.contains("Successful JSP test")

    traces.countSpansByKind(Span.SpanKind.SPAN_KIND_SERVER) == 1

    traces.countSpansByName('GET /app/jsp') == 1

    where:
    [appServer, jdk] << getTestParams()
  }
}

abstract class WildflyJdk8SmokeTest extends WildflySmokeTest {
  @Override
  protected Map<String, String> getExtraEnv() {
    // https://github.com/openjdk/jdk8u/commit/d72d28967d732ba32e02178b828255378c5a8938
    // introduces a changes that causes wildfly to throw java.io.FileNotFoundException: Invalid file
    // path on windows
    return Collections.singletonMap("JAVA_OPTS", "-Djdk.io.File.enableADS=true " +
      "-Djava.net.preferIPv4Stack=true -Djava.awt.headless=true")
  }
}

@AppServer(version = "13.0.0.Final", jdk = "8")
class Wildfly13Jdk8 extends WildflyJdk8SmokeTest {
} // this one passed (no inject)

@AppServer(version = "13.0.0.Final", jdk = "8-openj9")
class Wildfly13Jdk8Openj9 extends WildflySmokeTest {
}

@AppServer(version = "17.0.1.Final", jdk = "8")
class Wildfly17Jdk8 extends WildflyJdk8SmokeTest {
} // this one passed (no inject)

@AppServer(version = "17.0.1.Final", jdk = "11")
class Wildfly17Jdk11 extends WildflySmokeTest {
}

@AppServer(version = "17.0.1.Final", jdk = "17")
class Wildfly17Jdk17 extends WildflySmokeTest {
}

@AppServer(version = "17.0.1.Final", jdk = "19")
class Wildfly17Jdk19 extends WildflyJdk8SmokeTest {
}  // this one passed (no inject)

@AppServer(version = "17.0.1.Final", jdk = "20")
class Wildfly17Jdk20 extends WildflySmokeTest {
} // TODO: maybe?

@AppServer(version = "17.0.1.Final", jdk = "8-openj9")
class Wildfly17Jdk8Openj9 extends WildflySmokeTest {
}  // TODO: maybe?

@AppServer(version = "17.0.1.Final", jdk = "11-openj9")
class Wildfly17Jdk11Openj9 extends WildflySmokeTest {
}

@AppServer(version = "17.0.1.Final", jdk = "17-openj9")
class Wildfly17Jdk17Openj9 extends WildflySmokeTest {
}

@AppServer(version = "17.0.1.Final", jdk = "18-openj9")
class Wildfly17Jdk18Openj9 extends WildflySmokeTest {
}

@AppServer(version = "21.0.0.Final", jdk = "8")
class Wildfly21Jdk8 extends WildflyJdk8SmokeTest {
}

@AppServer(version = "21.0.0.Final", jdk = "11")
class Wildfly21Jdk11 extends WildflySmokeTest {
} // this one passed (no inject)

@AppServer(version = "21.0.0.Final", jdk = "17")
class Wildfly21Jdk17 extends WildflySmokeTest {
}

@AppServer(version = "21.0.0.Final", jdk = "19")
class Wildfly21Jdk19 extends WildflyJdk8SmokeTest {
}

@AppServer(version = "21.0.0.Final", jdk = "20")
class Wildfly21Jdk20 extends WildflySmokeTest {
}

@AppServer(version = "21.0.0.Final", jdk = "8-openj9")
class Wildfly21Jdk8Openj9 extends WildflySmokeTest {
}

@AppServer(version = "21.0.0.Final", jdk = "11-openj9")
class Wildfly21Jdk11Openj9 extends WildflySmokeTest {
} // this one passed (with inject)

@AppServer(version = "21.0.0.Final", jdk = "17-openj9")
class Wildfly21Jdk17Openj9 extends WildflySmokeTest {
}

@AppServer(version = "21.0.0.Final", jdk = "18-openj9")
class Wildfly21Jdk18Openj9 extends WildflySmokeTest {
}

@AppServer(version = "25.0.1.Final", jdk = "8")
class Wildfly25Jdk8 extends WildflyJdk8SmokeTest {
}

@AppServer(version = "25.0.1.Final", jdk = "11")
class Wildfly25Jdk11 extends WildflySmokeTest {
}

@AppServer(version = "25.0.1.Final", jdk = "17")
class Wildfly25Jdk17 extends WildflySmokeTest {
}

@AppServer(version = "25.0.1.Final", jdk = "19")
class Wildfly25Jdk19 extends WildflyJdk8SmokeTest {
}

@AppServer(version = "25.0.1.Final", jdk = "20")
class Wildfly25Jdk20 extends WildflySmokeTest {
}

@AppServer(version = "25.0.1.Final", jdk = "8-openj9")
class Wildfly25Jdk8Openj9 extends WildflySmokeTest {
}

@AppServer(version = "25.0.1.Final", jdk = "11-openj9")
class Wildfly25Jdk11Openj9 extends WildflySmokeTest {
}

@AppServer(version = "25.0.1.Final", jdk = "17-openj9")
class Wildfly25Jdk17Openj9 extends WildflySmokeTest {
}

@AppServer(version = "25.0.1.Final", jdk = "18-openj9")
class Wildfly25Jdk18Openj9 extends WildflySmokeTest {
}
