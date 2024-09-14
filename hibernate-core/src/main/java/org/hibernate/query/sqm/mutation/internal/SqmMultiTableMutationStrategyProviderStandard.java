/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm.mutation.internal;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategyProvider;

/**
 * Standard SqmMultiTableMutationStrategyProvider implementation
 *
 * @see org.hibernate.dialect.Dialect#getFallbackSqmMutationStrategy
 * @see org.hibernate.query.spi.QueryEngineOptions#getCustomSqmMultiTableMutationStrategy
 *
 * @author Steve Ebersole
 */
public class SqmMultiTableMutationStrategyProviderStandard implements SqmMultiTableMutationStrategyProvider {
	@Override
	public SqmMultiTableMutationStrategy createMutationStrategy(
			EntityMappingType rootEntityDescriptor,
			MappingModelCreationProcess creationProcess) {
		final RuntimeModelCreationContext creationContext = creationProcess.getCreationContext();
		final SessionFactoryOptions options = creationContext.getSessionFactoryOptions();

		final SqmMultiTableMutationStrategy specifiedStrategy = options.getCustomSqmMultiTableMutationStrategy();
		if ( specifiedStrategy != null ) {
			return specifiedStrategy;
		}

		return creationContext.getDialect().getFallbackSqmMutationStrategy( rootEntityDescriptor, creationContext );
	}

	@Override
	public SqmMultiTableInsertStrategy createInsertStrategy(
			EntityMappingType rootEntityDescriptor,
			MappingModelCreationProcess creationProcess) {
		final RuntimeModelCreationContext creationContext = creationProcess.getCreationContext();
		final SessionFactoryOptions options = creationContext.getSessionFactoryOptions();

		final SqmMultiTableInsertStrategy specifiedStrategy = options.getCustomSqmMultiTableInsertStrategy();
		if ( specifiedStrategy != null ) {
			return specifiedStrategy;
		}

		return creationContext.getDialect().getFallbackSqmInsertStrategy( rootEntityDescriptor, creationContext );
	}
}
