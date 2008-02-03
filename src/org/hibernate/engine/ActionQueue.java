/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.engine;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.action.BulkOperationCleanupAction;
import org.hibernate.action.CollectionRecreateAction;
import org.hibernate.action.CollectionRemoveAction;
import org.hibernate.action.CollectionUpdateAction;
import org.hibernate.action.EntityDeleteAction;
import org.hibernate.action.EntityIdentityInsertAction;
import org.hibernate.action.EntityInsertAction;
import org.hibernate.action.EntityUpdateAction;
import org.hibernate.action.Executable;
import org.hibernate.cache.CacheException;
import org.hibernate.type.Type;

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
				Executable exec = ( Executable ) executions.get( i );
				try {
					exec.afterTransactionCompletion( success );
				}
				finally {
					if ( invalidateQueryCache ) {
						session.getFactory().getUpdateTimestampsCache().invalidate( exec.getPropertySpaces() );
					}
				}
			}
			catch ( CacheException ce ) {
				log.error( "could not release a cache lock", ce );
				// continue loop
			}
			catch ( Exception e ) {
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
	 *
	 * @return True if we contain pending actions against any of the given
	 *         tables; false otherwise.
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
			Serializable[] spaces = ( ( Executable ) executables.get( j ) ).getPropertySpaces();
			for ( int i = 0; i < spaces.length; i++ ) {
				if ( tablespaces.contains( spaces[i] ) ) {
					if ( log.isDebugEnabled() ) {
						log.debug( "changes must be flushed to space: " + spaces[i] );
					}
					return true;
				}
			}
		}
		return false;
	}

	private void executeActions(List list) throws HibernateException {
		int size = list.size();
		for ( int i = 0; i < size; i++ ) {
			execute( ( Executable ) list.get( i ) );
		}
		list.clear();
		session.getBatcher().executeBatch();
	}

	public void execute(Executable executable) {
		final boolean lockQueryCache = session.getFactory().getSettings().isQueryCacheEnabled();
		if ( executable.hasAfterTransactionCompletion() || lockQueryCache ) {
			executions.add( executable );
		}
		if ( lockQueryCache ) {
			session.getFactory()
					.getUpdateTimestampsCache()
					.preinvalidate( executable.getPropertySpaces() );
		}
		executable.execute();
	}

	private void prepareActions(List queue) throws HibernateException {
		int size = queue.size();
		for ( int i = 0; i < size; i++ ) {
			Executable executable = ( Executable ) queue.get( i );
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
				.append( "ActionQueue[insertions=" ).append( insertions )
				.append( " updates=" ).append( updates )
				.append( " deletions=" ).append( deletions )
				.append( " collectionCreations=" ).append( collectionCreations )
				.append( " collectionRemovals=" ).append( collectionRemovals )
				.append( " collectionUpdates=" ).append( collectionUpdates )
				.append( "]" )
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
	 * Order the {@link #insertions} queue such that we group inserts
	 * against the same entity together (without violating constraints).  The
	 * original order is generated by cascade order, which in turn is based on
	 * the directionality of foreign-keys.  So even though we will be changing
	 * the ordering here, we need to make absolutely certain that we do not
	 * circumvent this FK ordering to the extent of causing constraint
	 * violations
	 */
	private void sortInsertActions() {
		new InsertActionSorter().sort();
	}

	public ArrayList cloneDeletions() {
		return ( ArrayList ) deletions.clone();
	}

	public void clearFromFlushNeededCheck(int previousCollectionRemovalSize) {
		collectionCreations.clear();
		collectionUpdates.clear();
		updates.clear();
		// collection deletions are a special case since update() can add
		// deletions of collections not loaded by the session.
		for ( int i = collectionRemovals.size() - 1; i >= previousCollectionRemovalSize; i-- ) {
			collectionRemovals.remove( i );
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
	 *
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
	 *
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


	/**
	 * Sorts the insert actions using more hashes.
	 *
	 * @author Jay Erb
	 */
	private class InsertActionSorter {

		// the mapping of entity names to their latest batch numbers.
		private HashMap latestBatches = new HashMap();
		private HashMap entityBatchNumber;

		// the map of batch numbers to EntityInsertAction lists
		private HashMap actionBatches = new HashMap();

		public InsertActionSorter() {
			//optimize the hash size to eliminate a rehash.
			entityBatchNumber = new HashMap( insertions.size() + 1, 1.0f );
		}

		/**
		 * Sort the insert actions.
		 */
		public void sort() {

			// the list of entity names that indicate the batch number
			for ( Iterator actionItr = insertions.iterator(); actionItr.hasNext(); ) {
				EntityInsertAction action = ( EntityInsertAction ) actionItr.next();
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

					batchNumber = new Integer( actionBatches.size() );
					latestBatches.put( entityName, batchNumber );
				}
				entityBatchNumber.put( currentEntity, batchNumber );
				addToBatch( batchNumber, action );
			}
			insertions.clear();

			// now rebuild the insertions list. There is a batch for each entry in the name list.
			for ( int i = 0; i < actionBatches.size(); i++ ) {
				List batch = ( List ) actionBatches.get( new Integer( i ) );
				for ( Iterator batchItr = batch.iterator(); batchItr.hasNext(); ) {
					EntityInsertAction action = ( EntityInsertAction ) batchItr.next();
					insertions.add( action );
				}
			}
		}

		/**
		 * Finds an acceptable batch for this entity to be a member.
		 */
		private Integer findBatchNumber(EntityInsertAction action,
										String entityName) {
			// loop through all the associated entities and make sure they have been
			// processed before the latest
			// batch associated with this entity type.

			// the current batch number is the latest batch for this entity type.
			Integer latestBatchNumberForType = ( Integer ) latestBatches.get( entityName );

			// loop through all the associations of the current entity and make sure that they are processed
			// before the current batch number
			Object[] propertyValues = action.getState();
			Type[] propertyTypes = action.getPersister().getClassMetadata()
					.getPropertyTypes();

			for ( int i = 0; i < propertyValues.length; i++ ) {
				Object value = propertyValues[i];
				Type type = propertyTypes[i];
				if ( type.isEntityType() && value != null ) {
					// find the batch number associated with the current association, if any.
					Integer associationBatchNumber = ( Integer ) entityBatchNumber.get( value );
					if ( associationBatchNumber != null && associationBatchNumber.compareTo( latestBatchNumberForType ) > 0 ) {
						// create a new batch for this type. The batch number is the number of current batches.
						latestBatchNumberForType = new Integer( actionBatches.size() );
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

		private void addToBatch(Integer batchNumber, EntityInsertAction action) {
			List actions = ( List ) actionBatches.get( batchNumber );

			if ( actions == null ) {
				actions = new LinkedList();
				actionBatches.put( batchNumber, actions );
			}
			actions.add( action );
		}

	}

}
