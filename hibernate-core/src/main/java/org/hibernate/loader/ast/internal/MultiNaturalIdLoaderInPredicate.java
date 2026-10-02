package org.hibernate.loader.ast.internal;

import jakarta.annotation.Nonnull;

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

	public MultiNaturalIdLoaderInPredicate(@Nonnull EntityMappingType entityDescriptor) {
		super(entityDescriptor);
	}

	@Nonnull
	@Override
	public List<E> loadEntitiesWithUnresolvedIds(
			@Nonnull Object[] naturalIds,
			@Nonnull MultiNaturalIdLoadOptions loadOptions,
			@Nonnull LockOptions lockOptions,
			@Nonnull SharedSessionContractImplementor session) {
		return getBatcher( naturalIds, loadOptions, lockOptions, session )
				.multiLoad( naturalIds, session );
	}

	@Nonnull
	private MultiNaturalIdLoadingBatcher getBatcher(
			@Nonnull Object[] naturalIds,
			@Nonnull MultiNaturalIdLoadOptions loadOptions,
			@Nonnull LockOptions lockOptions,
			@Nonnull SharedSessionContractImplementor session) {
		final var entityDescriptor = getEntityDescriptor();
		return new MultiNaturalIdLoadingBatcher(
				entityDescriptor,
				entityDescriptor.getNaturalIdMapping(),
				Math.min( naturalIds.length, getMaxBatchSize( naturalIds, loadOptions, session ) ),
				// naturalId here is the one passed in by the API as part of the values array
				(naturalId, s) -> entityDescriptor.getNaturalIdMapping().normalizeInput( naturalId ),
				session.getLoadQueryInfluencers(),
				lockOptions,
				session.getFactory()
		);
	}

	private int getMaxBatchSize(
			@Nonnull Object[] naturalIds,
			@Nonnull MultiNaturalIdLoadOptions loadOptions,
			@Nonnull SharedSessionContractImplementor session) {
		final Integer batchSize = loadOptions.getBatchSize();
		if ( batchSize != null && batchSize > 0 ) {
			return batchSize;
		}
		else {
			return session.getJdbcServices().getJdbcEnvironment().getDialect()
					.getMultiKeyLoadSizingStrategy()
					.determineOptimalBatchLoadSize(
							getEntityDescriptor().getNaturalIdMapping().getJdbcTypeCount(),
							naturalIds.length,
							session.getFactory().getSessionFactoryOptions().inClauseParameterPaddingEnabled()
					);
		}
	}

}
