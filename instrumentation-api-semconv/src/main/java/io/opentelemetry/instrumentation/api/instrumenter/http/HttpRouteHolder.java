/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.instrumenter.http;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.ContextCustomizer;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.LocalRootSpan;
import io.opentelemetry.instrumentation.api.internal.HttpRouteState;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import javax.annotation.Nullable;

/**
 * A helper class that keeps track of the {@code http.route} attribute value during HTTP server
 * request processing.
 *
 * <p>Usually the route is not accessible when the request processing starts; and needs to be set
 * later, after the instrumented operation starts. This class provides several static methods that
 * allow the instrumentation author to provide the matching HTTP route to the instrumentation when
 * it is discovered.
 */
public final class HttpRouteHolder {

  /**
   * Returns a {@link ContextCustomizer} that initializes a {@link HttpRouteHolder} in the {@link
   * Context} returned from {@link Instrumenter#start(Context, Object)}.
   */
  public static <REQUEST> ContextCustomizer<REQUEST> create(
      HttpServerAttributesGetter<REQUEST, ?> getter) {
    return (context, request, startAttributes) -> {
      if (HttpRouteState.fromContextOrNull(context) != null) {
        return context;
      }
      String method = getter.getHttpRequestMethod(request);
      return context.with(HttpRouteState.create(method, null, 0));
    };
  }

  private HttpRouteHolder() {}

  /**
   * Updates the {@code http.route} attribute in the received {@code context}.
   *
   * <p>If there is a server span in the context, and the context has been customized with a {@link
   * HttpRouteHolder}, then this method will update the route using the provided {@code httpRoute}
   * if and only if the last {@link HttpRouteSource} to update the route using this method has
   * strictly lower priority than the provided {@link HttpRouteSource}, and the passed value is
   * non-null.
   *
   * <p>If there is a server span in the context, and the context has NOT been customized with a
   * {@link HttpRouteHolder}, then this method will update the route using the provided value if it
   * is non-null.
   */
  public static void updateHttpRoute(
      Context context, HttpRouteSource source, @Nullable String httpRoute) {
    updateHttpRoute(context, source, ConstantAdapter.INSTANCE, httpRoute);
  }

  /**
   * Updates the {@code http.route} attribute in the received {@code context}.
   *
   * <p>If there is a server span in the context, and the context has been customized with a {@link
   * HttpRouteHolder}, then this method will update the route using the provided {@link
   * HttpRouteGetter} if and only if the last {@link HttpRouteSource} to update the route using this
   * method has strictly lower priority than the provided {@link HttpRouteSource}, and the value
   * returned from the {@link HttpRouteGetter} is non-null.
   *
   * <p>If there is a server span in the context, and the context has NOT been customized with a
   * {@link HttpRouteHolder}, then this method will update the route using the provided {@link
   * HttpRouteGetter} if the value returned from it is non-null.
   */
  public static <T> void updateHttpRoute(
      Context context, HttpRouteSource source, HttpRouteGetter<T> httpRouteGetter, T arg1) {
    updateHttpRoute(context, source, OneArgAdapter.getInstance(), arg1, httpRouteGetter);
  }

  /**
   * Updates the {@code http.route} attribute in the received {@code context}.
   *
   * <p>If there is a server span in the context, and the context has been customized with a {@link
   * HttpRouteHolder}, then this method will update the route using the provided {@link
   * HttpRouteBiGetter} if and only if the last {@link HttpRouteSource} to update the route using
   * this method has strictly lower priority than the provided {@link HttpRouteSource}, and the
   * value returned from the {@link HttpRouteBiGetter} is non-null.
   *
   * <p>If there is a server span in the context, and the context has NOT been customized with a
   * {@code ServerSpanName}, then this method will update the route using the provided {@link
   * HttpRouteBiGetter} if the value returned from it is non-null.
   */
  public static <T, U> void updateHttpRoute(
      Context context,
      HttpRouteSource source,
      HttpRouteBiGetter<T, U> httpRouteGetter,
      T arg1,
      U arg2) {
    Span serverSpan = LocalRootSpan.fromContextOrNull(context);
    // even if the server span is not sampled, we have to continue - we need to compute the
    // http.route properly so that it can be captured by the server metrics
    if (serverSpan == null) {
      return;
    }
    HttpRouteState httpRouteState = HttpRouteState.fromContextOrNull(context);
    if (httpRouteState == null) {
      // TODO: remove this branch?
      String httpRoute = httpRouteGetter.get(context, arg1, arg2);
      if (httpRoute != null && !httpRoute.isEmpty()) {
        // update just the attribute - without http.method we can't compute a proper span name here
        serverSpan.setAttribute(SemanticAttributes.HTTP_ROUTE, httpRoute);
      }
      return;
    }
    // special case for servlet filters, even when we have a route from previous filter see whether
    // the new route is better and if so use it instead
    boolean onlyIfBetterRoute =
        !source.useFirst && source.order == httpRouteState.getUpdatedBySourceOrder();
    if (source.order > httpRouteState.getUpdatedBySourceOrder() || onlyIfBetterRoute) {
      String route = httpRouteGetter.get(context, arg1, arg2);
      if (route != null
          && !route.isEmpty()
          && (!onlyIfBetterRoute || isBetterRoute(httpRouteState, route))) {

        // update just the span name - the attribute will be picked up by the
        // HttpServerAttributesExtractor at the end of request processing
        updateSpanName(serverSpan, httpRouteState, route);

        httpRouteState.update(context, source.order, route);
      }
    }
  }

  // This is used when setting route from a servlet filter to pick the most descriptive (longest)
  // route.
  private static boolean isBetterRoute(HttpRouteState httpRouteState, String name) {
    String route = httpRouteState.getRoute();
    int routeLength = route == null ? 0 : route.length();
    return name.length() > routeLength;
  }

  private static void updateSpanName(Span serverSpan, HttpRouteState httpRouteState, String route) {
    String method = httpRouteState.getMethod();
    // method should never really be null - but in case it for some reason is, we'll rely on the
    // span name extractor behavior
    if (method != null) {
      serverSpan.updateName(method + " " + route);
    }
  }

  /**
   * Returns the {@code http.route} attribute value that's stored in the {@code context}, or null if
   * it was not set before.
   */
  @Nullable
  static String getRoute(Context context) {
    HttpRouteState httpRouteState = HttpRouteState.fromContextOrNull(context);
    return httpRouteState == null ? null : httpRouteState.getRoute();
  }

  private static final class OneArgAdapter<T> implements HttpRouteBiGetter<T, HttpRouteGetter<T>> {

    private static final OneArgAdapter<Object> INSTANCE = new OneArgAdapter<>();

    @SuppressWarnings("unchecked")
    static <T> OneArgAdapter<T> getInstance() {
      return (OneArgAdapter<T>) INSTANCE;
    }

    @Override
    @Nullable
    public String get(Context context, T arg, HttpRouteGetter<T> httpRouteGetter) {
      return httpRouteGetter.get(context, arg);
    }
  }

  private static final class ConstantAdapter implements HttpRouteGetter<String> {

    private static final ConstantAdapter INSTANCE = new ConstantAdapter();

    @Nullable
    @Override
    public String get(Context context, String route) {
      return route;
    }
  }
}
