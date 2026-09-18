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
import org.hibernate.resource.beans.container.spi.ContainedBeanImplementor;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.ServiceRegistry;

/**
 * @author Sean Okafor
 */
public class ScopedInterceptorStrategy implements InterceptorStrategy {
	private final Class<? extends Interceptor> interceptorClass;
	private final ManagedBeanRegistry mbr;
	private final Map<Interceptor, ManagedBean<? extends Interceptor>> sessionBeans =
			Collections.synchronizedMap( new IdentityHashMap<>() );

	public ScopedInterceptorStrategy(Class<? extends Interceptor> interceptorClass,
									ServiceRegistry serviceRegistry) {
		this.interceptorClass = interceptorClass;
		this.mbr = serviceRegistry.getService( ManagedBeanRegistry.class );
	}

	@Override
	public Interceptor getInterceptorForSession(SessionFactoryImplementor factory) {
		final var bean = mbr.getUncachedBootstrapSafeBean( interceptorClass );
		final var instance = bean.getBeanInstance();
		sessionBeans.put( instance, bean );
		return instance;
	}

	@Override
	public void releaseInterceptor(Interceptor interceptor) {
		final var bean = sessionBeans.remove( interceptor );
		if ( bean instanceof ContainedBeanImplementor<?> releasable ) {
			releasable.release();
		}
	}
}
