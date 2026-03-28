/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import java.io.Serializable;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.action.internal.CollectionRecreateAction;
import org.hibernate.action.internal.CollectionRemoveAction;
import org.hibernate.action.internal.CollectionUpdateAction;
import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.bind.BindPlan;
import org.hibernate.action.queue.bind.JdbcValueBindings;
import org.hibernate.action.queue.exec.ExecutionContext;
import org.hibernate.action.queue.meta.CollectionTableDescriptor;
import org.hibernate.action.queue.meta.TableDescriptorAsTableMapping;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.persister.collection.BasicCollectionPersister;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.builder.TableDeleteBuilderStandard;
import org.hibernate.sql.model.ast.builder.TableInsertBuilderStandard;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderStandard;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import static org.hibernate.action.queue.CollectionOrdinalSupport.Slot;
import static org.hibernate.action.queue.CollectionOrdinalSupport.calculateOrdinal;
import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;

/// Decomposition support for [BasicCollectionPersister] which managed inserts, updates and deletes
/// into the collection table.
///
/// @author Steve Ebersole
public class BasicCollectionDecomposer extends AbstractCollectionDecomposer {
	private final BasicCollectionPersister persister;
	private final CollectionTableDescriptor tableDescriptor;
	private final boolean shouldBundleOperations;
	private final CollectionJdbcOperations jdbcOperations;

