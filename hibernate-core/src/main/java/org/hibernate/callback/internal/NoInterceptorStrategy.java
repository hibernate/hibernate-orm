/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.callback.internal;

import org.hibernate.Interceptor;
import org.hibernate.callback.spi.InterceptorStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.EmptyInterceptor;

/**
 * @author Sean Okafor
 */
public class NoInterceptorStrategy  implements InterceptorStrategy {
	public static final NoInterceptorStrategy INSTANCE = new NoInterceptorStrategy();

	@Override
	public Interceptor getInterceptorForSession(SessionFactoryImplementor factory) {
		return EmptyInterceptor.INSTANCE;
	}
}
