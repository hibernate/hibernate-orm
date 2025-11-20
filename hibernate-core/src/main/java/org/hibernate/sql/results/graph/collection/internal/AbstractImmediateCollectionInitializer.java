/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.collection.internal;

import java.util.List;
import java.util.function.BiConsumer;

import org.hibernate.LockMode;
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
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerData;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.collection.LoadingCollectionEntry;
import org.hibernate.sql.results.internal.LoadingCollectionEntryImpl;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Base support for CollectionInitializer implementations that represent
 * an immediate initialization of some sort (join, select, batch, sub-select)
 * for a persistent collection.
 *
 * @author Steve Ebersole
 * @implNote Mainly an intention contract wrt the immediacy of the fetch.
 */
public abstract class AbstractImmediateCollectionInitializer<Data extends AbstractImmediateCollectionInitializer.ImmediateCollectionInitializerData>
		extends AbstractCollectionInitializer<Data> implements BiConsumer<Data, List<Object>> {

	private final boolean isReadOnly;
	/**
	 * refers to the rows entry in the collection.  null indicates that the collection is empty
	 */
	protected final @Nullable DomainResultAssembler<?> collectionValueKeyResultAssembler;

	public static class ImmediateCollectionInitializerData extends CollectionInitializerData {

		protected final boolean shallowCached;

		/**
		 * The value of the collection side of the collection key (FK).  Identifies
		 * inclusion in the collection.  Can be null to indicate that the current row
		 * does not contain any collection values
		 */
		protected Object collectionValueKey;
		protected LoadingCollectionEntryImpl responsibility;

		public ImmediateCollectionInitializerData(AbstractImmediateCollectionInitializer<?> initializer, RowProcessingState rowProcessingState) {
			super( rowProcessingState );
			shallowCached = rowProcessingState.isQueryCacheHit()
					&& initializer.getInitializingCollectionDescriptor().useShallowQueryCacheLayout();
		}
	}

	public AbstractImmediateCollectionInitializer(
			NavigablePath collectionPath,
			PluralAttributeMapping collectionAttributeMapping,
			InitializerParent<?> parent,
			LockMode lockMode,
			DomainResult<?> collectionKeyResult,
			DomainResult<?> collectionValueKeyResult,
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
		this.collectionValueKeyResultAssembler = collectionKeyResult == collectionValueKeyResult
				? null
				: collectionValueKeyResult.createResultAssembler( this, creationState );
		this.isReadOnly = collectionAttributeMapping.isReadOnly();
	}

	@Override
	protected ImmediateCollectionInitializerData createInitializerData(RowProcessingState rowProcessingState) {
		return new ImmediateCollectionInitializerData( this, rowProcessingState );
	}

	@Override
	protected void forEachSubInitializer(BiConsumer<Initializer<?>, RowProcessingState> consumer, InitializerData data) {
		super.forEachSubInitializer( consumer, data );
		if ( collectionValueKeyResultAssembler != null ) {
			final Initializer<?> initializer = collectionValueKeyResultAssembler.getInitializer();
			if ( initializer != null ) {
				consumer.accept( initializer, data.getRowProcessingState() );
			}
		}
	}

	@Override
	public void resolveKey(Data data) {
		if ( data.getState() != State.UNINITIALIZED ) {
			// already resolved
			return;
		}
		super.resolveKey( data );
		data.collectionValueKey = null;
		// Can't resolve any sub-initializers if the collection is shallow cached
		if ( data.getState() != State.MISSING && !data.shallowCached ) {
			if ( collectionValueKeyResultAssembler == null ) {
				// A null collectionValueKeyResultAssembler means that we should use the parent key.
				// Since this method can only be called when the parent exists, we know the collection is not missing
				resolveKeySubInitializers( data );
			}
			else {
				resolveCollectionContentKey( data );
			}
		}
	}

	@Override
	public void resolveState(Data data) {
		super.resolveState( data );
		if ( collectionValueKeyResultAssembler != null ) {
			collectionValueKeyResultAssembler.resolveState( data.getRowProcessingState() );
		}
		final DomainResultAssembler<?> indexAssembler = getIndexAssembler();
		if ( indexAssembler != null ) {
			indexAssembler.resolveState( data.getRowProcessingState() );
		}
		final DomainResultAssembler<?> elementAssembler = getElementAssembler();
		if ( elementAssembler != null ) {
			elementAssembler.resolveState( data.getRowProcessingState() );
		}
	}

	@Override
	public void resolveFromPreviousRow(Data data) {
		super.resolveFromPreviousRow( data );
		if ( data.getState() == State.RESOLVED ) {
			resolveKeySubInitializers( data );
		}
	}

	/**
	 * Returns whether the collection value key is missing.
	 */
	private boolean resolveCollectionContentKey(Data data) {
		assert collectionValueKeyResultAssembler != null;
		final RowProcessingState rowProcessingState = data.getRowProcessingState();
		//noinspection unchecked
		final Initializer<InitializerData> initializer = (Initializer<InitializerData>) collectionValueKeyResultAssembler.getInitializer();
		if ( initializer != null ) {
			InitializerData subData = initializer.getData( rowProcessingState );
			initializer.resolveKey( subData );
			if ( subData.getState() == State.MISSING ) {
				return true;
			}
		}
		else {
			data.collectionValueKey = collectionValueKeyResultAssembler.assemble( rowProcessingState );
			if ( data.collectionValueKey == null ) {
				return true;
			}
		}
		// If we get here, a collectionValueKey exists or is likely to exist,
		// so we need to call resolveKey on the index and element initializers of the collection
		// to initialize this resolved collection instance later
		resolveKeySubInitializers( data );
		return false;
	}

	private void resolveKeySubInitializers(Data data) {
		final RowProcessingState rowProcessingState = data.getRowProcessingState();
		final DomainResultAssembler<?> indexAssembler = getIndexAssembler();
		final Initializer<?> indexInitializer;
		if ( indexAssembler != null && ( indexInitializer = indexAssembler.getInitializer() ) != null ) {
			indexInitializer.resolveKey( rowProcessingState );
		}
		final Initializer<?> elementInitializer = getElementAssembler().getInitializer();
		if ( elementInitializer != null ) {
			elementInitializer.resolveKey( rowProcessingState );
		}
	}

	@Override
	public void resolveInstance(Data data) {
		if ( data.getState() != State.KEY_RESOLVED ) {
			// already resolved
			return;
		}

		// Being a result initializer means that this collection initializer is for lazy loading,
		// which has a very high chance that a collection resolved of the previous row is the same for the current row,
		// so pass that flag as indicator whether to check previous row state.
		// Note that we don't need to check previous rows in other cases,
		// because the previous row checks are done by the owner of the collection initializer already.
		resolveCollectionKey( data, isResultInitializer );
		if ( data.getState() != State.KEY_RESOLVED ) {
			return;
		}

		data.setState( State.RESOLVED );
		data.responsibility = null;

		// determine the PersistentCollection instance to use and whether
		// we (this initializer) is responsible for loading its state

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// First, look for a LoadingCollectionEntry
		final RowProcessingState rowProcessingState = data.getRowProcessingState();
		final SharedSessionContractImplementor session = rowProcessingState.getSession();
		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
		final CollectionKey collectionKey = data.collectionKey;
		assert collectionKey != null;
		final LoadingCollectionEntry existingLoadingEntry = persistenceContext.getLoadContexts()
				.findLoadingCollectionEntry( collectionKey );
		final PersistentCollection<?> existing;
		final PersistentCollection<?> existingUnowned;
		if ( existingLoadingEntry != null ) {
			data.setCollectionInstance( existingLoadingEntry.getCollectionInstance() );

			if ( existingLoadingEntry.getInitializer() == this ) {
				assert !data.shallowCached;
				// we are responsible for loading the collection values
				data.responsibility = (LoadingCollectionEntryImpl) existingLoadingEntry;
			}
			else {
				// the entity is already being loaded elsewhere
				data.setState( State.INITIALIZED );
			}
		}
		else if ( ( existing = persistenceContext.getCollection( collectionKey ) ) != null ) {
			data.setCollectionInstance( existing );

			// we found the corresponding collection instance on the Session.  If
			// it is already initialized we have nothing to do

			if ( existing.wasInitialized() ) {
				data.setState( State.INITIALIZED );
			}
			else if ( !data.shallowCached ) {
				takeResponsibility( data );
			}
		}
		else if ( ( existingUnowned = persistenceContext.useUnownedCollection( collectionKey ) ) != null ) {
			data.setCollectionInstance( existingUnowned );

			// we found the corresponding collection instance as unowned on the Session.  If
			// it is already initialized we have nothing to do

			if ( existingUnowned.wasInitialized() ) {
				data.setState( State.INITIALIZED );
			}
			else if ( !data.shallowCached ) {
				takeResponsibility( data );
			}
		}
		else {
			final CollectionPersister collectionDescriptor = getCollectionAttributeMapping().getCollectionDescriptor();
			final CollectionSemantics<?, ?> collectionSemantics = collectionDescriptor.getCollectionSemantics();
			final PersistentCollection<?> persistentCollection = collectionSemantics.instantiateWrapper(
					collectionKey.getKey(),
					getInitializingCollectionDescriptor(),
					session
			);
			data.setCollectionInstance( persistentCollection );

			if ( owningEntityInitializer != null ) {
				assert owningEntityInitializer.getTargetInstance( rowProcessingState ) != null;
				data.getCollectionInstance().setOwner( owningEntityInitializer.getTargetInstance( rowProcessingState ) );
			}

			persistenceContext.addUninitializedCollection(
					collectionDescriptor,
					persistentCollection,
					collectionKey.getKey()
			);

			if ( !data.shallowCached ) {
				takeResponsibility( data );
			}
		}

		if ( data.shallowCached ) {
			assert data.responsibility == null;
			initializeShallowCached( data );
		}
	}

	protected void initializeShallowCached(Data data) {
		assert data.shallowCached;
		final PersistenceContext persistenceContext = data.getRowProcessingState().getSession()
				.getPersistenceContextInternal();
		// If this is a query cache hit with the shallow query cache layout,
		// we have to lazy load the collection instead
		final PersistentCollection<?> collectionInstance = data.getCollectionInstance();
		assert collectionInstance != null;
		collectionInstance.forceInitialization();
		if ( collectionAttributeMapping.getCollectionDescriptor()
				.getCollectionSemantics()
				.getCollectionClassification() == CollectionClassification.ARRAY ) {
			persistenceContext.addCollectionHolder( collectionInstance );
		}
		data.setState( State.INITIALIZED );
		initializeSubInstancesFromParent( data );
	}

	@Override
	protected void setMissing(Data data) {
		super.setMissing( data );
		data.collectionValueKey = null;
		data.responsibility = null;
	}

	@Override
	public void resolveInstance(Object instance, Data data) {
		assert data.getState() == State.UNINITIALIZED || instance == data.getCollectionInstance();
		if ( instance == null ) {
			if ( isReadOnly ) {
				// When the mapping is read-only, we can't trust the state of the persistence context
				resolveKey( data );
			}
			else {
				setMissing( data );
			}
			return;
		}
		final RowProcessingState rowProcessingState = data.getRowProcessingState();
		final PersistentCollection<?> persistentCollection;
		// Check if the given instance is different from the previous row state to avoid creating CollectionKey
		if ( data.getCollectionInstance() != instance ) {
			final PersistenceContext persistenceContext = rowProcessingState.getSession().getPersistenceContextInternal();
			if ( collectionAttributeMapping.getCollectionDescriptor()
					.getCollectionSemantics()
					.getCollectionClassification() == CollectionClassification.ARRAY ) {
				persistentCollection = persistenceContext.getCollectionHolder( instance );
			}
			else {
				persistentCollection = (PersistentCollection<?>) instance;
			}
			data.collectionKeyValue = persistentCollection.getKey();
			resolveCollectionKey( data, false );
			data.setCollectionInstance( persistentCollection );
			data.responsibility = null;
		}
		else {
			persistentCollection = (PersistentCollection<?>) instance;
		}
		data.collectionValueKey = null;
		if ( persistentCollection.wasInitialized() ) {
			data.setState( State.INITIALIZED );
			if ( data.shallowCached ) {
				initializeShallowCached( data );
			}
			else {
				resolveInstanceSubInitializers( data );
			}
			if ( rowProcessingState.needsResolveState() ) {
				// Resolve the state of the identifier if result caching is enabled and this is not a query cache hit
				if ( collectionKeyResultAssembler != null ) {
					collectionKeyResultAssembler.resolveState( rowProcessingState );
				}
				if ( !getInitializingCollectionDescriptor().useShallowQueryCacheLayout() ) {
					if ( collectionValueKeyResultAssembler != null ) {
						collectionValueKeyResultAssembler.resolveState( rowProcessingState );
					}
					resolveCollectionContentState( rowProcessingState );
				}
			}
		}
		else {
			if ( data.shallowCached ) {
				data.setState( State.INITIALIZED );
				initializeShallowCached( data );
			}
			else {
				data.setState( State.RESOLVED );
				final boolean rowContainsCollectionContent;
				if ( collectionValueKeyResultAssembler != null ) {
					rowContainsCollectionContent = resolveCollectionContentKey( data );
				}
				else {
					rowContainsCollectionContent = true;
				}
				if ( data.responsibility == null ) {
					final LoadingCollectionEntry existingLoadingEntry = rowProcessingState.getSession()
							.getPersistenceContextInternal()
							.getLoadContexts()
							.findLoadingCollectionEntry( data.collectionKey );
					if ( existingLoadingEntry != null ) {
						if ( existingLoadingEntry.getInitializer() == this ) {
							// we are responsible for loading the collection values
							data.responsibility = (LoadingCollectionEntryImpl) existingLoadingEntry;
						}
						else {
							// the collection is already being loaded elsewhere
							data.setState( State.INITIALIZED );
							if ( rowContainsCollectionContent  && rowProcessingState.needsResolveState()
									&& !getInitializingCollectionDescriptor().useShallowQueryCacheLayout() ) {
								// Resolve the state of the content if result caching is enabled and this is not a query cache hit
								// and the collection doesn't use a shallow query cache layout
								resolveCollectionContentState( rowProcessingState );
							}
						}
					}
					else {
						takeResponsibility( data );
					}
				}
			}
		}
	}

	protected abstract void resolveInstanceSubInitializers(Data data);

	private void resolveCollectionContentState(RowProcessingState rowProcessingState) {
		final DomainResultAssembler<?> indexAssembler = getIndexAssembler();
		if ( indexAssembler != null ) {
			indexAssembler.resolveState( rowProcessingState );
		}
		getElementAssembler().resolveState( rowProcessingState );
	}

	protected void takeResponsibility(Data data) {
		data.responsibility = new LoadingCollectionEntryImpl(
				getCollectionAttributeMapping().getCollectionDescriptor(),
				this,
				data.collectionKey.getKey(),
				data.getCollectionInstance()
		);
		data.getRowProcessingState().getJdbcValuesSourceProcessingState().registerLoadingCollection(
				data.collectionKey,
				data.responsibility
		);
	}

	@Override
	public void initializeInstance(Data data) {
		if ( data.getState() != State.RESOLVED || data.responsibility == null ) {
			return;
		}
		data.setState( State.INITIALIZED );

		if ( data.collectionValueKey == null && collectionValueKeyResultAssembler != null ) {
			final Initializer<?> initializer = collectionValueKeyResultAssembler.getInitializer();
			if ( initializer != null ) {
				data.collectionValueKey = collectionValueKeyResultAssembler.assemble( data.getRowProcessingState() );
			}
		}

		// the RHS key value of the association - determines if the row contains an element of the initializing collection
		if ( collectionValueKeyResultAssembler == null || data.collectionValueKey != null ) {
			// the row contains an element in the collection...
			data.responsibility.load( data, this );
		}
	}

	@Override
	public void initializeInstanceFromParent(Object parentInstance, Data data) {
		data.setCollectionInstance( (PersistentCollection<?>) getInitializedPart().getValue( parentInstance ) );
		data.setState( State.INITIALIZED );
		initializeSubInstancesFromParent( data );
	}

	@Override
	public boolean hasLazySubInitializers() {
		final DomainResultAssembler<?> indexAssembler = getIndexAssembler();
		if ( indexAssembler != null ) {
			final Initializer<?> initializer = indexAssembler.getInitializer();
			if ( initializer != null && ( !initializer.isEager() || initializer.hasLazySubInitializers() ) ) {
				return true;
			}
		}
		final Initializer<?> initializer = getElementAssembler().getInitializer();
		return initializer != null && ( !initializer.isEager() || initializer.hasLazySubInitializers() );
	}

	@Override
	public void accept(Data data, List<Object> objects) {
		readCollectionRow( data, objects );
	}

	protected abstract void readCollectionRow(Data data, List<Object> loadingState);

	protected abstract void initializeSubInstancesFromParent(Data data);

	public abstract @Nullable DomainResultAssembler<?> getIndexAssembler();

	public abstract DomainResultAssembler<?> getElementAssembler();
}
