/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.loader.collection;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.loader.Loader;
import org.hibernate.persister.collection.QueryableCollection;

/**
 * A batch-fetch capable CollectionInitializer that performs batch-fetching using the padded style.  See
 * {@link org.hibernate.loader.BatchFetchStyle} for a discussion of the different styles.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.loader.BatchFetchStyle#PADDED
 */
public class PaddedBatchingCollectionInitializerBuilder extends BatchingCollectionInitializerBuilder {
	public static final PaddedBatchingCollectionInitializerBuilder INSTANCE = new PaddedBatchingCollectionInitializerBuilder();

	@Override
	public CollectionInitializer createRealBatchingCollectionInitializer(
			QueryableCollection persister,
			int maxBatchSize,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers loadQueryInfluencers) throws MappingException {
		int[] batchSizes = ArrayHelper.getBatchSizes( maxBatchSize );
		Loader[] loaders = new Loader[ batchSizes.length ];
		for ( int i = 0; i < batchSizes.length; i++ ) {
			loaders[i] = new BasicCollectionLoader( persister, batchSizes[i], factory, loadQueryInfluencers );
		}
		return new PaddedBatchingCollectionInitializer( persister, batchSizes, loaders );
	}

	@Override
	public CollectionInitializer createRealBatchingOneToManyInitializer(
			QueryableCollection persister,
			int maxBatchSize,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers loadQueryInfluencers) throws MappingException {
		final int[] batchSizes = ArrayHelper.getBatchSizes( maxBatchSize );
		final Loader[] loaders = new Loader[ batchSizes.length ];
		for ( int i = 0; i < batchSizes.length; i++ ) {
			loaders[i] = new OneToManyLoader( persister, batchSizes[i], factory, loadQueryInfluencers );
		}
		return new PaddedBatchingCollectionInitializer( persister, batchSizes, loaders );
	}


	private static class PaddedBatchingCollectionInitializer extends BatchingCollectionInitializer {
		private final int[] batchSizes;
		private final Loader[] loaders;

		public PaddedBatchingCollectionInitializer(QueryableCollection persister, int[] batchSizes, Loader[] loaders) {
			super( persister );

			this.batchSizes = batchSizes;
			this.loaders = loaders;
		}

		@Override
		public void initialize(Serializable id, SessionImplementor session)	throws HibernateException {
			final Serializable[] batch = session.getPersistenceContext()
					.getBatchFetchQueue()
					.getCollectionBatch( collectionPersister(), id, batchSizes[0] );
			final int numberOfIds = ArrayHelper.countNonNull( batch );
			if ( numberOfIds <= 1 ) {
				loaders[batchSizes.length-1].loadCollection( session, id, collectionPersister().getKeyType() );
				return;
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

			loaders[indexToUse].loadCollectionBatch( session, idsToLoad, collectionPersister().getKeyType() );
		}
	}
}
