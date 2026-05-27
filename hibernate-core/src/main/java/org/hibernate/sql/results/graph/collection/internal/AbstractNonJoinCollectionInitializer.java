/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.collection.internal;

import java.util.IdentityHashMap;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.FetchOptions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.InitializerParent;

import org.checkerframework.checker.nullness.qual.Nullable;

import static java.util.Collections.newSetFromMap;

/**
 * Base support for CollectionInitializer implementations that don't join data
 *
 * @author Steve Ebersole
 */
public abstract class AbstractNonJoinCollectionInitializer<Data extends AbstractCollectionInitializer.CollectionInitializerData>
		extends AbstractCollectionInitializer<Data> {

	public AbstractNonJoinCollectionInitializer(
			NavigablePath collectionPath,
			PluralAttributeMapping collectionAttributeMapping,
			InitializerParent<?> parent,
			@Nullable DomainResult<?> collectionKeyResult,
			boolean isResultInitializer,
			FetchOptions fetchOptions,
			AssemblerCreationState creationState) {
		super(
				collectionPath,
				collectionAttributeMapping,
				parent,
				collectionKeyResult,
				isResultInitializer,
				fetchOptions,
				creationState
		);
	}

	protected void resolveInstance(Data data, boolean isEager) {
		if ( data.getState() == State.KEY_RESOLVED ) {
			resolveCollectionKey( data, false );
			if ( data.getState() == State.KEY_RESOLVED ) {
				assert owningEntityInitializer != null;
				final var rowProcessingState = data.getRowProcessingState();
				// We can avoid processing further if the parent is already initialized,
				// as the value produced by this initializer will never be used anyway.
				final var owningEntityData = owningEntityInitializer.getData( rowProcessingState );
				if ( owningEntityData.getState() == State.INITIALIZED ) {
					// It doesn't matter if it's eager or lazy, the collection object can not be referred to,
					// so it doesn't make sense to create or initialize it
					if ( isEager && hasLocalFetchOptions() ) {
						final Object targetInstance = owningEntityInitializer.getTargetInstance( owningEntityData );
						if ( targetInstance == null ) {
							data.setState( State.MISSING );
						}
						else {
							resolveInstance( getInitializedPart().getValue( targetInstance ), data, true );
						}
					}
					else {
						data.setState( State.MISSING );
					}
				}
				else {
					// This initializer is done initializing, since this is only invoked for delayed or select initializers
					data.setState( State.INITIALIZED );

					final var session = rowProcessingState.getSession();
					final var persistenceContext = session.getPersistenceContextInternal();
					final var collectionKey = data.collectionKey;
					assert collectionKey != null;

					final var loadingEntry =
							persistenceContext.getLoadContexts()
									.findLoadingCollectionEntry( collectionKey );
					if ( loadingEntry != null ) {
						final var collectionInstance = loadingEntry.getCollectionInstance();
						data.setCollectionInstance( collectionInstance );
						if ( collectionInstance.getOwner() == null ) {
							assert owningEntityInitializer.getTargetInstance( owningEntityData ) != null;
							collectionInstance.setOwner(
									owningEntityInitializer.getTargetInstance( owningEntityData ) );
						}
					}
					else {
						final var existing = persistenceContext.getCollection( collectionKey );
						if ( existing != null ) {
							data.setCollectionInstance( existing );
							if ( existing.getOwner() == null ) {
								assert owningEntityInitializer.getTargetInstance( owningEntityData ) != null;
								existing.setOwner( owningEntityInitializer.getTargetInstance( owningEntityData ) );
							}
						}
						else {
							final var persister = collectionAttributeMapping.getCollectionDescriptor();
							final Object key = collectionKey.getKey();
							final var collection =
									persister.getCollectionSemantics()
											.instantiateWrapper( key, persister, session );
							data.setCollectionInstance( collection );
							final Object targetInstance = owningEntityInitializer.getTargetInstance( owningEntityData );
							assert targetInstance != null;
							collection.setOwner( targetInstance );
							withFetchOptions(
									session,
									() -> {
										persistenceContext.addUninitializedCollection(
												persister,
												collection,
												key,
												isReadOnly( collectionKey, rowProcessingState, session )
										);
										return null;
									}
							);
							if ( isEager ) {
								addNonLazyCollection( data, collection );
							}
							if ( persister.isArray() ) {
								persistenceContext.addCollectionHolder( collection );
							}
						}
					}
				}
			}
		}
	}

	protected void resolveInstance(Object instance, Data data, boolean isEager) {
		if ( instance == null ) {
			setMissing( data );
		}
		else {
			final var rowProcessingState = data.getRowProcessingState();
			final var persistentCollection = getCollection( data, instance );
			// resolving the collection key seems unnecessary
//			collectionKeyValue = persistentCollection.getKey();
//			resolveCollectionKey( rowProcessingState, false );
			data.setCollectionInstance( persistentCollection );
			// This initializer is done initializing, since this is only invoked for delayed or select initializers
			data.setState( State.INITIALIZED );
			if ( isEager && !persistentCollection.wasInitialized() ) {
				addBatchLoadableCollection( persistentCollection, rowProcessingState.getSession() );
				addNonLazyCollection( data, persistentCollection );
			}
			if ( collectionKeyResultAssembler != null && rowProcessingState.needsResolveState() ) {
				// Resolve the state of the identifier if result caching is enabled, and this is not a query cache hit
				collectionKeyResultAssembler.resolveState( rowProcessingState );
			}
		}
	}

	private void addBatchLoadableCollection(
			PersistentCollection<?> collection,
			SharedSessionContractImplementor session) {
		withFetchOptions(
				session,
				() -> {
					final var persistenceContext = session.getPersistenceContextInternal();
					final var collectionEntry = persistenceContext.getCollectionEntry( collection );
					if ( collectionEntry != null ) {
						final var persister = collectionEntry.getLoadedPersister();
						if ( persister != null
								&& session.getLoadQueryInfluencers().effectivelyBatchLoadable( persister ) ) {
							persistenceContext.getBatchFetchQueue()
									.addBatchLoadableCollection( collection, collectionEntry );
						}
					}
					return null;
				}
		);
	}

	protected void addNonLazyCollection(Data data, PersistentCollection<?> collection) {
		if ( hasLocalFetchOptions() ) {
			var nonLazyCollections = data.nonLazyCollections;
			if ( nonLazyCollections == null ) {
				nonLazyCollections = data.nonLazyCollections = newSetFromMap( new IdentityHashMap<>() );
			}
			nonLazyCollections.add( collection );
		}
		else {
			data.getRowProcessingState().getSession().getPersistenceContextInternal()
					.addNonLazyCollection( collection );
		}
	}

	private boolean hasLocalFetchOptions() {
		return fetchOptions.hasOptions();
	}

	@Override
	public void endLoading(Data data) {
		super.endLoading( data );
		final var nonLazyCollections = data.nonLazyCollections;
		if ( nonLazyCollections != null ) {
			withFetchOptions(
					data.getRowProcessingState().getSession(),
					() -> {
						for ( var collection : nonLazyCollections ) {
							collection.forceInitialization();
						}
						return null;
					}
			);
			nonLazyCollections.clear();
			data.nonLazyCollections = null;
		}
	}

}
