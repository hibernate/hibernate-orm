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
package org.hibernate.loader.collection.plan;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.loader.Loader;
import org.hibernate.loader.collection.BasicCollectionLoader;
import org.hibernate.loader.collection.CollectionInitializer;
import org.hibernate.loader.collection.OneToManyLoader;
import org.hibernate.persister.collection.QueryableCollection;

/**
 * LoadPlan-based implementation of the the legacy batch collection initializer.
 *
 * @author Steve Ebersole
 */
public class LegacyBatchingCollectionInitializerBuilder extends AbstractBatchingCollectionInitializerBuilder {
	public static final LegacyBatchingCollectionInitializerBuilder INSTANCE = new LegacyBatchingCollectionInitializerBuilder();

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
		return new LegacyBatchingCollectionInitializer( persister, batchSizes, loaders );
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
		return new LegacyBatchingCollectionInitializer( persister, batchSizes, loaders );
	}


	public static class LegacyBatchingCollectionInitializer extends BatchingCollectionInitializer {
		private final int[] batchSizes;
		private final Loader[] loaders;

		public LegacyBatchingCollectionInitializer(
				QueryableCollection persister,
				int[] batchSizes,
				Loader[] loaders) {
			super( persister );
			this.batchSizes = batchSizes;
			this.loaders = loaders;
		}

		@Override
		public void initialize(Serializable id, SessionImplementor session)	throws HibernateException {
			Serializable[] batch = session.getPersistenceContext().getBatchFetchQueue()
					.getCollectionBatch( getCollectionPersister(), id, batchSizes[0] );

			for ( int i=0; i<batchSizes.length-1; i++) {
				final int smallBatchSize = batchSizes[i];
				if ( batch[smallBatchSize-1]!=null ) {
					Serializable[] smallBatch = new Serializable[smallBatchSize];
					System.arraycopy(batch, 0, smallBatch, 0, smallBatchSize);
					loaders[i].loadCollectionBatch( session, smallBatch, getCollectionPersister().getKeyType() );
					return; //EARLY EXIT!
				}
			}

			loaders[batchSizes.length-1].loadCollection( session, id, getCollectionPersister().getKeyType() );
		}
	}
}
