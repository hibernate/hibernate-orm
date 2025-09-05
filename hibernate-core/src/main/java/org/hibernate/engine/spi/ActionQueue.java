/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.PropertyValueException;
import org.hibernate.TransientPropertyValueException;
import org.hibernate.action.internal.AbstractEntityInsertAction;
import org.hibernate.action.internal.BulkOperationCleanupAction;
import org.hibernate.action.internal.CollectionRecreateAction;
import org.hibernate.action.internal.CollectionRemoveAction;
import org.hibernate.action.internal.CollectionUpdateAction;
import org.hibernate.action.internal.EntityActionVetoException;
import org.hibernate.action.internal.EntityDeleteAction;
import org.hibernate.action.internal.EntityIdentityInsertAction;
import org.hibernate.action.internal.EntityInsertAction;
import org.hibernate.action.internal.EntityUpdateAction;
import org.hibernate.action.internal.OrphanRemovalAction;
import org.hibernate.action.internal.QueuedOperationCollectionAction;
import org.hibernate.action.internal.UnresolvedEntityInsertActions;
import org.hibernate.action.spi.AfterTransactionCompletionProcess;
import org.hibernate.action.spi.BeforeTransactionCompletionProcess;
import org.hibernate.action.spi.Executable;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.engine.internal.NonNullableTransientDependencies;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.mapping.internal.EntityCollectionPart;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.EntityType;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.Type;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/**
 * Responsible for maintaining the queue of actions related to events.
 * <p>
 * The {@code ActionQueue} holds the DML operations queued as part of a session's transactional-write-behind semantics.
 * The DML operations are queued here until a flush forces them to be executed against the database.
 *
 * @author Steve Ebersole
 * @author Gail Badner
 * @author Anton Marsden
 */
public class ActionQueue {

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( ActionQueue.class );

	private final SessionImplementor session;

	private UnresolvedEntityInsertActions unresolvedInsertions;

	// NOTE: ExecutableList fields must be instantiated via ListProvider#init
	//       or #getOrInit to ensure that they are instantiated consistently.

	// Object insertions, updates, and deletions have list semantics because
	// they must happen in the right order to respect referential integrity
	private ExecutableList<AbstractEntityInsertAction> insertions;
	private ExecutableList<EntityDeleteAction> deletions;
	private ExecutableList<EntityUpdateAction> updates;

	// Actually the semantics of the next three are really "Bag"
	// Note that, unlike objects, collection insertions, updates,
	// deletions are not really remembered between flushes. We
	// just re-use the same Lists for convenience.
	private ExecutableList<CollectionRecreateAction> collectionCreations;
	private ExecutableList<CollectionUpdateAction> collectionUpdates;
	private ExecutableList<QueuedOperationCollectionAction> collectionQueuedOps;
	private ExecutableList<CollectionRemoveAction> collectionRemovals;
	private ExecutableList<CollectionRemoveAction> orphanCollectionRemovals;

	// TODO: The removeOrphan concept is a temporary "hack" for HHH-6484.
	//       This should be removed once action/task ordering is improved.
	private ExecutableList<OrphanRemovalAction> orphanRemovals;


	private transient boolean isTransactionCoordinatorShared;
	private AfterTransactionCompletionProcessQueue afterTransactionProcesses;
	private BeforeTransactionCompletionProcessQueue beforeTransactionProcesses;

	// Extract this as a constant to perform efficient iterations:
	// method values() otherwise allocates a new array on each invocation.
	private static final OrderedActions[] ORDERED_OPERATIONS = OrderedActions.values();

	// The order of these operations is very important
	private enum OrderedActions {
		OrphanCollectionRemoveAction {
			@Override
			public ExecutableList<?> getActions(ActionQueue instance) {
				return instance.orphanCollectionRemovals;
			}
			@Override
			public void ensureInitialized(ActionQueue instance) {
				if ( instance.orphanCollectionRemovals == null ) {
					instance.orphanCollectionRemovals = new ExecutableList<>( instance.isOrderUpdatesEnabled() );
				}
			}
		},
		OrphanRemovalAction {
			@Override
			public ExecutableList<?> getActions(ActionQueue instance) {
				return instance.orphanRemovals;
			}
			@Override
			public void ensureInitialized(ActionQueue instance) {
				if ( instance.orphanRemovals == null ) {
					instance.orphanRemovals = new ExecutableList<>( false );
				}
			}
		},
		EntityInsertAction {
			@Override
			public ExecutableList<?> getActions(ActionQueue instance) {
				return instance.insertions;
			}
			@Override
			public void ensureInitialized(final ActionQueue instance) {
				if ( instance.insertions == null ) {
					//Special case of initialization
					instance.insertions = instance.isOrderInsertsEnabled()
							? new ExecutableList<>( InsertActionSorter.INSTANCE )
							: new ExecutableList<>( false );
				}
			}
		},
		EntityUpdateAction {
			@Override
			public ExecutableList<?> getActions(ActionQueue instance) {
				return instance.updates;
			}
			@Override
			public void ensureInitialized(ActionQueue instance) {
				if ( instance.updates == null ) {
					instance.updates = new ExecutableList<>( instance.isOrderUpdatesEnabled() );
				}
			}
		},
		QueuedOperationCollectionAction {
			@Override
			public ExecutableList<?> getActions(ActionQueue instance) {
				return instance.collectionQueuedOps;
			}
			@Override
			public void ensureInitialized(ActionQueue instance) {
				if ( instance.collectionQueuedOps == null ) {
					instance.collectionQueuedOps = new ExecutableList<>( instance.isOrderUpdatesEnabled() );
				}
			}
		},
		CollectionRemoveAction {
			@Override
			public ExecutableList<?> getActions(ActionQueue instance) {
				return instance.collectionRemovals;
			}
			@Override
			public void ensureInitialized(ActionQueue instance) {
				if ( instance.collectionRemovals == null ) {
					instance.collectionRemovals = new ExecutableList<>( instance.isOrderUpdatesEnabled() );
				}
			}
		},
		CollectionUpdateAction {
			@Override
			public ExecutableList<?> getActions(ActionQueue instance) {
				return instance.collectionUpdates;
			}
			@Override
			public void ensureInitialized(ActionQueue instance) {
				if ( instance.collectionUpdates == null ) {
					instance.collectionUpdates = new ExecutableList<>( instance.isOrderUpdatesEnabled() );
				}
			}
		},
		CollectionRecreateAction {
			@Override
			public ExecutableList<?> getActions(ActionQueue instance) {
				return instance.collectionCreations;
			}
			@Override
			public void ensureInitialized(ActionQueue instance) {
				if ( instance.collectionCreations == null ) {
					instance.collectionCreations = new ExecutableList<>( instance.isOrderUpdatesEnabled() );
				}
			}
		},
		EntityDeleteAction {
			@Override
			public ExecutableList<?> getActions(ActionQueue instance) {
				return instance.deletions;
			}
			@Override
			public void ensureInitialized(ActionQueue instance) {
				if ( instance.deletions == null ) {
					instance.deletions = new ExecutableList<>( false );
				}
			}
		};

