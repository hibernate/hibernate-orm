/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection.internal;

import java.util.function.BiConsumer;

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
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;
import org.hibernate.sql.results.graph.collection.LoadingCollectionEntry;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.graph.internal.AbstractInitializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Base support for CollectionInitializer implementations
 *
 * @author Steve Ebersole
 */
public abstract class AbstractCollectionInitializer extends AbstractInitializer implements CollectionInitializer {
	protected final NavigablePath collectionPath;
	protected final PluralAttributeMapping collectionAttributeMapping;
	protected final boolean isResultInitializer;
	protected final @Nullable InitializerParent parent;
	protected final @Nullable EntityInitializer owningEntityInitializer;

	/**
	 * refers to the collection's container value - which collection-key?
	 */
	protected final @Nullable DomainResultAssembler<?> collectionKeyResultAssembler;

	protected @Nullable PersistentCollection<?> collectionInstance;
	protected @Nullable Object collectionKeyValue;
	protected @Nullable CollectionKey collectionKey;

	/**
	 * @deprecated Use {@link #AbstractCollectionInitializer(NavigablePath, PluralAttributeMapping, InitializerParent, DomainResult, boolean, AssemblerCreationState)} instead.
	 */
	@Deprecated(forRemoval = true)
	protected AbstractCollectionInitializer(
			NavigablePath collectionPath,
			PluralAttributeMapping collectionAttributeMapping,
			FetchParentAccess parent,
			@Nullable DomainResult<?> collectionKeyResult,
			boolean isResultInitializer,
			AssemblerCreationState creationState) {
		this(
				collectionPath,
				collectionAttributeMapping,
				(InitializerParent) parent,
				collectionKeyResult,
				isResultInitializer,
				creationState
		);
	}

	protected AbstractCollectionInitializer(
			NavigablePath collectionPath,
			PluralAttributeMapping collectionAttributeMapping,
			InitializerParent parent,
			@Nullable DomainResult<?> collectionKeyResult,
			boolean isResultInitializer,
			AssemblerCreationState creationState) {
		this.collectionPath = collectionPath;
		this.collectionAttributeMapping = collectionAttributeMapping;
		this.isResultInitializer = isResultInitializer;
		this.parent = parent;
		this.owningEntityInitializer = Initializer.findOwningEntityInitializer( parent );
		this.collectionKeyResultAssembler = collectionKeyResult == null
				? null
				: collectionKeyResult.createResultAssembler( (InitializerParent) this, creationState );
	}

	@Override
	public void resolveKey() {
		if ( state != State.UNINITIALIZED ) {
			// already resolved
			return;
		}
		state = State.KEY_RESOLVED;
		collectionKeyValue = null;
		if ( collectionKeyResultAssembler != null ) {
			final Initializer initializer = collectionKeyResultAssembler.getInitializer();
			if ( initializer != null ) {
				initializer.resolveKey();
				if ( initializer.getState() == State.MISSING ) {
					setMissing();
				}
				return;
			}
			collectionKeyValue = collectionKeyResultAssembler.assemble( rowProcessingState );
			if ( collectionKeyValue == null ) {
				setMissing();
			}
		}
	}

	protected void setMissing() {
		state = State.MISSING;
		collectionKey = null;
		collectionKeyValue = null;
		collectionInstance = null;
	}

	protected void resolveCollectionKey(RowProcessingState rowProcessingState, boolean checkPreviousRow) {
		final CollectionKey oldKey = collectionKey;
		final PersistentCollection<?> oldCollectionInstance = collectionInstance;
		collectionKey = null;
		collectionInstance = null;

		if ( collectionKeyValue == null ) {
			if ( collectionKeyResultAssembler == null ) {
				assert owningEntityInitializer != null;
				collectionKeyValue = owningEntityInitializer.getEntityIdentifier();
			}
			else {
				collectionKeyValue = collectionKeyResultAssembler.assemble( rowProcessingState );
			}
			if ( collectionKeyValue == null ) {
				state = State.MISSING;
				collectionKey = null;
				collectionInstance = null;
				return;
			}
		}
		final CollectionPersister persister = collectionAttributeMapping.getCollectionDescriptor();
		// Try to reuse the previous collection key and collection if possible
		if ( checkPreviousRow && oldKey != null && persister.getKeyType().isEqual(
				oldKey.getKey(),
				collectionKeyValue
		) ) {
			collectionKey = oldKey;
			collectionInstance = oldCollectionInstance;
			state = oldCollectionInstance == null ? State.MISSING : State.RESOLVED;
		}
		else {
			collectionKey = new CollectionKey( persister, collectionKeyValue );
			state = State.KEY_RESOLVED;
		}
	}

