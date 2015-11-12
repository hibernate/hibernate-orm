/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.PropertyValueException;
import org.hibernate.action.internal.AbstractEntityInsertAction;
import org.hibernate.action.internal.BulkOperationCleanupAction;
import org.hibernate.action.internal.CollectionRecreateAction;
import org.hibernate.action.internal.CollectionRemoveAction;
import org.hibernate.action.internal.CollectionUpdateAction;
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
import org.hibernate.cache.CacheException;
import org.hibernate.engine.internal.NonNullableTransientDependencies;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.type.Type;

/**
 * Responsible for maintaining the queue of actions related to events.
 *
 * The ActionQueue holds the DML operations queued as part of a session's transactional-write-behind semantics. The
 * DML operations are queued here until a flush forces them to be executed against the database.
 * 
 * @author Steve Ebersole
 * @author Gail Badner
 * @author Anton Marsden
 */
public class ActionQueue {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( ActionQueue.class );

	private SessionImplementor session;

	private UnresolvedEntityInsertActions unresolvedInsertions;

	// Object insertions, updates, and deletions have list semantics because
	// they must happen in the right order so as to respect referential
	// integrity
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
	
	// TODO: The removeOrphan concept is a temporary "hack" for HHH-6484.  This should be removed once action/task
	// ordering is improved.
	private ExecutableList<OrphanRemovalAction> orphanRemovals;


	private transient boolean isTransactionCoordinatorShared;
	private AfterTransactionCompletionProcessQueue afterTransactionProcesses;
	private BeforeTransactionCompletionProcessQueue beforeTransactionProcesses;

	/**
	 * An array containing providers for all the ExecutableLists in execution order
	 */
	private static final ListProvider[] EXECUTABLE_LISTS;

