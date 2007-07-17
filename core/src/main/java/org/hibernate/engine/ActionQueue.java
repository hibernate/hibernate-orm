// $Id: ActionQueue.java 11402 2007-04-11 14:24:35Z steve.ebersole@jboss.com $
package org.hibernate.engine;

import org.hibernate.action.EntityInsertAction;
import org.hibernate.action.EntityDeleteAction;
import org.hibernate.action.Executable;
import org.hibernate.action.EntityUpdateAction;
import org.hibernate.action.CollectionRecreateAction;
import org.hibernate.action.CollectionRemoveAction;
import org.hibernate.action.CollectionUpdateAction;
import org.hibernate.action.EntityIdentityInsertAction;
import org.hibernate.action.BulkOperationCleanupAction;
import org.hibernate.HibernateException;
import org.hibernate.AssertionFailure;
import org.hibernate.cache.CacheException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.Iterator;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.ObjectOutputStream;

/**
 * Responsible for maintaining the queue of actions related to events.
 * </p>
 * The ActionQueue holds the DML operations queued as part of a session's
 * transactional-write-behind semantics.  DML operations are queued here
 * until a flush forces them to be executed against the database.
 *
 * @author Steve Ebersole
 */
public class ActionQueue {

	private static final Log log = LogFactory.getLog( ActionQueue.class );
	private static final int INIT_QUEUE_LIST_SIZE = 5;

	private SessionImplementor session;

	// Object insertions, updates, and deletions have list semantics because
	// they must happen in the right order so as to respect referential
	// integrity
	private ArrayList insertions;
	private ArrayList deletions;
	private ArrayList updates;
	// Actually the semantics of the next three are really "Bag"
	// Note that, unlike objects, collection insertions, updates,
	// deletions are not really remembered between flushes. We
	// just re-use the same Lists for convenience.
	private ArrayList collectionCreations;
	private ArrayList collectionUpdates;
	private ArrayList collectionRemovals;

	private ArrayList executions;

	/**
	 * Constructs an action queue bound to the given session.
	 *
	 * @param session The session "owning" this queue.
	 */
	public ActionQueue(SessionImplementor session) {
		this.session = session;
		init();
	}

	private void init() {
		insertions = new ArrayList( INIT_QUEUE_LIST_SIZE );
		deletions = new ArrayList( INIT_QUEUE_LIST_SIZE );
		updates = new ArrayList( INIT_QUEUE_LIST_SIZE );

		collectionCreations = new ArrayList( INIT_QUEUE_LIST_SIZE );
		collectionRemovals = new ArrayList( INIT_QUEUE_LIST_SIZE );
		collectionUpdates = new ArrayList( INIT_QUEUE_LIST_SIZE );

		executions = new ArrayList( INIT_QUEUE_LIST_SIZE * 3 );
	}

	public void clear() {
		updates.clear();
		insertions.clear();
		deletions.clear();

		collectionCreations.clear();
		collectionRemovals.clear();
		collectionUpdates.clear();
	}

	public void addAction(EntityInsertAction action) {
		insertions.add( action );
	}

	public void addAction(EntityDeleteAction action) {
		deletions.add( action );
	}

	public void addAction(EntityUpdateAction action) {
		updates.add( action );
	}

	public void addAction(CollectionRecreateAction action) {
		collectionCreations.add( action );
	}

	public void addAction(CollectionRemoveAction action) {
		collectionRemovals.add( action );
	}

	public void addAction(CollectionUpdateAction action) {
		collectionUpdates.add( action );
	}

	public void addAction(EntityIdentityInsertAction insert) {
		insertions.add( insert );
	}

	public void addAction(BulkOperationCleanupAction cleanupAction) {
		// Add these directly to the executions queue
		executions.add( cleanupAction );
	}

	/**
	 * Perform all currently queued entity-insertion actions.
	 *
	 * @throws HibernateException error executing queued insertion actions.
	 */
	public void executeInserts() throws HibernateException {
		executeActions( insertions );
	}

	/**
	 * Perform all currently queued actions.
	 *
	 * @throws HibernateException error executing queued actions.
	 */
	public void executeActions() throws HibernateException {
		executeActions( insertions );
		executeActions( updates );
		executeActions( collectionRemovals );
		executeActions( collectionUpdates );
		executeActions( collectionCreations );
		executeActions( deletions );
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
	}

