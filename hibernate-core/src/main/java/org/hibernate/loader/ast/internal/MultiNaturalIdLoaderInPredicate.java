/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.MultiNaturalIdLoadOptions;
import org.hibernate.loader.ast.spi.SqlInPredicateMultiKeyLoader;
import org.hibernate.metamodel.mapping.EntityMappingType;

/**
 * MultiNaturalIdLoader implementation using SQL IN predicate to specify the ids
 */
public class MultiNaturalIdLoaderInPredicate<E> extends AbstractMultiNaturalIdLoader<E> implements SqlInPredicateMultiKeyLoader {

	public MultiNaturalIdLoaderInPredicate(EntityMappingType entityDescriptor) {
		super(entityDescriptor);
	}

	@Override
	public List<E> loadEntitiesWithUnresolvedIds(
			Object[] naturalIds,
			MultiNaturalIdLoadOptions loadOptions,
			LockOptions lockOptions,
			SharedSessionContractImplementor session) {
		return getBatcher( naturalIds, loadOptions, lockOptions, session )
				.multiLoad( naturalIds, session );
	}

	private MultiNaturalIdLoadingBatcher getBatcher(
			Object[] naturalIds,
			MultiNaturalIdLoadOptions loadOptions,
			LockOptions lockOptions,
			SharedSessionContractImplementor session) {
		final EntityMappingType descriptor = getEntityDescriptor();
		return new MultiNaturalIdLoadingBatcher(
				descriptor,
				descriptor.getNaturalIdMapping(),
				Math.min( naturalIds.length, getMaxBatchSize( naturalIds, loadOptions, session ) ),
				// naturalId here is the one passed in by the API as part of the values array
				(naturalId, s) -> descriptor.getNaturalIdMapping().normalizeInput( naturalId ),
				session.getLoadQueryInfluencers(),
				lockOptions,
				session.getFactory()
		);
	}

	private int getMaxBatchSize(
			Object[] naturalIds,
			MultiNaturalIdLoadOptions loadOptions,
			SharedSessionContractImplementor session) {
		final Integer batchSize = loadOptions.getBatchSize();
		if ( batchSize != null && batchSize > 0 ) {
			return batchSize;
		}
		else {
			return session.getJdbcServices().getJdbcEnvironment().getDialect()
					.getMultiKeyLoadSizingStrategy().determineOptimalBatchLoadSize(
							getEntityDescriptor().getNaturalIdMapping().getJdbcTypeCount(),
							naturalIds.length,
							session.getFactory().getSessionFactoryOptions().inClauseParameterPaddingEnabled()
					);
		}
	}

}
