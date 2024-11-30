/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import java.util.Collections;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.MultiNaturalIdLoadOptions;
import org.hibernate.loader.ast.spi.MultiNaturalIdLoader;
import org.hibernate.loader.ast.spi.SqlInPredicateMultiKeyLoader;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.sql.results.LoadingLogger;

/**
 * MultiNaturalIdLoader implementation using SQL IN predicate to specify the ids
 */
public class MultiNaturalIdLoaderInPredicate<E> implements MultiNaturalIdLoader<E>, SqlInPredicateMultiKeyLoader {
	private final EntityMappingType entityDescriptor;

	public MultiNaturalIdLoaderInPredicate(EntityMappingType entityDescriptor) {
		this.entityDescriptor = entityDescriptor;
	}

	@Override
	public <K> List<E> multiLoad(K[] naturalIds, MultiNaturalIdLoadOptions options, SharedSessionContractImplementor session) {
		if ( naturalIds == null ) {
			throw new IllegalArgumentException( "`naturalIds` is null" );
		}

		if ( naturalIds.length == 0 ) {
			return Collections.emptyList();
		}

		if ( LoadingLogger.LOGGER.isTraceEnabled() ) {
			LoadingLogger.LOGGER.tracef( "Starting multi natural-id loading for `%s`", entityDescriptor.getEntityName() );
		}

		final SessionFactoryImplementor sessionFactory = session.getFactory();

		final int maxBatchSize;
		if ( options.getBatchSize() != null && options.getBatchSize() > 0 ) {
			maxBatchSize = options.getBatchSize();
		}
		else {
			maxBatchSize = session.getJdbcServices().getJdbcEnvironment().getDialect().getMultiKeyLoadSizingStrategy().determineOptimalBatchLoadSize(
					entityDescriptor.getNaturalIdMapping().getJdbcTypeCount(),
					naturalIds.length,
					sessionFactory.getSessionFactoryOptions().inClauseParameterPaddingEnabled()
			);
		}

		final int batchSize = Math.min( maxBatchSize, naturalIds.length );

		final LockOptions lockOptions = (options.getLockOptions() == null)
				? new LockOptions( LockMode.NONE )
				: options.getLockOptions();

		final MultiNaturalIdLoadingBatcher batcher = new MultiNaturalIdLoadingBatcher(
				entityDescriptor,
				entityDescriptor.getNaturalIdMapping(),
				batchSize,
				(naturalId, session1) -> {
					// `naturalId` here is the one passed in by the API as part of the values array
					// todo (6.0) : use this to help create the ordered results
					return entityDescriptor.getNaturalIdMapping().normalizeInput( naturalId );
				},
				session.getLoadQueryInfluencers(),
				lockOptions,
				sessionFactory
		);

		final List<E> results = batcher.multiLoad( naturalIds, options, session );

		if ( results.size() == 1 ) {
			return results;
		}

		if ( options.isOrderReturnEnabled() ) {
			throw new UnsupportedOperationException( "Support for ordered loading by multiple natural-id values is not supported" );
		}

		return results;
	}

	@Override
	public EntityMappingType getLoadable() {
		return entityDescriptor;
	}
}
