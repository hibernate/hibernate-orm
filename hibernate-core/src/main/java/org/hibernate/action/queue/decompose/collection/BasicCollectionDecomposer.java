/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.decompose.collection;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.action.internal.CollectionRecreateAction;
import org.hibernate.action.internal.CollectionRemoveAction;
import org.hibernate.action.internal.CollectionUpdateAction;
import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.exec.BindPlan;
import org.hibernate.action.queue.exec.JdbcValueBindings;
import org.hibernate.action.queue.exec.ExecutionContext;
import org.hibernate.action.queue.decompose.DecompositionContext;
import org.hibernate.action.queue.meta.CollectionTableDescriptor;
import org.hibernate.action.queue.meta.TableDescriptorAsTableMapping;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.collection.spi.CollectionChangeSet;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.collection.spi.SnapshotIndexed;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.mapping.internal.ManyToManyCollectionPart;
import org.hibernate.persister.collection.BasicCollectionPersister;
import org.hibernate.persister.collection.mutation.OrderOnlyUpdateBindPlan;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.builder.TableDeleteBuilderStandard;
import org.hibernate.sql.model.ast.builder.TableInsertBuilderStandard;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderStandard;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import static org.hibernate.action.queue.CollectionOrdinalSupport.Slot;
import static org.hibernate.action.queue.CollectionOrdinalSupport.calculateOrdinal;
import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;

/// Decomposition support for [BasicCollectionPersister] which managed inserts, updates and deletes
/// into the collection table.
///
/// @author Steve Ebersole
public class BasicCollectionDecomposer implements CollectionDecomposer {
	private final BasicCollectionPersister persister;
	private final CollectionTableDescriptor tableDescriptor;
	private final CollectionJdbcOperations jdbcOperations;

	public BasicCollectionDecomposer(
			BasicCollectionPersister persister,
			SessionFactoryImplementor factory) {
		assert persister != null;

		this.persister = persister;
		this.tableDescriptor = persister.getCollectionTableDescriptor();
		this.jdbcOperations = buildJdbcOperations( factory );
	}

	/// Determines if an indexed collection needs recreate due to position shifts.
	/// For indexed collections where elements shift positions (e.g., insert at index 0),
	/// row-by-row UPDATE cannot reliably move elements to new positions because the
	/// WHERE clause may not uniquely identify rows (nullable elements are excluded).
	/// In these cases, we must use DELETE-all + INSERT-all (recreate).
	///
	/// > [!NOTE]
	/// > This check works around a limitation that has been in Hibernate forever.
	/// > The legacy queue code tries to manage these shifts in indexed collection elements
	/// > via an odd approach which can actually lead to data corruption.
	/// > Consider code like this (see `org.hibernate.orm.test.collection.original.CollectionTest#testUpdateOrder`
	/// > for reference):
	/// >
	/// > ````
	/// User u = session.get(...);
	/// assert u.getEmailAddresses().size() == 2;
	/// assert u.getEmailAddresses().get(0).equals( "me@first.com" )
	/// // this should be [0]
	/// u.getEmailAddresses().add( 0, "me@beforefirst.com" );
	/// ...
	/// ````
	/// >
	/// > The result of this is -
	/// >
	/// > ````
	/// [0] - "me@first.com"
	/// [1] - "me@first.com"
	/// ````
	/// > That's because the generated SQL is simply restricting on the foreign-key rather than the
	public List<PlannedOperation> decomposeRecreate(
			CollectionRecreateAction action,
			int ordinalBase,
			SharedSessionContractImplementor session, DecompositionContext decompositionContext) {

		// Always fire PRE event, even if no SQL operations will be needed
		DecompositionSupport.firePreRecreate( persister, action.getCollection(), session );

		var operations = planRecreateOperation(
				action.getCollection(),
				action.getKey(),
				ordinalBase,
				session
		);

		// Create post-execution callback to handle post-execution work (afterAction, cache, events, stats)
		var postRecreateHandling = new PostCollectionRecreateHandling(
				persister,
				action.getCollection(),
				action.getAffectedOwner(),
				action.getAffectedOwnerId(),
				DecompositionSupport.generateCacheKey( action, session )
		);

		if ( operations != null && !operations.isEmpty() ) {
			// Attach it to the last operation
			operations.get( operations.size() - 1 ).setPostExecutionCallback( postRecreateHandling );
			return operations;
		}
		else {
			// Operations unexpectedly empty - create no-op to defer POST callback
			return List.of( DecompositionSupport.createNoOpCallbackCarrier(
					tableDescriptor,
					calculateOrdinal( ordinalBase, Slot.INSERT ),
					postRecreateHandling
			) );
		}
	}

	private List<PlannedOperation> planRecreateOperation(
			PersistentCollection<?> collection,
			Object key,
			int ordinalBase,
			SharedSessionContractImplementor session) {

		final CollectionJdbcOperations.InsertRowPlan insertRowPlan = jdbcOperations.insertRowPlan();
		if ( insertRowPlan == null ) {
			return List.of();
		}

		// Pre-insert callback once for the whole collection
		collection.preInsert( persister );

		var insertOrdinal = calculateOrdinal( ordinalBase, Slot.INSERT );

		final var entries = collection.entries( persister );
		if ( !entries.hasNext() ) {
			return List.of();
		}

		final List<PlannedOperation> operations = new ArrayList<>();

		// One operation per row
		int entryCount = 0;
		while ( entries.hasNext() ) {
			final Object entry = entries.next();
			boolean include = collection.includeInRecreate( entry, entryCount, collection, persister.getAttributeMapping() );

			if ( include ) {
				final BindPlan bindPlan = new SingleRowInsertBindPlan(
						persister,
						jdbcOperations.insertRowPlan().values(),
						collection,
						key,
						entry,
						entryCount
				);

				final PlannedOperation plannedOp = new PlannedOperation(
						tableDescriptor,
						MutationKind.INSERT,
						jdbcOperations.insertRowPlan().jdbcOperation(),
						bindPlan,
						insertOrdinal,
						"InsertRow[" + entryCount + "](" + persister.getRolePath() + ")"
				);

				operations.add( plannedOp );
			}

			entryCount++;
		}

		return operations;
	}

