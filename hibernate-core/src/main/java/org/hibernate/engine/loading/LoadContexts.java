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
package org.hibernate.engine.loading;

import java.sql.ResultSet;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.HashMap;
import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.util.IdentityMap;
import org.hibernate.engine.PersistenceContext;
import org.hibernate.engine.CollectionKey;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.EntityMode;

/**
 * Maps {@link ResultSet result-sets} to specific contextual data
 * related to processing that {@link ResultSet result-sets}.
 * <p/>
 * Implementation note: internally an {@link IdentityMap} is used to maintain
 * the mappings; {@link IdentityMap} was chosen because I'd rather not be
 * dependent upon potentially bad {@link ResultSet#equals} and {ResultSet#hashCode}
 * implementations.
 * <p/>
 * Considering the JDBC-redesign work, would further like this contextual info
 * not mapped seperately, but available based on the result set being processed.
 * This would also allow maintaining a single mapping as we could reliably get
 * notification of the result-set closing...
 *
 * @author Steve Ebersole
 */
public class LoadContexts {
	private static final Logger log = LoggerFactory.getLogger( LoadContexts.class );

	private final PersistenceContext persistenceContext;
	private Map collectionLoadContexts;
	private Map entityLoadContexts;

	private Map xrefLoadingCollectionEntries;

	/**
	 * Creates and binds this to the given persistence context.
	 *
	 * @param persistenceContext The persistence context to which this
	 * will be bound.
	 */
	public LoadContexts(PersistenceContext persistenceContext) {
		this.persistenceContext = persistenceContext;
	}

	/**
	 * Retrieves the persistence context to which this is bound.
	 *
	 * @return The persistence context to which this is bound.
	 */
	public PersistenceContext getPersistenceContext() {
		return persistenceContext;
	}

	private SessionImplementor getSession() {
		return getPersistenceContext().getSession();
	}

	private EntityMode getEntityMode() {
		return getSession().getEntityMode();
	}


	// cleanup code ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

 	/**
	 * Release internal state associated with the given result set.
	 * <p/>
	 * This should be called when we are done with processing said result set,
	 * ideally as the result set is being closed.
	 *
	 * @param resultSet The result set for which it is ok to release
	 * associated resources.
	 */
	public void cleanup(ResultSet resultSet) {
		if ( collectionLoadContexts != null ) {
			CollectionLoadContext collectionLoadContext = ( CollectionLoadContext ) collectionLoadContexts.remove( resultSet );
			collectionLoadContext.cleanup();
		}
		if ( entityLoadContexts != null ) {
			EntityLoadContext entityLoadContext = ( EntityLoadContext ) entityLoadContexts.remove( resultSet );
			entityLoadContext.cleanup();
		}
	}

	/**
	 * Release internal state associated with *all* result sets.
	 * <p/>
	 * This is intended as a "failsafe" process to make sure we get everything
	 * cleaned up and released.
	 */
	public void cleanup() {
		if ( collectionLoadContexts != null ) {
			Iterator itr = collectionLoadContexts.values().iterator();
			while ( itr.hasNext() ) {
				CollectionLoadContext collectionLoadContext = ( CollectionLoadContext ) itr.next();
				log.warn( "fail-safe cleanup (collections) : " + collectionLoadContext );
				collectionLoadContext.cleanup();
			}
			collectionLoadContexts.clear();
		}
		if ( entityLoadContexts != null ) {
			Iterator itr = entityLoadContexts.values().iterator();
			while ( itr.hasNext() ) {
				EntityLoadContext entityLoadContext = ( EntityLoadContext ) itr.next();
				log.warn( "fail-safe cleanup (entities) : " + entityLoadContext );
				entityLoadContext.cleanup();
			}
			entityLoadContexts.clear();
		}
	}


	// Collection load contexts ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Do we currently have any internal entries corresponding to loading
	 * collections?
	 *
	 * @return True if we currently hold state pertaining to loading collections;
	 * false otherwise.
	 */
	public boolean hasLoadingCollectionEntries() {
		return ( collectionLoadContexts != null && !collectionLoadContexts.isEmpty() );
	}

	/**
	 * Do we currently have any registered internal entries corresponding to loading
	 * collections?
	 *
	 * @return True if we currently hold state pertaining to a registered loading collections;
	 * false otherwise.
	 */
	public boolean hasRegisteredLoadingCollectionEntries() {
		return ( xrefLoadingCollectionEntries != null && !xrefLoadingCollectionEntries.isEmpty() );
	}


	/**
	 * Get the {@link CollectionLoadContext} associated with the given
	 * {@link ResultSet}, creating one if needed.
	 *
	 * @param resultSet The result set for which to retrieve the context.
	 * @return The processing context.
	 */
	public CollectionLoadContext getCollectionLoadContext(ResultSet resultSet) {
		CollectionLoadContext context = null;
		if ( collectionLoadContexts == null ) {
			collectionLoadContexts = IdentityMap.instantiate( 8 );
		}
		else {
			context = ( CollectionLoadContext ) collectionLoadContexts.get( resultSet );
		}
		if ( context == null ) {
			if ( log.isTraceEnabled() ) {
				log.trace( "constructing collection load context for result set [" + resultSet + "]" );
			}
			context = new CollectionLoadContext( this, resultSet );
			collectionLoadContexts.put( resultSet, context );
		}
		return context;
	}

