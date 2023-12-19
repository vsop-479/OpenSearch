/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.telemetry.tracing;

import org.opensearch.common.annotation.InternalApi;

import java.util.Objects;

/**
 * Default implementation for {@link SpanScope}
 *
 * @opensearch.internal
 */
@InternalApi
class DefaultSpanScope implements SpanScope {
    private final Span span;
    private final SpanScope previousSpanScope;
    private final Span beforeSpan;
    private static final ThreadLocal<SpanScope> spanScopeThreadLocal = new ThreadLocal<>();
    private final TracerContextStorage<String, Span> tracerContextStorage;

    /**
     * Constructor
     * @param span span
     * @param previousSpanScope before attached span scope.
     */
    private DefaultSpanScope(
        Span span,
        final Span beforeSpan,
        SpanScope previousSpanScope,
        TracerContextStorage<String, Span> tracerContextStorage
    ) {
        this.span = Objects.requireNonNull(span);
        this.beforeSpan = beforeSpan;
        this.previousSpanScope = previousSpanScope;
        this.tracerContextStorage = tracerContextStorage;
    }

    /**
     * Creates the SpanScope object.
     * @param span span.
     * @param tracerContextStorage tracer context storage.
     * @return SpanScope spanScope
     */
    public static SpanScope create(Span span, TracerContextStorage<String, Span> tracerContextStorage) {
        final SpanScope beforeSpanScope = spanScopeThreadLocal.get();
        final Span beforeSpan = tracerContextStorage.get(TracerContextStorage.CURRENT_SPAN);
        SpanScope newSpanScope = new DefaultSpanScope(span, beforeSpan, beforeSpanScope, tracerContextStorage);
        return newSpanScope;
    }

    @Override
    public void close() {
        detach();
    }

    @Override
    public SpanScope attach() {
        spanScopeThreadLocal.set(this);
        tracerContextStorage.put(TracerContextStorage.CURRENT_SPAN, this.span);
        return this;
    }

    private void detach() {
        spanScopeThreadLocal.set(previousSpanScope);
        if (beforeSpan != null) {
            tracerContextStorage.put(TracerContextStorage.CURRENT_SPAN, beforeSpan);
        } else {
            tracerContextStorage.put(TracerContextStorage.CURRENT_SPAN, null);
        }
    }

    @Override
    public Span getSpan() {
        return span;
    }

    static SpanScope getCurrentSpanScope() {
        return spanScopeThreadLocal.get();
    }

}