	public List<PlannedOperation> decomposeUpdate(
			CollectionUpdateAction action,
			int ordinalBase,
			SharedSessionContractImplementor session, DecompositionContext decompositionContext) {
		var collection = action.getCollection();
		var key = action.getKey();

		DecompositionSupport.firePreUpdate( persister, collection, session );

		final List<PlannedOperation> operations = new ArrayList<>();

		if ( !collection.wasInitialized() ) {
			// If there were queued operations, they would have
			// been processed and cleared by now.
			if ( !collection.isDirty() ) {
				// The collection should still be dirty.
				throw new AssertionFailure( "collection is not dirty" );
			}
			// Do nothing - we only need to notify the cache
		}
		else {
			final boolean affectedByFilters = persister.isAffectedByEnabledFilters( session );
			final var eventMonitor = session.getEventMonitor();
			final var event = eventMonitor.beginCollectionUpdateEvent();
			boolean success = false;
			try {
				if ( !affectedByFilters && collection.empty() ) {
					if ( !action.isEmptySnapshot() ) {
						operations.addAll( planRemoveOperation( key, ordinalBase ) );
					}
				}
				else if ( collection.needsRecreate( persister ) ) {
					if ( affectedByFilters ) {
						throw new HibernateException( String.format( Locale.ROOT,
								"cannot recreate collection while filter is enabled [%s : %s]",
								persister.getRole(),
								key
						) );
					}
					if ( !action.isEmptySnapshot() ) {
						operations.addAll( planRemoveOperation( key, ordinalBase ) );
					}
					// Recreate INSERTs use INSERT_OFFSET which is higher than DELETE_OFFSET to avoid unique constraint violations
					operations.addAll( planRecreateOperation(
							collection,
							key,
							ordinalBase,
							session
					) );
				}
				else {
					// Try changeset-based approach for indexed collections (much more efficient)
					final var changeSet = collection.getChangeSet( persister );
					if ( changeSet != null && !changeSet.isEmpty() ) {
						planOperationsFromChangeSet( changeSet, collection, key, ordinalBase, session, operations::add );
					}
					else {
						// Fallback to original approach for non-indexed collections or empty changes
						planDeleteRowOperations( collection, key, ordinalBase, session, operations::add );
						planUpdateRowOperations( collection, key, ordinalBase, session, operations::add );
						planInsertRowOperations( collection, key, ordinalBase, session, operations::add );
					}
				}
				success = true;
			}
			finally {
				eventMonitor.completeCollectionUpdateEvent( event, key, persister.getRole(), success, session );
			}
		}

		// Create callback to handle post-execution work (afterAction, cache, events, stats)
		final PostCollectionUpdateHandling postUpdateHandling = new PostCollectionUpdateHandling(
				persister,
				collection,
				key,
				action.getAffectedOwner(),
				action.getAffectedOwnerId(),
				DecompositionSupport.generateCacheKey(action, session)
		);
		if ( !operations.isEmpty() ) {
			// Attach post-execution callback to the last operation
			final PlannedOperation lastOperation = operations.get( operations.size() - 1 );
			lastOperation.setPostExecutionCallback( postUpdateHandling );
			return operations;
		}
		else {
			// Operations empty - create no-op to defer POST callback
			// This can happen for uninitialized dirty collections
			return List.of( DecompositionSupport.createNoOpCallbackCarrier(
					tableDescriptor,
					calculateOrdinal( ordinalBase, Slot.UPDATE ),
					postUpdateHandling
			) );
		}
	}

	private void planDeleteRowOperations(
			PersistentCollection<?> collection,
			Object key,
			int ordinalBase,
			SharedSessionContractImplementor session,
			Consumer<PlannedOperation> operationConsumer) {
		var deleteRowPlan = jdbcOperations.deleteRowPlan();

		// For entity collections with index (join tables with @OrderColumn), we need special handling
		// because we must identify removals by entity identity, not position
		final boolean isIndexedEntityCollection = persister.getElementType().isEntityType() && persister.hasIndex();
		final Iterator<?> deletes;

		if ( isIndexedEntityCollection ) {
			// Build list of (entity, oldPosition) pairs for removed entities
			deletes = buildIndexedEntityDeletions( collection );
		}
		else {
			// Use existing logic for element collections or non-indexed entity collections
			deletes = collection.getDeletes( persister, !persister.hasPhysicalIndexColumn() );
		}

		if ( deleteRowPlan == null || !deletes.hasNext() ) {
			MODEL_MUTATION_LOGGER.noRowsToDelete();
			return;
		}

		var deleteOrdinal = calculateOrdinal( ordinalBase, Slot.DELETE );

		// One operation per row
		int deletionCount = 0;

		while ( deletes.hasNext() ) {
			final Object removal = deletes.next();

			final BindPlan bindPlan = new SingleRowDeleteBindPlan(
					collection,
					key,
					removal,
					deleteRowPlan.restrictions()
			);

			operationConsumer.accept( new PlannedOperation(
					tableDescriptor,
					MutationKind.DELETE,
					deleteRowPlan.jdbcOperation(),
					bindPlan,
					deleteOrdinal,
					"DeleteRow[" + deletionCount + "](" + persister.getRolePath() + ")"
			) );

			deletionCount++;
		}
	}

	private void planUpdateRowOperations(
			PersistentCollection<?> collection,
			Object key,
			int ordinalBase,
			SharedSessionContractImplementor session,
			Consumer<PlannedOperation> operationConsumer) {
		var updateRowPlan = jdbcOperations.updateRowPlan();

		if ( updateRowPlan == null ) {
			// EARLY EXIT!!
			return;
		}

		final boolean isIndexedEntityCollection = persister.getElementType().isEntityType() && persister.hasIndex();

		if ( isIndexedEntityCollection ) {
			// For indexed entity collections (join tables with @OrderColumn),
			// identify entities that exist in both snapshot and current but at different positions
			var orderUpdatePlan = jdbcOperations.updateIndexPlan();
			planOrderOnlyUpdateOperations( collection, key, ordinalBase, session, orderUpdatePlan, operationConsumer );
		}
		else {
			// For element collections, use position-based comparison
			planElementUpdateOperations( collection, key, ordinalBase, session, updateRowPlan, operationConsumer );
		}
	}