		public abstract ExecutableList<?> getActions(ActionQueue instance);
		public abstract void ensureInitialized(ActionQueue instance);
	}

	/**
	 * Constructs an action queue bound to the given session.
	 *
	 * @param session The session "owning" this queue.
	 */
	public ActionQueue(SessionImplementor session) {
		this.session = session;
		isTransactionCoordinatorShared = false;
	}

	public void clear() {
		for ( var value : ORDERED_OPERATIONS ) {
			final var queue = value.getActions( this );
			if ( queue != null ) {
				queue.clear();
			}
		}
		if ( unresolvedInsertions != null ) {
			unresolvedInsertions.clear();
		}
	}

	/**
	 * Adds an entity insert action
	 *
	 * @param action The action representing the entity insertion
	 */
	public void addAction(EntityInsertAction action) {
		LOG.tracev( "Adding an EntityInsertAction for [{0}] object", action.getEntityName() );
		addInsertAction( action );
	}

	private void addInsertAction(AbstractEntityInsertAction insert) {
		if ( insert.isEarlyInsert() ) {
			// For early inserts, must execute inserts before finding non-nullable transient entities.
			// TODO: find out why this is necessary
			LOG.tracev( "Executing inserts before finding non-nullable transient entities for early insert: [{0}]", insert );
			executeInserts();
		}
		final NonNullableTransientDependencies nonNullableTransientDependencies = insert.findNonNullableTransientEntities();
		if ( nonNullableTransientDependencies == null ) {
			LOG.tracev( "Adding insert with no non-nullable, transient entities: [{0}]", insert );
			addResolvedEntityInsertAction( insert );
		}
		else {
			if ( LOG.isTraceEnabled() ) {
				LOG.tracev( "Adding insert with non-nullable, transient entities; insert=[{0}], dependencies=[{1}]", insert,
							nonNullableTransientDependencies.toLoggableString( insert.getSession() ) );
			}
			if ( unresolvedInsertions == null ) {
				unresolvedInsertions = new UnresolvedEntityInsertActions();
			}
			unresolvedInsertions.addUnresolvedEntityInsertAction( insert, nonNullableTransientDependencies );
		}
	}

	private void addResolvedEntityInsertAction(AbstractEntityInsertAction insert) {
		if ( insert.isEarlyInsert() ) {
			LOG.trace( "Executing insertions before resolved early-insert" );
			executeInserts();
			LOG.trace( "Executing identity-insert immediately" );
			execute( insert );
		}
		else {
			LOG.trace( "Adding resolved non-early insert action." );
			OrderedActions.EntityInsertAction.ensureInitialized( this );
			insertions.add( insert );
		}
		if ( !insert.isVeto() ) {
			insert.makeEntityManaged();
			if ( unresolvedInsertions != null ) {
				for ( AbstractEntityInsertAction resolvedAction :
						unresolvedInsertions.resolveDependentActions( insert.getInstance(), session ) ) {
					addResolvedEntityInsertAction( resolvedAction );
				}
			}
		}
		else {
			throw new EntityActionVetoException(
				"The EntityInsertAction was vetoed.",
				insert
			);
		}
	}

	/**
	 * Adds an entity (IDENTITY) insert action
	 *
	 * @param action The action representing the entity insertion
	 */
	public void addAction(EntityIdentityInsertAction action) {
		LOG.tracev( "Adding an EntityIdentityInsertAction for [{0}] object", action.getEntityName() );
		addInsertAction( action );
	}

	/**
	 * Adds an entity delete action
	 *
	 * @param action The action representing the entity deletion
	 */
	public void addAction(EntityDeleteAction action) {
		OrderedActions.EntityDeleteAction.ensureInitialized( this );
		deletions.add( action );
	}

	/**
	 * Adds an orphan removal action
	 *
	 * @param action The action representing the orphan removal
	 */
	public void addAction(final OrphanRemovalAction action) {
		OrderedActions.OrphanRemovalAction.ensureInitialized( this );
		orphanRemovals.add( action );
	}

	/**
	 * Adds an entity update action
	 *
	 * @param action The action representing the entity update
	 */
	public void addAction(final EntityUpdateAction action) {
		OrderedActions.EntityUpdateAction.ensureInitialized( this );
		updates.add( action );
	}

	/**
	 * Adds a collection (re)create action
	 *
	 * @param action The action representing the (re)creation of a collection
	 */
	public void addAction(final CollectionRecreateAction action) {
		OrderedActions.CollectionRecreateAction.ensureInitialized( this );
		collectionCreations.add( action );
	}

	/**
	 * Adds a collection remove action
	 *
	 * @param action The action representing the removal of a collection
	 */
	public void addAction(final CollectionRemoveAction action) {
		if ( orphanRemovals != null && action.getAffectedOwner() != null
				&& session.getPersistenceContextInternal()
						.getEntry( action.getAffectedOwner() )
						.getStatus()
						.isDeletedOrGone() ) {
			// We need to check if this collection's owner is an orphan being removed,
			// which case we should remove the collection first to avoid constraint violations
			for ( var orphanRemoval : orphanRemovals ) {
				if ( orphanRemoval.getInstance() == action.getAffectedOwner() ) {
					OrderedActions.OrphanCollectionRemoveAction.ensureInitialized( this );
					orphanCollectionRemovals.add( action );
					return;
				}
			}
		}
		OrderedActions.CollectionRemoveAction.ensureInitialized( this );
		collectionRemovals.add( action );
	}

