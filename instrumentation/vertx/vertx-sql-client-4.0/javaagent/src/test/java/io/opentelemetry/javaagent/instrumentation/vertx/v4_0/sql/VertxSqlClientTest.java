/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.vertx.v4_0.sql;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.satisfies;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.DB_NAME;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.DB_OPERATION;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.DB_SQL_TABLE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.DB_STATEMENT;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.DB_USER;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.EXCEPTION_EVENT_NAME;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.EXCEPTION_MESSAGE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.EXCEPTION_STACKTRACE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.EXCEPTION_TYPE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_PEER_NAME;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_PEER_PORT;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgException;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Tuple;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

class VertxSqlClientTest {
  private static final Logger logger = LoggerFactory.getLogger(VertxSqlClientTest.class);

  private static final String USER_DB = "SA";
  private static final String PW_DB = "password123";
  private static final String DB = "tempdb";

  @RegisterExtension
  private static final InstrumentationExtension testing = AgentInstrumentationExtension.create();

  private static GenericContainer<?> container;
  private static Vertx vertx;
  private static Pool pool;
  private static int port;

  @BeforeAll
  static void setUp() throws Exception {
    container =
        new GenericContainer<>("postgres:9.6.8")
            .withEnv("POSTGRES_USER", USER_DB)
            .withEnv("POSTGRES_PASSWORD", PW_DB)
            .withEnv("POSTGRES_DB", DB)
            .withExposedPorts(5432)
            .withLogConsumer(new Slf4jLogConsumer(logger))
            .withStartupTimeout(Duration.ofMinutes(2));
    container.start();
    vertx = Vertx.vertx();
    port = container.getMappedPort(5432);
    PgConnectOptions options =
        new PgConnectOptions()
            .setPort(port)
            .setHost(container.getHost())
            .setDatabase(DB)
            .setUser(USER_DB)
            .setPassword(PW_DB);
    pool = Pool.pool(vertx, options, new PoolOptions().setMaxSize(4));
    pool.query("create table test(id int primary key, name varchar(255))")
        .execute()
        .compose(
            r ->
                // insert some test data
                pool.query("insert into test values (1, 'Hello'), (2, 'World')").execute())
        .toCompletionStage()
        .toCompletableFuture()
        .get(30, TimeUnit.SECONDS);
  }

  @AfterAll
  static void cleanUp() {
    pool.close();
    vertx.close();
    container.stop();
  }

  @Test
  void testSimpleSelect() throws Exception {
    CompletableFuture<Object> result = new CompletableFuture<>();
    result.whenComplete((rows, throwable) -> testing.runWithSpan("callback", () -> {}));
    testing.runWithSpan(
        "parent",
        () ->
            pool.query("select * from test")
                .execute(
                    rowSetAsyncResult -> {
                      if (rowSetAsyncResult.succeeded()) {
                        result.complete(rowSetAsyncResult.result());
                      } else {
                        result.completeExceptionally(rowSetAsyncResult.cause());
                      }
                    }));
    result.get(30, TimeUnit.SECONDS);

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasKind(SpanKind.INTERNAL),
                span ->
                    span.hasName("SELECT tempdb.test")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(DB_NAME, DB),
                            equalTo(DB_USER, USER_DB),
                            equalTo(DB_STATEMENT, "select * from test"),
                            equalTo(DB_OPERATION, "SELECT"),
                            equalTo(DB_SQL_TABLE, "test"),
                            equalTo(NET_PEER_NAME, "localhost"),
                            equalTo(NET_PEER_PORT, port)),
                span ->
                    span.hasName("callback")
                        .hasKind(SpanKind.INTERNAL)
                        .hasParent(trace.getSpan(0))));
  }

  @Test
  void testInvalidQuery() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    CompletableFuture<Object> result = new CompletableFuture<>();
    result.whenComplete(
        (rows, throwable) -> testing.runWithSpan("callback", () -> latch.countDown()));
    testing.runWithSpan(
        "parent",
        () ->
            pool.query("invalid")
                .execute(
                    rowSetAsyncResult -> {
                      if (rowSetAsyncResult.succeeded()) {
                        result.complete(rowSetAsyncResult.result());
                      } else {
                        result.completeExceptionally(rowSetAsyncResult.cause());
                      }
                    }));

    latch.await(30, TimeUnit.SECONDS);

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasKind(SpanKind.INTERNAL),
                span ->
                    span.hasName("tempdb")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasStatus(StatusData.error())
                        .hasEventsSatisfyingExactly(
                            event ->
                                event
                                    .hasName(EXCEPTION_EVENT_NAME)
                                    .hasAttributesSatisfyingExactly(
                                        equalTo(EXCEPTION_TYPE, PgException.class.getName()),
                                        satisfies(
                                            EXCEPTION_MESSAGE,
                                            val -> val.contains("syntax error at or near")),
                                        satisfies(
                                            EXCEPTION_STACKTRACE,
                                            val -> val.isInstanceOf(String.class))))
                        .hasAttributesSatisfyingExactly(
                            equalTo(DB_NAME, DB),
                            equalTo(DB_USER, USER_DB),
                            equalTo(DB_STATEMENT, "invalid"),
                            equalTo(NET_PEER_NAME, "localhost"),
                            equalTo(NET_PEER_PORT, port)),
                span ->
                    span.hasName("callback")
                        .hasKind(SpanKind.INTERNAL)
                        .hasParent(trace.getSpan(0))));
  }

  @Test
  void testPreparedSelect() throws Exception {
    testing
        .runWithSpan(
            "parent",
            () -> pool.preparedQuery("select * from test where id = $1").execute(Tuple.of(1)))
        .toCompletionStage()
        .toCompletableFuture()
        .get(30, TimeUnit.SECONDS);

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasKind(SpanKind.INTERNAL),
                span ->
                    span.hasName("SELECT tempdb.test")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(DB_NAME, DB),
                            equalTo(DB_USER, USER_DB),
                            equalTo(DB_STATEMENT, "select * from test where id = $?"),
                            equalTo(DB_OPERATION, "SELECT"),
                            equalTo(DB_SQL_TABLE, "test"),
                            equalTo(NET_PEER_NAME, "localhost"),
                            equalTo(NET_PEER_PORT, port))));
  }

  @Test
  void testBatch() throws Exception {
    testing
        .runWithSpan(
            "parent",
            () ->
                pool.preparedQuery("insert into test values ($1, $2) returning *")
                    .executeBatch(Arrays.asList(Tuple.of(3, "Three"), Tuple.of(4, "Four"))))
        .toCompletionStage()
        .toCompletableFuture()
        .get(30, TimeUnit.SECONDS);

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasKind(SpanKind.INTERNAL),
                span ->
                    span.hasName("INSERT tempdb.test")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(DB_NAME, DB),
                            equalTo(DB_USER, USER_DB),
                            equalTo(DB_STATEMENT, "insert into test values ($?, $?) returning *"),
                            equalTo(DB_OPERATION, "INSERT"),
                            equalTo(DB_SQL_TABLE, "test"),
                            equalTo(NET_PEER_NAME, "localhost"),
                            equalTo(NET_PEER_PORT, port))));
  }
}