	/**
	 * Plan all mutation operations based on a pre-computed change set.
	 * This is much more efficient than the original approach of multiple O(N²) scans.
	 * Processes changes in three phases: DELETE → UPDATE_ORDER → INSERT
	 */
	private void planOperationsFromChangeSet(
			CollectionChangeSet changeSet,
			PersistentCollection<?> collection,
			Object key,
			int ordinalBase,
			SharedSessionContractImplementor session,
			Consumer<PlannedOperation> operationConsumer) {

		final var deleteRowPlan = jdbcOperations.deleteRowPlan();
		final var insertRowPlan = jdbcOperations.insertRowPlan();
		final var orderUpdatePlan = jdbcOperations.updateIndexPlan();

		// Phase 1: DELETE removed elements
		if ( deleteRowPlan != null && !changeSet.removals().isEmpty() ) {
			final int deleteOrdinal = calculateOrdinal( ordinalBase, Slot.DELETE );
			for ( CollectionChangeSet.Removal removal : changeSet.removals() ) {
				// Removal implements SnapshotPositioned, so pass directly
				final BindPlan bindPlan = new SingleRowDeleteBindPlan(
						collection,
						key,
						removal,  // Implements SnapshotPositioned
						deleteRowPlan.restrictions()
				);

				operationConsumer.accept( new PlannedOperation(
						tableDescriptor,
						MutationKind.DELETE,
						deleteRowPlan.jdbcOperation(),
						bindPlan,
						deleteOrdinal,
						"DeleteRow[" + removal.snapshotIndex() + "](" + persister.getRolePath() + ")"
				) );
			}
		}

		// Phase 2: UPDATE_ORDER shifted elements (using two-phase unique constraint break)
		// Note: for reasons discussed on PersistentMap#computeEntityCollectionChangeSet and
		// 		#computeElementCollectionChangeSet, shifts are not supported for Maps only Lists
		if ( orderUpdatePlan != null && !changeSet.shifts().isEmpty() ) {
			final List<PlannedOperation> tempPhaseOps = new ArrayList<>();
			final List<PlannedOperation> finalPhaseOps = new ArrayList<>();

			final int updateOrdinalBase = calculateOrdinal( ordinalBase, Slot.UPDATE );
			final int tempPhaseOrdinal = updateOrdinalBase;
			final int finalPhaseOrdinal = updateOrdinalBase + 1;
			final int tempOffset = Integer.MAX_VALUE / 2;

			for ( CollectionChangeSet.Shift shift : changeSet.shifts() ) {
				final int tempPosition = tempOffset + (int) shift.currentIndex();

				// TEMP PHASE: Move from old position to temporary position
				tempPhaseOps.add( new PlannedOperation(
						tableDescriptor,
						MutationKind.UPDATE_ORDER,
						orderUpdatePlan.jdbcOperation(),
						new OrderOnlyUpdateBindPlan(
								collection,
								key,
								shift.element(),
								(int) shift.snapshotIndex(),  // WHERE: old position
								tempPosition,              // SET: temporary position
								orderUpdatePlan.values(),
								orderUpdatePlan.restrictions()
						),
						tempPhaseOrdinal,
						"UpdateRowTemp[" + shift.snapshotIndex() + "→" + tempPosition + "](" + persister.getRolePath() + ")"
				) );

				// FINAL PHASE: Move from temporary position to final position
				finalPhaseOps.add( new PlannedOperation(
						tableDescriptor,
						MutationKind.UPDATE_ORDER,
						orderUpdatePlan.jdbcOperation(),
						new OrderOnlyUpdateBindPlan(
								collection,
								key,
								shift.element(),
								tempPosition,              // WHERE: temporary position
								(int) shift.currentIndex(),   // SET: final position
								orderUpdatePlan.values(),
								orderUpdatePlan.restrictions()
						),
						finalPhaseOrdinal,
						"UpdateRowFinal[" + tempPosition + "→" + shift.currentIndex() + "](" + persister.getRolePath() + ")"
				) );
			}

			tempPhaseOps.forEach( operationConsumer );
			finalPhaseOps.forEach( operationConsumer );
		}

		// Phase 3: INSERT added elements
		if ( insertRowPlan != null && !changeSet.additions().isEmpty() ) {
			final int insertOrdinal = calculateOrdinal( ordinalBase, Slot.INSERT );
			for ( CollectionChangeSet.Addition addition : changeSet.additions() ) {
				// For Maps, create a Map.Entry from (key=addition.index(), value=addition.element())
				// For Lists, use the element directly and pass the numeric index
				final boolean isMap = persister.getCollectionSemantics().getCollectionClassification().isMap();
				final Object rowValue;
				final int entryIndex;

				if ( isMap ) {
					// For Maps: bindInsertRowValues expects a Map.Entry to extract both key and value
					rowValue = Map.entry( addition.index(), addition.element() );
					entryIndex = -1;  // Not used for Maps
				}
				else {
					// For Lists: rowValue is the element, and entryIndex is the position
					rowValue = addition.element();
					entryIndex = (Integer) addition.index();
				}

				final BindPlan bindPlan = new SingleRowInsertBindPlan(
						persister,
						insertRowPlan.values(),
						collection,
						key,
						rowValue,
						entryIndex
				);

				operationConsumer.accept( new PlannedOperation(
						tableDescriptor,
						MutationKind.INSERT,
						insertRowPlan.jdbcOperation(),
						bindPlan,
						insertOrdinal,
						"InsertRow[" + addition.index() + "](" + persister.getRolePath() + ")"
				) );
			}
		}

		// Phase 4: VALUE CHANGE for element collections
		// Update rows where the value changed at the same position/key
		final var updateRowPlan = jdbcOperations.updateRowPlan();
		if ( updateRowPlan != null && !changeSet.valueChanges().isEmpty() ) {
			final int updateOrdinal = calculateOrdinal( ordinalBase, Slot.UPDATE );
			for ( CollectionChangeSet.ValueChange valueChange : changeSet.valueChanges() ) {
				// For Maps, need to create a Map.Entry with (key, newValue)
				// For Lists, the index is the position and entry is just the element
				final Object entry;
				final int entryIndex;

				if ( persister.getCollectionSemantics().getCollectionClassification().isMap() ) {
					// For Maps: create Map.Entry(mapKey, newValue)
					entry = Map.entry( valueChange.index(), valueChange.newValue() );
					entryIndex = -1;  // Maps don't use numeric index
				}
				else {
					// For Lists: entry is the element, index is the position
					entry = valueChange.newValue();
					entryIndex = (Integer) valueChange.index();
				}

				final BindPlan bindPlan = new SingleRowUpdateBindPlan(
						collection,
						key,
						entry,
						entryIndex,
						updateRowPlan.values(),
						updateRowPlan.restrictions()
				);

				operationConsumer.accept( new PlannedOperation(
						tableDescriptor,
						MutationKind.UPDATE,
						updateRowPlan.jdbcOperation(),
						bindPlan,
						updateOrdinal,
						"UpdateValue[" + valueChange.index() + "](" + persister.getRolePath() + ")"
				) );
			}
		}
	}