	/**
	 * Adds a collection update action
	 *
	 * @param action The action representing the update of a collection
	 */
	public void addAction(final CollectionUpdateAction action) {
		OrderedActions.CollectionUpdateAction.ensureInitialized( this );
		collectionUpdates.add( action );
	}

	/**
	 * Adds an action relating to a collection queued operation (extra lazy).
	 *
	 * @param action The action representing the queued operation
	 */
	public void addAction(QueuedOperationCollectionAction action) {
		OrderedActions.QueuedOperationCollectionAction.ensureInitialized( this );
		this.collectionQueuedOps.add( action );
	}

	/**
	 * Adds an action defining a cleanup relating to a bulk operation (HQL/JPQL or Criteria based update/delete)
	 *
	 * @param action The action representing the queued operation
	 */
	public void addAction(BulkOperationCleanupAction action) {
		registerCleanupActions( action );
	}

	private void registerCleanupActions(Executable executable) {
		final var beforeTransactionCompletion = executable.getBeforeTransactionCompletionProcess();
		if ( beforeTransactionCompletion != null ) {
			if ( beforeTransactionProcesses == null ) {
				beforeTransactionProcesses = new BeforeTransactionCompletionProcessQueue( session );
			}
			beforeTransactionProcesses.register( beforeTransactionCompletion );
		}
		if ( getSessionFactoryOptions().isQueryCacheEnabled() ) {
			invalidateSpaces( executable.getPropertySpaces() );
		}
		final var afterTransactionCompletion = executable.getAfterTransactionCompletionProcess();
		if ( afterTransactionCompletion != null ) {
			if ( afterTransactionProcesses == null ) {
				afterTransactionProcesses = new AfterTransactionCompletionProcessQueue( session );
			}
			afterTransactionProcesses.register( afterTransactionCompletion );
		}
	}

	/**
	 * Are there unresolved entity insert actions that depend on non-nullable associations with a transient entity?
	 *
	 * @return true, if there are unresolved entity insert actions that depend on non-nullable associations with a
	 * transient entity; false, otherwise
	 */
	public boolean hasUnresolvedEntityInsertActions() {
		return unresolvedInsertions != null && !unresolvedInsertions.isEmpty();
	}

	/**
	 * Throws {@link PropertyValueException} if there are any unresolved entity insert actions that depend
	 * on non-nullable associations with a transient entity. This method should be called on completion of an operation
	 * (after all cascades are completed) that saves an entity.
	 *
	 * @throws PropertyValueException if there are any unresolved entity insert actions;
	 * {@link PropertyValueException#getEntityName()} and
	 * {@link PropertyValueException#getPropertyName()} will return the entity name and property value for
	 * the first unresolved entity insert action.
	 */
	public void checkNoUnresolvedActionsAfterOperation() throws PropertyValueException {
		if ( unresolvedInsertions != null ) {
			unresolvedInsertions.checkNoUnresolvedActionsAfterOperation();
		}
	}

	public void registerProcess(AfterTransactionCompletionProcess process) {
		if ( afterTransactionProcesses == null ) {
			afterTransactionProcesses = new AfterTransactionCompletionProcessQueue( session );
		}
		afterTransactionProcesses.register( process );
	}

	public void registerProcess(BeforeTransactionCompletionProcess process) {
		if ( beforeTransactionProcesses == null ) {
			beforeTransactionProcesses = new BeforeTransactionCompletionProcessQueue( session );
		}
		beforeTransactionProcesses.register( process );
	}

	/**
	 * Perform all currently queued entity-insertion actions.
	 *
	 * @throws HibernateException error executing queued insertion actions.
	 */
	public void executeInserts() throws HibernateException {
		if ( insertions != null && !insertions.isEmpty() ) {
			executeActions( insertions );
		}
	}

	/**
	 * Perform all currently queued actions.
	 *
	 * @throws HibernateException error executing queued actions.
	 */
	public void executeActions() throws HibernateException {
		if ( hasUnresolvedEntityInsertActions() ) {
			final var insertAction =
					unresolvedInsertions.getDependentEntityInsertActions()
							.iterator().next();
			final var transientEntities = insertAction.findNonNullableTransientEntities();
			final Object transientEntity = transientEntities.getNonNullableTransientEntities().iterator().next();
			final String path = transientEntities.getNonNullableTransientPropertyPaths( transientEntity ).iterator().next();
			final String transientEntityName = session.bestGuessEntityName( transientEntity );
			final String entityName = insertAction.getEntityName();
			throw new TransientPropertyValueException(
					"Persistent instance of '" + entityName + "' with id [" + insertAction.getId()
					+ "] references an unsaved transient instance of '" + transientEntityName
					+ "' (persist the transient instance before flushing)",
					entityName,
					transientEntityName,
					path );
		}

		for ( var action : ORDERED_OPERATIONS ) {
			executeActions( action.getActions( this ) );
		}
	}

	/**
	 * Prepares the internal action queues for execution.
	 *
	 * @throws HibernateException error preparing actions.
	 */
	public void prepareActions() throws HibernateException {
		prepareActions( collectionRemovals );
		prepareActions( collectionUpdates );
		prepareActions( collectionCreations );
		prepareActions( collectionQueuedOps );
	}

	private void prepareActions(@Nullable ExecutableList<?> queue) throws HibernateException {
		if ( queue != null ) {
			for ( var executable : queue ) {
				executable.beforeExecutions();
			}
		}
	}

	/**
	 * Performs cleanup of any held cache soft locks.
	 *
	 * @param success Was the transaction successful.
	 */
	public void afterTransactionCompletion(boolean success) {
		if ( !isTransactionCoordinatorShared ) {
			// Execute completion actions only in transaction owner (aka parent session).
			if ( afterTransactionProcesses != null ) {
				afterTransactionProcesses.afterTransactionCompletion( success );
			}
		}
	}

	/**
	 * Execute any registered {@link BeforeTransactionCompletionProcess}
	 */
	public void beforeTransactionCompletion() {
		if ( !isTransactionCoordinatorShared ) {
			// Execute completion actions only in transaction owner (aka parent session).
			if ( beforeTransactionProcesses != null ) {
				beforeTransactionProcesses.beforeTransactionCompletion();
			}
			// Make sure to always execute pending batches before the transaction completes.
			// One such pending batch could be the pessimistic version increment for an entity
			session.getJdbcCoordinator().executeBatch();
		}
	}

