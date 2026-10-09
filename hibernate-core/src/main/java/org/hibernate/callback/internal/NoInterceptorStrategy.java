package org.hibernate.callback.internal;

import org.hibernate.Interceptor;
import org.hibernate.callback.spi.InterceptorStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.EmptyInterceptor;

/**
 * Strategy that always returns {@link EmptyInterceptor#INSTANCE}.
 * Used when no interceptor configuration is present.
 *
 * @author Sean Okafor
 */
public class NoInterceptorStrategy implements InterceptorStrategy {

	public static final NoInterceptorStrategy INSTANCE = new NoInterceptorStrategy();

	private NoInterceptorStrategy() {
	}

	@Override
	public Interceptor getInterceptorForSession(SessionFactoryImplementor sessionFactory) {
		return EmptyInterceptor.INSTANCE;
	}
}