	private void planOrderOnlyUpdateOperations(
			PersistentCollection<?> collection,
			Object key,
			int ordinalBase,
			SharedSessionContractImplementor session,
			CollectionJdbcOperations.UpdateRowPlan orderUpdatePlan,
			Consumer<PlannedOperation> operationConsumer) {

		if ( orderUpdatePlan == null ) {
			// No order update plan available (shouldn't happen for indexed collections)
			return;
		}

		// @OrderColumn is only valid for Lists, not Maps
		// Maps use key-based ordering, not ordinal positions
		if ( persister.getCollectionSemantics().getCollectionClassification() != CollectionClassification.LIST ) {
			return;
		}

		final var snapshot = (List<?>) collection.getStoredSnapshot();
		if ( snapshot == null || snapshot.isEmpty() ) {
			return;
		}

		// Build list of current entities
		final var entries = collection.entries( persister );
		final List<Object> currentEntities = new ArrayList<>();
		while ( entries.hasNext() ) {
			currentEntities.add( entries.next() );
		}

		if ( currentEntities.isEmpty() ) {
			return;
		}

		// Use two-phase update strategy to avoid unique constraint violations:
		// Phase 1: Move all shifting elements to temporary positions (no conflicts)
		// Phase 2: Move from temporary positions to final positions
		// All operations in each phase have the SAME ordinal, enabling batching

		final List<PlannedOperation> finalPhaseOps = new ArrayList<>();

		// All temp-phase operations use same ordinal for batching
		final var tempPhaseOrdinal = calculateOrdinal( ordinalBase, Slot.UPDATE );

		// All final-phase operations use same ordinal for batching (executes after temp phase)
		final int finalPhaseOrdinal = tempPhaseOrdinal + 1;

		// Temporary position offset - use high value to avoid conflicts
		// Most check constraints only enforce lower bounds (e.g., >= 0)
		// todo : add a strategy - see https://hibernate.atlassian.net/browse/HHH-20312
		final int tempOffset = Integer.MAX_VALUE / 2;

		// For each entity in current collection, check if it exists in snapshot at a different position
		for ( int currentPos = 0; currentPos < currentEntities.size(); currentPos++ ) {
			final Object currentEntity = currentEntities.get( currentPos );
			if ( currentEntity == null ) {
				continue;
			}

			// Find this entity in the snapshot
			int snapshotPos = -1;
			for ( int i = 0; i < snapshot.size(); i++ ) {
				if ( snapshot.get( i ) == currentEntity ) {
					snapshotPos = i;
					break;
				}
			}

			// If entity exists in snapshot but at different position, create two-phase UPDATE
			if ( snapshotPos >= 0 && snapshotPos != currentPos ) {
				final int tempPosition = tempOffset + currentPos;

				// TEMP PHASE: Move from old position to temporary position
				operationConsumer.accept( new PlannedOperation(
						tableDescriptor,
						MutationKind.UPDATE_ORDER,
						orderUpdatePlan.jdbcOperation(),
						new OrderOnlyUpdateBindPlan(
								collection,
								key,
								currentEntity,
								snapshotPos,     // WHERE: old position
								tempPosition,    // SET: temporary position
								orderUpdatePlan.values(),
								orderUpdatePlan.restrictions()
						),
						tempPhaseOrdinal,  // Same ordinal for all temp ops - enables batching!
						"UpdateRowTemp[" + snapshotPos + "→" + tempPosition + "](" + persister.getRolePath() + ")"
				) );

				// FINAL PHASE: Move from temporary position to final position
				finalPhaseOps.add( new PlannedOperation(
						tableDescriptor,
						MutationKind.UPDATE_ORDER,
						orderUpdatePlan.jdbcOperation(),
						new OrderOnlyUpdateBindPlan(
								collection,
								key,
								currentEntity,
								tempPosition,    // WHERE: temporary position
								currentPos,      // SET: final position
								orderUpdatePlan.values(),
								orderUpdatePlan.restrictions()
						),
						finalPhaseOrdinal,  // Same ordinal for all final ops - enables batching!
						"UpdateRowFinal[" + tempPosition + "→" + currentPos + "](" + persister.getRolePath() + ")"
				) );
			}
		}

		// All temp-phase operations were already added as we created them - they should come first
		// for better planning.
		// Now, add all final-phase operations.
		// Same ordinals within each phase enable JDBC batching.
		finalPhaseOps.forEach( operationConsumer );
	}

	private void planElementUpdateOperations(
			PersistentCollection<?> collection,
			Object key,
			int ordinalBase,
			SharedSessionContractImplementor session,
			CollectionJdbcOperations.UpdateRowPlan updateRowPlan,
			Consumer<PlannedOperation> operationConsumer) {

		final var entries = collection.entries( persister );
		if ( !entries.hasNext() ) {
			return;
		}

		final var updateOrdinal = calculateOrdinal( ordinalBase, Slot.UPDATE );
		final List<PlannedOperation> updateOperations = new ArrayList<>();

		int entryCount = 0;
		while ( entries.hasNext() ) {
			final Object entry = entries.next();

			if ( collection.needsUpdating( entry, entryCount, persister.getAttributeMapping() ) ) {
				final BindPlan bindPlan = new SingleRowUpdateBindPlan(
						collection,
						key,
						entry,
						entryCount,
						updateRowPlan.values(),
						updateRowPlan.restrictions()
				);

				updateOperations.add( new PlannedOperation(
						tableDescriptor,
						MutationKind.UPDATE,
						updateRowPlan.jdbcOperation(),
						bindPlan,
						updateOrdinal,
						"UpdateRow[" + entryCount + "](" + persister.getRolePath() + ")"
				) );
			}

			entryCount++;
		}

		// For indexed collections where elements have been removed, process UPDATEs in reverse order
		// to avoid unique constraint violations when elements shift positions
		if ( collection.isElementRemoved() ) {
			java.util.Collections.reverse( updateOperations );
		}

		updateOperations.forEach( operationConsumer );
	}

