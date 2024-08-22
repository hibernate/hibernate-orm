/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import org.hibernate.EntityNameResolver;
import org.hibernate.Interceptor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;

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

		for ( EntityNameResolver resolver : sessionFactory.getSessionFactoryOptions().getEntityNameResolvers() ) {
			entityName = resolver.resolveEntityName( entity );
			if ( entityName != null ) {
				return entityName;
			}
		}

		final MappingMetamodelImplementor mappingMetamodel = sessionFactory.getRuntimeMetamodels().getMappingMetamodel();
		for ( EntityNameResolver resolver : mappingMetamodel.getEntityNameResolvers() ) {
			entityName = resolver.resolveEntityName( entity );
			if ( entityName != null ) {
				return entityName;
			}
		}

		// the old-time stand-by...
		return entity.getClass().getName();
	}
}
