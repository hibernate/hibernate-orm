/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.collection.internal;

import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.InitializerData;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.collection.LoadingCollectionEntry;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import org.checkerframework.checker.nullness.qual.Nullable;

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
			AssemblerCreationState creationState) {
		super(
				collectionPath,
				collectionAttributeMapping,
				parent,
				collectionKeyResult,
				isResultInitializer,
				creationState
		);
	}

	protected void resolveInstance(Data data, boolean isEager) {
		if ( data.getState() != State.KEY_RESOLVED ) {
			// already resolved
			return;
		}

		resolveCollectionKey( data, false );
		if ( data.getState() == State.KEY_RESOLVED ) {
			assert owningEntityInitializer != null;
			final RowProcessingState rowProcessingState = data.getRowProcessingState();
			// We can avoid processing further if the parent is already initialized,
			// as the value produced by this initializer will never be used anyway.
			final InitializerData owningEntityData = owningEntityInitializer.getData( rowProcessingState );
			if ( owningEntityData.getState() == State.INITIALIZED ) {
				// It doesn't matter if it's eager or lazy, the collection object can not be referred to,
				// so it doesn't make sense to create or initialize it
				data.setState( State.MISSING );
				return;
			}
			// This initializer is done initializing, since this is only invoked for delayed or select initializers
			data.setState( State.INITIALIZED );

			final SharedSessionContractImplementor session = rowProcessingState.getSession();
			final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
			final CollectionKey collectionKey = data.collectionKey;
			assert collectionKey != null;

			final LoadingCollectionEntry loadingEntry = persistenceContext.getLoadContexts()
					.findLoadingCollectionEntry( collectionKey );

			if ( loadingEntry != null ) {
				final PersistentCollection<?> collectionInstance = loadingEntry.getCollectionInstance();
				data.setCollectionInstance( collectionInstance );
				if ( collectionInstance.getOwner() == null ) {
					assert owningEntityInitializer.getTargetInstance( owningEntityData ) != null;
					collectionInstance.setOwner( owningEntityInitializer.getTargetInstance( owningEntityData ) );
				}
				return;
			}

			final PersistentCollection<?> existing = persistenceContext.getCollection( collectionKey );

			if ( existing != null ) {
				data.setCollectionInstance( existing );
				if ( existing.getOwner() == null ) {
					assert owningEntityInitializer.getTargetInstance( owningEntityData ) != null;
					existing.setOwner( owningEntityInitializer.getTargetInstance( owningEntityData ) );
				}
				return;
			}

			final CollectionPersister collectionDescriptor = collectionAttributeMapping.getCollectionDescriptor();
			final CollectionSemantics<?, ?> collectionSemantics = collectionDescriptor.getCollectionSemantics();
			final Object key = collectionKey.getKey();

			final PersistentCollection<?> persistentCollection = collectionSemantics.instantiateWrapper(
					key,
					collectionDescriptor,
					session
			);
			data.setCollectionInstance( persistentCollection );

			assert owningEntityInitializer.getTargetInstance( owningEntityData ) != null;
			persistentCollection.setOwner( owningEntityInitializer.getTargetInstance( owningEntityData ) );

			persistenceContext.addUninitializedCollection(
					collectionDescriptor,
					persistentCollection,
					key
			);

			if ( isEager ) {
				persistenceContext.addNonLazyCollection( persistentCollection );
			}

			if ( collectionSemantics.getCollectionClassification() == CollectionClassification.ARRAY ) {
				persistenceContext.addCollectionHolder( persistentCollection );
			}
		}
	}

	protected void resolveInstance(Object instance, Data data, boolean isEager) {
		if ( instance == null ) {
			setMissing( data );
		}
		else {
			final RowProcessingState rowProcessingState = data.getRowProcessingState();
			final PersistenceContext persistenceContext = rowProcessingState.getSession().getPersistenceContextInternal();
			final PersistentCollection<?> persistentCollection;
			if ( collectionAttributeMapping.getCollectionDescriptor()
					.getCollectionSemantics()
					.getCollectionClassification() == CollectionClassification.ARRAY ) {
				persistentCollection = persistenceContext.getCollectionHolder( instance );
			}
			else {
				persistentCollection = (PersistentCollection<?>) instance;
			}
			// resolving the collection key seems unnecessary
//			collectionKeyValue = persistentCollection.getKey();
//			resolveCollectionKey( rowProcessingState, false );
			data.setCollectionInstance( persistentCollection );
			// This initializer is done initializing, since this is only invoked for delayed or select initializers
			data.setState( State.INITIALIZED );
			if ( isEager && !persistentCollection.wasInitialized() ) {
				persistenceContext.addNonLazyCollection( persistentCollection );
			}
			if ( collectionKeyResultAssembler != null && rowProcessingState.needsResolveState() ) {
				// Resolve the state of the identifier if result caching is enabled and this is not a query cache hit
				collectionKeyResultAssembler.resolveState( rowProcessingState );
			}
		}
	}

}
