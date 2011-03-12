/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.loader.collection;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.LoadQueryInfluencers;
import org.hibernate.loader.Loader;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.util.ArrayHelper;

/**
 * "Batch" loads collections, using multiple foreign key values in the
 * SQL <tt>where</tt> clause.
 *
 * @see BasicCollectionLoader
 * @see OneToManyLoader
 * @author Gavin King
 */
public class BatchingCollectionInitializer implements CollectionInitializer {
	private final Loader[] loaders;
	private final int[] batchSizes;
	private final CollectionPersister collectionPersister;

	public BatchingCollectionInitializer(CollectionPersister collPersister, int[] batchSizes, Loader[] loaders) {
		this.loaders = loaders;
		this.batchSizes = batchSizes;
		this.collectionPersister = collPersister;
	}

	public CollectionPersister getCollectionPersister() {
		return collectionPersister;
	}

	public Loader[] getLoaders() {
		return loaders;
	}

	public int[] getBatchSizes() {
		return batchSizes;
	}

	public void initialize(Serializable id, SessionImplementor session)
	throws HibernateException {
		
		Serializable[] batch = session.getPersistenceContext().getBatchFetchQueue()
			.getCollectionBatch( collectionPersister, id, batchSizes[0], session.getEntityMode() );
		
		for ( int i=0; i<batchSizes.length-1; i++) {
			final int smallBatchSize = batchSizes[i];
			if ( batch[smallBatchSize-1]!=null ) {
				Serializable[] smallBatch = new Serializable[smallBatchSize];
				System.arraycopy(batch, 0, smallBatch, 0, smallBatchSize);
				loaders[i].loadCollectionBatch( session, smallBatch, collectionPersister.getKeyType() );
				return; //EARLY EXIT!
			}
		}
		
		loaders[batchSizes.length-1].loadCollection( session, id, collectionPersister.getKeyType() );

	}

	public static CollectionInitializer createBatchingOneToManyInitializer(
			final QueryableCollection persister,
			final int maxBatchSize,
			final SessionFactoryImplementor factory,
			final LoadQueryInfluencers loadQueryInfluencers) throws MappingException {
		if ( maxBatchSize > 1 ) {
			int[] batchSizesToCreate = ArrayHelper.getBatchSizes(maxBatchSize);
			Loader[] loadersToCreate = new Loader[ batchSizesToCreate.length ];
			for ( int i=0; i<batchSizesToCreate.length; i++ ) {
				loadersToCreate[i] = new OneToManyLoader( persister, batchSizesToCreate[i], factory, loadQueryInfluencers );
			}
			return new BatchingCollectionInitializer( persister, batchSizesToCreate, loadersToCreate );
		}
		else {
			return new OneToManyLoader( persister, factory, loadQueryInfluencers );
		}
	}

	public static CollectionInitializer createBatchingCollectionInitializer(
			final QueryableCollection persister,
			final int maxBatchSize,
			final SessionFactoryImplementor factory,
			final LoadQueryInfluencers loadQueryInfluencers) throws MappingException {
		if ( maxBatchSize > 1 ) {
			int[] batchSizesToCreate = ArrayHelper.getBatchSizes(maxBatchSize);
			Loader[] loadersToCreate = new Loader[ batchSizesToCreate.length ];
			for ( int i=0; i<batchSizesToCreate.length; i++ ) {
				loadersToCreate[i] = new BasicCollectionLoader( persister, batchSizesToCreate[i], factory, loadQueryInfluencers );
			}
			return new BatchingCollectionInitializer(persister, batchSizesToCreate, loadersToCreate);
		}
		else {
			return new BasicCollectionLoader( persister, factory, loadQueryInfluencers );
		}
	}

}
