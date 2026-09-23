/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.callback.internal;

import org.hibernate.Interceptor;
import org.hibernate.callback.spi.InterceptorStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.ServiceRegistry;

import jakarta.annotation.Nullable;

/// Strategy that resolves a single shared [Interceptor] from the bean container
/// via a bootstrap-safe cached bean. The same instance is returned for every
/// session and as the factory interceptor.
public class GlobalInterceptorStrategy implements InterceptorStrategy {

	private final ManagedBean<? extends Interceptor> managedBean;

	public GlobalInterceptorStrategy(Class<? extends Interceptor> interceptorClass, ServiceRegistry serviceRegistry) {
		final var registry = serviceRegistry.getService( ManagedBeanRegistry.class );
		this.managedBean = registry.getBootstrapSafeBean( interceptorClass );
	}

	@Override
	public Interceptor getInterceptorForSession(SessionFactoryImplementor sessionFactory) {
		return managedBean.getBeanInstance();
	}

	@Override
	public @Nullable Interceptor getFactoryInterceptor() {
		return managedBean.getBeanInstance();
	}
}
