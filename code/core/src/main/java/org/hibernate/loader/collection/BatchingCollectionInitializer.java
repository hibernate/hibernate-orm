//$Id: BatchingCollectionInitializer.java 7123 2005-06-13 20:10:20Z oneovthafew $
package org.hibernate.loader.collection;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
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
		final Map enabledFilters)
	throws MappingException {

		if ( maxBatchSize>1 ) {
			int[] batchSizesToCreate = ArrayHelper.getBatchSizes(maxBatchSize);
			Loader[] loadersToCreate = new Loader[ batchSizesToCreate.length ];
			for ( int i=0; i<batchSizesToCreate.length; i++ ) {
				loadersToCreate[i] = new OneToManyLoader(persister, batchSizesToCreate[i], factory, enabledFilters);
			}
			return new BatchingCollectionInitializer(persister, batchSizesToCreate, loadersToCreate);
		}
		else {
			return new OneToManyLoader(persister, factory, enabledFilters);
		}
	}

	public static CollectionInitializer createBatchingCollectionInitializer(
		final QueryableCollection persister,
		final int maxBatchSize,
		final SessionFactoryImplementor factory,
		final Map enabledFilters)
	throws MappingException {

		if ( maxBatchSize>1 ) {
			int[] batchSizesToCreate = ArrayHelper.getBatchSizes(maxBatchSize);
			Loader[] loadersToCreate = new Loader[ batchSizesToCreate.length ];
			for ( int i=0; i<batchSizesToCreate.length; i++ ) {
				loadersToCreate[i] = new BasicCollectionLoader(persister, batchSizesToCreate[i], factory, enabledFilters);
			}
			return new BatchingCollectionInitializer(persister, batchSizesToCreate, loadersToCreate);
		}
		else {
			return new BasicCollectionLoader(persister, factory, enabledFilters);
		}
	}

}
