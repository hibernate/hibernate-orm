/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.callback.spi;


import org.hibernate.Interceptor;
import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * @author Sean Okafor
 */
public interface InterceptorStrategy {
	Interceptor getInterceptorForSession(SessionFactoryImplementor factory);
	default void releaseInterceptor(Interceptor interceptor) {}
}