	/**
	 * Performs cleanup of any held cache softlocks.
	 *
	 * @param success Was the transaction successful.
	 */
	public void afterTransactionCompletion(boolean success) {
		int size = executions.size();
		final boolean invalidateQueryCache = session.getFactory().getSettings().isQueryCacheEnabled();
		for ( int i = 0; i < size; i++ ) {
			try {
				Executable exec = ( Executable ) executions.get(i);
				try {
					exec.afterTransactionCompletion( success );
				}
				finally {
					if ( invalidateQueryCache ) {
						session.getFactory().getUpdateTimestampsCache().invalidate( exec.getPropertySpaces() );
					}
				}
			}
			catch (CacheException ce) {
				log.error( "could not release a cache lock", ce );
				// continue loop
			}
			catch (Exception e) {
				throw new AssertionFailure( "Exception releasing cache locks", e );
			}
		}
		executions.clear();
	}

	/**
	 * Check whether the given tables/query-spaces are to be executed against
	 * given the currently queued actions.
	 *
	 * @param tables The table/query-spaces to check.
	 * @return True if we contain pending actions against any of the given
	 * tables; false otherwise.
	 */
	public boolean areTablesToBeUpdated(Set tables) {
		return areTablesToUpdated( updates, tables ) ||
		       areTablesToUpdated( insertions, tables ) ||
		       areTablesToUpdated( deletions, tables ) ||
		       areTablesToUpdated( collectionUpdates, tables ) ||
		       areTablesToUpdated( collectionCreations, tables ) ||
		       areTablesToUpdated( collectionRemovals, tables );
	}

	/**
	 * Check whether any insertion or deletion actions are currently queued.
	 *
	 * @return True if insertions or deletions are currently queued; false otherwise.
	 */
	public boolean areInsertionsOrDeletionsQueued() {
		return ( insertions.size() > 0 || deletions.size() > 0 );
	}

	private static boolean areTablesToUpdated(List executables, Set tablespaces) {
		int size = executables.size();
		for ( int j = 0; j < size; j++ ) {
			Serializable[] spaces = ( (Executable) executables.get(j) ).getPropertySpaces();
			for ( int i = 0; i < spaces.length; i++ ) {
				if ( tablespaces.contains( spaces[i] ) ) {
					if ( log.isDebugEnabled() ) log.debug( "changes must be flushed to space: " + spaces[i] );
					return true;
				}
			}
		}
		return false;
	}

	private void executeActions(List list) throws HibernateException {
		int size = list.size();
		for ( int i = 0; i < size; i++ ) {
			execute( (Executable) list.get(i) );
		}
		list.clear();
		session.getBatcher().executeBatch();
	}

	public void execute(Executable executable) {
		final boolean lockQueryCache = session.getFactory().getSettings().isQueryCacheEnabled();
		if ( executable.hasAfterTransactionCompletion() || lockQueryCache ) {
			executions.add( executable );
		}
		if (lockQueryCache) {
			session.getFactory()
				.getUpdateTimestampsCache()
				.preinvalidate( executable.getPropertySpaces() );
		}
		executable.execute();
	}

	private void prepareActions(List queue) throws HibernateException {
		int size = queue.size();
		for ( int i=0; i<size; i++ ) {
			Executable executable = ( Executable ) queue.get(i);
			executable.beforeExecutions();
		}
	}

	/**
	 * Returns a string representation of the object.
	 *
	 * @return a string representation of the object.
	 */
	public String toString() {
		return new StringBuffer()
				.append("ActionQueue[insertions=").append(insertions)
				.append(" updates=").append(updates)
		        .append(" deletions=").append(deletions)
				.append(" collectionCreations=").append(collectionCreations)
				.append(" collectionRemovals=").append(collectionRemovals)
				.append(" collectionUpdates=").append(collectionUpdates)
		        .append("]")
				.toString();
	}

	public int numberOfCollectionRemovals() {
		return collectionRemovals.size();
	}

	public int numberOfCollectionUpdates() {
		return collectionUpdates.size();
	}

	public int numberOfCollectionCreations() {
		return collectionCreations.size();
	}

	public int numberOfDeletions() {
		return deletions.size();
	}

	public int numberOfUpdates() {
		return updates.size();
	}

	public int numberOfInsertions() {
		return insertions.size();
	}

	public void sortCollectionActions() {
		if ( session.getFactory().getSettings().isOrderUpdatesEnabled() ) {
			//sort the updates by fk
			java.util.Collections.sort( collectionCreations );
			java.util.Collections.sort( collectionUpdates );
			java.util.Collections.sort( collectionRemovals );
		}
	}

