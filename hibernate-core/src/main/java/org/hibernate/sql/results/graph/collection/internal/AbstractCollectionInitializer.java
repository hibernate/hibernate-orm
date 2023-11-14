/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection.internal;

import java.util.function.Consumer;

import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;
import org.hibernate.sql.results.graph.collection.CollectionLoadingLogger;
import org.hibernate.sql.results.graph.collection.LoadingCollectionEntry;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Base support for CollectionInitializer implementations
 *
 * @author Steve Ebersole
 */
public abstract class AbstractCollectionInitializer implements CollectionInitializer {
	private final NavigablePath collectionPath;
	private final FetchParentAccess owningParent;
	private final EntityMappingType ownedModelPartDeclaringType;
	protected final PluralAttributeMapping collectionAttributeMapping;
	protected final boolean isResultInitializer;
	protected final @Nullable FetchParentAccess parentAccess;

	/**
	 * refers to the collection's container value - which collection-key?
	 */
	protected final @Nullable DomainResultAssembler<?> collectionKeyResultAssembler;

	protected @Nullable PersistentCollection<?> collectionInstance;
	protected @Nullable CollectionKey collectionKey;

	protected boolean parentShallowCached;

	// per-row state
	protected State state = State.UNINITIALIZED;

	protected AbstractCollectionInitializer(
			NavigablePath collectionPath,
			PluralAttributeMapping collectionAttributeMapping,
			FetchParentAccess parentAccess,
			@Nullable DomainResult<?> collectionKeyResult,
			boolean isResultInitializer,
			AssemblerCreationState creationState) {
		this.collectionPath = collectionPath;
		this.owningParent = FetchParentAccess.determineOwningParent( parentAccess );
		this.ownedModelPartDeclaringType = FetchParentAccess.determineOwnedModelPartDeclaringType( collectionAttributeMapping, parentAccess, owningParent );
		this.collectionAttributeMapping = collectionAttributeMapping;
		this.isResultInitializer = isResultInitializer;
		this.parentAccess = parentAccess;
		this.collectionKeyResultAssembler = collectionKeyResult == null
				? null
				: collectionKeyResult.createResultAssembler( this, creationState );
	}

	@Override
	public void resolveKey(RowProcessingState rowProcessingState) {
		if ( state != State.UNINITIALIZED ) {
			// already resolved
			return;
		}

		final CollectionKey oldKey = collectionKey;
		final PersistentCollection<?> oldCollectionInstance = collectionInstance;
		collectionKey = null;
		collectionInstance = null;
		state = State.MISSING;
		if ( parentShallowCached || shouldSkipInitializer( rowProcessingState ) ) {
			return;
		}

		// A null collection key result assembler means that we can use the parent key
		final Object collectionKeyValue;
		if ( collectionKeyResultAssembler == null ) {
			assert parentAccess != null;
			collectionKeyValue = parentAccess.getParentKey();
		}
		else {
			collectionKeyValue = collectionKeyResultAssembler.assemble( rowProcessingState );
		}

		if ( collectionKeyValue != null ) {
			final CollectionPersister persister = collectionAttributeMapping.getCollectionDescriptor();
			// Try to reuse the previous collection key and collection if possible
			if ( oldKey != null && persister.getKeyType().isEqual( oldKey.getKey(), collectionKeyValue ) ) {
				collectionKey = oldKey;
				collectionInstance = oldCollectionInstance;
				state = oldCollectionInstance == null ? State.MISSING : State.RESOLVED;
			}
			else {
				collectionKey = new CollectionKey( persister, collectionKeyValue );
				state = State.KEY_RESOLVED;
			}

			if ( CollectionLoadingLogger.COLL_LOAD_LOGGER.isDebugEnabled() ) {
				CollectionLoadingLogger.COLL_LOAD_LOGGER.debugf(
						"(%s) Current row collection key : %s",
						this.getClass().getSimpleName(),
						LoggingHelper.toLoggableString( getNavigablePath(), collectionKey.getKey() )
				);
			}
		}
	}

	protected void resolveInstance(RowProcessingState rowProcessingState, boolean isEager) {
		if ( state == State.KEY_RESOLVED ) {
			assert parentAccess != null;
			// We can avoid processing further if the parent is already initialized,
			// as the value produced by this initializer will never be used anyway.
			final EntityInitializer entityInitializer = parentAccess.findFirstEntityInitializer();
			if ( entityInitializer != null && entityInitializer.isEntityInitialized() ) {
				// It doesn't matter if it's eager or lazy, the collection object can not be referred to,
				// so it doesn't make sense to create or initialize it
				state = State.MISSING;
				return;
			}
			state = State.RESOLVED;

			final SharedSessionContractImplementor session = rowProcessingState.getSession();
			final PersistenceContext persistenceContext = session.getPersistenceContext();
			final FetchParentAccess fetchParentAccess = parentAccess.findFirstEntityDescriptorAccess();

			final LoadingCollectionEntry loadingEntry = persistenceContext.getLoadContexts()
					.findLoadingCollectionEntry( collectionKey );

			if ( loadingEntry != null ) {
				collectionInstance = loadingEntry.getCollectionInstance();
				if ( collectionInstance.getOwner() == null ) {
					fetchParentAccess.registerResolutionListener(
							owner -> collectionInstance.setOwner( owner )
					);
				}
				return;
			}

			final PersistentCollection<?> existing = persistenceContext.getCollection( collectionKey );

			if ( existing != null ) {
				collectionInstance = existing;
				if ( collectionInstance.getOwner() == null ) {
					fetchParentAccess.registerResolutionListener(
							owner -> collectionInstance.setOwner( owner )
					);
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

			fetchParentAccess.registerResolutionListener(
					owner -> collectionInstance.setOwner( owner )
			);

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
		return parentAccess;
	}

	@Override
	public @Nullable FetchParentAccess getOwningParent() {
		return owningParent;
	}

	@Override
	public @Nullable EntityMappingType getOwnedModelPartDeclaringType() {
		return ownedModelPartDeclaringType;
	}

	@Override
	public @Nullable FetchParentAccess findFirstEntityDescriptorAccess() {
		return parentAccess == null ? null : parentAccess.findFirstEntityDescriptorAccess();
	}

	@Override
	public Object getParentKey() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void registerResolutionListener(Consumer<Object> resolvedParentConsumer) {
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
		resolveKey( rowProcessingState );
		return collectionKey;
	}

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {
		state = State.UNINITIALIZED;
	}

	@Override
	public void markShallowCached() {
		parentShallowCached = true;
	}

	@Override
	public void endLoading(ExecutionContext executionContext) {
		parentShallowCached = false;
	}

	protected enum State {
		UNINITIALIZED,
		MISSING,
		KEY_RESOLVED,
		RESOLVED,
		INITIALIZED
	}
}
