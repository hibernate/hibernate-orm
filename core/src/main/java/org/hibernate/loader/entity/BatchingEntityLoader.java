//$Id: BatchingEntityLoader.java 6929 2005-05-27 03:54:08Z oneovthafew $
package org.hibernate.loader.entity;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.loader.Loader;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.type.Type;
import org.hibernate.util.ArrayHelper;

/**
 * "Batch" loads entities, using multiple primary key values in the
 * SQL <tt>where</tt> clause.
 *
 * @see EntityLoader
 * @author Gavin King
 */
public class BatchingEntityLoader implements UniqueEntityLoader {

	private final Loader[] loaders;
	private final int[] batchSizes;
	private final EntityPersister persister;
	private final Type idType;

	public BatchingEntityLoader(EntityPersister persister, int[] batchSizes, Loader[] loaders) {
		this.batchSizes = batchSizes;
		this.loaders = loaders;
		this.persister = persister;
		idType = persister.getIdentifierType();
	}

	private Object getObjectFromList(List results, Serializable id, SessionImplementor session) {
		// get the right object from the list ... would it be easier to just call getEntity() ??
		Iterator iter = results.iterator();
		while ( iter.hasNext() ) {
			Object obj = iter.next();
			final boolean equal = idType.isEqual( 
					id, 
					session.getContextEntityIdentifier(obj), 
					session.getEntityMode(), 
					session.getFactory() 
			);
			if ( equal ) return obj;
		}
		return null;
	}

	public Object load(Serializable id, Object optionalObject, SessionImplementor session)
	throws HibernateException {
		
		Serializable[] batch = session.getPersistenceContext()
			.getBatchFetchQueue()
			.getEntityBatch( persister, id, batchSizes[0], session.getEntityMode() );
		
		for ( int i=0; i<batchSizes.length-1; i++) {
			final int smallBatchSize = batchSizes[i];
			if ( batch[smallBatchSize-1]!=null ) {
				Serializable[] smallBatch = new Serializable[smallBatchSize];
				System.arraycopy(batch, 0, smallBatch, 0, smallBatchSize);
				final List results = loaders[i].loadEntityBatch(
						session, 
						smallBatch, 
						idType, 
						optionalObject, 
						persister.getEntityName(), 
						id, 
						persister
				);
				return getObjectFromList(results, id, session); //EARLY EXIT
			}
		}
		
		return ( (UniqueEntityLoader) loaders[batchSizes.length-1] ).load(id, optionalObject, session);

	}

	public static UniqueEntityLoader createBatchingEntityLoader(
		final OuterJoinLoadable persister,
		final int maxBatchSize,
		final LockMode lockMode,
		final SessionFactoryImplementor factory,
		final Map enabledFilters)
	throws MappingException {

		if ( maxBatchSize>1 ) {
			int[] batchSizesToCreate = ArrayHelper.getBatchSizes(maxBatchSize);
			Loader[] loadersToCreate = new Loader[ batchSizesToCreate.length ];
			for ( int i=0; i<batchSizesToCreate.length; i++ ) {
				loadersToCreate[i] = new EntityLoader(persister, batchSizesToCreate[i], lockMode, factory, enabledFilters);
			}
			return new BatchingEntityLoader(persister, batchSizesToCreate, loadersToCreate);
		}
		else {
			return new EntityLoader(persister, lockMode, factory, enabledFilters);
		}
	}

}