	/**
	 * Check whether any insertion or deletion actions are currently queued.
	 *
	 * @return {@code true} if insertions or deletions are currently queued; {@code false} otherwise.
	 */
	public boolean areInsertionsOrDeletionsQueued() {
		return insertions != null && !insertions.isEmpty()
			|| hasUnresolvedEntityInsertActions()
			|| deletions != null && !deletions.isEmpty()
			|| orphanRemovals != null && !orphanRemovals.isEmpty();
	}

	/**
	 * Check whether the given tables/query-spaces are to be executed against given the currently queued actions.
	 *
	 * @param tables The table/query-spaces to check.
	 *
	 * @return {@code true} if we contain pending actions against any of the given tables; {@code false} otherwise.
	 */
	public boolean areTablesToBeUpdated(Set<? extends Serializable> tables) {
		if ( tables.isEmpty() ) {
			return false;
		}
		for ( var action : ORDERED_OPERATIONS ) {
			final var queue = action.getActions( this );
			if ( areTablesToBeUpdated( queue, tables ) ) {
				return true;
			}
		}
		if ( unresolvedInsertions == null ) {
			return false;
		}
		return areTablesToBeUpdated( unresolvedInsertions, tables );
	}

	private static boolean areTablesToBeUpdated(@Nullable ExecutableList<?> queue, Set<? extends Serializable> tableSpaces) {
		if ( queue == null || queue.isEmpty() ) {
			return false;
		}
		else {
			for ( var actionSpace : queue.getQuerySpaces() ) {
				if ( tableSpaces.contains( actionSpace ) ) {
					LOG.tracef( "Changes must be flushed to space: %s", actionSpace );
					return true;
				}
			}
			return false;
		}
	}

