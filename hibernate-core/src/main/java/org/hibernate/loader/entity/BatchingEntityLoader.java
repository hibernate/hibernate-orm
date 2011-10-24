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
package org.hibernate.loader.entity;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.loader.Loader;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.type.Type;

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
					session.getFactory()
			);
			if ( equal ) return obj;
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public Object load(Serializable id, Object optionalObject, SessionImplementor session) {
		// this form is deprecated!
		return load( id, optionalObject, session, LockOptions.NONE );
	}

	public Object load(Serializable id, Object optionalObject, SessionImplementor session, LockOptions lockOptions) {
		Serializable[] batch = session.getPersistenceContext()
				.getBatchFetchQueue()
				.getEntityBatch( persister, id, batchSizes[0], persister.getEntityMode() );

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
						persister,
						lockOptions
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
		final LoadQueryInfluencers loadQueryInfluencers) throws MappingException {

		if ( maxBatchSize>1 ) {
			int[] batchSizesToCreate = ArrayHelper.getBatchSizes(maxBatchSize);
			Loader[] loadersToCreate = new Loader[ batchSizesToCreate.length ];
			for ( int i=0; i<batchSizesToCreate.length; i++ ) {
				loadersToCreate[i] = new EntityLoader(persister, batchSizesToCreate[i], lockMode, factory, loadQueryInfluencers);
			}
			return new BatchingEntityLoader(persister, batchSizesToCreate, loadersToCreate);
		}
		else {
			return new EntityLoader(persister, lockMode, factory, loadQueryInfluencers);
		}
	}

	public static UniqueEntityLoader createBatchingEntityLoader(
		final OuterJoinLoadable persister,
		final int maxBatchSize,
		final LockOptions lockOptions,
		final SessionFactoryImplementor factory,
		final LoadQueryInfluencers loadQueryInfluencers) throws MappingException {

		if ( maxBatchSize>1 ) {
			int[] batchSizesToCreate = ArrayHelper.getBatchSizes(maxBatchSize);
			Loader[] loadersToCreate = new Loader[ batchSizesToCreate.length ];
			for ( int i=0; i<batchSizesToCreate.length; i++ ) {
				loadersToCreate[i] = new EntityLoader(persister, batchSizesToCreate[i], lockOptions, factory, loadQueryInfluencers);
			}
			return new BatchingEntityLoader(persister, batchSizesToCreate, loadersToCreate);
		}
		else {
			return new EntityLoader(persister, lockOptions, factory, loadQueryInfluencers);
		}
	}

}
