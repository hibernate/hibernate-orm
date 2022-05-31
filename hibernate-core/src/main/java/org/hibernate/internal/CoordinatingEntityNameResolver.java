/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import org.hibernate.EmptyInterceptor;
import org.hibernate.EntityNameResolver;
import org.hibernate.Interceptor;
import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * This is the default EntityNameResolver and is shared across the SessionFactory
 * @author Steve Ebersole
 */
public final class CoordinatingEntityNameResolver implements EntityNameResolver {
	private final Interceptor interceptor;
	private final EntityNameResolver[] entityNameResolvers;

	public CoordinatingEntityNameResolver(SessionFactoryImplementor sessionFactory, Interceptor interceptor) {
		this.interceptor = interceptor;
		//We're assuming this instance is initialized only once per SessionFactory, so make this copy to benefit the many iterations we'll do at runtime:
		this.entityNameResolvers = sessionFactory.getRuntimeMetamodels().getMappingMetamodel().getEntityNameResolvers().toArray(new EntityNameResolver[0]);
	}

	@Override
	public String resolveEntityName(Object entity) {
		String entityName = interceptor.getEntityName( entity );
		if ( entityName != null ) {
			return entityName;
		}

		for ( EntityNameResolver resolver : entityNameResolvers ) {
			entityName = resolver.resolveEntityName( entity );
			if ( entityName != null ) {
				break;
			}
		}

		if ( entityName != null ) {
			return entityName;
		}

		// the old-time stand-by...
		return entity.getClass().getName();
	}

	public EntityNameResolver createSessionScopedEntityNameResolver(Interceptor sessionInterceptor) {
		if ( sessionInterceptor == null || sessionInterceptor == EmptyInterceptor.INSTANCE ) {
			return this;
		}
		else {
			return new SessionScopedEntityNameResolver(this, sessionInterceptor );
		}
	}
}
