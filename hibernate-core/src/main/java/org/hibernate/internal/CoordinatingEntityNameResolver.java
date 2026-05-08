/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import org.hibernate.EntityNameResolver;
import org.hibernate.Interceptor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Steve Ebersole
 */
class CoordinatingEntityNameResolver implements EntityNameResolver {
	private final SessionFactoryImplementor sessionFactory;
	private final Interceptor interceptor;
	private final SharedSessionContractImplementor session;

	CoordinatingEntityNameResolver(SessionFactoryImplementor sessionFactory, Interceptor interceptor) {
		this( sessionFactory, interceptor, null );
	}

	CoordinatingEntityNameResolver(
			SessionFactoryImplementor sessionFactory,
			Interceptor interceptor,
			SharedSessionContractImplementor session) {
		this.sessionFactory = sessionFactory;
		this.interceptor = interceptor;
		this.session = session;
	}

	@Override
	public String resolveEntityName(Object entity) {
		final String interceptorEntityName = session == null
				? interceptor.getEntityName( entity )
				: session.callInterceptorCallback( () -> interceptor.getEntityName( entity ) );
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