	public BasicCollectionDecomposer(
			BasicCollectionPersister persister,
			boolean shouldBundleOperations,
			SessionFactoryImplementor factory) {
		assert persister != null;

		this.persister = persister;
		this.shouldBundleOperations = shouldBundleOperations;
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
	/// > foreign-key + *old* index.
	/// >
	/// > But that's the rub - currently Hibernate does not track the old index for elements.
	/// > Ultimately I'd like to have this work better, but it would require knowledge of the old index
	/// > allowing indexed updates where the graph builder and planner manage the unique-key edges for
	/// > deciding which operations are needed first.
	private boolean needsRecreateForIndexedShifts(PersistentCollection<?> collection, BasicCollectionPersister persister) {
		if ( !persister.hasIndex() ) {
			return false;
		}

		final Serializable snapshot = collection.getStoredSnapshot();
		if ( snapshot == null ) {
			return false;
		}

		final int snapshotSize;
		if ( snapshot instanceof java.util.Collection ) {
			snapshotSize = ((java.util.Collection<?>) snapshot).size();
		}
		else {
			// For maps or other types, no position shift issue
			return false;
		}

		// For indexed collections, if the size increased AND there are updates,
		// we likely have position shifts (elements inserted in the middle).
		// This is conservative but safe - we use recreate to avoid data corruption.
		final var entries = collection.entries( persister );
		boolean hasUpdates = false;
		int currentSize = 0;

		while ( entries.hasNext() ) {
			final Object entry = entries.next();
			if ( collection.needsUpdating( entry, currentSize, persister.getAttributeMapping() ) ) {
				hasUpdates = true;
			}
			currentSize++;
		}

		// If size increased and there are updates, assume position shifts
		return currentSize > snapshotSize && hasUpdates;
	}

	public List<PlannedOperation> decomposeRecreate(
			CollectionRecreateAction action,
			int ordinalBase,
			SharedSessionContractImplementor session) {
		// Create callback to handle post-execution work (afterAction, cache, events, stats)
		final Object cacheKey = lockCacheItem( action, session );
		final PostCollectionRecreateHandling postCollectionRecreateHandling = new PostCollectionRecreateHandling( action, cacheKey );

		final List<PlannedOperation> operations = planRecreateOperation( action.getCollection(), action.getKey(), ordinalBase, session );

		// Attach post-execution callback to the last operation
		if ( !operations.isEmpty() ) {
			operations.get( operations.size() - 1 ).setPostExecutionCallback( postCollectionRecreateHandling );
		}

		return operations;
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

		if ( shouldBundleOperations ) {
			// Bundled: all rows in a single PlannedOperation with a bundled BindPlan
			final List<Object> entryList = new ArrayList<>();
			final List<Integer> entryIndices = new ArrayList<>();
			int entryCount = 0;

			while ( entries.hasNext() ) {
				final Object entry = entries.next();
				boolean include = collection.includeInRecreate( entry, entryCount, collection, persister.getAttributeMapping() );

				if ( include ) {
					entryList.add( entry );
					entryIndices.add( entryCount );
				}

				entryCount++;
			}

			if ( !entryList.isEmpty() ) {
				final BindPlan bundledBindPlan = new BundledCollectionInsertBindPlan(
						insertRowPlan.values(),
						collection,
						key,
						entryList,
						entryIndices
				);

				operations.add( new PlannedOperation(
						tableDescriptor,
						MutationKind.INSERT,
						insertRowPlan.jdbcOperation(),
						bundledBindPlan,
						insertOrdinal,
						"InsertRows(" + persister.getRolePath() + ")"
				) );
			}
		}
		else {
			// Non-bundled: one operation per row
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
		}

		return operations;
	}

	public List<PlannedOperation> decomposeUpdate(
			CollectionUpdateAction action,
			int ordinalBase,
			SharedSessionContractImplementor session) {
		action.preUpdate();

		final Object cacheKey = lockCacheItem(action, session);

		// Create callback to handle post-execution work (afterAction, cache, events, stats)
		final PostCollectionUpdateHandling postCollectionUpdateHandling = new PostCollectionUpdateHandling(
				persister,
				action.getCollection(),
				action.getKey(),
				action.getAffectedOwner(),
				action.getAffectedOwnerId(),
				cacheKey
		);

		var collection = action.getCollection();
		var key = action.getKey();
		var attribute = persister.getAttributeMapping();

		final List<PlannedOperation> operations = new ArrayList<>();

		if ( persister.isPerformingUpdates() ) {

		}
		else if ( !collection.wasInitialized() ) {
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
						operations.addAll( planRemoveOperation( key, ordinalBase, session ) );
					}
				}
				else if ( collection.needsRecreate( persister ) || needsRecreateForIndexedShifts( collection, persister ) ) {
					if ( affectedByFilters ) {
						throw new HibernateException( String.format( Locale.ROOT,
								"cannot recreate collection while filter is enabled [%s : %s]",
								persister.getRole(),
								key
						) );
					}
					if ( !action.isEmptySnapshot() ) {
						operations.addAll( planRemoveOperation( key, ordinalBase, session ) );
					}
					// Recreate INSERTs use INSERT_OFFSET which is higher than DELETE_OFFSET to avoid unique constraint violations
					operations.addAll( planRecreateOperation(  collection, key, ordinalBase, session ) );
				}
				else {
					planDeleteRowOperations( collection, key, ordinalBase, session, operations::add );

					if ( shouldBundleOperations ) {
						planBundledChangeAndAdditionOperations( collection, key, ordinalBase, session, operations::add );
					}
					else {
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

		// Attach post-execution callback to the last operation
		if ( !operations.isEmpty() ) {
			operations.get( operations.size() - 1 ).setPostExecutionCallback( postCollectionUpdateHandling );
		}
		else {
			postCollectionUpdateHandling.handle( (SessionImplementor) session );
		}

		return operations;
	}

	private void planDeleteRowOperations(
			PersistentCollection<?> collection,
			Object key,
			int ordinalBase,
			SharedSessionContractImplementor session,
			Consumer<PlannedOperation> operationConsumer) {
		var deleteRowPlan = jdbcOperations.deleteRowPlan();
		final var deletes = collection.getDeletes( persister, !persister.hasPhysicalIndexColumn() );
		if ( deleteRowPlan == null || !deletes.hasNext() ) {
			MODEL_MUTATION_LOGGER.noRowsToDelete();
			return;
		}

		var deleteOrdinal = calculateOrdinal( ordinalBase, Slot.DELETE );

		if ( shouldBundleOperations ) {
			// Bundle all rows into a single PlannedOperation with a bundled BindPlan
			final List<Object> deletionList = new ArrayList<>();

			while ( deletes.hasNext() ) {
				deletionList.add( deletes.next() );
			}

			if ( !deletionList.isEmpty() ) {
				final BindPlan bundledBindPlan = new BundledCollectionDeleteBindPlan(
						collection,
						key,
						deleteRowPlan.restrictions(),
						deletionList
				);

				operationConsumer.accept( new PlannedOperation(
						tableDescriptor,
						MutationKind.DELETE,
						deleteRowPlan.jdbcOperation(),
						bundledBindPlan,
						deleteOrdinal,
						"DeleteRows(" + persister.getRolePath() + ")"
				) );
			}
		}
		else {
			// Original behavior: one operation per row
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
	}

	private void planBundledChangeAndAdditionOperations(
			PersistentCollection<?> collection,
			Object key,
			int ordinalBase,
			SharedSessionContractImplementor session,
			Consumer<PlannedOperation> operationConsumer) {
		assert shouldBundleOperations;

		var updateRowPlan = jdbcOperations.updateRowPlan();
		var insertRowPlan = jdbcOperations.insertRowPlan();
		var entries = collection.entries( persister );

		if ( (updateRowPlan != null || insertRowPlan != null) && entries.hasNext() ) {
			var changeEntries = updateRowPlan == null ? null : new ArrayList<BundledBindPlanEntry>();
			var additionEntries = insertRowPlan == null ? null : new ArrayList<BundledBindPlanEntry>();
			int entryCount = 0;

			while ( entries.hasNext() ) {
				final Object entry = entries.next();

				var isAddition = collection.needsInserting( entry, entryCount, persister.getElementType() );
				var isChange = collection.needsUpdating( entry, entryCount, persister.getAttributeMapping() );

				if ( isAddition && isChange ) {
					// Log a warning?  This typically means bad equals/hashCode, though can happen I guess
					// with UserCollectionType too...
				}
				if ( updateRowPlan != null && isChange ) {
					changeEntries.add( new BundledBindPlanEntry( entry, entryCount ) );
				}
				if ( insertRowPlan != null && isAddition ) {
					additionEntries.add( new BundledBindPlanEntry( entry, entryCount ) );
				}

				entryCount++;
			}

			// UPDATE modified entries
			applyBundledUpdateChanges( collection, key, ordinalBase, changeEntries, updateRowPlan, operationConsumer );

			// INSERT entries
			applyBundledUpdateAdditions( collection, key, ordinalBase, additionEntries, insertRowPlan, operationConsumer );
		}
	}

	protected void applyBundledUpdateChanges(
			PersistentCollection<?> collection,
			Object key,
			int ordinalBase,
			List<BundledBindPlanEntry> changeEntries,
			CollectionJdbcOperations.UpdateRowPlan updateRowPlan,
			Consumer<PlannedOperation> operationConsumer) {
		if ( CollectionHelper.isEmpty( changeEntries ) ) {
			return;
		}

		// For indexed collections where elements have been removed, process UPDATEs in reverse order
		// to avoid unique constraint violations. When elements shift positions, processing from
		// highest index to lowest ensures elements move "out of the way" before lower indices
		// need their positions.
		final List<BundledBindPlanEntry> orderedEntries;
		if ( collection.isElementRemoved() ) {
			orderedEntries = new ArrayList<>( changeEntries );
			java.util.Collections.reverse( orderedEntries );
		}
		else {
			orderedEntries = changeEntries;
		}

		final BindPlan bundledBindPlan = new BundledCollectionUpdateBindPlan(
				collection,
				key,
				updateRowPlan.values(),
				updateRowPlan.restrictions(),
				orderedEntries
		);

		operationConsumer.accept( new PlannedOperation(
				persister.getCollectionTableDescriptor(),
				MutationKind.UPDATE,
				updateRowPlan.jdbcOperation(),
				bundledBindPlan,
				calculateOrdinal( ordinalBase, Slot.UPDATE ),
				"BundledUpdateRows(" + persister.getRolePath() + ")"
		) );
	}

	protected void applyBundledUpdateAdditions(
			PersistentCollection<?> collection,
			Object key,
			int ordinalBase,
			List<BundledBindPlanEntry> additionEntries,
			CollectionJdbcOperations.InsertRowPlan insertRowPlan,
			Consumer<PlannedOperation> operationConsumer) {
		if ( CollectionHelper.isEmpty( additionEntries ) ) {
			return;
		}

		// Pre-insert callback once for the whole collection
		collection.preInsert( persister );

		final BindPlan bundledBindPlan = new BundledCollectionInsertBindPlan(
				insertRowPlan.values(),
				collection,
				key,
				additionEntries
		);

		operationConsumer.accept( new PlannedOperation(
				persister.getCollectionTableDescriptor(),
				MutationKind.INSERT,
				insertRowPlan.jdbcOperation(),
				bundledBindPlan,
				calculateOrdinal( ordinalBase, Slot.INSERT ),
				"BundledInsertRows(" + persister.getRolePath() + ")"
		) );
	}

	private void planUpdateRowOperations(
			PersistentCollection<?> collection,
			Object key,
			int ordinalBase,
			SharedSessionContractImplementor session,
			Consumer<PlannedOperation> operationConsumer) {
		var updateRowPlan = jdbcOperations.updateRowPlan();
		final var entries = collection.entries( persister );

		if ( updateRowPlan == null || !entries.hasNext() ) {
			// EARLY EXIT!!
			return;
		}

		var updateOrdinal = calculateOrdinal( ordinalBase, Slot.UPDATE );

		// Collect all update operations first
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

		// Add operations to consumer in the correct order
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
		final var entries = collection.entries( persister );

		if ( insertRowPlan == null || !entries.hasNext() ) {
			// EARLY EXIT!!
			return;
		}

		var insertOrdinal = calculateOrdinal( ordinalBase, Slot.INSERT );

		// One operation per row
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

	public List<PlannedOperation> decomposeRemove(
			CollectionRemoveAction action,
			int ordinalBase,
			SharedSessionContractImplementor session) {
		// Create callback to handle post-execution work (afterAction, cache, events, stats)
		final Object cacheKey = lockCacheItem( action, session );
		final PostCollectionRemoveHandling postCollectionRemoveHandling = new PostCollectionRemoveHandling( action, cacheKey );

		final List<PlannedOperation> operations = planRemoveOperation( action.getKey(), ordinalBase,  session );

		// Attach post-execution callback to the last operation
		if ( !operations.isEmpty() ) {
			operations.get( operations.size() - 1 ).setPostExecutionCallback( postCollectionRemoveHandling );
		}

		return operations;
	}

	private List<PlannedOperation> planRemoveOperation(Object key, int ordinalBase, SharedSessionContractImplementor session) {
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

		final CollectionJdbcOperations.DeleteRowPlan deleteRowPlan = buildDeleteRowPlan( factory );

		return new CollectionJdbcOperations(
				persister,
				insertRowPlan,
				updateRowPlan,
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
			insertBuilder.addValueColumn( columnMapping );
		});

		final var identifierDescriptor = attributeMapping.getIdentifierDescriptor();
		final var indexDescriptor = attributeMapping.getIndexDescriptor();
		if ( identifierDescriptor != null ) {
			identifierDescriptor.forEachInsertable( (i, columnMapping) -> {
				insertBuilder.addValueColumn( columnMapping );
			} );
		}
		else if ( indexDescriptor != null ) {
			indexDescriptor.forEachInsertable( (i, columnMapping) -> {
				insertBuilder.addValueColumn( columnMapping );
			} );
		}

		// Add element columns
		attributeMapping.getElementDescriptor().forEachInsertable( (i, columnMapping) -> {
			insertBuilder.addValueColumn( columnMapping );
		} );

		final var softDeleteMapping = attributeMapping.getSoftDeleteMapping();
		if ( softDeleteMapping != null ) {
			final var columnReference = new ColumnReference( insertBuilder.getMutatingTable(), softDeleteMapping );
			insertBuilder.addValueColumn( softDeleteMapping.createNonDeletedValueBinding( columnReference ) );
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
						jdbcValueBindings::bindAssignment,
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
		var builder = new TableUpdateBuilderStandard(
				persister,
				new MutatingTableReference(tableMapping),
				factory,
				persister.getSqlWhereString()
		);

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// SET clause: element columns

		attribute.getElementDescriptor().forEachUpdatable(
			(selectionIndex, jdbcMapping) -> {
				builder.addValueColumn( jdbcMapping );
			}
		);

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// WHERE clause: key columns (restrict by owner FK) + index/element for row identification

		attribute.getKeyDescriptor().getKeyPart().forEachColumn(
			(selectionIndex, jdbcMapping) -> {
				builder.addKeyRestriction( jdbcMapping );
			}
		);

		// For indexed collections, also restrict by the OLD index value to identify the specific row
		// This is critical to avoid updating all rows when index values change
		final var indexDescriptor = attribute.getIndexDescriptor();
		if ( indexDescriptor != null ) {
			indexDescriptor.forEachSelectable( (index, jdbcMapping) -> {
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

		elementDescriptor.decompose(
				rowValue,
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

		// Bind index/element to identify the specific row to update
		final var indexDescriptor = attribute.getIndexDescriptor();
		if ( indexDescriptor != null ) {
			// For indexed collections (lists, maps), restrict by the index position
			// This identifies which row in the collection table to update
			indexDescriptor.decompose(
					collection.getIndex( rowValue, rowPosition, persister ),
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

	private CollectionJdbcOperations.DeleteRowPlan buildDeleteRowPlan(SessionFactoryImplementor factory) {
		if ( persister.isRowDeleteEnabled() ) {
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
			builder.addKeyRestriction( jdbcMapping );
		} );

		// For row-based deletion, also restrict by element/index
		// This differentiates deleteRows (specific rows) from remove (entire collection)
		final var indexDescriptor = attribute.getIndexDescriptor();
		if ( indexDescriptor != null ) {
			// For indexed collections (lists, maps), restrict by index
			indexDescriptor.forEachSelectable( (index, jdbcMapping) -> {
				builder.addKeyRestriction( jdbcMapping );
			} );
		}
		else {
			// For non-indexed collections (sets, bags), restrict by element
			attribute.getElementDescriptor().forEachSelectable((index, jdbcMapping) -> {
				builder.addKeyRestriction( jdbcMapping );
			} );
		}

		return new CollectionJdbcOperations.DeleteRowPlan(
				builder.buildMutation().createMutationOperation( null, factory ),
				this::bindDeleteRestrictions
		);
	}

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

		// For row-based deletion, also restrict by element/index
		// This differentiates deleteRows (specific rows) from remove (entire collection)
		final var indexDescriptor = attribute.getIndexDescriptor();
		if ( indexDescriptor != null ) {
			// For indexed collections (lists, maps), restrict by index
			// NOTE: For Maps, rowValue from getDeletes() is already the key (or value if indexIsFormula),
			// not a Map.Entry, so we use it directly instead of calling collection.getIndex()
			// which expects an Entry.
			final Object indexValue = (collection instanceof org.hibernate.collection.spi.PersistentMap)
					? rowValue
					: collection.getIndex( rowValue, rowPosition, persister );
			indexDescriptor.decompose(
					indexValue,
					jdbcValueBindings::bindRestriction,
					session
			);
		}
		else {
			// For non-indexed collections (sets, bags), restrict by element
			attribute.getElementDescriptor().decompose(
					rowValue,
					jdbcValueBindings::bindRestriction,
					session
			);
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
			builder.addKeyRestriction( jdbcMapping );
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
