/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.collection.CollectionLoadingLogger;
import org.hibernate.sql.results.graph.collection.LoadingCollectionEntry;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.internal.LoadingCollectionEntryImpl;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.sql.results.graph.collection.CollectionLoadingLogger.COLL_LOAD_LOGGER;

/**
 * Base support for CollectionInitializer implementations that represent
 * an immediate initialization of some sort (join, select, batch, sub-select)
 * for a persistent collection.
 *
 * @author Steve Ebersole
 * @implNote Mainly an intention contract wrt the immediacy of the fetch.
 */
public abstract class AbstractImmediateCollectionInitializer extends AbstractCollectionInitializer {

	/**
	 * refers to the rows entry in the collection.  null indicates that the collection is empty
	 */
	private final @Nullable DomainResultAssembler<?> collectionValueKeyResultAssembler;

	private boolean shallowCached;

	// per-row state

	/**
	 * The value of the collection side of the collection key (FK).  Identifies
	 * inclusion in the collection.  Can be null to indicate that the current row
	 * does not contain any collection values
	 */
	private Object collectionValueKey;
	private LoadingCollectionEntryImpl responsibility;

	/**
	 * @deprecated Use {@link #AbstractImmediateCollectionInitializer(NavigablePath, PluralAttributeMapping, InitializerParent, LockMode, DomainResult, DomainResult, boolean, AssemblerCreationState)} instead.
	 */
	@Deprecated(forRemoval = true)
	public AbstractImmediateCollectionInitializer(
			NavigablePath collectionPath,
			PluralAttributeMapping collectionAttributeMapping,
			FetchParentAccess parentAccess,
			LockMode lockMode,
			DomainResult<?> collectionKeyResult,
			DomainResult<?> collectionValueKeyResult,
			boolean isResultInitializer,
			AssemblerCreationState creationState) {
		this(
				collectionPath,
				collectionAttributeMapping,
				(InitializerParent) parentAccess,
				lockMode,
				collectionKeyResult,
				collectionValueKeyResult,
				isResultInitializer,
				creationState
		);
	}

	public AbstractImmediateCollectionInitializer(
			NavigablePath collectionPath,
			PluralAttributeMapping collectionAttributeMapping,
			InitializerParent parent,
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
				: collectionValueKeyResult.createResultAssembler( (InitializerParent) this, creationState );
	}

	protected abstract String getSimpleConcreteImplName();

	@Override
	protected <X> void forEachSubInitializer(BiConsumer<Initializer, X> consumer, X arg) {
		super.forEachSubInitializer( consumer, arg );
		if ( collectionValueKeyResultAssembler != null ) {
			final Initializer initializer = collectionValueKeyResultAssembler.getInitializer();
			if ( initializer != null ) {
				consumer.accept( initializer, arg );
			}
		}
	}

	@Override
	public void startLoading(RowProcessingState rowProcessingState) {
		if ( rowProcessingState.isQueryCacheHit() && getInitializingCollectionDescriptor().useShallowQueryCacheLayout() ) {
			shallowCached = true;
		}
		super.startLoading( rowProcessingState );
	}

	@Override
	public void resolveKey() {
		if ( state != State.UNINITIALIZED ) {
			// already resolved
			return;
		}
		super.resolveKey();
		collectionValueKey = null;
		// Can't resolve any sub-initializers if the collection is shallow cached
		if ( state != State.MISSING && !shallowCached ) {
			if ( collectionValueKeyResultAssembler == null ) {
				// A null collectionValueKeyResultAssembler means that we should use the parent key.
				// Since this method can only be called when the parent exists, we know the collection is not missing
				resolveKeySubInitializers( rowProcessingState );
			}
			else {
				resolveCollectionContentKey( rowProcessingState );
			}
		}
	}

	/**
	 * Returns whether the collection value key is missing.
	 */
	private boolean resolveCollectionContentKey(RowProcessingState rowProcessingState) {
		assert collectionValueKeyResultAssembler != null;
		final Initializer initializer = collectionValueKeyResultAssembler.getInitializer();
		if ( initializer != null ) {
			initializer.resolveKey();
			if ( initializer.getState() == State.MISSING ) {
				return true;
			}
		}
		else {
			collectionValueKey = collectionValueKeyResultAssembler.assemble( rowProcessingState );
			if ( collectionValueKey == null ) {
				return true;
			}
		}
		// If we get here, a collectionValueKey exists or is likely to exist,
		// so we need to call resolveKey on the index and element initializers of the collection
		// to initialize this resolved collection instance later
		resolveKeySubInitializers( rowProcessingState );
		return false;
	}

