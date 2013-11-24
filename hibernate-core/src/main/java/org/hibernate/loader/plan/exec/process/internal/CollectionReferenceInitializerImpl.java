/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.loader.plan.exec.process.internal;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.internal.CoreLogging;
import org.hibernate.loader.plan.exec.process.spi.CollectionReferenceInitializer;
import org.hibernate.loader.plan.exec.process.spi.ResultSetProcessingContext;
import org.hibernate.loader.plan.exec.spi.CollectionReferenceAliases;
import org.hibernate.loader.plan.spi.CollectionReference;
import org.hibernate.loader.plan.spi.Fetch;
import org.hibernate.pretty.MessageHelper;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class CollectionReferenceInitializerImpl implements CollectionReferenceInitializer {
	private static final Logger log = CoreLogging.logger( CollectionReferenceInitializerImpl.class );

	private final CollectionReference collectionReference;
	private final CollectionReferenceAliases aliases;

	public CollectionReferenceInitializerImpl(CollectionReference collectionReference, CollectionReferenceAliases aliases) {
		this.collectionReference = collectionReference;
		this.aliases = aliases;
	}

	@Override
	public CollectionReference getCollectionReference() {
		return collectionReference;
	}

	@Override
	public void finishUpRow(ResultSet resultSet, ResultSetProcessingContextImpl context) {

		try {
			// read the collection key for this reference for the current row.
			final PersistenceContext persistenceContext = context.getSession().getPersistenceContext();
			final Serializable collectionRowKey = (Serializable) collectionReference.getCollectionPersister().readKey(
					resultSet,
					aliases.getCollectionColumnAliases().getSuffixedKeyAliases(),
					context.getSession()
			);

			if ( collectionRowKey != null ) {
				// we found a collection element in the result set

				if ( log.isDebugEnabled() ) {
					log.debugf(
							"Found row of collection: %s",
							MessageHelper.collectionInfoString(
									collectionReference.getCollectionPersister(),
									collectionRowKey,
									context.getSession().getFactory()
							)
					);
				}

				Object collectionOwner = findCollectionOwner( collectionRowKey, resultSet, context );

				PersistentCollection rowCollection = persistenceContext.getLoadContexts()
						.getCollectionLoadContext( resultSet )
						.getLoadingCollection( collectionReference.getCollectionPersister(), collectionRowKey );

				if ( rowCollection != null ) {
					rowCollection.readFrom(
							resultSet,
							collectionReference.getCollectionPersister(),
							aliases.getCollectionColumnAliases(),
							collectionOwner
					);
				}

			}
			else {
				final Serializable optionalKey = findCollectionOwnerKey( context );
				if ( optionalKey != null ) {
					// we did not find a collection element in the result set, so we
					// ensure that a collection is created with the owner's identifier,
					// since what we have is an empty collection
					if ( log.isDebugEnabled() ) {
						log.debugf(
								"Result set contains (possibly empty) collection: %s",
								MessageHelper.collectionInfoString(
										collectionReference.getCollectionPersister(),
										optionalKey,
										context.getSession().getFactory()
								)
						);
					}
					// handle empty collection
					persistenceContext.getLoadContexts()
							.getCollectionLoadContext( resultSet )
							.getLoadingCollection( collectionReference.getCollectionPersister(), optionalKey );

				}
			}
			// else no collection element, but also no owner
		}
		catch ( SQLException sqle ) {
			// TODO: would be nice to have the SQL string that failed...
			throw context.getSession().getFactory().getSQLExceptionHelper().convert(
					sqle,
					"could not read next row of results"
			);
		}
	}

	protected Object findCollectionOwner(
			Serializable collectionRowKey,
			ResultSet resultSet,
			ResultSetProcessingContextImpl context) {
		final Object collectionOwner = context.getSession().getPersistenceContext().getCollectionOwner(
				collectionRowKey,
				collectionReference.getCollectionPersister()
		);
		// todo : try org.hibernate.loader.plan.exec.process.spi.ResultSetProcessingContext.getOwnerProcessingState() ??
		//			-- specifically to return its ResultSetProcessingContext.EntityReferenceProcessingState#getEntityInstance()
		if ( collectionOwner == null ) {
			//TODO: This is assertion is disabled because there is a bug that means the
			//	  original owner of a transient, uninitialized collection is not known
			//	  if the collection is re-referenced by a different object associated
			//	  with the current Session
			//throw new AssertionFailure("bug loading unowned collection");
		}
		return collectionOwner;
	}

	protected Serializable findCollectionOwnerKey(ResultSetProcessingContextImpl context) {
		ResultSetProcessingContext.EntityReferenceProcessingState ownerState = context.getOwnerProcessingState( (Fetch) collectionReference );

		if(ownerState == null || ownerState.getEntityKey()==null){
			return null;
		}
		return ownerState.getEntityKey().getIdentifier();
	}

	@Override
	public void endLoading(ResultSetProcessingContextImpl context) {
		context.getSession().getPersistenceContext()
				.getLoadContexts()
				.getCollectionLoadContext( context.getResultSet() )
				.endLoadingCollections( collectionReference.getCollectionPersister() );
	}
}
