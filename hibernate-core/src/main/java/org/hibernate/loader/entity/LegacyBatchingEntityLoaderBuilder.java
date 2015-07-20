/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.entity;

import java.io.Serializable;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.loader.Loader;
import org.hibernate.persister.entity.OuterJoinLoadable;

/**
 * No longer used, see {@link org.hibernate.loader.entity.plan.LegacyBatchingEntityLoaderBuilder} instead.
 *
 * @author Steve Ebersole
 */
public class LegacyBatchingEntityLoaderBuilder extends BatchingEntityLoaderBuilder {
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

	public static class LegacyBatchingEntityLoader extends BatchingEntityLoader implements UniqueEntityLoader {
		private final int[] batchSizes;
		private final Loader[] loaders;

		public LegacyBatchingEntityLoader(
				OuterJoinLoadable persister,
				int maxBatchSize,
				LockMode lockMode,
				SessionFactoryImplementor factory,
				LoadQueryInfluencers loadQueryInfluencers) {
			super( persister );
			this.batchSizes = ArrayHelper.getBatchSizes( maxBatchSize );
			this.loaders = new Loader[ batchSizes.length ];
			for ( int i = 0; i < batchSizes.length; i++ ) {
				this.loaders[i] = new EntityLoader( persister, batchSizes[i], lockMode, factory, loadQueryInfluencers);
			}
		}

		public LegacyBatchingEntityLoader(
				OuterJoinLoadable persister,
				int maxBatchSize,
				LockOptions lockOptions,
				SessionFactoryImplementor factory,
				LoadQueryInfluencers loadQueryInfluencers) {
			super( persister );
			this.batchSizes = ArrayHelper.getBatchSizes( maxBatchSize );
			this.loaders = new Loader[ batchSizes.length ];
			for ( int i = 0; i < batchSizes.length; i++ ) {
				this.loaders[i] = new EntityLoader( persister, batchSizes[i], lockOptions, factory, loadQueryInfluencers);
			}
		}

		@Override
		public Object load(Serializable id, Object optionalObject, SessionImplementor session, LockOptions lockOptions) {
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
					return getObjectFromList(results, id, session); //EARLY EXIT
				}
			}
			return ( (UniqueEntityLoader) loaders[batchSizes.length-1] ).load(id, optionalObject, session);
		}
	}

}
