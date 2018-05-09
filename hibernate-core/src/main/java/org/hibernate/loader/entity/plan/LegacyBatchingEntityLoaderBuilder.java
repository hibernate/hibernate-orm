/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.entity.plan;

import java.io.Serializable;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.internal.BatchFetchQueueHelper;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.loader.entity.UniqueEntityLoader;
import org.hibernate.persister.entity.OuterJoinLoadable;

/**
 * LoadPlan-based implementation of the legacy batch loading strategy
 *
 * @author Steve Ebersole
 */
public class LegacyBatchingEntityLoaderBuilder extends AbstractBatchingEntityLoaderBuilder {
	public static final LegacyBatchingEntityLoaderBuilder INSTANCE = new LegacyBatchingEntityLoaderBuilder();

	@Override
	protected UniqueEntityLoader buildBatchingLoader(
			OuterJoinLoadable persister,
			int batchSize,
			LockMode lockMode,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers influencers) {
		return new LegacyBatchingEntityLoader( persister, batchSize, lockMode, factory, influencers );
	}

	@Override
	protected UniqueEntityLoader buildBatchingLoader(
			OuterJoinLoadable persister,
			int batchSize,
			LockOptions lockOptions,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers influencers) {
		return new LegacyBatchingEntityLoader( persister, batchSize, lockOptions, factory, influencers );
	}

	@Override
	protected UniqueEntityLoader buildBatchingLoader(
			OuterJoinLoadable persister,
			UniqueEntityLoader entityLoaderTemplate,
			int batchSize,
			LockMode lockMode,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers influencers) {
		if (entityLoaderTemplate instanceof LegacyBatchingEntityLoader) {
			return new LegacyBatchingEntityLoader( persister, (LegacyBatchingEntityLoader) entityLoaderTemplate, batchSize, lockMode, null, factory,
					influencers );
		}
		else {
			return new LegacyBatchingEntityLoader( persister, batchSize, lockMode, factory, influencers );
		}
	}

	@Override
	protected UniqueEntityLoader buildBatchingLoader(
			OuterJoinLoadable persister,
			UniqueEntityLoader entityLoaderTemplate,
			int batchSize,
			LockOptions lockOptions,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers influencers) {
		if (entityLoaderTemplate instanceof LegacyBatchingEntityLoader) {
			return new LegacyBatchingEntityLoader( persister, (LegacyBatchingEntityLoader) entityLoaderTemplate, batchSize, null, lockOptions, factory,
					influencers );
		}
		else {
			return new LegacyBatchingEntityLoader( persister, batchSize, lockOptions, factory, influencers );
		}
	}

	public static class LegacyBatchingEntityLoader extends BatchingEntityLoader  {
		private final int[] batchSizes;
		private final EntityLoader[] loaders;

		public LegacyBatchingEntityLoader(
				OuterJoinLoadable persister,
				int maxBatchSize,
				LockMode lockMode,
				SessionFactoryImplementor factory,
				LoadQueryInfluencers loadQueryInfluencers) {
			this( persister, null, maxBatchSize, lockMode, null, factory, loadQueryInfluencers );
		}

		public LegacyBatchingEntityLoader(
				OuterJoinLoadable persister,
				int maxBatchSize,
				LockOptions lockOptions,
				SessionFactoryImplementor factory,
				LoadQueryInfluencers loadQueryInfluencers) {
			this( persister, null, maxBatchSize, null, lockOptions, factory, loadQueryInfluencers );
		}

		private LegacyBatchingEntityLoader(
				OuterJoinLoadable persister,
				LegacyBatchingEntityLoader batchingEntityLoaderTemplate,
				int maxBatchSize,
				LockMode lockMode,
				LockOptions lockOptions,
				SessionFactoryImplementor factory,
				LoadQueryInfluencers loadQueryInfluencers) {
			super( persister );
			this.batchSizes = ArrayHelper.getBatchSizes( maxBatchSize );
			this.loaders = new EntityLoader[ batchSizes.length ];
			final EntityLoader.Builder entityLoaderBuilder = EntityLoader.forEntity( persister )
					.withInfluencers( loadQueryInfluencers )
					.withLockMode( lockMode )
					.withLockOptions( lockOptions );

			if ( batchingEntityLoaderTemplate == null ) {
				// if we don't have a template, we load the first EntityLoader from scratch and then we use it as a template
				this.loaders[0] = entityLoaderBuilder.withBatchSize( batchSizes[0] ).byPrimaryKey();
			}
			else {
				// if we have a template, we use it immediately
				this.loaders[0] = entityLoaderBuilder.withEntityLoaderTemplate( batchingEntityLoaderTemplate.loaders[0] ).withBatchSize( batchSizes[0] )
						.byPrimaryKey();
			}

			// then we generate the other loaders
			for ( int i = 1; i < batchSizes.length; i++ ) {
				this.loaders[i] = entityLoaderBuilder.withEntityLoaderTemplate( this.loaders[0] ).withBatchSize( batchSizes[i] ).byPrimaryKey();
			}
		}

		@Override
		public Object load(Serializable id, Object optionalObject, SharedSessionContractImplementor session, LockOptions lockOptions) {
			final Serializable[] batch = session.getPersistenceContext()
					.getBatchFetchQueue()
					.getEntityBatch( persister(), id, batchSizes[0], persister().getEntityMode() );

			for ( int i = 0; i < batchSizes.length-1; i++) {
				final int smallBatchSize = batchSizes[i];
				if ( batch[smallBatchSize-1] != null ) {
					Serializable[] smallBatch = new Serializable[smallBatchSize];
					System.arraycopy(batch, 0, smallBatch, 0, smallBatchSize);
					// for now...
					final List results = loaders[i].loadEntityBatch(
							session,
							smallBatch,
							persister().getIdentifierType(),
							optionalObject,
							persister().getEntityName(),
							id,
							persister(),
							lockOptions
					);
					// The EntityKey for any entity that is not found will remain in the batch.
					// Explicitly remove the EntityKeys for entities that were not found to
					// avoid including them in future batches that get executed.
					BatchFetchQueueHelper.removeNotFoundBatchLoadableEntityKeys(
							smallBatch,
							results,
							persister(),
							session
					);

					//EARLY EXIT
					return getObjectFromList( results, id, session );
				}
			}
			final Object result = ( loaders[batchSizes.length-1] ).load( id, optionalObject, session, lockOptions );
			if ( result == null ) {
				// There was no entity with the specified ID. Make sure the EntityKey does not remain
				// in the batch to avoid including it in future batches that get executed.
				BatchFetchQueueHelper.removeBatchLoadableEntityKey( id, persister(), session );
			}
			return result;
		}
	}
}