	private void resolveKeySubInitializers(RowProcessingState rowProcessingState) {
		final DomainResultAssembler<?> indexAssembler = getIndexAssembler();
		final Initializer indexInitializer;
		if ( indexAssembler != null && ( indexInitializer = indexAssembler.getInitializer() ) != null ) {
			indexInitializer.resolveKey();
		}
		final Initializer elementInitializer = getElementAssembler().getInitializer();
		if ( elementInitializer != null ) {
			elementInitializer.resolveKey();
		}
	}

	@Override
	public void resolveInstance() {
		if ( state != State.KEY_RESOLVED ) {
			// already resolved
			return;
		}

		resolveCollectionKey( rowProcessingState, true );
		if ( state != State.KEY_RESOLVED ) {
			return;
		}

		if ( CollectionLoadingLogger.COLL_LOAD_LOGGER.isDebugEnabled() ) {
			CollectionLoadingLogger.COLL_LOAD_LOGGER.debugf(
					"(%s) Current row collection key : %s",
					this.getClass().getSimpleName(),
					LoggingHelper.toLoggableString( getNavigablePath(), collectionKey.getKey() )
			);
		}

		if ( CollectionLoadingLogger.COLL_LOAD_LOGGER.isTraceEnabled() ) {
			COLL_LOAD_LOGGER.tracef(
					"(%s) Beginning Initializer#resolveInstance for collection : %s",
					getSimpleConcreteImplName(),
					LoggingHelper.toLoggableString( getNavigablePath(), collectionKey.getKey() )
			);
		}

		state = State.RESOLVED;
		responsibility = null;

		// determine the PersistentCollection instance to use and whether
		// we (this initializer) is responsible for loading its state

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// First, look for a LoadingCollectionEntry
		final SharedSessionContractImplementor session = rowProcessingState.getSession();
		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
		final LoadingCollectionEntry existingLoadingEntry = persistenceContext.getLoadContexts()
				.findLoadingCollectionEntry( collectionKey );
		final PersistentCollection<?> existing;
		final PersistentCollection<?> existingUnowned;
		if ( existingLoadingEntry != null ) {
			collectionInstance = existingLoadingEntry.getCollectionInstance();

			if ( CollectionLoadingLogger.COLL_LOAD_LOGGER.isDebugEnabled() ) {
				COLL_LOAD_LOGGER.debugf(
						"(%s) Found existing loading collection entry [%s]; using loading collection instance - %s",
						getSimpleConcreteImplName(),
						LoggingHelper.toLoggableString( getNavigablePath(), collectionKey.getKey() ),
						toLoggableString( collectionInstance )
				);
			}

			if ( existingLoadingEntry.getInitializer() == this ) {
				assert !shallowCached;
				// we are responsible for loading the collection values
				responsibility = (LoadingCollectionEntryImpl) existingLoadingEntry;
			}
			else {
				// the entity is already being loaded elsewhere
				if ( CollectionLoadingLogger.COLL_LOAD_LOGGER.isDebugEnabled() ) {
					COLL_LOAD_LOGGER.debugf(
							"(%s) Collection [%s] being loaded by another initializer [%s] - skipping processing",
							getSimpleConcreteImplName(),
							LoggingHelper.toLoggableString( getNavigablePath(), collectionKey.getKey() ),
							existingLoadingEntry.getInitializer()
					);
				}
				state = State.INITIALIZED;
			}
		}
		else if ( ( existing = persistenceContext.getCollection( collectionKey ) ) != null ) {
			collectionInstance = existing;

			// we found the corresponding collection instance on the Session.  If
			// it is already initialized we have nothing to do

			if ( collectionInstance.wasInitialized() ) {
				if ( CollectionLoadingLogger.COLL_LOAD_LOGGER.isDebugEnabled() ) {
					COLL_LOAD_LOGGER.debugf(
							"(%s) Found existing collection instance [%s] in Session; skipping processing - [%s]",
							getSimpleConcreteImplName(),
							LoggingHelper.toLoggableString( getNavigablePath(), collectionKey.getKey() ),
							toLoggableString( collectionInstance )
					);
				}
				state = State.INITIALIZED;
			}
			else if ( !shallowCached ) {
				takeResponsibility( rowProcessingState, collectionKey );
			}
		}
		else if ( ( existingUnowned = persistenceContext.useUnownedCollection( collectionKey ) ) != null ) {
			collectionInstance = existingUnowned;

			// we found the corresponding collection instance as unowned on the Session.  If
			// it is already initialized we have nothing to do

			if ( collectionInstance.wasInitialized() ) {
				if ( CollectionLoadingLogger.COLL_LOAD_LOGGER.isDebugEnabled() ) {
					COLL_LOAD_LOGGER.debugf(
							"(%s) Found existing unowned collection instance [%s] in Session; skipping processing - [%s]",
							getSimpleConcreteImplName(),
							LoggingHelper.toLoggableString( getNavigablePath(), collectionKey.getKey() ),
							toLoggableString( collectionInstance )
					);
				}
				state = State.INITIALIZED;
			}
			else if ( !shallowCached ) {
				takeResponsibility( rowProcessingState, collectionKey );
			}
		}
		else {
			final CollectionPersister collectionDescriptor = getCollectionAttributeMapping().getCollectionDescriptor();
			final CollectionSemantics<?, ?> collectionSemantics = collectionDescriptor.getCollectionSemantics();

			collectionInstance = collectionSemantics.instantiateWrapper(
					collectionKey.getKey(),
					getInitializingCollectionDescriptor(),
					session
			);

			if ( CollectionLoadingLogger.COLL_LOAD_LOGGER.isDebugEnabled() ) {
				COLL_LOAD_LOGGER.debugf(
						"(%s) Created new collection wrapper [%s] : %s",
						getSimpleConcreteImplName(),
						LoggingHelper.toLoggableString( getNavigablePath(), collectionKey.getKey() ),
						toLoggableString( collectionInstance )
				);
			}

			if ( owningEntityInitializer != null ) {
				assert owningEntityInitializer.getTargetInstance() != null;
				collectionInstance.setOwner( owningEntityInitializer.getTargetInstance() );
			}

			persistenceContext.addUninitializedCollection(
					collectionDescriptor,
					collectionInstance,
					collectionKey.getKey()
			);

			if ( !shallowCached ) {
				takeResponsibility( rowProcessingState, collectionKey );
			}
		}

		if ( responsibility != null ) {
			if ( CollectionLoadingLogger.COLL_LOAD_LOGGER.isDebugEnabled() ) {
				COLL_LOAD_LOGGER.debugf(
						"(%s) Responsible for loading collection [%s] : %s",
						getSimpleConcreteImplName(),
						LoggingHelper.toLoggableString( getNavigablePath(), collectionKey.getKey() ),
						toLoggableString( collectionInstance )
				);
			}
		}
		if ( shallowCached ) {
			assert responsibility == null;
			initializeShallowCached( rowProcessingState );
		}
	}

