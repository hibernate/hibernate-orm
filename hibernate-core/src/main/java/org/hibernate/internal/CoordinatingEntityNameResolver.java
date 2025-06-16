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
public class CoordinatingEntityNameResolver implements EntityNameResolver {
	private final SessionFactoryImplementor sessionFactory;
	private final Interceptor interceptor;

	public CoordinatingEntityNameResolver(SessionFactoryImplementor sessionFactory, Interceptor interceptor) {
		this.sessionFactory = sessionFactory;
		this.interceptor = interceptor;
	}

	@Override
	public String resolveEntityName(Object entity) {
		String entityName = interceptor.getEntityName( entity );
		if ( entityName != null ) {
			return entityName;
		}

		for ( EntityNameResolver resolver :
				sessionFactory.getSessionFactoryOptions().getEntityNameResolvers() ) {
			entityName = resolver.resolveEntityName( entity );
			if ( entityName != null ) {
				return entityName;
			}
		}

		for ( EntityNameResolver resolver :
				sessionFactory.getMappingMetamodel().getEntityNameResolvers() ) {
			entityName = resolver.resolveEntityName( entity );
			if ( entityName != null ) {
				return entityName;
			}
		}

		// the old-time stand-by...
		return entity.getClass().getName();
	}
}
