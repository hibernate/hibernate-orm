/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.callback.internal;

import org.hibernate.Interceptor;
import org.hibernate.callback.spi.InterceptorStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * @author Sean Okafor
 */
public class ProvidedInterceptorStrategy implements InterceptorStrategy {
	private final Interceptor interceptor;

	public ProvidedInterceptorStrategy(Interceptor interceptor) {
		this.interceptor = interceptor;
	}

	@Override
	public Interceptor getInterceptorForSession(SessionFactoryImplementor factory) {
		return interceptor;
	}
}
