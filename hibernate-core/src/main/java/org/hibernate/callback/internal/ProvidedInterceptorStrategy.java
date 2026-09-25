/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.callback.internal;

import org.hibernate.Interceptor;
import org.hibernate.callback.spi.InterceptorStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import jakarta.annotation.Nullable;

/// Strategy that wraps a user-provided [Interceptor] instance.
/// The same instance is returned for every session and as the factory interceptor.
public class ProvidedInterceptorStrategy implements InterceptorStrategy {

	private final Interceptor interceptor;

	public ProvidedInterceptorStrategy(Interceptor interceptor) {
		this.interceptor = interceptor;
	}

	@Override
	public Interceptor getInterceptorForSession(SessionFactoryImplementor sessionFactory) {
		return interceptor;
	}

	@Override
	public @Nullable Interceptor getFactoryInterceptor() {
		return interceptor;
	}
}
