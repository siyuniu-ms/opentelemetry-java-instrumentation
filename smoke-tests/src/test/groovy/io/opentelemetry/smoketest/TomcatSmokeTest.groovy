/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.smoketest

import java.time.Duration

abstract class TomcatSmokeTest extends AppServerTest {
  
  @Override
  protected Map<String, String> getExtraEnv() {
    return Collections.singletonMap("JAVA_OPTS", "-Dotel.experimental.javascript-snippet=\\<script\\>console.log\\('hi'\\)\\</script\\>")
  }

  protected String getTargetImagePrefix() {
    "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-servlet-tomcat"
  }

  @Override
  protected TargetWaitStrategy getWaitStrategy() {
    return new TargetWaitStrategy.Log(Duration.ofMinutes(1), ".*Server startup in.*")
  }
}

@AppServer(version = "7.0.109", jdk = "8")
class Tomcat7Jdk8 extends TomcatSmokeTest {
}

@AppServer(version = "7.0.109", jdk = "8-openj9")
class Tomcat7Jdk8Openj9 extends TomcatSmokeTest {
}

@AppServer(version = "8.5.72", jdk = "8")
class Tomcat8Jdk8 extends TomcatSmokeTest {
}

@AppServer(version = "8.5.72", jdk = "11")
class Tomcat8Jdk11 extends TomcatSmokeTest {
}

@AppServer(version = "8.5.72", jdk = "17")
class Tomcat8Jdk17 extends TomcatSmokeTest {
}

@AppServer(version = "8.5.72", jdk = "19")
class Tomcat8Jdk19 extends TomcatSmokeTest {
}

@AppServer(version = "8.5.72", jdk = "20")
class Tomcat8Jdk20 extends TomcatSmokeTest {
}

@AppServer(version = "8.5.72", jdk = "8-openj9")
class Tomcat8Jdk8Openj9 extends TomcatSmokeTest {
}

@AppServer(version = "8.5.72", jdk = "11-openj9")
class Tomcat8Jdk11Openj9 extends TomcatSmokeTest {
}

@AppServer(version = "8.5.72", jdk = "17-openj9")
class Tomcat8Jdk17Openj9 extends TomcatSmokeTest {
}

@AppServer(version = "8.5.72", jdk = "18-openj9")
class Tomcat8Jdk18Openj9 extends TomcatSmokeTest {
}

@AppServer(version = "9.0.54", jdk = "8")
class Tomcat9Jdk8 extends TomcatSmokeTest {
}

@AppServer(version = "9.0.54", jdk = "11")
class Tomcat9Jdk11 extends TomcatSmokeTest {
}

@AppServer(version = "9.0.54", jdk = "17")
class Tomcat9Jdk17 extends TomcatSmokeTest {
}

@AppServer(version = "9.0.54", jdk = "19")
class Tomcat9Jdk19 extends TomcatSmokeTest {
}

@AppServer(version = "9.0.54", jdk = "20")
class Tomcat9Jdk20 extends TomcatSmokeTest {
}

@AppServer(version = "9.0.54", jdk = "8-openj9")
class Tomcat9Jdk8Openj9 extends TomcatSmokeTest {
}

@AppServer(version = "9.0.54", jdk = "11-openj9")
class Tomcat9Jdk11Openj9 extends TomcatSmokeTest {
}

@AppServer(version = "9.0.54", jdk = "17-openj9")
class Tomcat9Jdk17Openj9 extends TomcatSmokeTest {
}

@AppServer(version = "9.0.54", jdk = "18-openj9")
class Tomcat9Jdk18Openj9 extends TomcatSmokeTest {
}

@AppServer(version = "10.0.12", jdk = "8")
class Tomcat10Jdk8 extends TomcatSmokeTest {
}

@AppServer(version = "10.0.12", jdk = "11")
class Tomcat10Jdk11 extends TomcatSmokeTest {
}

@AppServer(version = "10.0.12", jdk = "17")
class Tomcat10Jdk17 extends TomcatSmokeTest {
}

@AppServer(version = "10.0.12", jdk = "19")
class Tomcat10Jdk19 extends TomcatSmokeTest {
}

@AppServer(version = "10.0.12", jdk = "20")
class Tomcat10Jdk20 extends TomcatSmokeTest {
}

@AppServer(version = "10.0.12", jdk = "8-openj9")
class Tomcat10Jdk8Openj9 extends TomcatSmokeTest {
}

@AppServer(version = "10.0.12", jdk = "11-openj9")
class Tomcat10Jdk11Openj9 extends TomcatSmokeTest {
}

@AppServer(version = "10.0.12", jdk = "17-openj9")
class Tomcat10Jdk17Openj9 extends TomcatSmokeTest {
}

@AppServer(version = "10.0.12", jdk = "18-openj9")
class Tomcat10Jdk18Openj9 extends TomcatSmokeTest {
}