	public void sortActions() {
		if ( session.getFactory().getSettings().isOrderUpdatesEnabled() ) {
			//sort the updates by pk
			java.util.Collections.sort( updates );
		}
		if ( session.getFactory().getSettings().isOrderInsertsEnabled() ) {
			sortInsertActions();
		}
	}

	/**
	 * Provided the option is set ({@link org.hibernate.cfg.Environment#ORDER_INSERTS}),
	 * then order the {@link #insertions} queue such that we group inserts
	 * against the same entity together (without violating constraints).  The
	 * original order is generated by cascade order, which in turn is based on
	 * the directionality of foreign-keys.  So even though we will be changing
	 * the ordering here, we need to make absolutely certain that we do not
	 * circumvent this FK ordering to the extent of causing constraint
	 * violations
	 */
	private void sortInsertActions() {
		// IMPLEMENTATION NOTES:
		//
		// The main data structure in this ordering algorithm is the 'positionToAction'
		// map.  Essentially this can be thought of as an put-ordered map (the problem with
		// actually implementing it that way and doing away with the 'nameList' is that
		// we'd end up having potential duplicate key values).  'positionToAction' maitains
		// a mapping from a position within the 'nameList' structure to a "partial queue"
		// of actions.

		HashMap positionToAction = new HashMap();
		List nameList = new ArrayList();

		loopInsertion: while( !insertions.isEmpty() ) {
			EntityInsertAction action = ( EntityInsertAction ) insertions.remove( 0 );
			String thisEntityName = action.getEntityName();

			// see if we have already encountered this entity-name...
			if ( ! nameList.contains( thisEntityName ) ) {
				// we have not, so create the proper entries in nameList and positionToAction
				ArrayList segmentedActionQueue = new ArrayList();
				segmentedActionQueue.add( action );
				nameList.add( thisEntityName );
				positionToAction.put( new Integer( nameList.indexOf( thisEntityName ) ), segmentedActionQueue );
			}
			else {
				// we have seen it before, so we need to determine if this insert action is
				// is depenedent upon a previously processed action in terms of FK
				// relationships (this FK checking is done against the entity's property-state
				// associated with the action...)
				int lastPos = nameList.lastIndexOf( thisEntityName );
				Object[] states = action.getState();
				for ( int i = 0; i < states.length; i++ ) {
					for ( int j = 0; j < nameList.size(); j++ ) {
						ArrayList tmpList = ( ArrayList ) positionToAction.get( new Integer( j ) );
						for ( int k = 0; k < tmpList.size(); k++ ) {
							final EntityInsertAction checkAction = ( EntityInsertAction ) tmpList.get( k );
							if ( checkAction.getInstance() == states[i] && j > lastPos ) {
								// 'checkAction' is inserting an entity upon which 'action'
								// depends...
								// note: this is an assumption and may not be correct in the case of one-to-one
								ArrayList segmentedActionQueue = new ArrayList();
								segmentedActionQueue.add( action );
								nameList.add( thisEntityName );
								positionToAction.put(new Integer( nameList.lastIndexOf( thisEntityName ) ), segmentedActionQueue );
								continue loopInsertion;
							}
						}
					}
				}

				ArrayList actionQueue = ( ArrayList ) positionToAction.get( new Integer( lastPos ) );
 				actionQueue.add( action );
 			}
 		}

 		// now iterate back through positionToAction map and move entityInsertAction back to insertion list
		for ( int p = 0; p < nameList.size(); p++ ) {
			ArrayList actionQueue = ( ArrayList ) positionToAction.get( new Integer( p ) );
			Iterator itr = actionQueue.iterator();
			while ( itr.hasNext() ) {
				insertions.add( itr.next() );
			}
		}
	}

	public ArrayList cloneDeletions() {
		return (ArrayList) deletions.clone();
	}

	public void clearFromFlushNeededCheck(int previousCollectionRemovalSize) {
		collectionCreations.clear();
		collectionUpdates.clear();
		updates.clear();
		// collection deletions are a special case since update() can add
		// deletions of collections not loaded by the session.
		for ( int i = collectionRemovals.size()-1; i >= previousCollectionRemovalSize; i-- ) {
			collectionRemovals.remove(i);
		}
	}

