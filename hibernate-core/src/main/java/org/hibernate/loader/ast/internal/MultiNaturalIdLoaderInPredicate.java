/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
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

		final SessionFactoryImplementor sessionFactory = session.getFactory();

		final int maxBatchSize;
		if ( loadOptions.getBatchSize() != null && loadOptions.getBatchSize() > 0 ) {
			maxBatchSize = loadOptions.getBatchSize();
		}
		else {
			maxBatchSize =
					session.getJdbcServices().getJdbcEnvironment().getDialect()
							.getMultiKeyLoadSizingStrategy().determineOptimalBatchLoadSize(
									getEntityDescriptor().getNaturalIdMapping().getJdbcTypeCount(),
									naturalIds.length,
									sessionFactory.getSessionFactoryOptions().inClauseParameterPaddingEnabled()
							);
		}

		final int batchSize = Math.min( maxBatchSize, naturalIds.length );

		final MultiNaturalIdLoadingBatcher batcher = new MultiNaturalIdLoadingBatcher(
				getEntityDescriptor(),
				getEntityDescriptor().getNaturalIdMapping(),
				batchSize,
				(naturalId, s) -> {
					// `naturalId` here is the one passed in by the API as part of the values array
					return getEntityDescriptor().getNaturalIdMapping().normalizeInput( naturalId );
				},
				session.getLoadQueryInfluencers(),
				lockOptions,
				sessionFactory
		);

		return batcher.multiLoad( naturalIds, session );
	}

}
