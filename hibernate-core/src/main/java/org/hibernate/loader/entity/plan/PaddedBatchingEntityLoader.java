/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.entity.plan;

import java.io.Serializable;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.engine.internal.BatchFetchQueueHelper;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.loader.entity.UniqueEntityLoader;
import org.hibernate.persister.entity.OuterJoinLoadable;

/**
 * Batching entity loader using padded where-clause
 */
public class PaddedBatchingEntityLoader extends BatchingEntityLoader {
	private final int[] batchSizes;
	private final EntityLoader[] loaders;

	public PaddedBatchingEntityLoader(
			OuterJoinLoadable persister,
			int maxBatchSize,
			LockOptions lockOptions,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers loadQueryInfluencers) {
		super( persister );

		this.batchSizes = ArrayHelper.getBatchSizes( maxBatchSize );
		this.loaders = new EntityLoader[ batchSizes.length ];
		final EntityLoader.Builder entityLoaderBuilder = EntityLoader.forEntity( persister )
				.withInfluencers( loadQueryInfluencers )
				.withLockOptions( lockOptions );

		// we create a first entity loader to use it as a template for the others
		this.loaders[0] = entityLoaderBuilder.withBatchSize( batchSizes[0] ).byPrimaryKey();

		for ( int i = 1; i < batchSizes.length; i++ ) {
			this.loaders[i] = entityLoaderBuilder.withEntityLoaderTemplate( this.loaders[0] ).withBatchSize( batchSizes[i] ).byPrimaryKey();
		}

		validate( maxBatchSize );
	}

	private void validate(int max) {
		// these are more indicative of internal problems then user error...
		if ( batchSizes[0] != max ) {
			throw new HibernateException( "Unexpected batch size spread" );
		}
		if ( batchSizes[batchSizes.length-1] != 1 ) {
			throw new HibernateException( "Unexpected batch size spread" );
		}
	}

	@Override
	public Object load(Serializable id, Object optionalObject, SharedSessionContractImplementor session) {
		return load( id, optionalObject, session, LockOptions.NONE, null );
	}

	@Override
	public Object load(
			Serializable id,
			Object optionalObject,
			SharedSessionContractImplementor session,
			LockOptions lockOptions) {
		return load( id, optionalObject, session, lockOptions, null );
	}

	@Override
	public Object load(Serializable id, Object optionalObject, SharedSessionContractImplementor session, LockOptions lockOptions, Boolean readOnly) {
		final Serializable[] batch = session.getPersistenceContextInternal()
				.getBatchFetchQueue()
				.getEntityBatch( persister(), id, batchSizes[0], persister().getEntityMode() );

		final int numberOfIds = ArrayHelper.countNonNull( batch );

		if ( numberOfIds <= 1 ) {
			final Object result =  ( (UniqueEntityLoader) loaders[batchSizes.length-1] ).load( id, optionalObject, session, lockOptions );
			if ( result == null ) {
				// There was no entity with the specified ID. Make sure the EntityKey does not remain
				// in the batch to avoid including it in future batches that get executed.
				BatchFetchQueueHelper.removeBatchLoadableEntityKey( id, persister(), session );
			}
			return result;
		}

		// Uses the first batch-size bigger than the number of actual ids in the batch
		int indexToUse = batchSizes.length-1;
		for ( int i = 0; i < batchSizes.length-1; i++ ) {
			if ( batchSizes[i] >= numberOfIds ) {
				indexToUse = i;
			}
			else {
				break;
			}
		}

		final Serializable[] idsToLoad = new Serializable[ batchSizes[indexToUse] ];
		System.arraycopy( batch, 0, idsToLoad, 0, numberOfIds );
		for ( int i = numberOfIds; i < batchSizes[indexToUse]; i++ ) {
			idsToLoad[i] = id;
		}

		final List<?> results = loaders[indexToUse].loadEntityBatch(
				session,
				idsToLoad,
				persister().getIdentifierType(),
				optionalObject,
				persister().getEntityName(),
				id,
				persister(),
				lockOptions,
				readOnly
		);

		// The EntityKey for any entity that is not found will remain in the batch.
		// Explicitly remove the EntityKeys for entities that were not found to
		// avoid including them in future batches that get executed.
		BatchFetchQueueHelper.removeNotFoundBatchLoadableEntityKeys(
				idsToLoad,
				results,
				persister(),
				session
		);

		return getObjectFromList( results, id, session );
	}
}