	private static boolean areTablesToBeUpdated(UnresolvedEntityInsertActions actions, Set<? extends Serializable> tableSpaces) {
		for ( var action : actions.getDependentEntityInsertActions() ) {
			for ( var space : action.getPropertySpaces() ) {
				if ( tableSpaces.contains( space ) ) {
					LOG.tracef( "Changes must be flushed to space: %s", space );
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Perform {@link Executable#execute()} on each element of the list
	 *
	 * @param queue The list of Executable elements to be performed
	 *
	 */
	private <E extends ComparableExecutable> void executeActions(@Nullable ExecutableList<E> queue)
			throws HibernateException {
		if ( queue != null && !queue.isEmpty() ) {
			// todo : consider ways to improve the double iteration of Executables here:
			//		1) we explicitly iterate list here to perform Executable#execute()
			//		2) ExecutableList#getQuerySpaces also iterates the Executables to collect query spaces.
			try {
				for ( var executable : queue ) {
					try {
						executable.execute();
					}
					finally {
						final var beforeTransactionCompletion = executable.getBeforeTransactionCompletionProcess();
						if ( beforeTransactionCompletion != null ) {
							if ( beforeTransactionProcesses == null ) {
								beforeTransactionProcesses = new BeforeTransactionCompletionProcessQueue( session );
							}
							beforeTransactionProcesses.register( beforeTransactionCompletion );
						}
						final var afterTransactionCompletion = executable.getAfterTransactionCompletionProcess();
						if ( afterTransactionCompletion != null ) {
							if ( afterTransactionProcesses == null ) {
								afterTransactionProcesses = new AfterTransactionCompletionProcessQueue( session );
							}
							afterTransactionProcesses.register( afterTransactionCompletion );
						}
					}
				}
			}
			finally {
				if ( getSessionFactoryOptions().isQueryCacheEnabled() ) {
					// Strictly speaking, only a subset of the list may have been processed if a RuntimeException occurs.
					// We still invalidate all spaces. I don't see this as a big deal - after all, RuntimeExceptions are
					// unexpected.
					invalidateSpaces( queue.getQuerySpaces().toArray( new String[0] ) );
				}
			}

			queue.clear();
			session.getJdbcCoordinator().executeBatch();
		}
	}

	/**
	 * @param executable The action to execute
	 */
	public <E extends Executable & Comparable<?>> void execute(E executable) {
		try {
			executable.execute();
		}
		finally {
			registerCleanupActions( executable );
		}
	}

	/**
	 * This method is now called once per execution of an ExecutableList or once for execution of an Execution.
	 *
	 * @param spaces The spaces to invalidate
	 */
	private void invalidateSpaces(String @Nullable [] spaces) {
		if ( spaces != null && spaces.length > 0 ) {
			for ( var space : spaces ) {
				if ( afterTransactionProcesses == null ) {
					afterTransactionProcesses = new AfterTransactionCompletionProcessQueue( session );
				}
				afterTransactionProcesses.addSpaceToInvalidate( space );
			}
			// Performance win: If we are processing an ExecutableList, this will only be called once
			session.getFactory().getCache().getTimestampsCache().preInvalidate( spaces, session );
		}
	}

	/**
	 * Returns a string representation of the object.
	 *
	 * @return a string representation of the object.
	 */
	@Override
	public String toString() {
		return "ActionQueue[insertions=" + toString( insertions )
				+ " updates=" + toString( updates )
				+ " deletions=" + toString( deletions )
				+ " orphanRemovals=" + toString( orphanRemovals )
				+ " collectionCreations=" + toString( collectionCreations )
				+ " collectionRemovals=" + toString( collectionRemovals )
				+ " collectionUpdates=" + toString( collectionUpdates )
				+ " collectionQueuedOps=" + toString( collectionQueuedOps )
				+ " unresolvedInsertDependencies=" + unresolvedInsertions
				+ "]";
	}

	private static String toString(@Nullable ExecutableList<?> queue) {
		return queue == null ? "ExecutableList{size=0}" : queue.toString();
	}

	public int numberOfCollectionRemovals() {
		return collectionRemovals == null ? 0 : collectionRemovals.size();
	}

	public int numberOfCollectionUpdates() {
		return collectionUpdates == null ? 0 : collectionUpdates.size();
	}

	public int numberOfCollectionCreations() {
		return collectionCreations == null ? 0 : collectionCreations.size();
	}

	public int numberOfDeletions() {
		final int deletes = deletions == null ? 0 : deletions.size();
		final int orphans = orphanRemovals == null ? 0 : orphanRemovals.size();
		return deletes + orphans;
	}

	public int numberOfUpdates() {
		return updates == null ? 0 : updates.size();
	}

	public int numberOfInsertions() {
		return insertions == null ? 0 : insertions.size();
	}

	public TransactionCompletionProcesses getTransactionCompletionProcesses() {
		if ( beforeTransactionProcesses == null ) {
			beforeTransactionProcesses = new BeforeTransactionCompletionProcessQueue( session );
		}
		if ( afterTransactionProcesses == null ) {
			afterTransactionProcesses = new AfterTransactionCompletionProcessQueue( session );
		}
		return new TransactionCompletionProcesses( beforeTransactionProcesses, afterTransactionProcesses );
	}

	/**
	 * Bind transaction completion processes to make them shared between primary and secondary session.
	 * Transaction completion processes are always executed by transaction owner (primary session),
	 * but can be registered using secondary session too.
	 *
	 * @param processes Transaction completion processes.
	 * @param isTransactionCoordinatorShared Flag indicating shared transaction context.
	 */
	public void setTransactionCompletionProcesses(
			TransactionCompletionProcesses processes,
			boolean isTransactionCoordinatorShared) {
		this.isTransactionCoordinatorShared = isTransactionCoordinatorShared;
		this.beforeTransactionProcesses = processes.beforeTransactionCompletionProcesses;
		this.afterTransactionProcesses = processes.afterTransactionCompletionProcesses;
	}

	public void sortCollectionActions() {
		if ( isOrderUpdatesEnabled() ) {
			// sort the updates by fk
			if ( collectionCreations != null ) {
				collectionCreations.sort();
			}
			if ( collectionUpdates != null ) {
				collectionUpdates.sort();
			}
			if ( collectionQueuedOps != null ) {
				collectionQueuedOps.sort();
			}
			if ( collectionRemovals != null ) {
				collectionRemovals.sort();
			}
		}
	}

	public void sortActions() {
		if ( isOrderUpdatesEnabled() && updates != null ) {
			// sort the updates by pk
			updates.sort();
		}
		if ( isOrderInsertsEnabled() && insertions != null ) {
			insertions.sort();
		}
	}

	private SessionFactoryOptions getSessionFactoryOptions() {
		return session.getFactory().getSessionFactoryOptions();
	}

	private boolean isOrderUpdatesEnabled() {
		return getSessionFactoryOptions().isOrderUpdatesEnabled();
	}

	private boolean isOrderInsertsEnabled() {
		return getSessionFactoryOptions().isOrderInsertsEnabled();
	}

	public void clearFromFlushNeededCheck(int previousCollectionRemovalSize) {
		if ( collectionCreations != null ) {
			collectionCreations.clear();
		}
		if ( collectionUpdates != null ) {
			collectionUpdates.clear();
		}
		if ( collectionQueuedOps != null ) {
			collectionQueuedOps.clear();
		}
		if ( updates != null) {
			updates.clear();
		}
		// collection deletions are a special case since update() can add
		// deletions of collections not loaded by the session.
		if ( collectionRemovals != null && collectionRemovals.size() > previousCollectionRemovalSize ) {
			collectionRemovals.removeLastN( collectionRemovals.size() - previousCollectionRemovalSize );
		}
	}

	public boolean hasAfterTransactionActions() {
		return !isTransactionCoordinatorShared
			&& afterTransactionProcesses != null
			&& afterTransactionProcesses.hasActions();
	}

	public boolean hasBeforeTransactionActions() {
		return !isTransactionCoordinatorShared
			&& beforeTransactionProcesses != null
			&& beforeTransactionProcesses.hasActions();
	}

	public boolean hasAnyQueuedActions() {
		return hasUnresolvedEntityInsertActions()
			|| nonempty( updates )
			|| nonempty( insertions )
			|| nonempty( deletions )
			|| nonempty( collectionUpdates )
			|| nonempty( collectionQueuedOps )
			|| nonempty( collectionRemovals )
			|| nonempty( collectionCreations );
	}

	private boolean nonempty(@Nullable ExecutableList<?> queue) {
		return queue != null && !queue.isEmpty();
	}

	public void unScheduleUnloadedDeletion(Object newEntity) {
		final var entityPersister = session.getEntityPersister( null, newEntity );
		final Object identifier = entityPersister.getIdentifier( newEntity, session );
		if ( deletions != null ) {
			for ( int i = 0; i < deletions.size(); i++ ) {
				final var action = deletions.get( i );
				if ( action.getInstance() == null
						&& action.getEntityName().equals( entityPersister.getEntityName() )
						&& entityPersister.getIdentifierMapping().areEqual( action.getId(), identifier, session ) ) {
					session.getPersistenceContextInternal()
							.removeDeletedUnloadedEntityKey( session.generateEntityKey( identifier, entityPersister ) );
					deletions.remove( i );
					return;
				}
			}
		}
		throw new AssertionFailure( "Unable to perform un-delete for unloaded entity delete " + entityPersister.getEntityName() );
	}

	public void unScheduleDeletion(EntityEntry entry, Object rescuedEntity) {
		final var lazyInitializer = extractLazyInitializer( rescuedEntity );
		if ( lazyInitializer != null && !lazyInitializer.isUninitialized() ) {
			rescuedEntity = lazyInitializer.getImplementation( session );
		}
		if ( deletions != null ) {
			for ( int i = 0; i < deletions.size(); i++ ) {
				final var action = deletions.get( i );
				if ( action.getInstance() == rescuedEntity ) {
					deletions.remove( i );
					return;
				}
			}
		}
		if ( orphanRemovals != null ) {
			for ( int i = 0; i < orphanRemovals.size(); i++ ) {
				final var action = orphanRemovals.get( i );
				if ( action.getInstance() == rescuedEntity ) {
					orphanRemovals.remove( i );
					return;
				}
			}
		}
		throw new AssertionFailure( "Unable to perform un-delete for instance " + entry.getEntityName() );
	}

	/**
	 * Used by the owning session to explicitly control serialization of the action queue
	 *
	 * @param oos The stream to which the action queue should get written
	 * @throws IOException Indicates an error writing to the stream
	 */
	public void serialize(ObjectOutputStream oos) throws IOException {
		LOG.trace( "Serializing action-queue" );
		if ( unresolvedInsertions == null ) {
			unresolvedInsertions = new UnresolvedEntityInsertActions();
		}
		unresolvedInsertions.serialize( oos );

		for ( var action : ORDERED_OPERATIONS ) {
			final var queue = action.getActions( this );
			if ( queue == null ) {
				oos.writeBoolean( false );
			}
			else {
				oos.writeBoolean( true );
				queue.writeExternal( oos );
			}
		}
	}

	/**
	 * Used by the owning session to explicitly control deserialization of the action queue.
	 *
	 * @param ois The stream from which to read the action queue
	 * @param session The session to which the action queue belongs
	 * @return The deserialized action queue
	 * @throws IOException indicates a problem reading from the stream
	 * @throws ClassNotFoundException Generally means we were unable to locate user classes.
	 */
	public static ActionQueue deserialize(ObjectInputStream ois, EventSource session)
			throws IOException, ClassNotFoundException {
		final boolean traceEnabled = LOG.isTraceEnabled();
		if ( traceEnabled ) {
			LOG.trace( "Deserializing action-queue" );
		}
		final var actionQueue = new ActionQueue( session );
		actionQueue.unresolvedInsertions = UnresolvedEntityInsertActions.deserialize( ois, session );
		for ( var action : ORDERED_OPERATIONS ) {
			final boolean notNull = ois.readBoolean();
			if ( notNull ) {
				var queue = action.getActions( actionQueue );
				if ( queue == null ) {
					// trying hard to avoid generic initializations mess
					action.ensureInitialized( actionQueue );
					queue = action.getActions( actionQueue );
				}
				queue.readExternal( ois );
				if ( traceEnabled ) {
					LOG.tracev( "Deserialized [{0}] entries", queue.size() );
				}
				queue.afterDeserialize( session );
			}
		}
		return actionQueue;
	}

	private abstract static class AbstractTransactionCompletionProcessQueue<T> {
		protected SessionImplementor session;
		// Concurrency handling required when transaction completion process
		// is dynamically registered inside the event listener (HHH-7478).
		protected ConcurrentLinkedQueue<@NonNull T> processes = new ConcurrentLinkedQueue<>();

		private AbstractTransactionCompletionProcessQueue(SessionImplementor session) {
			this.session = session;
		}

		public void register(@Nullable T process) {
			if ( process != null ) {
				processes.add( process );
			}
		}

		public boolean hasActions() {
			return !processes.isEmpty();
		}
	}

	/**
	 * Encapsulates behavior needed for before transaction processing
	 */
	private static class BeforeTransactionCompletionProcessQueue
			extends AbstractTransactionCompletionProcessQueue<BeforeTransactionCompletionProcess> {

		private BeforeTransactionCompletionProcessQueue(SessionImplementor session) {
			super( session );
		}

		public void beforeTransactionCompletion() {
			BeforeTransactionCompletionProcess process;
			while ( ( process = processes.poll() ) != null ) {
				try {
					process.doBeforeTransactionCompletion( session );
				}
				catch (HibernateException he) {
					throw he;
				}
				catch (Exception e) {
					throw new HibernateException( "Unable to perform beforeTransactionCompletion callback: " + e.getMessage(), e );
				}
			}
		}
	}

	/**
	 * Encapsulates behavior needed for after transaction processing
	 */
	private static class AfterTransactionCompletionProcessQueue
			extends AbstractTransactionCompletionProcessQueue<AfterTransactionCompletionProcess> {
		private final Set<String> querySpacesToInvalidate = new HashSet<>();

		private AfterTransactionCompletionProcessQueue(SessionImplementor session) {
			super( session );
		}

		public void addSpaceToInvalidate(String space) {
			querySpacesToInvalidate.add( space );
		}

		public void afterTransactionCompletion(boolean success) {
			AfterTransactionCompletionProcess process;
			while ( ( process = processes.poll() ) != null ) {
				try {
					process.doAfterTransactionCompletion( success, session );
				}
				catch (CacheException ce) {
					LOG.unableToReleaseCacheLock( ce );
					// continue loop
				}
				catch (Exception e) {
					throw new HibernateException( "Unable to perform afterTransactionCompletion callback: " + e.getMessage(), e );
				}
			}

			final var factory = session.getFactory();
			if ( factory.getSessionFactoryOptions().isQueryCacheEnabled() ) {
				factory.getCache().getTimestampsCache()
						.invalidate( querySpacesToInvalidate.toArray(new String[0]), session );
			}
			querySpacesToInvalidate.clear();
		}
	}

	/**
	 * Wrapper class allowing to bind the same transaction completion process queues in different sessions.
	 */
	public static class TransactionCompletionProcesses {
		private final BeforeTransactionCompletionProcessQueue beforeTransactionCompletionProcesses;
		private final AfterTransactionCompletionProcessQueue afterTransactionCompletionProcesses;

		private TransactionCompletionProcesses(
				BeforeTransactionCompletionProcessQueue beforeTransactionCompletionProcessQueue,
				AfterTransactionCompletionProcessQueue afterTransactionCompletionProcessQueue) {
			this.beforeTransactionCompletionProcesses = beforeTransactionCompletionProcessQueue;
			this.afterTransactionCompletionProcesses = afterTransactionCompletionProcessQueue;
		}
	}

	/**
	 * Order the {@link #insertions} queue such that we group inserts against the same entity together (without
	 * violating constraints). The original order is generated by cascade order, which in turn is based on the
	 * directionality of foreign-keys. So even though we will be changing the ordering here, we need to make absolutely
	 * certain that we do not circumvent this FK ordering to the extent of causing constraint violations.
	 * <p>
	 * The algorithm first discovers the transitive incoming dependencies for every insert action
	 * and groups all inserts by the entity name.
	 * Finally, it schedules these groups one by one, as long as all the dependencies of the groups are fulfilled.
	 * </p>
	 * The implementation will only produce an optimal insert order for the insert groups that can be perfectly scheduled serially.
	 * Scheduling serially means, that there is an order which doesn't violate the FK constraint dependencies.
	 * The inserts of insert groups which can't be scheduled, are going to be inserted in the original order.
	 */
	private static class InsertActionSorter implements ExecutableList.Sorter<AbstractEntityInsertAction> {
		/**
		 * Singleton access
		 */
		public static final InsertActionSorter INSTANCE = new InsertActionSorter();

		private static class InsertInfo {
			private final AbstractEntityInsertAction insertAction;
			// Inserts in this set must be executed before this insert
			private Set<InsertInfo> transitiveIncomingDependencies;
			// Child dependencies of i.e. one-to-many or inverse one-to-one
			// It's necessary to have this for unidirectional associations, to propagate incoming dependencies
			private Set<InsertInfo> outgoingDependencies;
			// The current index of the insert info within an insert schedule
			private int index;

			private InsertInfo(AbstractEntityInsertAction insertAction, int index) {
				this.insertAction = insertAction;
				this.index = index;
			}

			private void buildDirectDependencies(IdentityHashMap<Object, InsertInfo> insertInfosByEntity) {
				final Object[] propertyValues = insertAction.getState();
				final Type[] propertyTypes = insertAction.getPersister().getPropertyTypes();
				for ( int i = 0, propertyTypesLength = propertyTypes.length; i < propertyTypesLength; i++ ) {
					addDirectDependency( propertyTypes[i], propertyValues[i], insertInfosByEntity );
				}
			}

			private void propagateChildDependencies() {
				if ( outgoingDependencies != null ) {
					for ( var childDependency : outgoingDependencies ) {
						if ( childDependency.transitiveIncomingDependencies == null ) {
							childDependency.transitiveIncomingDependencies = new HashSet<>();
						}
						childDependency.transitiveIncomingDependencies.add( this );
					}
				}
			}

			private void buildTransitiveDependencies(Set<InsertInfo> visited) {
				if ( transitiveIncomingDependencies != null ) {
					visited.addAll( transitiveIncomingDependencies );
					for ( var insertInfo : transitiveIncomingDependencies.toArray( new InsertInfo[0] ) ) {
						insertInfo.addTransitiveDependencies(this, visited);
					}
					visited.clear();
				}
			}

			private void addTransitiveDependencies(InsertInfo origin, Set<InsertInfo> visited) {
				if ( transitiveIncomingDependencies != null ) {
					for ( var insertInfo : transitiveIncomingDependencies ) {
						if ( visited.add( insertInfo ) ) {
							origin.transitiveIncomingDependencies.add( insertInfo );
							insertInfo.addTransitiveDependencies( origin, visited );
						}
					}
				}
			}

			private void addDirectDependency(Type type, @Nullable Object value, IdentityHashMap<Object, InsertInfo> insertInfosByEntity) {
				if ( value != null ) {
					if ( type instanceof EntityType entityType ) {
						final InsertInfo insertInfo = insertInfosByEntity.get( value );
						if ( insertInfo != null ) {
							if ( entityType.isOneToOne()
									&& entityType.getForeignKeyDirection() == ForeignKeyDirection.TO_PARENT ) {
								if ( !entityType.isReferenceToPrimaryKey() ) {
									if ( outgoingDependencies == null ) {
										outgoingDependencies = new HashSet<>();
									}
									outgoingDependencies.add( insertInfo );
								}
							}
							else {
								if ( transitiveIncomingDependencies == null ) {
									transitiveIncomingDependencies = new HashSet<>();
								}
								transitiveIncomingDependencies.add( insertInfo );
							}
						}
					}
					else if ( type instanceof CollectionType collectionType ) {
						final var pluralAttributeMapping =
								insertAction.getSession().getFactory().getMappingMetamodel()
										.getCollectionDescriptor( collectionType.getRole() )
										.getAttributeMapping();
						// We only care about mappedBy one-to-many associations, because for these,
						// the elements depend on the collection owner
						if ( pluralAttributeMapping.getCollectionDescriptor().isOneToMany()
								&& pluralAttributeMapping.getElementDescriptor() instanceof EntityCollectionPart ) {
							final var elementsIterator = collectionType.getElementsIterator( value );
							while ( elementsIterator.hasNext() ) {
								final Object element = elementsIterator.next();
								final InsertInfo insertInfo = insertInfosByEntity.get( element );
								if ( insertInfo != null ) {
									if ( outgoingDependencies == null ) {
										outgoingDependencies = new HashSet<>();
									}
									outgoingDependencies.add( insertInfo );
								}
							}
						}
					}
					else if ( type instanceof ComponentType compositeType ) {
						// Support recursive checks of composite type properties for associations and collections.
						final var session = insertAction.getSession();
						final Object[] componentValues = compositeType.getPropertyValues( value, session );
						for ( int j = 0; j < componentValues.length; ++j ) {
							final Type componentValueType = compositeType.getSubtypes()[j];
							final Object componentValue = componentValues[j];
							addDirectDependency( componentValueType, componentValue, insertInfosByEntity );
						}
					}
				}
			}

			@Override
			public boolean equals(@Nullable Object object) {
				return this == object
					|| object instanceof InsertInfo that
						&& insertAction.equals( that.insertAction );
			}

			@Override
			public int hashCode() {
				return insertAction.hashCode();
			}

			@Override
			public String toString() {
				return "InsertInfo{" +
					"insertAction=" + insertAction +
					'}';
			}
		}

		public InsertActionSorter() {
		}

		/**
		 * Sort the insert actions.
		 */
		public void sort(List<AbstractEntityInsertAction> insertions) {
			final int insertInfoCount = insertions.size();
			// Build up dependency metadata for insert actions
			final InsertInfo[] insertInfos = new InsertInfo[insertInfoCount];
			// A map of all insert infos keyed by the entity instance
			// This is needed to discover insert infos for direct dependencies
			final IdentityHashMap<Object, InsertInfo> insertInfosByEntity =
					new IdentityHashMap<>( insertInfos.length );
			// Construct insert infos and build a map for that, keyed by entity instance
			for (int i = 0; i < insertInfoCount; i++) {
				final var insertAction = insertions.get(i);
				final var insertInfo = new InsertInfo( insertAction, i );
				insertInfosByEntity.put( insertAction.getInstance(), insertInfo );
				insertInfos[i] = insertInfo;
			}
			// First we must discover the direct dependencies
			for (int i = 0; i < insertInfoCount; i++) {
				insertInfos[i].buildDirectDependencies( insertInfosByEntity );
			}
			// Then we can propagate child dependencies to the insert infos incoming dependencies
			for (int i = 0; i < insertInfoCount; i++) {
				insertInfos[i].propagateChildDependencies();
			}
			// Finally, we add all the transitive incoming dependencies
			// and then group insert infos into EntityInsertGroup keyed by entity name
			final Set<InsertInfo> visited = new HashSet<>();
			final Map<String, EntityInsertGroup> insertInfosByEntityName = new LinkedHashMap<>();
			for (int i = 0; i < insertInfoCount; i++) {
				final InsertInfo insertInfo = insertInfos[i];
				insertInfo.buildTransitiveDependencies( visited );

				final String entityName = insertInfo.insertAction.getPersister().getEntityName();
				var entityInsertGroup = insertInfosByEntityName.get( entityName );
				if (entityInsertGroup == null) {
					entityInsertGroup = new EntityInsertGroup( entityName );
					insertInfosByEntityName.put( entityName, entityInsertGroup );
				}
				entityInsertGroup.add( insertInfo );
			}
			// Now we can go through the EntityInsertGroups and schedule all the ones
			// for which we have already scheduled all the dependentEntityNames
			final Set<String> scheduledEntityNames = new HashSet<>( insertInfosByEntityName.size() );
			int schedulePosition = 0;
			int lastScheduleSize;
			do {
				lastScheduleSize = scheduledEntityNames.size();
				final var iterator = insertInfosByEntityName.values().iterator();
				while ( iterator.hasNext() ) {
					final var insertGroup = iterator.next();
					if ( scheduledEntityNames.containsAll( insertGroup.dependentEntityNames) ) {
						schedulePosition = schedule( insertInfos, insertGroup.insertInfos, schedulePosition );
						scheduledEntityNames.add( insertGroup.entityName );
						iterator.remove();
					}
				}
				// we try to schedule entity groups over and over again, until we can't schedule any further
			}
			while ( lastScheduleSize != scheduledEntityNames.size() );
			if ( !insertInfosByEntityName.isEmpty() ) {
				LOG.warn( "The batch containing " + insertions.size() + " statements could not be sorted. "
						+ "This might indicate a circular entity relationship.");
			}
			insertions.clear();
			for ( InsertInfo insertInfo : insertInfos ) {
				insertions.add( insertInfo.insertAction );
			}
		}

		private int schedule(InsertInfo [] insertInfos, List<InsertInfo> insertInfosToSchedule, int schedulePosition) {
			final InsertInfo[] newInsertInfos = new InsertInfo[insertInfos.length];
			// The bitset is there to quickly query if an index is already scheduled
			final BitSet bitSet = new BitSet( insertInfos.length );
			// Remember the smallest index of the insertInfosToSchedule to check if we actually need to reorder anything
			int smallestScheduledIndex = -1;
			// The biggestScheduledIndex is needed as upper bound for shifting elements that were replaced by insertInfosToSchedule
			int biggestScheduledIndex = -1;
			for ( int i = 0; i < insertInfosToSchedule.size(); i++ ) {
				final int index = insertInfosToSchedule.get(i).index;
				bitSet.set(index);
				smallestScheduledIndex = Math.min( smallestScheduledIndex, index );
				biggestScheduledIndex = Math.max( biggestScheduledIndex, index );
			}
			final int nextSchedulePosition = schedulePosition + insertInfosToSchedule.size();
			if ( smallestScheduledIndex == schedulePosition && biggestScheduledIndex == nextSchedulePosition ) {
				// In this case, the order is already correct and we can skip some copying
				return nextSchedulePosition;
			}
			// The index to which we start to shift elements that appear within the range of [schedulePosition, nextSchedulePosition)
			int shiftSchedulePosition = nextSchedulePosition;
			for (int i = 0; i < insertInfosToSchedule.size(); i++) {
				final InsertInfo insertInfoToSchedule = insertInfosToSchedule.get(i);
				final int targetSchedulePosition = schedulePosition + i;
				newInsertInfos[targetSchedulePosition] = insertInfoToSchedule;
				insertInfoToSchedule.index = targetSchedulePosition;
				final InsertInfo oldInsertInfo = insertInfos[targetSchedulePosition];
				// Move the insert info previously located at the target schedule position to the current shift position
				if ( !bitSet.get( targetSchedulePosition ) ) {
					oldInsertInfo.index = shiftSchedulePosition;
					// Also set this index in the bitset to skip copying the value later, as it is considered scheduled
					bitSet.set( targetSchedulePosition );
					newInsertInfos[shiftSchedulePosition++]= oldInsertInfo;
				}
			}
			// We have to shift all the elements up to the biggestMovedIndex + 1
			biggestScheduledIndex++;
			for ( int i = bitSet.nextClearBit(schedulePosition); i < biggestScheduledIndex; i++ ) {
				// Only copy the old insert info over if it wasn't already scheduled
				if ( !bitSet.get(i) ) {
					final InsertInfo insertInfo = insertInfos[i];
					insertInfo.index = shiftSchedulePosition;
					newInsertInfos[shiftSchedulePosition++] = insertInfo;
				}
			}
			// Copy over the newly reordered array part into the main array
			System.arraycopy( newInsertInfos, schedulePosition, insertInfos, schedulePosition,
					biggestScheduledIndex - schedulePosition );
			return nextSchedulePosition;
		}

		public static class EntityInsertGroup {
			private final String entityName;
			private final List<InsertInfo> insertInfos = new ArrayList<>();
			private final Set<String> dependentEntityNames = new HashSet<>();

			public EntityInsertGroup(String entityName) {
				this.entityName = entityName;
			}

			public void add(InsertInfo insertInfo) {
				insertInfos.add(insertInfo);
				if ( insertInfo.transitiveIncomingDependencies != null ) {
					for ( InsertInfo dependency : insertInfo.transitiveIncomingDependencies ) {
						dependentEntityNames.add( dependency.insertAction.getEntityName() );
					}
				}
			}

			@Override
			public String toString() {
				return "EntityInsertGroup{" +
					"entityName='" + entityName + '\'' +
					'}';
			}
		}

	}

}