	private void planInsertRowOperations(
			PersistentCollection<?> collection,
			Object key,
			int ordinalBase,
			SharedSessionContractImplementor session,
			Consumer<PlannedOperation> operationConsumer) {
		// Pre-insert callback once for the whole collection
		collection.preInsert( persister );

		var insertRowPlan = jdbcOperations.insertRowPlan();

		if ( insertRowPlan == null ) {
			// EARLY EXIT!!
			return;
		}

		// For entity collections (join tables), use entity-based tracking
		final boolean useEntityTracking = persister.getElementType().isEntityType();
		final var insertOrdinal = calculateOrdinal( ordinalBase, Slot.INSERT );

		if ( useEntityTracking ) {
			// Get entities that were added to the collection
			final var addedEntities = collection.getAddedEntities( persister );
			if ( !addedEntities.hasNext() ) {
				return;
			}

			// For each added entity, find its current position and create an INSERT operation
			final var entries = collection.entries( persister );
			final List<Object> entryList = new ArrayList<>();
			while ( entries.hasNext() ) {
				entryList.add( entries.next() );
			}

			int insertCount = 0;
			while ( addedEntities.hasNext() ) {
				final Object addedEntity = addedEntities.next();

				// Find this entity's current position in the collection
				int position = -1;
				for ( int i = 0; i < entryList.size(); i++ ) {
					if ( entryList.get( i ) == addedEntity ) {
						position = i;
						break;
					}
				}

				if ( position >= 0 ) {
					final BindPlan bindPlan = new SingleRowInsertBindPlan(
							persister,
							insertRowPlan.values(),
							collection,
							key,
							addedEntity,
							position
					);

					operationConsumer.accept( new PlannedOperation(
							tableDescriptor,
							MutationKind.INSERT,
							insertRowPlan.jdbcOperation(),
							bindPlan,
							insertOrdinal,
							"InsertRow[" + insertCount + "](" + persister.getRolePath() + ")"
					) );
					insertCount++;
				}
			}
		}
		else {
			// Original position-based logic for element collections
			final var entries = collection.entries( persister );
			if ( !entries.hasNext() ) {
				return;
			}

			int entryCount = 0;
			while ( entries.hasNext() ) {
				final Object entry = entries.next();

				if ( collection.includeInInsert( entry, entryCount, collection, persister.getAttributeMapping() ) ) {
					final BindPlan bindPlan = new SingleRowInsertBindPlan(
							persister,
							insertRowPlan.values(),
							collection,
							key,
							entry,
							entryCount
					);

					operationConsumer.accept( new PlannedOperation(
							tableDescriptor,
							MutationKind.INSERT,
							insertRowPlan.jdbcOperation(),
							bindPlan,
							insertOrdinal,
							"InsertRow[" + entryCount + "](" + persister.getRolePath() + ")"
					) );
				}

				entryCount++;
			}
		}
	}

	public List<PlannedOperation> decomposeRemove(
			CollectionRemoveAction action,
			int ordinalBase,
			SharedSessionContractImplementor session, DecompositionContext decompositionContext) {
		var collection = action.getCollection();
		var affectedOwner = action.getAffectedOwner();

		// Always fire PRE event, even if no SQL operations will be needed
		DecompositionSupport.firePreRemove( persister, collection, affectedOwner, session );

		// Create callback to handle post-execution work (afterAction, cache, events, stats)
		var postRemoveHandling = new PostCollectionRemoveHandling(
				persister,
				collection,
				affectedOwner,
				action.getAffectedOwnerId(),
				DecompositionSupport.generateCacheKey( action, session )
		);

		var operations = planRemoveOperation( action.getKey(), ordinalBase );

		if ( !operations.isEmpty() ) {
			// should be just one...
			operations.get(0).setPostExecutionCallback( postRemoveHandling );
			return operations;
		}
		else {
			return List.of( DecompositionSupport.createNoOpCallbackCarrier(
					tableDescriptor,
					calculateOrdinal( ordinalBase, Slot.DELETE ),
					postRemoveHandling
			) );
		}
	}

	private List<PlannedOperation> planRemoveOperation(
			Object key,
			int ordinalBase) {
		final var jdbcOperation = jdbcOperations.removeOperation();
		if ( jdbcOperation == null ) {
			return List.of();
		}

		final PlannedOperation plannedOp = new PlannedOperation(
				tableDescriptor,
				MutationKind.DELETE,
				jdbcOperation,
				new RemoveBindPlan( key, persister ),
				calculateOrdinal( ordinalBase, Slot.DELETE ),
				"RemoveAllRows(" + persister.getRolePath() + ")"
		);

		return List.of( plannedOp );
	}


	private CollectionJdbcOperations buildJdbcOperations(
			SessionFactoryImplementor factory) {
		final CollectionJdbcOperations.InsertRowPlan insertRowPlan = buildInsertRowPlan( factory );

		final CollectionJdbcOperations.UpdateRowPlan updateRowPlan = buildUpdateRowPlan( factory );

		// Build separate update plan for order-only updates (UPDATE_ORDER operations)
		final CollectionJdbcOperations.UpdateRowPlan orderUpdatePlan = buildOrderUpdatePlan( factory );

		final CollectionJdbcOperations.DeleteRowPlan deleteRowPlan = buildDeleteRowPlan( factory );

		return new CollectionJdbcOperations(
				persister,
				insertRowPlan,
				updateRowPlan,
				orderUpdatePlan,
				deleteRowPlan,
				buildRemoveOperation( factory )
		);
	}

	private CollectionJdbcOperations.InsertRowPlan buildInsertRowPlan(
			SessionFactoryImplementor factory) {
		if ( persister.isInverse() || !persister.isRowInsertEnabled() ) {
			return null;
		}

		// Ignore custom SQL for now...
		final TableDescriptorAsTableMapping tableMapping = new TableDescriptorAsTableMapping(
				tableDescriptor,
				0, // relativePosition
				false, // isIdentifierTable
				false // isInverse
		);
		var builder = new TableInsertBuilderStandard(
				persister,
				new MutatingTableReference(tableMapping),
				factory
		);

		applyInsertDetails( builder, persister, factory );

		return new CollectionJdbcOperations.InsertRowPlan(
				builder.buildMutation().createMutationOperation(null, factory),
				this::bindInsertRowValues
		);
	}

	private void applyInsertDetails(
			TableInsertBuilderStandard insertBuilder,
			BasicCollectionPersister persister,
			SessionFactoryImplementor factory) {
		final var attributeMapping = persister.getAttributeMapping();
		attributeMapping.getKeyDescriptor().getKeyPart().forEachInsertable( (i, columnMapping) -> {
			insertBuilder.addColumnAssignment( columnMapping );
		});

		final var identifierDescriptor = attributeMapping.getIdentifierDescriptor();
		final var indexDescriptor = attributeMapping.getIndexDescriptor();
		if ( identifierDescriptor != null ) {
			identifierDescriptor.forEachInsertable( (i, columnMapping) -> {
				insertBuilder.addColumnAssignment( columnMapping );
			} );
		}
		else if ( indexDescriptor != null ) {
			indexDescriptor.forEachInsertable( (i, columnMapping) -> {
				insertBuilder.addColumnAssignment( columnMapping );
			} );
		}

		// Add element columns
		attributeMapping.getElementDescriptor().forEachInsertable( (i, columnMapping) -> {
			insertBuilder.addColumnAssignment( columnMapping );
		} );

		final var softDeleteMapping = attributeMapping.getSoftDeleteMapping();
		if ( softDeleteMapping != null ) {
			final var columnReference = new ColumnReference( insertBuilder.getMutatingTable(), softDeleteMapping );
			insertBuilder.addColumnAssignment( softDeleteMapping.createNonDeletedValueBinding( columnReference ) );
		}
	}

