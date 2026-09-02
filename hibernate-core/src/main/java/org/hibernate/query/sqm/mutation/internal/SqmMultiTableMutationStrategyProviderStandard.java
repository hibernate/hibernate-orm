/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.internal;


import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.dialect.mutation.internal.MultiTableMutationStrategyFactory;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategyProvider;

/**
 * Standard SqmMultiTableMutationStrategyProvider implementation
 *
 * @see org.hibernate.dialect.Dialect#getMultiTableMutationSupport
 * @see org.hibernate.query.spi.QueryEngineOptions#getCustomSqmMultiTableMutationStrategy
 *
 * @author Steve Ebersole
 */
public class SqmMultiTableMutationStrategyProviderStandard implements SqmMultiTableMutationStrategyProvider {
	@Override
	public SqmMultiTableMutationStrategy createMutationStrategy(
			EntityMappingType rootEntityDescriptor,
			RuntimeModelCreationContext creationContext) {
		final SessionFactoryOptions options = creationContext.getSessionFactoryOptions();

		final SqmMultiTableMutationStrategy specifiedStrategy = options.getCustomSqmMultiTableMutationStrategy();
		if ( specifiedStrategy != null ) {
			return specifiedStrategy;
		}
		final SqmMultiTableMutationStrategy specifiedEntityBaseStrategy =
				options.resolveCustomSqmMultiTableMutationStrategy( rootEntityDescriptor, creationContext );
		if ( specifiedEntityBaseStrategy != null ) {
			return specifiedEntityBaseStrategy;
		}
		return MultiTableMutationStrategyFactory.createMutationStrategy(
				creationContext.getDialect(),
				rootEntityDescriptor,
				creationContext
		);
	}

	@Override
	public SqmMultiTableInsertStrategy createInsertStrategy(
			EntityMappingType rootEntityDescriptor,
			RuntimeModelCreationContext creationContext) {
		final SessionFactoryOptions options = creationContext.getSessionFactoryOptions();

		final SqmMultiTableInsertStrategy specifiedStrategy = options.getCustomSqmMultiTableInsertStrategy();
		if ( specifiedStrategy != null ) {
			return specifiedStrategy;
		}
		final SqmMultiTableInsertStrategy specifiedEntityBaseStrategy =
				options.resolveCustomSqmMultiTableInsertStrategy( rootEntityDescriptor, creationContext );
		if ( specifiedEntityBaseStrategy != null ) {
			return specifiedEntityBaseStrategy;
		}

		return MultiTableMutationStrategyFactory.createInsertStrategy(
				creationContext.getDialect(),
				rootEntityDescriptor,
				creationContext
		);
	}
}
