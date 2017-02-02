/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.loading.internal;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.pretty.MessageHelper;

/**
 * Maps {@link ResultSet result-sets} to specific contextual data related to processing that result set
 * <p/>
 * Considering the JDBC-redesign work, would further like this contextual info not mapped separately, but available
 * based on the result set being processed.  This would also allow maintaining a single mapping as we could reliably
 * get notification of the result-set closing...
 *
 * @author Steve Ebersole
 */
public class LoadContexts {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( LoadContexts.class );

	private final PersistenceContext persistenceContext;
	private Map<ResultSet,CollectionLoadContext> collectionLoadContexts;
	private Map<ResultSet,EntityLoadContext> entityLoadContexts;

	private Map<CollectionKey,LoadingCollectionEntry> xrefLoadingCollectionEntries;

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

	private SharedSessionContractImplementor getSession() {
		return getPersistenceContext().getSession();
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
			final CollectionLoadContext collectionLoadContext = collectionLoadContexts.remove( resultSet );
			collectionLoadContext.cleanup();
		}
		if ( entityLoadContexts != null ) {
			final EntityLoadContext entityLoadContext = entityLoadContexts.remove( resultSet );
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
			for ( CollectionLoadContext collectionLoadContext : collectionLoadContexts.values() ) {
				LOG.failSafeCollectionsCleanup( collectionLoadContext );
				collectionLoadContext.cleanup();
			}
			collectionLoadContexts.clear();
		}
		if ( entityLoadContexts != null ) {
			for ( EntityLoadContext entityLoadContext : entityLoadContexts.values() ) {
				LOG.failSafeEntitiesCleanup( entityLoadContext );
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
			collectionLoadContexts = new IdentityHashMap<ResultSet, CollectionLoadContext>( 8 );
		}
		else {
			context = collectionLoadContexts.get(resultSet);
		}
		if ( context == null ) {
			LOG.tracev( "Constructing collection load context for result set [{0}]", resultSet );
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
		final LoadingCollectionEntry lce = locateLoadingCollectionEntry( new CollectionKey( persister, ownerKey ) );
		if ( lce != null ) {
			if ( LOG.isTraceEnabled() ) {
				LOG.tracef(
						"Returning loading collection: %s",
						MessageHelper.collectionInfoString( persister, ownerKey, getSession().getFactory() )
				);
			}
			return lce.getCollection();
		}
		return null;
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
			xrefLoadingCollectionEntries = new HashMap<CollectionKey,LoadingCollectionEntry>();
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
		xrefLoadingCollectionEntries.remove( key );
	}

	@SuppressWarnings( {"UnusedDeclaration"})
	Map getLoadingCollectionXRefs() {
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
		LOG.tracev( "Attempting to locate loading collection entry [{0}] in any result-set context", key );
		final LoadingCollectionEntry rtn = xrefLoadingCollectionEntries.get( key );
		if ( rtn == null ) {
			LOG.tracev( "Collection [{0}] not located in load context", key );
		}
		else {
			LOG.tracev( "Collection [{0}] located in load context", key );
		}
		return rtn;
	}

	/*package*/void cleanupCollectionXRefs(Set<CollectionKey> entryKeys) {
		for ( CollectionKey entryKey : entryKeys ) {
			xrefLoadingCollectionEntries.remove( entryKey );
		}
	}


	// Entity load contexts ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// 	* currently, not yet used...

	/**
	 * Currently unused
	 *
	 * @param resultSet The result set
	 *
	 * @return The entity load context
	 */
	@SuppressWarnings( {"UnusedDeclaration"})
	public EntityLoadContext getEntityLoadContext(ResultSet resultSet) {
		EntityLoadContext context = null;
		if ( entityLoadContexts == null ) {
			entityLoadContexts = new IdentityHashMap<ResultSet, EntityLoadContext>( 8 );
		}
		else {
			context = entityLoadContexts.get( resultSet );
		}
		if ( context == null ) {
			context = new EntityLoadContext( this, resultSet );
			entityLoadContexts.put( resultSet, context );
		}
		return context;
	}
}