	private void bindInsertRowValues(
			PersistentCollection<?> collection,
			Object key,
			Object rowValue,
			int rowPosition,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		if ( key == null ) {
			throw new IllegalArgumentException( "null key for collection: " + persister.getNavigableRole().getFullPath() );
		}

		final var attributeMapping = persister.getAttributeMapping();
		attributeMapping.getKeyDescriptor().getKeyPart().decompose(
				key,
				jdbcValueBindings::bindAssignment,
				session
		);

		final var identifierDescriptor = attributeMapping.getIdentifierDescriptor();
		if ( identifierDescriptor != null ) {
			identifierDescriptor.decompose(
					collection.getIdentifier( rowValue, rowPosition ),
					jdbcValueBindings::bindAssignment,
					session
			);
		}
		else {
			final var indexDescriptor = attributeMapping.getIndexDescriptor();
			if ( indexDescriptor != null ) {
				indexDescriptor.decompose(
						persister.incrementIndexByBase( collection.getIndex( rowValue, rowPosition, persister ) ),
						jdbcValueBindings::bindInsertAssignment,
						session
				);
			}
		}

		attributeMapping.getElementDescriptor().decompose(
				collection.getElement( rowValue ),
				jdbcValueBindings::bindAssignment,
				session
		);
	}

	private CollectionJdbcOperations.UpdateRowPlan buildUpdateRowPlan(
			SessionFactoryImplementor factory) {
		if ( !persister.isPerformingUpdates() ) {
			return null;
		}

		var attribute = persister.getAttributeMapping();

		final TableDescriptorAsTableMapping tableMapping = new TableDescriptorAsTableMapping(
				tableDescriptor,
				0, // relativePosition
				false, // isIdentifierTable
				false // isInverse
		);
		var builder = new TableUpdateBuilderStandard<>(
				persister,
				new MutatingTableReference(tableMapping),
				factory,
				persister.getSqlWhereString()
		);

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// SET clause: element columns

		attribute.getElementDescriptor().forEachUpdatable(
			(selectionIndex, jdbcMapping) -> {
				builder.addColumnAssignment( jdbcMapping );
			}
		);

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// WHERE clause: key columns (restrict by owner FK) + index/element for row identification

		attribute.getKeyDescriptor().getKeyPart().forEachColumn(
			(selectionIndex, jdbcMapping) -> {
				builder.addKeyRestrictionLeniently( jdbcMapping );
			}
		);

		// For indexed collections, also restrict by the OLD index value to identify the specific row
		// This is critical to avoid updating all rows when index values change
		final var indexDescriptor = attribute.getIndexDescriptor();
		final var identifierDescriptor = attribute.getIdentifierDescriptor();
		if ( indexDescriptor != null ) {
			indexDescriptor.forEachSelectable( (index, jdbcMapping) -> {
				builder.addKeyRestriction( jdbcMapping );
			} );
		}
		else if ( identifierDescriptor != null ) {
			// For IdBag collections, restrict by the synthetic identifier column
			identifierDescriptor.forEachSelectable( (index, jdbcMapping) -> {
				builder.addKeyRestriction( jdbcMapping );
			} );
		}
		else {
			// For non-indexed collections (sets, bags), restrict by element
			attribute.getElementDescriptor().forEachSelectable((index, jdbcMapping) -> {
				builder.addKeyRestriction( jdbcMapping );
			} );
		}

		return new CollectionJdbcOperations.UpdateRowPlan(
				builder.buildMutation().createMutationOperation(null, factory),
				this::bindUpdateRowValues,
				this::bindUpdateRowRestrictions
		);
	}

	private CollectionJdbcOperations.UpdateRowPlan buildOrderUpdatePlan(
			SessionFactoryImplementor factory) {
		var attribute = persister.getAttributeMapping();
		final var indexDescriptor = attribute.getIndexDescriptor();

		// Only needed for indexed collections
		if ( indexDescriptor == null ) {
			return null;
		}

		final TableDescriptorAsTableMapping tableMapping = new TableDescriptorAsTableMapping(
				tableDescriptor,
				0, // relativePosition
				false, // isIdentifierTable
				false // isInverse
		);
		var builder = new TableUpdateBuilderStandard<>(
				persister,
				new MutatingTableReference(tableMapping),
				factory,
				persister.getSqlWhereString()
		);

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// SET clause: index column (to update position)

		indexDescriptor.forEachUpdatable(
			(selectionIndex, jdbcMapping) -> {
				builder.addColumnAssignment( jdbcMapping );
			}
		);

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// WHERE clause: key columns + OLD index value to identify the row

		attribute.getKeyDescriptor().getKeyPart().forEachColumn(
			(selectionIndex, jdbcMapping) -> {
				builder.addKeyRestrictionLeniently( jdbcMapping );
			}
		);

		indexDescriptor.forEachSelectable( (index, jdbcMapping) -> {
			builder.addKeyRestriction( jdbcMapping );
		} );

		return new CollectionJdbcOperations.UpdateRowPlan(
				builder.buildMutation().createMutationOperation(null, factory),
				this::bindOrderUpdateValues,
				this::bindOrderUpdateRestrictions
		);
	}

	private void bindUpdateRowValues(
			PersistentCollection<?> collection,
			Object key,
			Object rowValue,
			int rowPosition,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		if ( key == null ) {
			throw new IllegalArgumentException( "null key for collection: " + persister.getNavigableRole().getFullPath() );
		}

		var attribute = persister.getAttributeMapping();
		var elementDescriptor = attribute.getElementDescriptor();

		// For Maps, rowValue is a Map.Entry - extract the element (value) first
		final Object element = collection.getElement( rowValue );
		elementDescriptor.decompose(
				element,
				jdbcValueBindings::bindAssignment,
				session
		);
	}