	static {
		EXECUTABLE_LISTS = new ListProvider[8];
		EXECUTABLE_LISTS[0] = new ListProvider() {
			ExecutableList<?> get(ActionQueue instance) {
				return instance.orphanRemovals;
			}

			ExecutableList<?> init(ActionQueue instance) {
				return instance.orphanRemovals = new ExecutableList<OrphanRemovalAction>();
			}
		};
		EXECUTABLE_LISTS[1] = new ListProvider() {
			ExecutableList<?> get(ActionQueue instance) {
				return instance.insertions;
			}

			ExecutableList<?> init(ActionQueue instance) {
				return instance.insertions = new ExecutableList<AbstractEntityInsertAction>( new InsertActionSorter() );
			}
		};
		EXECUTABLE_LISTS[2] = new ListProvider() {
			ExecutableList<?> get(ActionQueue instance) {
				return instance.updates;
			}

			ExecutableList<?> init(ActionQueue instance) {
				return instance.updates = new ExecutableList<EntityUpdateAction>();
			}
		};
		EXECUTABLE_LISTS[3] = new ListProvider() {
			ExecutableList<?> get(ActionQueue instance) {
				return instance.collectionQueuedOps;
			}

			ExecutableList<?> init(ActionQueue instance) {
				return instance.collectionQueuedOps = new ExecutableList<QueuedOperationCollectionAction>();
			}
		};
		EXECUTABLE_LISTS[4] = new ListProvider() {
			ExecutableList<?> get(ActionQueue instance) {
				return instance.collectionRemovals;
			}

			ExecutableList<?> init(ActionQueue instance) {
				return instance.collectionRemovals = new ExecutableList<CollectionRemoveAction>();
			}
		};
		EXECUTABLE_LISTS[5] = new ListProvider() {
			ExecutableList<?> get(ActionQueue instance) {
				return instance.collectionUpdates;
			}

			ExecutableList<?> init(ActionQueue instance) {
				return instance.collectionUpdates = new ExecutableList<CollectionUpdateAction>();
			}
		};
		EXECUTABLE_LISTS[6] = new ListProvider() {
			ExecutableList<?> get(ActionQueue instance) {
				return instance.collectionCreations;
			}

			ExecutableList<?> init(ActionQueue instance) {
				return instance.collectionCreations = new ExecutableList<CollectionRecreateAction>();
			}
		};
		EXECUTABLE_LISTS[7] = new ListProvider() {
			ExecutableList<?> get(ActionQueue instance) {
				return instance.deletions;
			}

			ExecutableList<?> init(ActionQueue instance) {
				return instance.deletions = new ExecutableList<EntityDeleteAction>();
			}
		};
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
		for ( int i = 0; i < EXECUTABLE_LISTS.length; ++i ) {
			ExecutableList<?> l = EXECUTABLE_LISTS[i].get(this);
			if( l != null ) {
				l.clear();
			}
		}
		if( unresolvedInsertions != null ) {
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
		NonNullableTransientDependencies nonNullableTransientDependencies = insert.findNonNullableTransientEntities();
		if ( nonNullableTransientDependencies == null ) {
			LOG.tracev( "Adding insert with no non-nullable, transient entities: [{0}]", insert );
			addResolvedEntityInsertAction( insert );
		}
		else {
			if ( LOG.isTraceEnabled() ) {
				LOG.tracev( "Adding insert with non-nullable, transient entities; insert=[{0}], dependencies=[{1}]", insert,
							nonNullableTransientDependencies.toLoggableString( insert.getSession() ) );
			}
			if( unresolvedInsertions == null ) {
				unresolvedInsertions = new UnresolvedEntityInsertActions();
			}
			unresolvedInsertions.addUnresolvedEntityInsertAction( insert, nonNullableTransientDependencies );
		}
	}

	private void addResolvedEntityInsertAction(AbstractEntityInsertAction insert) {
		if ( insert.isEarlyInsert() ) {
			LOG.trace( "Executing insertions before resolved early-insert" );
			executeInserts();
			LOG.debug( "Executing identity-insert immediately" );
			execute( insert );
		}
		else {
			LOG.trace( "Adding resolved non-early insert action." );
			if( insertions == null ) {
				insertions = new ExecutableList<AbstractEntityInsertAction>( new InsertActionSorter() );
			}
			insertions.add(insert);
		}
		insert.makeEntityManaged();
		if( unresolvedInsertions != null ) {
			for (AbstractEntityInsertAction resolvedAction : unresolvedInsertions.resolveDependentActions(insert.getInstance(), session)) {
				addResolvedEntityInsertAction(resolvedAction);
			}
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
		if( deletions == null ) {
			deletions = new ExecutableList<EntityDeleteAction>();
		}
		deletions.add( action );
	}

	/**
	 * Adds an orphan removal action
	 *
	 * @param action The action representing the orphan removal
	 */
	public void addAction(OrphanRemovalAction action) {
		if( orphanRemovals == null ) {
			orphanRemovals = new ExecutableList<OrphanRemovalAction>();
		}
		orphanRemovals.add( action );
	}

	/**
	 * Adds an entity update action
	 *
	 * @param action The action representing the entity update
	 */
	public void addAction(EntityUpdateAction action) {
		if( updates == null ) {
			updates = new ExecutableList<EntityUpdateAction>();
		}
		updates.add( action );
	}

	/**
	 * Adds a collection (re)create action
	 *
	 * @param action The action representing the (re)creation of a collection
	 */
	public void addAction(CollectionRecreateAction action) {
		if( collectionCreations == null) {
			collectionCreations = new ExecutableList<CollectionRecreateAction>();
		}
		collectionCreations.add( action );
	}

	/**
	 * Adds a collection remove action
	 *
	 * @param action The action representing the removal of a collection
	 */
	public void addAction(CollectionRemoveAction action) {
		if( collectionRemovals == null ) {
			collectionRemovals = new ExecutableList<CollectionRemoveAction>();
		}
		collectionRemovals.add( action );
	}

	/**
	 * Adds a collection update action
	 *
	 * @param action The action representing the update of a collection
	 */
	public void addAction(CollectionUpdateAction action) {
		if( collectionUpdates == null ) {
			collectionUpdates = new ExecutableList<CollectionUpdateAction>();
		}
		collectionUpdates.add( action );
	}

	/**
	 * Adds an action relating to a collection queued operation (extra lazy).
	 *
	 * @param action The action representing the queued operation
	 */
	public void addAction(QueuedOperationCollectionAction action) {
		if( collectionQueuedOps == null) {
			collectionQueuedOps = new ExecutableList<QueuedOperationCollectionAction>();
		}
		collectionQueuedOps.add( action );
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
		if( executable.getBeforeTransactionCompletionProcess() != null ) {
			if( beforeTransactionProcesses == null ) {
				beforeTransactionProcesses = new BeforeTransactionCompletionProcessQueue( session );
			}
			beforeTransactionProcesses.register(executable.getBeforeTransactionCompletionProcess());
		}
		if ( session.getFactory().getSessionFactoryOptions().isQueryCacheEnabled() ) {
			invalidateSpaces( executable.getPropertySpaces() );
		}
		if( executable.getAfterTransactionCompletionProcess() != null ) {
			if( afterTransactionProcesses == null ) {
				afterTransactionProcesses = new AfterTransactionCompletionProcessQueue( session );
			}
			afterTransactionProcesses.register(executable.getAfterTransactionCompletionProcess());
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
	 * Throws {@link org.hibernate.PropertyValueException} if there are any unresolved entity insert actions that depend
	 * on non-nullable associations with a transient entity. This method should be called on completion of an operation
	 * (after all cascades are completed) that saves an entity.
	 * 
	 * @throws org.hibernate.PropertyValueException if there are any unresolved entity insert actions;
	 * {@link org.hibernate.PropertyValueException#getEntityName()} and
	 * {@link org.hibernate.PropertyValueException#getPropertyName()} will return the entity name and property value for
	 * the first unresolved entity insert action.
	 */
	public void checkNoUnresolvedActionsAfterOperation() throws PropertyValueException {
		if(unresolvedInsertions != null) {
			unresolvedInsertions.checkNoUnresolvedActionsAfterOperation();
		}
	}

	public void registerProcess(AfterTransactionCompletionProcess process) {
		if( afterTransactionProcesses == null ) {
			afterTransactionProcesses = new AfterTransactionCompletionProcessQueue( session );
		}
		afterTransactionProcesses.register( process );
	}

	public void registerProcess(BeforeTransactionCompletionProcess process) {
		if( beforeTransactionProcesses == null ) {
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
			throw new IllegalStateException( "About to execute actions, but there are unresolved entity insert actions." );
		}

		for ( int i = 0; i < EXECUTABLE_LISTS.length; ++i ) {
			ExecutableList<?> l = EXECUTABLE_LISTS[i].get(this);
			if ( l != null && !l.isEmpty() ) {
				executeActions( l );
			}
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

	private void prepareActions(ExecutableList<?> queue) throws HibernateException {
		if( queue == null ) {
			return;
		}
		for ( Executable executable : queue ) {
			executable.beforeExecutions();
		}
	}

	/**
	 * Performs cleanup of any held cache softlocks.
	 * 
	 * @param success Was the transaction successful.
	 */
	public void afterTransactionCompletion(boolean success) {
		if ( !isTransactionCoordinatorShared ) {
			// Execute completion actions only in transaction owner (aka parent session).
			if( afterTransactionProcesses != null ) {
				afterTransactionProcesses.afterTransactionCompletion(success);
			}
		}
	}

	/**
	 * Execute any registered {@link org.hibernate.action.spi.BeforeTransactionCompletionProcess}
	 */
	public void beforeTransactionCompletion() {
		if ( !isTransactionCoordinatorShared ) {
			// Execute completion actions only in transaction owner (aka parent session).
			if( beforeTransactionProcesses != null ) {
				beforeTransactionProcesses.beforeTransactionCompletion();
			}
		}
	}

	/**
	 * Check whether any insertion or deletion actions are currently queued.
	 *
	 * @return {@code true} if insertions or deletions are currently queued; {@code false} otherwise.
	 */
	public boolean areInsertionsOrDeletionsQueued() {
		return ( insertions != null && !insertions.isEmpty() ) || hasUnresolvedEntityInsertActions() || (deletions != null && !deletions.isEmpty()) || (orphanRemovals != null && !orphanRemovals.isEmpty());
	}

	/**
	 * Check whether the given tables/query-spaces are to be executed against given the currently queued actions.
	 * 
	 * @param tables The table/query-spaces to check.
	 *
	 * @return {@code true} if we contain pending actions against any of the given tables; {@code false} otherwise.
	 */
	public boolean areTablesToBeUpdated(@SuppressWarnings("rawtypes") Set tables) {
		if ( tables.isEmpty() ) {
			return false;
		}
		for ( int i = 0; i < EXECUTABLE_LISTS.length; ++i ) {
			ExecutableList<?> l = EXECUTABLE_LISTS[i].get(this);
			if ( areTablesToBeUpdated(l, tables) ) {
				return true;
			}
		}
		if(unresolvedInsertions == null) {
			return false;
		}
		return areTablesToBeUpdated( unresolvedInsertions, tables );
	}

	private static boolean areTablesToBeUpdated(ExecutableList<?> actions, @SuppressWarnings("rawtypes") Set tableSpaces) {
		if ( actions == null || actions.isEmpty() ) {
			return false;
		}

		for ( Serializable actionSpace : actions.getQuerySpaces() ) {
			if ( tableSpaces.contains( actionSpace ) ) {
				LOG.debugf( "Changes must be flushed to space: %s", actionSpace );
				return true;
			}
		}

		return false;
	}

	private static boolean areTablesToBeUpdated(UnresolvedEntityInsertActions actions, @SuppressWarnings("rawtypes") Set tableSpaces) {
		for ( Executable action : actions.getDependentEntityInsertActions() ) {
			final Serializable[] spaces = action.getPropertySpaces();
			for ( Serializable space : spaces ) {
				if ( tableSpaces.contains( space ) ) {
					LOG.debugf( "Changes must be flushed to space: %s", space );
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Perform {@link org.hibernate.action.spi.Executable#execute()} on each element of the list
	 * 
	 * @param list The list of Executable elements to be performed
	 *
	 * @throws HibernateException
	 */
	private <E extends Executable & Comparable<?> & Serializable> void executeActions(ExecutableList<E> list) throws HibernateException {
		// todo : consider ways to improve the double iteration of Executables here:
		//		1) we explicitly iterate list here to perform Executable#execute()
		//		2) ExecutableList#getQuerySpaces also iterates the Executables to collect query spaces.
		try {
			for ( E e : list ) {
				try {
					e.execute();
				}
				finally {
					if( e.getBeforeTransactionCompletionProcess() != null ) {
						if( beforeTransactionProcesses == null ) {
							beforeTransactionProcesses = new BeforeTransactionCompletionProcessQueue( session );
						}
						beforeTransactionProcesses.register(e.getBeforeTransactionCompletionProcess());
					}
					if( e.getAfterTransactionCompletionProcess() != null ) {
						if( afterTransactionProcesses == null ) {
							afterTransactionProcesses = new AfterTransactionCompletionProcessQueue( session );
						}
						afterTransactionProcesses.register(e.getAfterTransactionCompletionProcess());
					}
				}
			}
		}
		finally {
			if ( session.getFactory().getSessionFactoryOptions().isQueryCacheEnabled() ) {
				// Strictly speaking, only a subset of the list may have been processed if a RuntimeException occurs.
				// We still invalidate all spaces. I don't see this as a big deal - after all, RuntimeExceptions are
				// unexpected.
				Set<Serializable> propertySpaces = list.getQuerySpaces();
				invalidateSpaces( propertySpaces.toArray( new Serializable[propertySpaces.size()] ) );
			}
		}

		list.clear();
		session.getJdbcCoordinator().executeBatch();
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
	private void invalidateSpaces(Serializable... spaces) {
		if ( spaces != null && spaces.length > 0 ) {
			for ( Serializable s : spaces ) {
				if( afterTransactionProcesses == null ) {
					afterTransactionProcesses = new AfterTransactionCompletionProcessQueue( session );
				}
				afterTransactionProcesses.addSpaceToInvalidate( (String) s );
			}
			// Performance win: If we are processing an ExecutableList, this will only be called once
			session.getFactory().getUpdateTimestampsCache().preInvalidate( spaces, session );
		}
	}

	/**
	 * Returns a string representation of the object.
	 * 
	 * @return a string representation of the object.
	 */
	@Override
	public String toString() {
		return "ActionQueue[insertions=" + toString(insertions)
				+ " updates=" + toString(updates)
				+ " deletions=" + toString(deletions)
				+ " orphanRemovals=" + toString(orphanRemovals)
				+ " collectionCreations=" + toString(collectionCreations)
				+ " collectionRemovals=" + toString(collectionRemovals)
				+ " collectionUpdates=" + toString(collectionUpdates)
				+ " collectionQueuedOps=" + toString(collectionQueuedOps)
				+ " unresolvedInsertDependencies=" + unresolvedInsertions
				+ "]";
	}

	private static String toString(ExecutableList q) {
		return q == null ? "ExecutableList{size=0}" : q.toString();
	}

	public int numberOfCollectionRemovals() {
		if( collectionRemovals == null ) {
			return 0;
		}
		return collectionRemovals.size();
	}

	public int numberOfCollectionUpdates() {
		if( collectionUpdates == null ) {
			return 0;
		}
		return collectionUpdates.size();
	}

	public int numberOfCollectionCreations() {
		if( collectionCreations == null ) {
			return 0;
		}
		return collectionCreations.size();
	}

	public int numberOfDeletions() {
		int del = deletions == null ? 0 : deletions.size();
		int orph = orphanRemovals == null ? 0 : orphanRemovals.size();
		return del + orph;
	}

	public int numberOfUpdates() {
		if( updates == null ) {
			return 0;
		}
		return updates.size();
	}

	public int numberOfInsertions() {
		if( insertions == null ) {
			return 0;
		}
		return insertions.size();
	}

	public TransactionCompletionProcesses getTransactionCompletionProcesses() {
		if( beforeTransactionProcesses == null ) {
			beforeTransactionProcesses = new BeforeTransactionCompletionProcessQueue( session );
		}
		if( afterTransactionProcesses == null ) {
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
	public void setTransactionCompletionProcesses(TransactionCompletionProcesses processes, boolean isTransactionCoordinatorShared) {
		this.isTransactionCoordinatorShared = isTransactionCoordinatorShared;
		this.beforeTransactionProcesses = processes.beforeTransactionCompletionProcesses;
		this.afterTransactionProcesses = processes.afterTransactionCompletionProcesses;
	}

	public void sortCollectionActions() {
		if ( session.getFactory().getSessionFactoryOptions().isOrderUpdatesEnabled() ) {
			// sort the updates by fk
			if( collectionCreations != null ) {
				collectionCreations.sort();
			}
			if( collectionUpdates != null ) {
				collectionUpdates.sort();
			}
			if( collectionQueuedOps != null ) {
				collectionQueuedOps.sort();
			}
			if( collectionRemovals != null ) {
				collectionRemovals.sort();
			}
		}
	}

	public void sortActions() {
		if ( session.getFactory().getSessionFactoryOptions().isOrderUpdatesEnabled() && updates != null ) {
			// sort the updates by pk
			updates.sort();
		}
		if ( session.getFactory().getSessionFactoryOptions().isOrderInsertsEnabled() && insertions != null ) {
			insertions.sort();
		}
	}

	public void clearFromFlushNeededCheck(int previousCollectionRemovalSize) {
		if( collectionCreations != null ) {
			collectionCreations.clear();
		}
		if( collectionUpdates != null ) {
			collectionUpdates.clear();
		}
		if( collectionQueuedOps != null ) {
			collectionQueuedOps.clear();
		}
		if( updates != null) {
			updates.clear();
		}
		// collection deletions are a special case since update() can add
		// deletions of collections not loaded by the session.
		if ( collectionRemovals != null && collectionRemovals.size() > previousCollectionRemovalSize ) {
			collectionRemovals.removeLastN( collectionRemovals.size() - previousCollectionRemovalSize );
		}
	}

	@SuppressWarnings("SimplifiableConditionalExpression")
	public boolean hasAfterTransactionActions() {
		return isTransactionCoordinatorShared ? false : afterTransactionProcesses != null && afterTransactionProcesses.hasActions();
	}

	@SuppressWarnings("SimplifiableConditionalExpression")
	public boolean hasBeforeTransactionActions() {
		return isTransactionCoordinatorShared ? false : beforeTransactionProcesses != null && beforeTransactionProcesses.hasActions();
	}

	public boolean hasAnyQueuedActions() {
		return ( updates != null && !updates.isEmpty() ) || ( insertions != null && !insertions.isEmpty() ) || hasUnresolvedEntityInsertActions()
				|| ( deletions != null && !deletions.isEmpty()) || ( collectionUpdates != null && !collectionUpdates.isEmpty() )
				|| ( collectionQueuedOps != null && !collectionQueuedOps.isEmpty() ) || ( collectionRemovals != null && !collectionRemovals.isEmpty() )
				|| ( collectionCreations != null && !collectionCreations.isEmpty() );
	}

	public void unScheduleDeletion(EntityEntry entry, Object rescuedEntity) {
		if ( rescuedEntity instanceof HibernateProxy ) {
			LazyInitializer initializer = ( ( HibernateProxy ) rescuedEntity ).getHibernateLazyInitializer();
			if ( !initializer.isUninitialized() ) {
				rescuedEntity = initializer.getImplementation( session );
			}
		}
		if( deletions != null ) {
			for ( int i = 0; i < deletions.size(); i++ ) {
				EntityDeleteAction action = deletions.get(i);
				if (action.getInstance() == rescuedEntity) {
					deletions.remove(i);
					return;
				}
			}
		}
		if( orphanRemovals != null ) {
			for ( int i = 0; i < orphanRemovals.size(); i++ ) {
				EntityDeleteAction action = orphanRemovals.get(i);
				if (action.getInstance() == rescuedEntity) {
					orphanRemovals.remove(i);
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
		if( unresolvedInsertions == null ) {
			unresolvedInsertions = new UnresolvedEntityInsertActions();
		}
		unresolvedInsertions.serialize( oos );

		for ( ListProvider p : EXECUTABLE_LISTS ) {
			ExecutableList<?> l = p.get(this);
			if( l == null ) {
				oos.writeBoolean(false);
			}
			else {
				oos.writeBoolean( true );
				l.writeExternal( oos );
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
	public static ActionQueue deserialize(ObjectInputStream ois, SessionImplementor session) throws IOException, ClassNotFoundException {
		final boolean traceEnabled = LOG.isTraceEnabled();
		if ( traceEnabled ) {
			LOG.trace("Deserializing action-queue");
		}
		ActionQueue rtn = new ActionQueue( session );

		rtn.unresolvedInsertions = UnresolvedEntityInsertActions.deserialize( ois, session );

		for ( int i = 0; i < EXECUTABLE_LISTS.length; ++i ) {
			ListProvider provider = EXECUTABLE_LISTS[i];
			ExecutableList<?> l = provider.get(rtn);
			boolean notNull = ois.readBoolean();
			if( notNull ) {
				if(l == null) {
					l = provider.init(rtn);
				}
				l.readExternal( ois );

				if ( traceEnabled ) {
					LOG.tracev( "Deserialized [{0}] entries", l.size() );
				}
				l.afterDeserialize( session );
			}
		}

		return rtn;
	}

	private static abstract class AbstractTransactionCompletionProcessQueue<T> {
		protected SessionImplementor session;
		// Concurrency handling required when transaction completion process is dynamically registered
		// inside event listener (HHH-7478).
		protected Queue<T> processes = new ConcurrentLinkedQueue<T>();

		private AbstractTransactionCompletionProcessQueue(SessionImplementor session) {
			this.session = session;
		}

		public void register(T process) {
			if ( process == null ) {
				return;
			}
			processes.add( process );
		}

		public boolean hasActions() {
			return !processes.isEmpty();
		}
	}

	/**
	 * Encapsulates behavior needed for before transaction processing
	 */
	private static class BeforeTransactionCompletionProcessQueue extends AbstractTransactionCompletionProcessQueue<BeforeTransactionCompletionProcess> {
		private BeforeTransactionCompletionProcessQueue(SessionImplementor session) {
			super( session );
		}

		public void beforeTransactionCompletion() {
			while ( !processes.isEmpty() ) {
				try {
					processes.poll().doBeforeTransactionCompletion( session );
				}
				catch (HibernateException he) {
					throw he;
				}
				catch (Exception e) {
					throw new AssertionFailure( "Unable to perform beforeTransactionCompletion callback", e );
				}
			}
		}
	}

	/**
	 * Encapsulates behavior needed for after transaction processing
	 */
	private static class AfterTransactionCompletionProcessQueue extends AbstractTransactionCompletionProcessQueue<AfterTransactionCompletionProcess> {
		private Set<String> querySpacesToInvalidate = new HashSet<String>();

		private AfterTransactionCompletionProcessQueue(SessionImplementor session) {
			super( session );
		}

		public void addSpaceToInvalidate(String space) {
			querySpacesToInvalidate.add( space );
		}

		public void afterTransactionCompletion(boolean success) {
			while ( !processes.isEmpty() ) {
				try {
					processes.poll().doAfterTransactionCompletion( success, session );
				}
				catch (CacheException ce) {
					LOG.unableToReleaseCacheLock( ce );
					// continue loop
				}
				catch (Exception e) {
					throw new AssertionFailure( "Exception releasing cache locks", e );
				}
			}

			if ( session.getFactory().getSessionFactoryOptions().isQueryCacheEnabled() ) {
				session.getFactory().getUpdateTimestampsCache().invalidate(
						querySpacesToInvalidate.toArray( new String[querySpacesToInvalidate.size()] ),
						session
				);
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
	 * Sorts the insert actions using more hashes.
	 * </p>
	 * NOTE: this class is not thread-safe.
	 * 
	 * @author Jay Erb
	 */
	private static class InsertActionSorter implements ExecutableList.Sorter<AbstractEntityInsertAction> {
		/**
		 * Singleton access
		 */
		public static final InsertActionSorter INSTANCE = new InsertActionSorter();

		// the mapping of entity names to their latest batch numbers.
		private Map<String, Integer> latestBatches;
		private Map<Object, Integer> entityBatchNumber;

		// the map of batch numbers to EntityInsertAction lists
		private Map<Integer, List<AbstractEntityInsertAction>> actionBatches;

		public InsertActionSorter() {
		}

		/**
		 * Sort the insert actions.
		 */
		public void sort(List<AbstractEntityInsertAction> insertions) {
			// optimize the hash size to eliminate a rehash.
			this.latestBatches = new HashMap<String, Integer>();
			this.entityBatchNumber = new HashMap<Object, Integer>( insertions.size() + 1, 1.0f );
			this.actionBatches = new HashMap<Integer, List<AbstractEntityInsertAction>>();

			// the list of entity names that indicate the batch number
			for ( AbstractEntityInsertAction action : insertions ) {
				// remove the current element from insertions. It will be added back later.
				String entityName = action.getEntityName();

				// the entity associated with the current action.
				Object currentEntity = action.getInstance();

				Integer batchNumber;
				if ( latestBatches.containsKey( entityName ) ) {
					// There is already an existing batch for this type of entity.
					// Check to see if the latest batch is acceptable.
					batchNumber = findBatchNumber( action, entityName );
				}
				else {
					// add an entry for this type of entity.
					// we can be assured that all referenced entities have already
					// been processed,
					// so specify that this entity is with the latest batch.
					// doing the batch number before adding the name to the list is
					// a faster way to get an accurate number.

					batchNumber = actionBatches.size();
					latestBatches.put( entityName, batchNumber );
				}
				entityBatchNumber.put( currentEntity, batchNumber );
				addToBatch( batchNumber, action );
			}
			insertions.clear();

			// now rebuild the insertions list. There is a batch for each entry in the name list.
			for ( int i = 0; i < actionBatches.size(); i++ ) {
				List<AbstractEntityInsertAction> batch = actionBatches.get( i );
				insertions.addAll( batch );
			}
		}

		/**
		 * Finds an acceptable batch for this entity to be a member as part of the {@link InsertActionSorter}
		 * 
		 * @param action The action being sorted
		 * @param entityName The name of the entity affected by the action
		 * @return An appropriate batch number; todo document this process better
		 */
		private Integer findBatchNumber(AbstractEntityInsertAction action, String entityName) {
			// loop through all the associated entities and make sure they have been
			// processed before the latest
			// batch associated with this entity type.

			// the current batch number is the latest batch for this entity type.
			Integer latestBatchNumberForType = latestBatches.get( entityName );

			// loop through all the associations of the current entity and make sure that they are processed
			// before the current batch number
			Object[] propertyValues = action.getState();
			Type[] propertyTypes = action.getPersister().getClassMetadata().getPropertyTypes();

			for ( int i = 0; i < propertyValues.length; i++ ) {
				Object value = propertyValues[i];
				Type type = propertyTypes[i];
				if ( type.isEntityType() && value != null ) {
					// find the batch number associated with the current association, if any.
					Integer associationBatchNumber = entityBatchNumber.get( value );
					if ( associationBatchNumber != null && associationBatchNumber.compareTo( latestBatchNumberForType ) > 0 ) {
						// create a new batch for this type. The batch number is the number of current batches.
						latestBatchNumberForType = actionBatches.size();
						latestBatches.put( entityName, latestBatchNumberForType );
						// since this entity will now be processed in the latest possible batch,
						// we can be assured that it will come after all other associations,
						// there's not need to continue checking.
						break;
					}
				}
			}
			return latestBatchNumberForType;
		}

		private void addToBatch(Integer batchNumber, AbstractEntityInsertAction action) {
			List<AbstractEntityInsertAction> actions = actionBatches.get( batchNumber );

			if ( actions == null ) {
				actions = new LinkedList<AbstractEntityInsertAction>();
				actionBatches.put( batchNumber, actions );
			}
			actions.add( action );
		}

	}


	private static abstract class ListProvider {
		abstract ExecutableList<?> get(ActionQueue instance);
		abstract ExecutableList<?> init(ActionQueue instance);
	}
}