	public boolean hasAnyQueuedActions() {
		return updates.size() > 0 ||
		       insertions.size() > 0 ||
		       deletions.size() > 0 ||
		       collectionUpdates.size() > 0 ||
		       collectionRemovals.size() > 0 ||
		       collectionCreations.size() > 0;
	}

	/**
	 * Used by the owning session to explicitly control serialization of the
	 * action queue
	 *
	 * @param oos The stream to which the action queue should get written
	 * @throws IOException
	 */
	public void serialize(ObjectOutputStream oos) throws IOException {
		log.trace( "serializing action-queue" );

		int queueSize = insertions.size();
		log.trace( "starting serialization of [" + queueSize + "] insertions entries" );
		oos.writeInt( queueSize );
		for ( int i = 0; i < queueSize; i++ ) {
			oos.writeObject( insertions.get( i ) );
		}

		queueSize = deletions.size();
		log.trace( "starting serialization of [" + queueSize + "] deletions entries" );
		oos.writeInt( queueSize );
		for ( int i = 0; i < queueSize; i++ ) {
			oos.writeObject( deletions.get( i ) );
		}

		queueSize = updates.size();
		log.trace( "starting serialization of [" + queueSize + "] updates entries" );
		oos.writeInt( queueSize );
		for ( int i = 0; i < queueSize; i++ ) {
			oos.writeObject( updates.get( i ) );
		}

		queueSize = collectionUpdates.size();
		log.trace( "starting serialization of [" + queueSize + "] collectionUpdates entries" );
		oos.writeInt( queueSize );
		for ( int i = 0; i < queueSize; i++ ) {
			oos.writeObject( collectionUpdates.get( i ) );
		}

		queueSize = collectionRemovals.size();
		log.trace( "starting serialization of [" + queueSize + "] collectionRemovals entries" );
		oos.writeInt( queueSize );
		for ( int i = 0; i < queueSize; i++ ) {
			oos.writeObject( collectionRemovals.get( i ) );
		}

		queueSize = collectionCreations.size();
		log.trace( "starting serialization of [" + queueSize + "] collectionCreations entries" );
		oos.writeInt( queueSize );
		for ( int i = 0; i < queueSize; i++ ) {
			oos.writeObject( collectionCreations.get( i ) );
		}
	}

	/**
	 * Used by the owning session to explicitly control deserialization of the
	 * action queue
	 *
	 * @param ois The stream from which to read the action queue
	 * @throws IOException
	 */
	public static ActionQueue deserialize(
			ObjectInputStream ois,
	        SessionImplementor session) throws IOException, ClassNotFoundException {
		log.trace( "deserializing action-queue" );
		ActionQueue rtn = new ActionQueue( session );

		int queueSize = ois.readInt();
		log.trace( "starting deserialization of [" + queueSize + "] insertions entries" );
		rtn.insertions = new ArrayList( queueSize );
		for ( int i = 0; i < queueSize; i++ ) {
			rtn.insertions.add( ois.readObject() );
		}

		queueSize = ois.readInt();
		log.trace( "starting deserialization of [" + queueSize + "] deletions entries" );
		rtn.deletions = new ArrayList( queueSize );
		for ( int i = 0; i < queueSize; i++ ) {
			rtn.deletions.add( ois.readObject() );
		}

		queueSize = ois.readInt();
		log.trace( "starting deserialization of [" + queueSize + "] updates entries" );
		rtn.updates = new ArrayList( queueSize );
		for ( int i = 0; i < queueSize; i++ ) {
			rtn.updates.add( ois.readObject() );
		}

		queueSize = ois.readInt();
		log.trace( "starting deserialization of [" + queueSize + "] collectionUpdates entries" );
		rtn.collectionUpdates = new ArrayList( queueSize );
		for ( int i = 0; i < queueSize; i++ ) {
			rtn.collectionUpdates.add( ois.readObject() );
		}

		queueSize = ois.readInt();
		log.trace( "starting deserialization of [" + queueSize + "] collectionRemovals entries" );
		rtn.collectionRemovals = new ArrayList( queueSize );
		for ( int i = 0; i < queueSize; i++ ) {
			rtn.collectionRemovals.add( ois.readObject() );
		}

		queueSize = ois.readInt();
		log.trace( "starting deserialization of [" + queueSize + "] collectionCreations entries" );
		rtn.collectionCreations = new ArrayList( queueSize );
		for ( int i = 0; i < queueSize; i++ ) {
			rtn.collectionCreations.add( ois.readObject() );
		}
		return rtn;
	}

}