	private void bindUpdateRowRestrictions(
			PersistentCollection<?> collection,
			Object key,
			Object rowValue,
			int rowPosition,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		if ( key == null ) {
			throw new IllegalArgumentException( "null key for collection: " + persister.getNavigableRole().getFullPath() );
		}

		final var attribute = persister.getAttributeMapping();

		attribute.getKeyDescriptor().getKeyPart().decompose(
				key,
				jdbcValueBindings::bindRestriction,
				session
		);

		// Bind index/element/identifier to identify the specific row to update
		final var indexDescriptor = attribute.getIndexDescriptor();
		final var identifierDescriptor = attribute.getIdentifierDescriptor();
		if ( indexDescriptor != null ) {
			// For indexed collections (lists, maps), restrict by the index position
			// This identifies which row in the collection table to update
			final Object indexValue = collection.getIndex( rowValue, rowPosition, persister );
			indexDescriptor.decompose(
					persister.incrementIndexByBase( indexValue ),
					jdbcValueBindings::bindUpdateRestriction,
					session
			);
		}
		else if ( identifierDescriptor != null ) {
			// For IdBag collections, restrict by the synthetic identifier
			// rowValue for IdBag updates should contain the identifier
			identifierDescriptor.decompose(
					rowValue,  // For IdBag, rowValue is the identifier value
					jdbcValueBindings::bindRestriction,
					session
			);
		}
		else {
			// For non-indexed collections (sets, bags), restrict by element value
			attribute.getElementDescriptor().decompose(
					rowValue,
					jdbcValueBindings::bindRestriction,
					session
			);
		}
	}

	private void bindOrderUpdateValues(
			PersistentCollection<?> collection,
			Object key,
			Object rowValue,
			int rowPosition,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		final var attribute = persister.getAttributeMapping();
		final var indexDescriptor = attribute.getIndexDescriptor();

		if ( indexDescriptor != null ) {
			// For UPDATE_ORDER values, rowPosition is the NEW position
			// Use it directly rather than calling getIndex which may give incorrect results
			indexDescriptor.decompose(
					persister.incrementIndexByBase( rowPosition ),
					jdbcValueBindings::bindUpdateAssignment,
					session
			);
		}
	}

	private void bindOrderUpdateRestrictions(
			PersistentCollection<?> collection,
			Object key,
			Object rowValue,
			int rowPosition,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		final var attribute = persister.getAttributeMapping();

		attribute.getKeyDescriptor().getKeyPart().decompose(
				key,
				jdbcValueBindings::bindRestriction,
				session
		);

		final var indexDescriptor = attribute.getIndexDescriptor();
		if ( indexDescriptor != null ) {
			// For UPDATE_ORDER restrictions, rowPosition is the OLD snapshot position
			// Use it directly rather than calling getIndex which looks at current collection state
			indexDescriptor.decompose(
					persister.incrementIndexByBase( rowPosition ),
					jdbcValueBindings::bindUpdateRestriction,
					session
			);
		}
	}

	private CollectionJdbcOperations.DeleteRowPlan buildDeleteRowPlan(SessionFactoryImplementor factory) {
		if ( !persister.isRowDeleteEnabled() ) {
			// Row delete not enabled - no plan needed
			return null;
		}

		var attribute = persister.getAttributeMapping();

		final TableDescriptorAsTableMapping tableMapping = new TableDescriptorAsTableMapping(
				persister.getCollectionTableDescriptor(),
				0, // relativePosition
				false, // isIdentifierTable
				false // isInverse
		);

		var builder = new TableDeleteBuilderStandard(
				persister,
				new MutatingTableReference(tableMapping),
				factory,
				persister.getSqlWhereString()
		);

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// WHERE clause: restrict by
		// 		- key columns (restrict by owner FK)
		//		-  element/index

		attribute.getKeyDescriptor().getKeyPart().forEachSelectable( (index, jdbcMapping) -> {
			builder.addKeyRestrictionLeniently( jdbcMapping );
		} );

		// For row-based deletion, also restrict by element/index/identifier
		// This differentiates deleteRows (specific rows) from remove (entire collection)
		final var indexDescriptor = attribute.getIndexDescriptor();
		final var identifierDescriptor = attribute.getIdentifierDescriptor();
		if ( indexDescriptor != null ) {
			// For indexed collections (lists, maps), restrict by index
			indexDescriptor.forEachSelectable( (index, jdbcMapping) -> {
				builder.addKeyRestriction( jdbcMapping );
			} );
		}
		else if ( identifierDescriptor != null ) {
			// For IdBag collections, restrict by the synthetic identifier column
			identifierDescriptor.forEachSelectable( (index, jdbcMapping) -> {
				builder.addKeyRestriction( jdbcMapping );
			} );
		}
		else {
			// For non-indexed collections (sets, bags), restrict by element
			// todo : consider adding some form of `forEachSelectable` and `decompose` which is
			//  	local to the element descriptor -
			//  		* the value column(s) for element collections
			//			* the fk column(s) for to-many with join-table
			var elementDescriptor = attribute.getElementDescriptor();
			if ( elementDescriptor instanceof ManyToManyCollectionPart manyToMany ) {
				manyToMany.getForeignKeyDescriptor().getKeyPart().forEachSelectable( (index, jdbcMapping) -> {
					builder.addKeyRestriction( jdbcMapping );
				} );
			}
			else {
				elementDescriptor.forEachSelectable( (index, jdbcMapping) -> {
					builder.addKeyRestriction( jdbcMapping );
				} );
			}
		}

		return new CollectionJdbcOperations.DeleteRowPlan(
				builder.buildMutation().createMutationOperation( null, factory ),
				this::bindDeleteRestrictions
		);
	}

