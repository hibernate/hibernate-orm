/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import org.hibernate.EntityNameResolver;
import org.hibernate.Interceptor;
import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * @author Steve Ebersole
 */
class CoordinatingEntityNameResolver implements EntityNameResolver {
	private final SessionFactoryImplementor sessionFactory;
	private final Interceptor interceptor;

	CoordinatingEntityNameResolver(SessionFactoryImplementor sessionFactory, Interceptor interceptor) {
		this.sessionFactory = sessionFactory;
		this.interceptor = interceptor;
	}

	@Override
	public String resolveEntityName(Object entity) {
		final String interceptorEntityName = interceptor.getEntityName( entity );
		if ( interceptorEntityName != null ) {
			return interceptorEntityName;
		}

		for ( var resolver : sessionFactory.getSessionFactoryOptions().getEntityNameResolvers() ) {
			final String resolverEntityName = resolver.resolveEntityName( entity );
			if ( resolverEntityName != null ) {
				return resolverEntityName;
			}
		}

		for ( var resolver : sessionFactory.getMappingMetamodel().getEntityNameResolvers() ) {
			final String resolverEntityName = resolver.resolveEntityName( entity );
			if ( resolverEntityName != null ) {
				return resolverEntityName;
			}
		}

		// the old-time stand-by...
		return entity.getClass().getName();
	}
}
