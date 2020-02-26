/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection.internal;

import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;
import org.hibernate.sql.results.graph.collection.LoadingCollectionEntry;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

/**
 * @author Steve Ebersole
 */
public class DelayedCollectionInitializer implements CollectionInitializer {

	private final NavigablePath fetchedPath;
	private final PluralAttributeMapping fetchedMapping;
	private final FetchParentAccess parentAccess;

	private CollectionKey collectionKey;
	private PersistentCollection instance;

	public DelayedCollectionInitializer(
			NavigablePath fetchedPath,
			PluralAttributeMapping fetchedMapping,
			FetchParentAccess parentAccess) {
		this.fetchedPath = fetchedPath;
		this.fetchedMapping = fetchedMapping;
		this.parentAccess = parentAccess;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return fetchedPath;
	}

	@Override
	public void resolveKey(RowProcessingState rowProcessingState) {
		if ( collectionKey != null ) {
			// already resolved
			return;
		}

		final Object parentKey = parentAccess.getParentKey();
		if ( parentKey != null ) {
			collectionKey = new CollectionKey(
					fetchedMapping.getCollectionDescriptor(),
					parentKey
			);

			parentAccess.registerResolutionListener( owner -> instance.setOwner( owner ) );
		}
	}

	@Override
	public void resolveInstance(RowProcessingState rowProcessingState) {
		final SharedSessionContractImplementor session = rowProcessingState.getSession();
		final PersistenceContext persistenceContext = session.getPersistenceContext();

		if ( collectionKey != null ) {
			EntityInitializer entityInitializer = getEntityInitializer( rowProcessingState );


			final Object entityUsingInterceptor = persistenceContext.getEntity( entityInitializer.getEntityKey() );
			if ( entityUsingInterceptor != null ) {
				return;
			}

			final Object key = collectionKey.getKey();

			final LoadingCollectionEntry loadingEntry = persistenceContext.getLoadContexts()
					.findLoadingCollectionEntry( collectionKey );
			final PersistentCollection registeredInstance = persistenceContext.getCollection( collectionKey );

			if ( loadingEntry != null ) {
				instance = loadingEntry.getCollectionInstance();
				return;
			}

			if ( registeredInstance != null ) {
				instance = registeredInstance;
				return;
			}

			instance = makePersistentCollection( fetchedMapping, key, rowProcessingState );

			persistenceContext.addUninitializedCollection(
					getInitializingCollectionDescriptor(),
					instance,
					key
			);
		}
		else {
			instance = makePersistentCollection( fetchedMapping, collectionKey, rowProcessingState );
			instance.initializeEmptyCollection( getInitializingCollectionDescriptor() );
			persistenceContext.addNonLazyCollection( instance );
		}
	}

	private static PersistentCollection makePersistentCollection(
			PluralAttributeMapping fetchedMapping,
			Object collectionKey,
			RowProcessingState rowProcessingState) {
		final CollectionPersister collectionDescriptor = fetchedMapping.getCollectionDescriptor();
		final CollectionSemantics collectionSemantics = collectionDescriptor.getCollectionSemantics();

		return collectionSemantics.instantiateWrapper(
				collectionKey,
				collectionDescriptor,
				rowProcessingState.getSession()
		);
	}

	private EntityInitializer getEntityInitializer(RowProcessingState rowProcessingState) {
		Initializer initializer = rowProcessingState.resolveInitializer( getNavigablePath().getParent() );
		while ( !( initializer instanceof EntityInitializer ) ) {
			initializer = rowProcessingState.resolveInitializer( initializer.getNavigablePath().getParent() );
		}
		return (EntityInitializer) initializer;

	}


	@Override
	public void initializeInstance(RowProcessingState rowProcessingState) {
	}

	@Override
	public String toString() {
		return "DelayedCollectionInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {
		collectionKey = null;
		instance = null;
	}

	@Override
	public PluralAttributeMapping getInitializedPart() {
		return fetchedMapping;
	}

	@Override
	public PersistentCollection getCollectionInstance() {
		return instance;
	}

	@Override
	public CollectionKey resolveCollectionKey(RowProcessingState rowProcessingState) {
		resolveKey( rowProcessingState );
		return collectionKey;
	}
}