	/**
	 * Attempt to locate the loading collection given the owner's key.  The lookup here
	 * occurs against all result-set contexts...
	 *
	 * @param persister The collection persister
	 * @param ownerKey The owner key
	 * @return The loading collection, or null if not found.
	 */
	public PersistentCollection locateLoadingCollection(CollectionPersister persister, Serializable ownerKey) {
		LoadingCollectionEntry lce = locateLoadingCollectionEntry( new CollectionKey( persister, ownerKey, getEntityMode() ) );
		if ( lce != null ) {
			if ( log.isTraceEnabled() ) {
				log.trace( "returning loading collection:" + MessageHelper.collectionInfoString( persister, ownerKey, getSession().getFactory() ) );
			}
			return lce.getCollection();
		}
		else {
			// todo : should really move this log statement to CollectionType, where this is used from...
			if ( log.isTraceEnabled() ) {
				log.trace( "creating collection wrapper:" + MessageHelper.collectionInfoString( persister, ownerKey, getSession().getFactory() ) );
			}
			return null;
		}
	}

	// loading collection xrefs ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Register a loading collection xref.
	 * <p/>
	 * This xref map is used because sometimes a collection is in process of
	 * being loaded from one result set, but needs to be accessed from the
	 * context of another "nested" result set processing.
	 * <p/>
	 * Implementation note: package protected, as this is meant solely for use
	 * by {@link CollectionLoadContext} to be able to locate collections
	 * being loaded by other {@link CollectionLoadContext}s/{@link ResultSet}s.
	 *
	 * @param entryKey The xref collection key
	 * @param entry The corresponding loading collection entry
	 */
	void registerLoadingCollectionXRef(CollectionKey entryKey, LoadingCollectionEntry entry) {
		if ( xrefLoadingCollectionEntries == null ) {
			xrefLoadingCollectionEntries = new HashMap();
		}
		xrefLoadingCollectionEntries.put( entryKey, entry );
	}

	/**
	 * The inverse of {@link #registerLoadingCollectionXRef}.  Here, we are done
	 * processing the said collection entry, so we remove it from the
	 * load context.
	 * <p/>
	 * The idea here is that other loading collections can now reference said
	 * collection directly from the {@link PersistenceContext} because it
	 * has completed its load cycle.
	 * <p/>
	 * Implementation note: package protected, as this is meant solely for use
	 * by {@link CollectionLoadContext} to be able to locate collections
	 * being loaded by other {@link CollectionLoadContext}s/{@link ResultSet}s.
	 *
	 * @param key The key of the collection we are done processing.
	 */
	void unregisterLoadingCollectionXRef(CollectionKey key) {
		if ( !hasRegisteredLoadingCollectionEntries() ) {
			return;
		}
		xrefLoadingCollectionEntries.remove(key);
	 }

	/*package*/Map getLoadingCollectionXRefs() {
 		return xrefLoadingCollectionEntries;
 	}


	/**
	 * Locate the LoadingCollectionEntry within *any* of the tracked
	 * {@link CollectionLoadContext}s.
	 * <p/>
	 * Implementation note: package protected, as this is meant solely for use
	 * by {@link CollectionLoadContext} to be able to locate collections
	 * being loaded by other {@link CollectionLoadContext}s/{@link ResultSet}s.
	 *
	 * @param key The collection key.
	 * @return The located entry; or null.
	 */
	LoadingCollectionEntry locateLoadingCollectionEntry(CollectionKey key) {
		if ( xrefLoadingCollectionEntries == null ) {
			return null;
		}
		if ( log.isTraceEnabled() ) {
			log.trace( "attempting to locate loading collection entry [" + key + "] in any result-set context" );
		}
		LoadingCollectionEntry rtn = ( LoadingCollectionEntry ) xrefLoadingCollectionEntries.get( key );
		if ( log.isTraceEnabled() ) {
			if ( rtn == null ) {
				log.trace( "collection [" + key + "] not located in load context" );
			}
			else {
				log.trace( "collection [" + key + "] located in load context" );
			}
		}
		return rtn;
	}

	/*package*/void cleanupCollectionXRefs(Set entryKeys) {
		Iterator itr = entryKeys.iterator();
		while ( itr.hasNext() ) {
			final CollectionKey entryKey = ( CollectionKey ) itr.next();
			xrefLoadingCollectionEntries.remove( entryKey );
		}
	}


	// Entity load contexts ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// 	* currently, not yet used...

	public EntityLoadContext getEntityLoadContext(ResultSet resultSet) {
		EntityLoadContext context = null;
		if ( entityLoadContexts == null ) {
			entityLoadContexts = IdentityMap.instantiate( 8 );
		}
		else {
			context = ( EntityLoadContext ) entityLoadContexts.get( resultSet );
		}
		if ( context == null ) {
			context = new EntityLoadContext( this, resultSet );
			entityLoadContexts.put( resultSet, context );
		}
		return context;
	}

}
