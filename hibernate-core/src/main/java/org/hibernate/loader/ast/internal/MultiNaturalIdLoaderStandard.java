/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.ast.internal;

import java.util.Collections;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.MultiNaturalIdLoadOptions;
import org.hibernate.loader.ast.spi.MultiNaturalIdLoader;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.sql.results.LoadingLogger;

/**
 * Standard MultiNaturalIdLoader implementation
 */
public class MultiNaturalIdLoaderStandard<E> implements MultiNaturalIdLoader<E> {

	// todo (6.0) : much of the execution logic here is borrowed from `org.hibernate.loader.ast.internal.MultiIdEntityLoaderStandardImpl`
	// 	- consider ways to consolidate/share logic
	//		- actually, org.hibernate.loader.ast.internal.MultiNaturalIdLoadingBatcher is pretty close

	private final EntityMappingType entityDescriptor;

	public MultiNaturalIdLoaderStandard(EntityMappingType entityDescriptor) {
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
			maxBatchSize = session.getJdbcServices().getJdbcEnvironment().getDialect().getDefaultBatchLoadSizingStrategy().determineOptimalBatchLoadSize(
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
					return entityDescriptor.getNaturalIdMapping().normalizeInput( naturalId, session );
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
			throw new NotYetImplementedFor6Exception( getClass() );
		}

		return results;
	}

	@Override
	public EntityMappingType getLoadable() {
		return entityDescriptor;
	}
}
