/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import org.hibernate.EntityNameResolver;
import org.hibernate.Interceptor;

/**
 * The EntityNameResolver used by default by each Session, when a Session scoped Interceptor
 * is present.
 * When there is no Interceptor the Session might use directly the global CoordinatingEntityNameResolver
 * from the SessionFactory.
 */
final class SessionScopedEntityNameResolver implements EntityNameResolver {

	private final CoordinatingEntityNameResolver coordinatingEntityNameResolver;
	private final Interceptor sessionInterceptor;

	public SessionScopedEntityNameResolver(
			CoordinatingEntityNameResolver coordinatingEntityNameResolver,
			Interceptor sessionInterceptor) {
		this.coordinatingEntityNameResolver = coordinatingEntityNameResolver;
		this.sessionInterceptor = sessionInterceptor;
	}

	@Override
	public String resolveEntityName(final Object entity) {
		String entityName = sessionInterceptor.getEntityName( entity );
		if ( entityName != null ) {
			return entityName;
		}
		return coordinatingEntityNameResolver.resolveEntityName( entity );
	}

}
