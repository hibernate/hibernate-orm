package org.hibernate.callback.internal;

import org.hibernate.Interceptor;
import org.hibernate.callback.spi.InterceptorStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.resource.beans.spi.ManagedBean;

import jakarta.annotation.Nullable;

/**
 * Strategy that wraps a user-provided {@link Interceptor} instance.
 * The same instance is returned for every session and as the factory interceptor.
 *
 * @author Sean Okafor
 */
public class ProvidedInterceptorStrategy implements InterceptorStrategy {

	private final Interceptor interceptor;
	private final ManagedBean<Interceptor> bean;

	public ProvidedInterceptorStrategy(Interceptor interceptor) {
		this.interceptor = interceptor;
		this.bean = new ManagedBean<>() {
			@Override
			public Class<Interceptor> getBeanClass() {
				return Interceptor.class;
			}

			@Override
			public Interceptor getBeanInstance() {
				return interceptor;
			}
		};
	}

	@Override
	public Interceptor getInterceptorForSession(SessionFactoryImplementor sessionFactory) {
		return interceptor;
	}

	@Override
	public @Nullable ManagedBean<? extends Interceptor> getFactoryInterceptorBean() {
		return bean;
	}
}
