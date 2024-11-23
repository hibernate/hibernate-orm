/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.entity.plan;

import java.io.Serializable;
import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.engine.internal.BatchFetchQueueHelper;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.pretty.MessageHelper;

import org.jboss.logging.Logger;

/**
 * Batching entity loader using dynamic where-clause
 */
public class DynamicBatchingEntityLoader extends BatchingEntityLoader {
	private static final Logger log = Logger.getLogger( DynamicBatchingEntityLoader.class );

	private final int maxBatchSize;
	private final LoadQueryInfluencers loadQueryInfluencers;

	public DynamicBatchingEntityLoader(
			OuterJoinLoadable persister,
			int maxBatchSize,
			LockOptions lockOptions,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers loadQueryInfluencers) {
		super( persister );
		this.maxBatchSize = maxBatchSize;
		this.loadQueryInfluencers = loadQueryInfluencers;
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
				.getEntityBatch( persister(), id, maxBatchSize, persister().getEntityMode() );

		final int numberOfIds = ArrayHelper.countNonNull( batch );
		final Serializable[] idsToLoad = new Serializable[ numberOfIds ];

		System.arraycopy( batch, 0, idsToLoad, 0, numberOfIds );

		if ( log.isDebugEnabled() ) {
			log.debugf( "Batch loading entity: %s", MessageHelper.infoString( persister(), idsToLoad, session.getFactory() ) );
		}


		final EntityLoader dynamicLoader = EntityLoader.forEntity( (OuterJoinLoadable) persister() )
				.withInfluencers( loadQueryInfluencers )
				.withLockOptions( lockOptions )
				.withBatchSize( idsToLoad.length )
				.byPrimaryKey();

		final List<?> results = dynamicLoader.loadEntityBatch(
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
