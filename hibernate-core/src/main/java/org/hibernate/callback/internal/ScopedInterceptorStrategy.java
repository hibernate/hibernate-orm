/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.callback.internal;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

import org.hibernate.Interceptor;
import org.hibernate.callback.spi.InterceptorStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.resource.beans.spi.BeanInstanceCaching;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.ServiceRegistry;

/// Strategy that creates a distinct [Interceptor] for each session via a
/// bootstrap-safe uncached bean. Each bean is tracked and released when
/// the session closes.
public class ScopedInterceptorStrategy implements InterceptorStrategy {

	private final Class<? extends Interceptor> interceptorClass;
	private final ServiceRegistry serviceRegistry;
	private final Map<Interceptor, ManagedBean<? extends Interceptor>> activeBeans =
			Collections.synchronizedMap( new IdentityHashMap<>() );

	public ScopedInterceptorStrategy(Class<? extends Interceptor> interceptorClass, ServiceRegistry serviceRegistry) {
		this.interceptorClass = interceptorClass;
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public Interceptor getInterceptorForSession(SessionFactoryImplementor sessionFactory) {
		final var registry = serviceRegistry.getService( ManagedBeanRegistry.class );
		final ManagedBean<? extends Interceptor> bean =
				registry.getBootstrapSafeBean( interceptorClass, BeanInstanceCaching.DISALLOW );
		final Interceptor interceptor = bean.getBeanInstance();
		activeBeans.put( interceptor, bean );
		return interceptor;
	}

	@Override
	public void releaseInterceptor(Interceptor interceptor) {
		final ManagedBean<? extends Interceptor> bean = activeBeans.remove( interceptor );
		if ( bean != null ) {
			final var registry = serviceRegistry.getService( ManagedBeanRegistry.class );
			registry.releaseBean( bean );
		}
	}
}
