/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;

/**
 * @author Steve Ebersole
 */
public class SqmMutationStrategyHelper {
	/**
	 * Singleton access
	 */
	public static final SqmMutationStrategyHelper INSTANCE = new SqmMutationStrategyHelper();

	private SqmMutationStrategyHelper() {
	}

	/**
	 * Standard resolution of SqmMutationStrategy to use for a given
	 * entity hierarchy.
	 */
	public static SqmMultiTableMutationStrategy resolveStrategy(
			RootClass entityBootDescriptor,
			EntityMappingType rootEntityDescriptor,
			MappingModelCreationProcess creationProcess) {
		final RuntimeModelCreationContext creationContext = creationProcess.getCreationContext();
		final SessionFactoryImplementor sessionFactory = creationContext.getSessionFactory();
		final SessionFactoryOptions options = sessionFactory.getSessionFactoryOptions();

		final SqmMultiTableMutationStrategy specifiedStrategy = options.getSqmMultiTableMutationStrategy();
		if ( specifiedStrategy != null ) {
			return specifiedStrategy;
		}

		// todo (6.0) : add capability define strategy per-hierarchy

		return sessionFactory.getServiceRegistry().getService( JdbcServices.class )
				.getJdbcEnvironment()
				.getDialect()
				.getFallbackSqmMutationStrategy( rootEntityDescriptor, creationContext );
	}

}
