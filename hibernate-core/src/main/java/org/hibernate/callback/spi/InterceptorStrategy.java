/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.callback.spi;

import org.hibernate.Incubating;
import org.hibernate.Interceptor;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import jakarta.annotation.Nullable;

/// Strategy for resolving and managing [Interceptor] instances
/// across session lifecycle boundaries.
///
/// @since 8.0
@Incubating(since = "8.0")
public interface InterceptorStrategy {

	/// Obtain an interceptor for a newly created session.
	Interceptor getInterceptorForSession(SessionFactoryImplementor sessionFactory);

	/// Release an interceptor when its session closes.
	default void releaseInterceptor(Interceptor interceptor) {
	}

	/// Return a factory-level interceptor, or {@code null} if this
	/// strategy does not provide one.
	default @Nullable Interceptor getFactoryInterceptor() {
		return null;
	}
}
