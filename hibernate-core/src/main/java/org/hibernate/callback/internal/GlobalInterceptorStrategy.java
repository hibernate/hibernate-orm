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

/**
 * @author Sean Okafor
 */
public class GlobalInterceptorStrategy implements InterceptorStrategy {
	private final ManagedBean<? extends Interceptor> interceptorBean;

	public GlobalInterceptorStrategy(Class<? extends Interceptor> interceptorClass,
									ServiceRegistry serviceRegistry) {
		final var mbr = serviceRegistry.getService( ManagedBeanRegistry.class );
		interceptorBean = mbr.getBootstrapSafeBean( interceptorClass );
	}

	@Override
	public Interceptor getInterceptorForSession(SessionFactoryImplementor factory) {
		return interceptorBean.getBeanInstance();
	}

}
