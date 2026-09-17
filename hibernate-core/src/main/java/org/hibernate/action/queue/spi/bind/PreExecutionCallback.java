/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.spi.bind;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.SessionImplementor;

import static java.util.Objects.requireNonNull;

/// Callback invoked immediately before executing a planned operation.
///
/// @author Steve Ebersole
/// @since 8.0
@Incubating(since = "8.0", group = "action-queue")
public interface PreExecutionCallback {
	/// Whether pending JDBC work must execute before invoking this callback.
	/// A callback which has already completed its database checks may return `false`.
	default boolean requiresBatchFlush() {
		return true;
	}

	/// @return `true` to execute the operation; `false` to skip it.
	boolean beforeExecution(SessionImplementor session);

	/// Compose this callback with another, invoked only if this callback returns `true`.
	/// Pending JDBC work must execute first if either callback currently requires it.
	default PreExecutionCallback and(PreExecutionCallback next) {
		requireNonNull( next );
		final var first = this;
		return new PreExecutionCallback() {
			@Override
			public boolean requiresBatchFlush() {
				return first.requiresBatchFlush() || next.requiresBatchFlush();
			}

			@Override
			public boolean beforeExecution(SessionImplementor session) {
				return first.beforeExecution( session ) && next.beforeExecution( session );
			}
		};
	}
}