	/**
	 * For indexed entity collections (join tables with @OrderColumn), builds deletions
	 * tracking entity identity and snapshot position.
	 * Returns EntityPosition records where entity is the removed entity and position is its old index.
	 */
	private Iterator<?> buildIndexedEntityDeletions(PersistentCollection<?> collection) {
		// we have either a Map or a List, which need to be handled differently...
		if ( persister.getCollectionSemantics().getCollectionClassification() == CollectionClassification.MAP ) {
			final var snapshot = (Map<?, ?>) collection.getStoredSnapshot();
			if ( snapshot == null || snapshot.isEmpty() ) {
				return java.util.Collections.emptyIterator();
			}

			final var entries = collection.entries( persister );
			final List<Object> currentEntities = new ArrayList<>();
			while ( entries.hasNext() ) {
				currentEntities.add( entries.next() );
			}

			// Find entities in snapshot that are no longer in current map (by identity)
			final List<EntityPosition<?>> removedEntities = new ArrayList<>();
			for ( Map.Entry<?, ?> snapshotEntry : snapshot.entrySet() ) {
				final Object snapshotEntity = snapshotEntry.getValue();
				if ( snapshotEntity != null ) {
					boolean found = false;
					for ( Object currentEntity : currentEntities ) {
						if ( currentEntity == snapshotEntity ) {
							found = true;
							break;
						}
					}
					if ( !found ) {
						// For maps, use the actual map key (not iteration index)
						removedEntities.add( new EntityPosition<>( snapshotEntity, snapshotEntry.getKey() ) );
					}
				}
			}
			return removedEntities.iterator();
		}
		else {
			assert persister.getCollectionSemantics().getCollectionClassification() == CollectionClassification.LIST;

			final var snapshot = (List<?>) collection.getStoredSnapshot();
			if ( snapshot == null || snapshot.isEmpty() ) {
				return java.util.Collections.emptyIterator();
			}

			final var entries = collection.entries( persister );
			final List<Object> currentEntities = new ArrayList<>();
			while ( entries.hasNext() ) {
				currentEntities.add( entries.next() );
			}

			// Find entities in snapshot that are no longer in current list (by identity)
			final List<EntityPosition<Integer>> removedEntities = new ArrayList<>();
			for ( int snapshotIndex = 0; snapshotIndex < snapshot.size(); snapshotIndex++ ) {
				final Object snapshotEntity = snapshot.get( snapshotIndex );
				if ( snapshotEntity != null ) {
					boolean found = false;
					for ( Object currentEntity : currentEntities ) {
						if ( currentEntity == snapshotEntity ) {
							found = true;
							break;
						}
					}
					if ( !found ) {
						// For lists, use the numeric position
						removedEntities.add( new EntityPosition<>( snapshotEntity, snapshotIndex ) );
					}
				}
			}
			return removedEntities.iterator();
		}
	}

	/** Helper record to track entity and its snapshot position/key for indexed deletions */
	private record EntityPosition<K>(Object element, K snapshotIndex) implements SnapshotIndexed<K> {}

	private void bindDeleteRestrictions(
			PersistentCollection<?> collection,
			Object key,
			Object rowValue,
			int rowPosition,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		var attribute = persister.getAttributeMapping();

		attribute.getKeyDescriptor().getKeyPart().decompose(
				key,
				jdbcValueBindings::bindRestriction,
				session
		);

		// Handle SnapshotPositioned objects (Removal, EntityPosition, etc.)
		final Object actualElement;
		final Object actualKey;
		if ( rowValue instanceof SnapshotIndexed<?> positioned ) {
			actualElement = positioned.element();
			actualKey = positioned.snapshotIndex();
		}
		else {
			actualElement = rowValue;
			actualKey = rowPosition;
		}

		// For row-based deletion, also restrict by element/index/identifier
		// This differentiates deleteRows (specific rows) from remove (entire collection)
		final var indexDescriptor = attribute.getIndexDescriptor();
		final var identifierDescriptor = attribute.getIdentifierDescriptor();
		if ( indexDescriptor != null ) {
			// For indexed collections (lists, maps), restrict by index/key
			// - For Lists: actualKey is the numeric position (Integer)
			// - For Maps: actualKey is the actual map key (any type)
			final Object indexValue = (rowValue instanceof SnapshotIndexed<?>)
					? actualKey
					: rowValue;
			indexDescriptor.decompose(
					persister.incrementIndexByBase( indexValue ),
					jdbcValueBindings::bindUpdateRestriction,
					session
			);
		}
		else if ( identifierDescriptor != null ) {
			// For IdBag collections, restrict by the synthetic identifier
			// The identifier value is stored in the collection's snapshot
			identifierDescriptor.decompose(
					actualElement,  // For IdBag, actualElement is the identifier value
					jdbcValueBindings::bindRestriction,
					session
			);
		}
		else {
			// For non-indexed collections (sets, bags), restrict by element
			var elementDescriptor = attribute.getElementDescriptor();
			if ( elementDescriptor instanceof ManyToManyCollectionPart manyToMany ) {
				var id = manyToMany.getAssociatedEntityMappingType().getIdentifierMapping().getIdentifier( actualElement );
				manyToMany.getForeignKeyDescriptor().getKeyPart().decompose(
						id,
						jdbcValueBindings::bindRestriction,
						session
				);
			}
			else {
				elementDescriptor.decompose(
						actualElement,
						jdbcValueBindings::bindRestriction,
						session
				);
			}
		}
	}

	private MutationOperation buildRemoveOperation(SessionFactoryImplementor factory) {
		if ( !persister.needsRemove() ) {
			return null;
		}

		var tableDescriptor = persister.getCollectionTableDescriptor();
		var attribute = persister.getAttributeMapping();

		final TableDescriptorAsTableMapping tableMapping = new TableDescriptorAsTableMapping(
				tableDescriptor,
				0, // relativePosition
				false, // isIdentifierTable
				false // isInverse
		);
		var builder = new TableDeleteBuilderStandard(
				persister,
				new MutatingTableReference(tableMapping),
				tableDescriptor.deleteAllDetails(),
				persister.getSqlWhereString(),
				factory
		);

		attribute.getKeyDescriptor().getKeyPart().forEachSelectable( (index, jdbcMapping) -> {
			builder.addKeyRestrictionLeniently( jdbcMapping );
		} );

		return builder.buildMutation().createMutationOperation(null, factory);
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// BindPlan for collection removals (full deletion).

	public static class RemoveBindPlan implements BindPlan {
		private final Object key;
		private final BasicCollectionPersister mutationTarget;

		public RemoveBindPlan(Object key, BasicCollectionPersister mutationTarget) {
			this.key = key;
			this.mutationTarget = mutationTarget;
		}

		@Override
		public void execute(
				ExecutionContext context,
				PlannedOperation plannedOperation,
				SharedSessionContractImplementor session) {
			context.executeRow(
					plannedOperation,
					(valueBindings, s) -> {
						var fkDescriptor = mutationTarget.getAttributeMapping().getKeyDescriptor();
						fkDescriptor.getKeyPart().decompose(
								key,
								(valueIndex, value, jdbcValueMapping) -> {
									valueBindings.bindValue(
											value,
											jdbcValueMapping.getSelectionExpression(),
											ParameterUsage.RESTRICT
									);
								},
								session
						);
					},
					null
			);
		}
	}
}