	protected void initializeShallowCached(RowProcessingState rowProcessingState) {
		assert shallowCached;
		final PersistenceContext persistenceContext = rowProcessingState.getSession()
				.getPersistenceContextInternal();
		// If this is a query cache hit with the shallow query cache layout,
		// we have to lazy load the collection instead
		collectionInstance.forceInitialization();
		if ( collectionAttributeMapping.getCollectionDescriptor()
				.getCollectionSemantics()
				.getCollectionClassification() == CollectionClassification.ARRAY ) {
			persistenceContext.addCollectionHolder( collectionInstance );
		}
		state = State.INITIALIZED;
		initializeSubInstancesFromParent( rowProcessingState );
	}

	@Override
	protected void setMissing() {
		super.setMissing();
		collectionValueKey = null;
		responsibility = null;
	}

	@Override
	public void resolveInstance(Object instance) {
		assert state == State.UNINITIALIZED;
		if ( instance == null ) {
			setMissing();
			return;
		}
		// Check if the given instance is different from the previous row state to avoid creating CollectionKey
		if ( collectionInstance != instance ) {
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
			collectionKeyValue = persistentCollection.getKey();
			resolveCollectionKey( rowProcessingState, false );
			collectionInstance = persistentCollection;
			responsibility = null;
		}
		collectionValueKey = null;
		if ( collectionInstance.wasInitialized() ) {
			state = State.INITIALIZED;
			if ( shallowCached ) {
				initializeShallowCached( rowProcessingState );
			}
			else {
				resolveInstanceSubInitializers( rowProcessingState );
			}
			if ( !rowProcessingState.isQueryCacheHit() && rowProcessingState.getQueryOptions().isResultCachingEnabled() == Boolean.TRUE ) {
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
			if ( shallowCached ) {
				state = State.INITIALIZED;
				initializeShallowCached( rowProcessingState );
			}
			else {
				state = State.RESOLVED;
				final boolean rowContainsCollectionContent;
				if ( collectionValueKeyResultAssembler != null ) {
					rowContainsCollectionContent = resolveCollectionContentKey( rowProcessingState );
				}
				else {
					rowContainsCollectionContent = true;
				}
				if ( responsibility == null ) {
					final LoadingCollectionEntry existingLoadingEntry = rowProcessingState.getSession()
							.getPersistenceContextInternal()
							.getLoadContexts()
							.findLoadingCollectionEntry( collectionKey );
					if ( existingLoadingEntry != null ) {
						if ( existingLoadingEntry.getInitializer() == this ) {
							// we are responsible for loading the collection values
							responsibility = (LoadingCollectionEntryImpl) existingLoadingEntry;
						}
						else {
							// the collection is already being loaded elsewhere
							if ( CollectionLoadingLogger.COLL_LOAD_LOGGER.isDebugEnabled() ) {
								COLL_LOAD_LOGGER.debugf(
										"(%s) Collection [%s] being loaded by another initializer [%s] - skipping processing",
										getSimpleConcreteImplName(),
										LoggingHelper.toLoggableString( getNavigablePath(), collectionKey.getKey() ),
										existingLoadingEntry.getInitializer()
								);
							}
							state = State.INITIALIZED;
							if ( rowContainsCollectionContent && !rowProcessingState.isQueryCacheHit()
									&& rowProcessingState.getQueryOptions().isResultCachingEnabled() == Boolean.TRUE
									&& !getInitializingCollectionDescriptor().useShallowQueryCacheLayout() ) {
								// Resolve the state of the content if result caching is enabled and this is not a query cache hit
								// and the collection doesn't use a shallow query cache layout
								resolveCollectionContentState( rowProcessingState );
							}
						}
					}
					else {
						takeResponsibility( rowProcessingState, collectionKey );
					}
				}
			}
		}
	}

	protected abstract void resolveInstanceSubInitializers(RowProcessingState rowProcessingState);

	private void resolveCollectionContentState(RowProcessingState rowProcessingState) {
		final DomainResultAssembler<?> indexAssembler = getIndexAssembler();
		if ( indexAssembler != null ) {
			indexAssembler.resolveState( rowProcessingState );
		}
		getElementAssembler().resolveState( rowProcessingState );
	}

	/**
	 * Specialized toString handling for PersistentCollection.  All `PersistentCollection#toString`
	 * implementations are crazy expensive as they trigger a load
	 */
	private String toLoggableString(PersistentCollection<?> collectionInstance) {
		return collectionInstance == null
				? LoggingHelper.NULL
				: collectionInstance.getClass().getName() + "@" + System.identityHashCode( collectionInstance );
	}

	protected void takeResponsibility(RowProcessingState rowProcessingState, CollectionKey collectionKey) {
		responsibility = new LoadingCollectionEntryImpl(
				getCollectionAttributeMapping().getCollectionDescriptor(),
				this,
				collectionKey.getKey(),
				collectionInstance
		);
		rowProcessingState.getJdbcValuesSourceProcessingState().registerLoadingCollection(
				collectionKey,
				responsibility
		);
	}

	@Override
	public void initializeInstance() {
		if ( state != State.RESOLVED || responsibility == null ) {
			return;
		}
		state = State.INITIALIZED;

		if ( collectionValueKey == null && collectionValueKeyResultAssembler != null ) {
			final Initializer initializer = collectionValueKeyResultAssembler.getInitializer();
			if ( initializer != null ) {
				collectionValueKey = collectionValueKeyResultAssembler.assemble( rowProcessingState );
			}
		}

		// the RHS key value of the association - determines if the row contains an element of the initializing collection
		if ( collectionValueKeyResultAssembler == null || collectionValueKey != null ) {
			// the row contains an element in the collection...
			if ( CollectionLoadingLogger.COLL_LOAD_LOGGER.isDebugEnabled() ) {
				COLL_LOAD_LOGGER.debugf(
						"(%s) Reading element from row for collection [%s] -> %s",
						getSimpleConcreteImplName(),
						LoggingHelper.toLoggableString( getNavigablePath(), collectionKey.getKey() ),
						toLoggableString( collectionInstance )
				);
			}

			responsibility.load(
					loadingState -> readCollectionRow( collectionKey, loadingState, rowProcessingState )
			);
		}
	}

	@Override
	public void initializeInstanceFromParent(Object parentInstance) {
		collectionInstance = (PersistentCollection<?>) getInitializedPart().getValue( parentInstance );
		state = State.INITIALIZED;
		initializeSubInstancesFromParent( rowProcessingState );
	}

	protected abstract void readCollectionRow(
			CollectionKey collectionKey,
			List<Object> loadingState,
			RowProcessingState rowProcessingState);

	protected abstract void initializeSubInstancesFromParent(RowProcessingState rowProcessingState);

	public abstract @Nullable DomainResultAssembler<?> getIndexAssembler();

	public abstract DomainResultAssembler<?> getElementAssembler();

	@Override
	public void endLoading(ExecutionContext executionContext) {
		super.endLoading( executionContext );
		shallowCached = false;
	}

}