	protected void resolveInstance(RowProcessingState rowProcessingState, boolean isEager) {
		if ( state != State.KEY_RESOLVED ) {
			// already resolved
			return;
		}

		resolveCollectionKey( rowProcessingState, false );
		if ( state == State.KEY_RESOLVED ) {
			assert parent != null;
			// We can avoid processing further if the parent is already initialized,
			// as the value produced by this initializer will never be used anyway.
			if ( owningEntityInitializer != null && owningEntityInitializer.isEntityInitialized() ) {
				// It doesn't matter if it's eager or lazy, the collection object can not be referred to,
				// so it doesn't make sense to create or initialize it
				state = State.MISSING;
				return;
			}
			state = State.RESOLVED;

			final SharedSessionContractImplementor session = rowProcessingState.getSession();
			final PersistenceContext persistenceContext = session.getPersistenceContext();

			final LoadingCollectionEntry loadingEntry = persistenceContext.getLoadContexts()
					.findLoadingCollectionEntry( collectionKey );

			if ( loadingEntry != null ) {
				collectionInstance = loadingEntry.getCollectionInstance();
				if ( collectionInstance.getOwner() == null ) {
					assert owningEntityInitializer.getTargetInstance() != null;
					collectionInstance.setOwner( owningEntityInitializer.getTargetInstance() );
				}
				return;
			}

			final PersistentCollection<?> existing = persistenceContext.getCollection( collectionKey );

			if ( existing != null ) {
				collectionInstance = existing;
				if ( collectionInstance.getOwner() == null ) {
					assert owningEntityInitializer.getTargetInstance() != null;
					collectionInstance.setOwner( owningEntityInitializer.getTargetInstance() );
				}
				return;
			}

			final CollectionPersister collectionDescriptor = collectionAttributeMapping.getCollectionDescriptor();
			final CollectionSemantics<?, ?> collectionSemantics = collectionDescriptor.getCollectionSemantics();
			final Object key = collectionKey.getKey();

			collectionInstance = collectionSemantics.instantiateWrapper(
					key,
					collectionDescriptor,
					session
			);

			assert owningEntityInitializer.getTargetInstance() != null;
			collectionInstance.setOwner( owningEntityInitializer.getTargetInstance() );

			persistenceContext.addUninitializedCollection(
					collectionDescriptor,
					collectionInstance,
					key
			);

			if ( isEager ) {
				persistenceContext.addNonLazyCollection( collectionInstance );
			}

			if ( collectionSemantics.getCollectionClassification() == CollectionClassification.ARRAY ) {
				session.getPersistenceContext().addCollectionHolder( collectionInstance );
			}
		}
	}
	public void resolveInstance(Object instance, RowProcessingState rowProcessingState, boolean isEager) {
		if ( instance == null ) {
			setMissing();
		}
		else {
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
			collectionInstance = persistentCollection;
			state = State.RESOLVED;
			if ( isEager && !collectionInstance.wasInitialized() ) {
				persistenceContext.addNonLazyCollection( collectionInstance );
			}
			if ( collectionKeyResultAssembler != null
					&& !rowProcessingState.isQueryCacheHit()
					&& rowProcessingState.getQueryOptions().isResultCachingEnabled() == Boolean.TRUE ) {
				// Resolve the state of the identifier if result caching is enabled and this is not a query cache hit
				collectionKeyResultAssembler.resolveState( rowProcessingState );
			}
		}
	}

	@Override
	protected <X> void forEachSubInitializer(BiConsumer<Initializer, X> consumer, X arg) {
		if ( collectionKeyResultAssembler != null ) {
			final Initializer initializer = collectionKeyResultAssembler.getInitializer();
			if ( initializer != null ) {
				consumer.accept( initializer, arg );
			}
		}
	}

	@Override
	public @Nullable PersistentCollection<?> getCollectionInstance() {
		return state == State.UNINITIALIZED || state == State.MISSING ? null : collectionInstance;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return collectionPath;
	}

	public PluralAttributeMapping getCollectionAttributeMapping() {
		return collectionAttributeMapping;
	}

	@Override
	public PluralAttributeMapping getInitializedPart() {
		return getCollectionAttributeMapping();
	}

	@Override
	public @Nullable FetchParentAccess getFetchParentAccess() {
		return (FetchParentAccess) parent;
	}

	@Override
	public @Nullable InitializerParent getParent() {
		return parent;
	}

	@Override
	public Object getParentKey() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isPartOfKey() {
		// A collection can never be part of a key
		return false;
	}

	@Override
	public boolean isResultInitializer() {
		return isResultInitializer;
	}

	@Override
	public @Nullable CollectionKey resolveCollectionKey(RowProcessingState rowProcessingState) {
		resolveInstance();
		return collectionKey;
	}
}
