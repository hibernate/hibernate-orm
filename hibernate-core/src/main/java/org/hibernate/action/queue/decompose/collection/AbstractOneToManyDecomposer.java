/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.decompose.collection;

import org.hibernate.action.internal.CollectionRecreateAction;
import org.hibernate.action.internal.CollectionUpdateAction;
import org.hibernate.action.internal.QueuedOperationCollectionAction;
import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.exec.BindPlan;
import org.hibernate.action.queue.exec.JdbcValueBindings;
import org.hibernate.action.queue.decompose.DecompositionContext;
import org.hibernate.action.queue.meta.TableDescriptor;
import org.hibernate.action.queue.meta.TableDescriptorAsTableMapping;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.collection.spi.CollectionChangeSet;
import org.hibernate.jdbc.Expectation;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.ColumnValueParameterList;
import org.hibernate.sql.model.ast.ColumnWriteFragment;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.builder.CollectionRowDeleteByUpdateSetNullBuilder;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.model.internal.TableUpdateCustomSql;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.internal.EntityCollectionPart;
import org.hibernate.persister.collection.mutation.CollectionTableMapping;
import org.hibernate.persister.collection.OneToManyPersister;
import org.hibernate.persister.collection.mutation.OrderOnlyUpdateBindPlan;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.model.jdbc.JdbcMutationOperation;
import org.hibernate.sql.model.jdbc.JdbcValueDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static org.hibernate.action.queue.CollectionOrdinalSupport.Slot;
import static org.hibernate.action.queue.CollectionOrdinalSupport.calculateOrdinal;
import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;
import static org.hibernate.sql.model.ast.builder.TableMutationBuilder.NULL;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractOneToManyDecomposer implements OneToManyDecomposer {
	protected final OneToManyPersister persister;
	protected final SessionFactoryImplementor factory;

	public AbstractOneToManyDecomposer(
			OneToManyPersister persister,
			SessionFactoryImplementor factory) {
		this.persister = persister;
		this.factory = factory;
	}


	protected abstract CollectionJdbcOperations selectJdbcOperations(Object entry, SharedSessionContractImplementor session);

	@Override
	public void decomposeRecreate(
			CollectionRecreateAction action,
			int ordinalBase,
			SharedSessionContractImplementor session,
			DecompositionContext decompositionContext,
			Consumer<PlannedOperation> operationConsumer) {
		var collection = action.getCollection();
		var key = action.getKey();

		var attribute = persister.getAttributeMapping();

		// Always fire PRE event, even if no SQL operations will be needed
		DecompositionSupport.firePreRecreate( persister, collection, session );
		collection.preInsert( persister );

		// Create post-execution callback to handle post-execution work (afterAction, cache, events, stats)
		var postRecreateHandling = new PostCollectionRecreateHandling(
				persister,
				collection,
				action.getAffectedOwner(),
				action.getAffectedOwnerId(),
				DecompositionSupport.generateCacheKey( action, session )
		);
		final var postExecutionCallback = DecompositionSupport.withOwnerUpdateCallbacks(
				persister,
				affectedOwner( action.getAffectedOwner(), collection ),
				decompositionContext,
				postRecreateHandling
		);

		final var entries = collection.entries( persister );
		if ( !entries.hasNext() ) {
			// No entries - create no-op to defer POST callback
			operationConsumer.accept( DecompositionSupport.createNoOpCallbackCarrier(
					persister.getCollectionTableDescriptor(),
					calculateOrdinal( ordinalBase, Slot.INSERT ),
					postExecutionCallback
			) );
			return;
		}

		final List<PlannedOperation> operations = new ArrayList<>();

		var insertOrdinal = calculateOrdinal( ordinalBase, Slot.INSERT );
		var writeIndexOrdinal = calculateOrdinal( ordinalBase, Slot.WRITEINDEX );

		int entryCount = 0;
		while ( entries.hasNext() ) {
			final Object entry = entries.next();
			var jdbcOperations = selectJdbcOperations( entry, session );
			var insertRowPlan = jdbcOperations.insertRowPlan();

			// For inverse one-to-many collections, insertRowPlan will be null (inserts are managed by the owning side)
			if ( insertRowPlan != null && collection.includeInRecreate( entry, entryCount, collection, attribute ) ) {
				final BindPlan bindPlan = new SingleRowInsertBindPlan(
						persister,
						insertRowPlan.values(),
						collection,
						key,
						entry,
						entryCount
				);

				// For one-to-many collections, the "insert" is actually an UPDATE that sets the FK
				// Use MutationKind.UPDATE so it's ordered AFTER entity INSERTs via FK edges
				final PlannedOperation plannedOp = new PlannedOperation(
						persister.getCollectionTableDescriptor(),
						MutationKind.UPDATE,
						insertRowPlan.jdbcOperation(),
						bindPlan,
						insertOrdinal,
						"InsertRow[" + entryCount + "](" + persister.getRolePath() + ")"
				);

				operations.add( plannedOp );
			}

			if ( jdbcOperations.updateIndexPlan() != null ) {
				var writeIndexBindPlan = new SingleRowUpdateBindPlan(
						persister,
						collection,
						key,
						entry,
						entryCount,
						jdbcOperations.updateIndexPlan().values(),
						jdbcOperations.updateIndexPlan().restrictions()
				);


				var  writeIndexPlannedOp = new PlannedOperation(
						persister.getCollectionTableDescriptor(),
						MutationKind.UPDATE_ORDER,
						jdbcOperations.updateIndexPlan().jdbcOperation(),
						writeIndexBindPlan,
						writeIndexOrdinal,
						"WriteIndex[" + entryCount + "](" + persister.getRolePath() + ")"
				);

				//postExecCallbackRegistry.accept(... );
				// on recreate we should be able to write the index as a normal planned-op
				operations.add( writeIndexPlannedOp );
			}

			entryCount++;
		}

		if ( !operations.isEmpty() ) {
			// Attach it to the last operation
			operations.get( operations.size() - 1 ).setPostExecutionCallback( postExecutionCallback );
			operations.forEach( operationConsumer );
		}
		else {
			// Operations unexpectedly empty - create no-op to defer POST callback
			operationConsumer.accept( DecompositionSupport.createNoOpCallbackCarrier(
					persister.getCollectionTableDescriptor(),
					calculateOrdinal( ordinalBase, Slot.INSERT ),
					postExecutionCallback
			) );
		}
	}

	@Override
	public final void decomposeUpdate(
			CollectionUpdateAction action,
			int ordinalBase,
			SharedSessionContractImplementor session,
			DecompositionContext decompositionContext,
			Consumer<PlannedOperation> operationConsumer) {
		final var collection = action.getCollection();
		final var key = action.getKey();

		// Always fire PRE event, even if collection is not initialized
		DecompositionSupport.firePreUpdate( persister, collection, session );

		// Create callback to handle post-execution work (afterAction, cache, events, stats)
		var postUpdateHandling = new PostCollectionUpdateHandling(
				persister,
				collection,
				key,
				action.getAffectedOwner(),
				action.getAffectedOwnerId(),
				DecompositionSupport.generateCacheKey( action, session )
		);
		final var postExecutionCallback = DecompositionSupport.withOwnerUpdateCallbacks(
				persister,
				affectedOwner( action.getAffectedOwner(), collection ),
				decompositionContext,
				postUpdateHandling
		);

		final List<PlannedOperation> operations = new ArrayList<>();

		if ( !collection.wasInitialized() ) {
			// If the collection wasn't initialized, we cannot access entries/deletes
			// The collection should still be marked dirty for queued operations
			// We only need to notify the cache via the post-execution callback
		}
		else if ( !persister.isAffectedByEnabledFilters( session ) && collection.empty() ) {
			if ( !action.isEmptySnapshot() ) {
				final var removeOperation = buildRemoveOperation( persister.getCollectionTableDescriptor() );
				if ( removeOperation != null ) {
					operations.add( new PlannedOperation(
							persister.getCollectionTableDescriptor(),
							MutationKind.UPDATE,
							removeOperation,
							new RemoveBindPlan( key, persister ),
							calculateOrdinal( ordinalBase, Slot.DELETE ),
							"RemoveAllRows(" + persister.getRolePath() + ")"
					) );
				}
			}
		}
		else {
			final var changeSet = collection.getChangeSet( persister );
			if ( changeSet != null && !changeSet.shifts().isEmpty() && persister.hasIndex() ) {
				applyShiftChanges( changeSet, collection, key, ordinalBase, session, operations::add );
			}
			else {
				applyUpdateRemovals( collection, key, ordinalBase, session, operations::add );
				applyUpdateChanges( collection, key, ordinalBase + 1, session, operations::add );
				applyUpdateAdditions( collection, key, ordinalBase + 2, session, operations::add );
			}
		}

		if ( !operations.isEmpty() ) {
			// Attach post-execution callback to the last operation
			operations.get( operations.size() - 1 ).setPostExecutionCallback( postExecutionCallback );
			operations.forEach( operationConsumer );
		}
		else {
			// Operations empty (e.g., uninitialized dirty collection) - create no-op to defer POST callback
			operationConsumer.accept( DecompositionSupport.createNoOpCallbackCarrier(
					persister.getCollectionTableDescriptor(),
					calculateOrdinal( ordinalBase, Slot.UPDATE ),
					postExecutionCallback
			) );
		}
	}

	private Object affectedOwner(Object affectedOwner, PersistentCollection<?> collection) {
		return affectedOwner == null ? collection.getOwner() : affectedOwner;
	}

	private void applyShiftChanges(
			CollectionChangeSet changeSet,
			PersistentCollection<?> collection,
			Object key,
			int ordinalBase,
			SharedSessionContractImplementor session,
			Consumer<PlannedOperation> operationConsumer) {
		final int deleteOrdinal = calculateOrdinal( ordinalBase, Slot.DELETE );
		final int tempOrderOrdinal = calculateOrdinal( ordinalBase, Slot.UPDATE );
		final int writeIndexOrdinal = calculateOrdinal( ordinalBase, Slot.WRITEINDEX );
		final int finalOrderOrdinal = writeIndexOrdinal + 1;
		final int tempOffset = Integer.MAX_VALUE / 2;
		final List<PlannedOperation> finalOrderOperations = new ArrayList<>();

		for ( CollectionChangeSet.Shift shift : changeSet.shifts() ) {
			final var jdbcOperations = selectJdbcOperations( shift.element(), session );
			assert jdbcOperations != null;
			final var deleteRowPlan = jdbcOperations.deleteRowPlan();
			if ( deleteRowPlan != null ) {
				operationConsumer.accept( new PlannedOperation(
						persister.getCollectionTableDescriptor(),
						// technically an UPDATE
						MutationKind.UPDATE,
						wrapShiftOperation(
								deleteRowPlan.jdbcOperation(),
								true
						),
						new SingleRowDeleteBindPlan(
								persister,
								collection,
								key,
								shift.element(),
								deleteRowPlan.restrictions()
						),
						deleteOrdinal,
						"DeleteShift[" + shift.snapshotIndex() + "](" + persister.getRolePath() + ")"
				) );
			}
			else if ( jdbcOperations.updateIndexPlan() != null ) {
				final int tempPosition = tempOffset + ( (Number) shift.currentIndex() ).intValue();
				operationConsumer.accept( new PlannedOperation(
						persister.getCollectionTableDescriptor(),
						MutationKind.UPDATE_ORDER,
						jdbcOperations.updateIndexPlan().jdbcOperation(),
						new OrderOnlyUpdateBindPlan(
								collection,
								key,
								shift.element(),
								( (Number) shift.snapshotIndex() ).intValue(),
								tempPosition,
								jdbcOperations.updateIndexPlan().values(),
								jdbcOperations.updateIndexPlan().restrictions()
						),
						tempOrderOrdinal,
						"UpdateShiftTemp[" + shift.snapshotIndex() + "->" + tempPosition + "](" + persister.getRolePath() + ")"
				) );

				finalOrderOperations.add( new PlannedOperation(
						persister.getCollectionTableDescriptor(),
						MutationKind.UPDATE_ORDER,
						jdbcOperations.updateIndexPlan().jdbcOperation(),
						new OrderOnlyUpdateBindPlan(
								collection,
								key,
								shift.element(),
								tempPosition,
								( (Number) shift.currentIndex() ).intValue(),
								jdbcOperations.updateIndexPlan().values(),
								jdbcOperations.updateIndexPlan().restrictions()
						),
						finalOrderOrdinal,
						"UpdateShiftFinal[" + tempPosition + "->" + shift.currentIndex() + "](" + persister.getRolePath() + ")"
				) );
			}
		}

		final int insertOrdinal = calculateOrdinal( ordinalBase, Slot.INSERT );
		for ( CollectionChangeSet.Shift shift : changeSet.shifts() ) {
			final var jdbcOperations = selectJdbcOperations( shift.element(), session );
			assert jdbcOperations != null;
			final var insertRowPlan = jdbcOperations.insertRowPlan();
			if ( insertRowPlan != null ) {
				operationConsumer.accept( new PlannedOperation(
						persister.getCollectionTableDescriptor(),
						// technically an UPDATE
						MutationKind.UPDATE,
						wrapShiftOperation(
								insertRowPlan.jdbcOperation(),
								false
						),
						new SingleRowInsertBindPlan(
								persister,
								insertRowPlan.values(),
								collection,
								key,
								shift.element(),
								( (Number) shift.currentIndex() ).intValue()
						),
						insertOrdinal,
						"InsertShift[" + shift.currentIndex() + "](" + persister.getRolePath() + ")"
				) );
			}
		}

		for ( CollectionChangeSet.Addition addition : changeSet.additions() ) {
			final var jdbcOperations = selectJdbcOperations( addition.element(), session );
			assert jdbcOperations != null;
			final var insertRowPlan = jdbcOperations.insertRowPlan();
			if ( insertRowPlan != null ) {
				operationConsumer.accept( new PlannedOperation(
						persister.getCollectionTableDescriptor(),
						// technically an UPDATE
						MutationKind.UPDATE,
						insertRowPlan.jdbcOperation(),
						new SingleRowInsertBindPlan(
								persister,
								insertRowPlan.values(),
								collection,
								key,
								addition.element(),
								( (Number) addition.index() ).intValue()
						),
						insertOrdinal,
						"InsertRow[" + addition.index() + "](" + persister.getRolePath() + ")"
				) );
			}
			else {
				planWriteIndex(
						collection,
						key,
						addition.element(),
						( (Number) addition.index() ).intValue(),
						jdbcOperations.updateIndexPlan(),
						writeIndexOrdinal,
						"WriteIndex[" + addition.index() + "](" + persister.getRolePath() + ")",
						operationConsumer
				);
			}
		}

		finalOrderOperations.forEach( operationConsumer );
	}

	@Override
	public void decomposeQueuedOperations(
			QueuedOperationCollectionAction action,
			int ordinalBase,
			SharedSessionContractImplementor session,
			Consumer<PlannedOperation> operationConsumer) {
		final var collection = action.getCollection();
		final var key = action.getKey();
		final List<PlannedOperation> operations = new ArrayList<>();

		final int writeIndexOrdinal = calculateOrdinal( ordinalBase, Slot.WRITEINDEX );
		final List<QueuedIndexWrite> queuedIndexWrites = new ArrayList<>();
		final var queuedAdditions = collection.queuedAdditionIterator();
		while ( queuedAdditions.hasNext() ) {
			final Object entry = queuedAdditions.next();
			if ( entry != null ) {
				final var jdbcOperations = selectJdbcOperations( entry, session );
				assert jdbcOperations != null;
				if ( jdbcOperations.updateIndexPlan() != null ) {
					queuedIndexWrites.add( new QueuedIndexWrite( entry, jdbcOperations.updateIndexPlan() ) );
				}
			}
		}

		if ( !queuedIndexWrites.isEmpty() ) {
			int entryPosition = persister.getSize( key, session );
			for ( QueuedIndexWrite queuedIndexWrite : queuedIndexWrites ) {
				if ( collection.entryExists( queuedIndexWrite.entry(), entryPosition ) ) {
					planWriteIndex(
							collection,
							key,
							queuedIndexWrite.entry(),
							entryPosition,
							queuedIndexWrite.updateIndexPlan(),
							writeIndexOrdinal,
							"WriteQueuedIndex[" + entryPosition + "](" + persister.getRolePath() + ")",
							operations::add
					);
					entryPosition++;
				}
			}
		}

		operations.add( DecompositionSupport.createNoOpCallbackCarrier(
				persister.getCollectionTableDescriptor(),
				writeIndexOrdinal + 1,
				sessionImplementor -> action.afterQueuedOperationsProcessed()
		) );
		operations.forEach( operationConsumer );
	}

	private record QueuedIndexWrite(Object entry, CollectionJdbcOperations.UpdateRowPlan updateIndexPlan) {
	}

	@SuppressWarnings("removal")
	private MutationOperation wrapShiftOperation(MutationOperation operation, boolean ignoreRowCount) {
		if ( !( operation instanceof JdbcMutationOperation jdbcOperation ) ) {
			return operation;
		}

		return new JdbcMutationOperation() {
			@Override
			public String getSqlString() {
				return jdbcOperation.getSqlString();
			}

			@Override
			public Set<String> getAffectedTableNames() {
				return jdbcOperation.getAffectedTableNames();
			}

			@Override
			public List<JdbcParameterBinder> getParameterBinders() {
				return jdbcOperation.getParameterBinders();
			}

			@Override
			public boolean isCallable() {
				return jdbcOperation.isCallable();
			}

			@Override
			public Expectation getExpectation() {
				return ignoreRowCount ? Expectation.None.INSTANCE : jdbcOperation.getExpectation();
			}

			@Override
			public org.hibernate.sql.model.MutationType getMutationType() {
				return jdbcOperation.getMutationType();
			}

			@Override
			public org.hibernate.sql.model.MutationTarget<?, ?> getMutationTarget() {
				return jdbcOperation.getMutationTarget();
			}

			@Override
			public org.hibernate.sql.model.TableMapping getTableDetails() {
				return jdbcOperation.getTableDetails();
			}

			@Override
			public JdbcValueDescriptor findValueDescriptor(String columnName, ParameterUsage usage) {
				return jdbcOperation.findValueDescriptor( columnName, usage );
			}

			@Override
			public JdbcValueDescriptor resolveValueDescriptor(String tableName, String columnName, ParameterUsage usage) {
				return jdbcOperation.resolveValueDescriptor( tableName, columnName, usage );
			}
		};
	}

	protected void applyUpdateRemovals(
			PersistentCollection<?> collection,
			Object key,
			int ordinalBase,
			SharedSessionContractImplementor session,
			Consumer<PlannedOperation> operationConsumer) {
		if ( !persister.needsRemove() ) {
			// EARLY EXIT!!
			return;
		}

		final var deletes = collection.getDeletes( persister, true );
		if ( !deletes.hasNext() ) {
			MODEL_MUTATION_LOGGER.noRowsToDelete();
			// EARLY EXIT!!
			return;
		}

		var deleteOrdinal = calculateOrdinal( ordinalBase, Slot.DELETE );

		int deletionCount = 0;
		while ( deletes.hasNext() ) {
			var removal = deletes.next();

			var jdbcOperations = selectJdbcOperations( removal, session );
			assert jdbcOperations != null;
			var deleteRowPlan = jdbcOperations.deleteRowPlan();

			final BindPlan bindPlan = new SingleRowDeleteBindPlan(
					persister,
					collection,
					key,
					removal,
					deleteRowPlan.restrictions()
			);

			final PlannedOperation plannedOp = new PlannedOperation(
					persister.getCollectionTableDescriptor(),
					// technically an UPDATE
					MutationKind.UPDATE,
					deleteRowPlan.jdbcOperation(),
					bindPlan,
					deleteOrdinal,
					"DeleteRow[" + deletionCount + "](" + persister.getRole() + ")"
			);

			operationConsumer.accept( plannedOp );
			deletionCount++;
		}
	}

	protected void applyUpdateChanges(
			PersistentCollection<?> collection,
			Object key,
			int ordinalBase,
			SharedSessionContractImplementor session,
			Consumer<PlannedOperation> operationConsumer) {
		if ( !persister.isDoWriteEvenWhenInverse() ) {
			// EARLY EXIT!!
			return;
		}

		final var entries = collection.entries( persister);

		var updateOrdinal = calculateOrdinal( ordinalBase, Slot.UPDATE );

		int entryCount = 0;
		while ( entries.hasNext() ) {
			final Object entry = entries.next();

			var jdbcOperations = selectJdbcOperations( entry, session );
			assert jdbcOperations != null;
			var updateIndexPlan = jdbcOperations.updateIndexPlan();

			if ( updateIndexPlan != null && ( persister.hasIndex() || collection.needsUpdating( entry, entryCount, persister.getAttributeMapping() ) ) ) {
				final BindPlan bindPlan = new SingleRowUpdateBindPlan(
						persister,
						collection,
						key,
						entry,
						entryCount,
						updateIndexPlan.values(),
						updateIndexPlan.restrictions()
				);

				final PlannedOperation plannedOp = new PlannedOperation(
						persister.getCollectionTableDescriptor(),
						MutationKind.UPDATE_ORDER,
						updateIndexPlan.jdbcOperation(),
						bindPlan,
						updateOrdinal,
						"UpdateRow[" + entryCount + "](" + persister.getRolePath() + ")"
				);

				operationConsumer.accept( plannedOp );
			}

			entryCount++;
		}
	}

	protected void applyUpdateAdditions(
			PersistentCollection<?> collection,
			Object key,
			int ordinalBase,
			SharedSessionContractImplementor session,
			Consumer<PlannedOperation> operationConsumer) {
		// todo (ActionQueue2) : where should this come from?
		var allowInserts = true;
		if ( !allowInserts ) {
			// EARLY EXIT!!
			return;
		}

		final var entries = collection.entries( persister );
		if ( !entries.hasNext() ) {
			MODEL_MUTATION_LOGGER.noCollectionRowsToInsert( persister.getRolePath(), key );
			return;
		}

		collection.preInsert( persister );

		var insertOrdinal = calculateOrdinal( ordinalBase, Slot.INSERT );
		var writeIndexOrdinal = calculateOrdinal( ordinalBase, Slot.WRITEINDEX );

		int entryCount = 0;
		while ( entries.hasNext() ) {
			final Object entry = entries.next();
			if ( collection.includeInInsert( entry, entryCount, collection, persister.getAttributeMapping() ) ) {

				var jdbcOperations = selectJdbcOperations( entry, session );
				assert jdbcOperations != null;
				var insertRowPlan = jdbcOperations.insertRowPlan();

				// For inverse one-to-many collections, insertRowPlan will be null
				// (inserts are managed by the owning side)
				if ( insertRowPlan != null ) {
					final BindPlan bindPlan = new SingleRowInsertBindPlan(
							persister,
							insertRowPlan.values(),
							collection,
							key,
							entry,
							entryCount
					);

					// For one-to-many collections, the "insert" is actually an UPDATE that sets the FK
					// Use MutationKind.UPDATE so it's ordered AFTER entity INSERTs via FK edges
					final PlannedOperation plannedOp = new PlannedOperation(
							persister.getCollectionTableDescriptor(),
							// technically an UPDATE
							MutationKind.UPDATE,
							insertRowPlan.jdbcOperation(),
							bindPlan,
							insertOrdinal,
							"InsertRow[" + entryCount + "](" + persister.getRolePath() + ")"
					);

					operationConsumer.accept( plannedOp );
				}
				else {
					planWriteIndex(
							collection,
							key,
							entry,
							entryCount,
							jdbcOperations.updateIndexPlan(),
							writeIndexOrdinal,
							"WriteIndex[" + entryCount + "](" + persister.getRolePath() + ")",
							operationConsumer
					);
				}
			}

			entryCount++;
		}

		MODEL_MUTATION_LOGGER.doneInsertingCollectionRows( entryCount, persister.getRolePath() );
	}

	private void planWriteIndex(
			PersistentCollection<?> collection,
			Object key,
			Object entry,
			int entryPosition,
			CollectionJdbcOperations.UpdateRowPlan updateIndexPlan,
			int ordinal,
			String description,
			Consumer<PlannedOperation> operationConsumer) {
		if ( updateIndexPlan == null ) {
			return;
		}

		operationConsumer.accept( new PlannedOperation(
				persister.getCollectionTableDescriptor(),
				MutationKind.UPDATE_ORDER,
				updateIndexPlan.jdbcOperation(),
				new SingleRowUpdateBindPlan(
						persister,
						collection,
						key,
						entry,
						entryPosition,
						updateIndexPlan.values(),
						updateIndexPlan.restrictions()
				),
				ordinal,
				description
		) );
	}


	protected static class RemoveBindPlan implements BindPlan {
		private final Object key;
		private final OneToManyPersister mutationTarget;

		public RemoveBindPlan(Object key, OneToManyPersister mutationTarget) {
			this.key = key;
			this.mutationTarget = mutationTarget;
		}

		@Override
		public void execute(
				org.hibernate.action.queue.exec.ExecutionContext context,
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


	protected CollectionJdbcOperations buildJdbcOperations(
			TableDescriptor tableDescriptor,
			SessionFactoryImplementor factory) {
		final CollectionJdbcOperations.InsertRowPlan insertRowPlan = buildInsertRowPlan( tableDescriptor );
		final CollectionJdbcOperations.UpdateRowPlan updateIndexPlan = buildUpdateIndexPlan( tableDescriptor );
		final CollectionJdbcOperations.DeleteRowPlan deleteRowPlan = buildDeleteRowPlan( tableDescriptor );

		return new CollectionJdbcOperations(
				persister,
				insertRowPlan,
				null, // one-to-many doesn't update element values, only FK/ORDER
				updateIndexPlan,  // ORDER column updates for inverse @OrderColumn collections
				deleteRowPlan,
				buildRemoveOperation( tableDescriptor )
		);
	}

	/// Generates operation to perform "insert" SQL in the form -
	///
	/// ```sql
	/// update orders set customer_fk = ?, order_number = ? where id = ?
	/// ```
	///
	/// Which, in the case of a unidirectional one-to-many, actually "adds the collection row"
	private CollectionJdbcOperations.InsertRowPlan buildInsertRowPlan(TableDescriptor tableDescriptor) {
		if ( persister.isInverse() || !persister.isRowInsertEnabled() ) {
			return null;
		}

		final TableDescriptorAsTableMapping tableMapping = new TableDescriptorAsTableMapping(
				tableDescriptor,
				0, // relativePosition
				false, // isIdentifierTable
				false // isInverse
		);
		var updateBuilder = new TableUpdateBuilderStandard<>(
				persister,
				new MutatingTableReference(tableMapping),
				factory,
				persister.getSqlWhereString()
		);

		final var attributeMapping = persister.getAttributeMapping();
		attributeMapping.getKeyDescriptor().getKeyPart().forEachUpdatable( (index, selectableMapping) -> {
			updateBuilder.addColumnAssignment( selectableMapping );
		} );

		final var indexDescriptor = attributeMapping.getIndexDescriptor();
		if ( indexDescriptor != null ) {
			indexDescriptor.forEachUpdatable( (index, selectableMapping) -> {
				updateBuilder.addColumnAssignment( selectableMapping );
			} );
		}

		final var elementDescriptor = (EntityCollectionPart) attributeMapping.getElementDescriptor();
		final var elementType = elementDescriptor.getAssociatedEntityMappingType();
		elementType.getIdentifierMapping().forEachColumn( (index, selectableMapping) -> {
			updateBuilder.addKeyRestriction( selectableMapping );
		} );


		return new CollectionJdbcOperations.InsertRowPlan(
				updateBuilder.buildMutation().createMutationOperation(null, factory),
				this::applyInsertRowValues
		);
	}

	private void applyInsertRowValues(
			PersistentCollection<?> collection,
			Object keyValue,
			Object rowValue,
			int rowPosition,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		final var attributeMapping = persister.getAttributeMapping();
		attributeMapping.getKeyDescriptor().getKeyPart().decompose(
				keyValue,
				jdbcValueBindings::bindAssignment,
				session
		);
		final var indexDescriptor = attributeMapping.getIndexDescriptor();
		if ( indexDescriptor != null ) {
			indexDescriptor.decompose(
					persister.incrementIndexByBase( collection.getIndex( rowValue, rowPosition, persister ) ),
					(valueIndex, jdbcValue, jdbcValueMapping) -> {
						if ( jdbcValueMapping.isUpdateable() ) {
							jdbcValueBindings.bindAssignment( valueIndex, jdbcValue, jdbcValueMapping );
						}
					},
					session
			);
		}

		final Object elementValue = collection.getElement( rowValue );
		final var elementDescriptor = (EntityCollectionPart) attributeMapping.getElementDescriptor();
		final var identifierMapping = elementDescriptor.getAssociatedEntityMappingType().getIdentifierMapping();
		identifierMapping.decompose(
				identifierMapping.getIdentifier( elementValue ),
				jdbcValueBindings::bindRestriction,
				session
		);
	}

	private CollectionJdbcOperations.UpdateRowPlan buildUpdateIndexPlan(TableDescriptor tableDescriptor) {
		if ( !persister.isDoWriteEvenWhenInverse() ) {
			return null;
		}

		final TableDescriptorAsTableMapping tableMapping = new TableDescriptorAsTableMapping(
				tableDescriptor,
				0, // relativePosition
				false, // isIdentifierTable
				false // isInverse
		);
		final var updateBuilder = new TableUpdateBuilderStandard<>(
				persister,
				new MutatingTableReference(tableMapping),
				factory,
				persister.getSqlWhereString()
		);

		final var attributeMapping = persister.getAttributeMapping();

		attributeMapping.getIndexDescriptor().forEachUpdatable( (index, selectableMapping) -> {
			updateBuilder.addColumnAssignment( selectableMapping );
		} );

		// Add element identifier restrictions (WHERE clause for element's PK)
		final var elementDescriptor = (EntityCollectionPart) attributeMapping.getElementDescriptor();
		final var elementType = elementDescriptor.getAssociatedEntityMappingType();
		elementType.getIdentifierMapping().forEachColumn( (index, selectableMapping) -> {
			updateBuilder.addKeyRestriction( selectableMapping );
		} );

		return new CollectionJdbcOperations.UpdateRowPlan(
				updateBuilder.buildMutation().createMutationOperation(null, factory),
				this::applyWriteIndexValues,
				this::applyWriteIndexRestrictions
		);
	}

	private void applyWriteIndexValues(
			PersistentCollection<?> collection,
			Object key,
			Object entry,
			int entryPosition,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		persister.getAttributeMapping().getIndexDescriptor().decompose(
				persister.incrementIndexByBase( collection.getIndex( entry, entryPosition, persister ) ),
				(valueIndex, jdbcValue, jdbcValueMapping) -> {
					if ( jdbcValueMapping.isUpdateable() ) {
						jdbcValueBindings.bindValue(
								jdbcValue,
								jdbcValueMapping.getSelectionExpression(),
								ParameterUsage.SET
						);
					}
				},
				session
		);
	}

	private void applyWriteIndexRestrictions(
			PersistentCollection<?> collection,
			Object key,
			Object entry,
			int entryPosition,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		final var attributeMapping = persister.getAttributeMapping();
		final var elementDescriptor = (EntityCollectionPart) attributeMapping.getElementDescriptor();
		final var associatedType = elementDescriptor.getAssociatedEntityMappingType();
		final Object element = collection.getElement( entry );
		final var identifierMapping = associatedType.getIdentifierMapping();
		identifierMapping.decompose(
				identifierMapping.getIdentifier( element ),
				jdbcValueBindings::bindRestriction,
				session
		);
	}

	/// Generates operation to perform "delete" SQL in the form -
	///
	/// ```sql
	/// update orders set customer_fk = null, order_number = null where id = ?
	/// ```
	///
	/// Which, in the case of a unidirectional one-to-many, actually "removes the collection row"
	private CollectionJdbcOperations.DeleteRowPlan buildDeleteRowPlan(TableDescriptor collectionTableDescriptor) {
		if ( !persister.needsRemove() ) {
			return null;
		}

		final var tableReference = new MutatingTableReference( persister.getCollectionTableMapping() );
		final var updateBuilder = new CollectionRowDeleteByUpdateSetNullBuilder<>(
				persister,
				tableReference,
				factory,
				persister.getSqlWhereString()
		);

		final var foreignKeyDescriptor = persister.getAttributeMapping().getKeyDescriptor();
		foreignKeyDescriptor.getKeyPart().forEachUpdatable( (index, selectableMapping) -> {
			// set null
			updateBuilder.addColumnAssignment( selectableMapping, NULL );
			updateBuilder.addKeyRestrictionLeniently( selectableMapping );
		} );

		// set the value for each index column to null
		if ( persister.hasPhysicalIndexColumn() ) {
			final var indexDescriptor = persister.getAttributeMapping().getIndexDescriptor();
			assert indexDescriptor != null;
			indexDescriptor.forEachUpdatable( (index, selectableMapping) -> {
				updateBuilder.addColumnAssignment( selectableMapping, NULL );
			} );
		}

		// for one-to-many, we know the element is an entity and need to restrict the update
		// based on the element's id
		final var entityPart = (EntityCollectionPart) persister.getAttributeMapping().getElementDescriptor();
		entityPart.getAssociatedEntityMappingType().getIdentifierMapping().forEachColumn(  (index, selectableMapping) -> {
			updateBuilder.addKeyRestriction( selectableMapping );
		} );

		return new CollectionJdbcOperations.DeleteRowPlan(
				updateBuilder.buildMutation().createMutationOperation(null, factory),
				this::applyDeleteRowRestrictions
		);
	}

	private void applyDeleteRowRestrictions(
			PersistentCollection<?> collection,
			Object keyValue,
			Object rowValue,
			int rowPosition,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		final var foreignKeyDescriptor = persister.getAttributeMapping().getKeyDescriptor();
		foreignKeyDescriptor.getKeyPart().decompose(
				keyValue,
				(valueIndex, jdbcValue, jdbcValueMapping) -> {
					if ( !jdbcValueMapping.isFormula() ) {
						jdbcValueBindings.bindValue(
								jdbcValue,
								jdbcValueMapping.getSelectionExpression(),
								ParameterUsage.RESTRICT
						);
					}
				},
				session
		);

		final var entityPart = (EntityCollectionPart) persister.getAttributeMapping().getElementDescriptor();
		final var identifierMapping = entityPart.getAssociatedEntityMappingType().getIdentifierMapping();
		// Extract the ID from the actual element entity (rowValue), not the owner's FK (keyValue)
		final Object elementId = identifierMapping.getIdentifier( rowValue );
		identifierMapping.decompose(
				elementId,
				(index, val, jdbcMapping) -> jdbcValueBindings.bindValue(
						val,
						( jdbcMapping.getSelectionExpression() ),
						ParameterUsage.RESTRICT
				),
				session
		);
	}

	private MutationOperation buildRemoveOperation(TableDescriptor tableDescriptor) {
		if ( !persister.needsRemove() ) {
			return null;
		}

		final var collectionTableMapping = persister.getCollectionTableMapping();
		final var tableReference = new MutatingTableReference( collectionTableMapping );
		if ( collectionTableMapping.getDeleteDetails().getCustomSql() != null ) {
			return buildCustomDeleteAllOperation( tableReference, collectionTableMapping );
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

		final var attributeMapping = persister.getAttributeMapping();
		assert attributeMapping != null;

		final var foreignKeyDescriptor = attributeMapping.getKeyDescriptor();
		assert foreignKeyDescriptor != null;

		foreignKeyDescriptor.getKeyPart().forEachSelectable( (selectionIndex, selectableMapping) -> {
			builder.addColumnAssignment( selectableMapping, NULL );
			builder.addKeyRestrictionLeniently( selectableMapping );
		} );

		if ( persister.hasPhysicalIndexColumn() ) {
			attributeMapping.getIndexDescriptor().forEachColumn( (selectionIndex, selectableMapping) -> {
				if ( selectableMapping.isUpdateable() ) {
					builder.addColumnAssignment( selectableMapping, NULL );
				}
			} );
		}

		return builder.buildMutation().createMutationOperation(null, factory);
	}

	private MutationOperation buildCustomDeleteAllOperation(
			MutatingTableReference tableReference,
			CollectionTableMapping collectionTableMapping) {
		final var foreignKeyDescriptor = persister.getAttributeMapping().getKeyDescriptor();
		final var parameterBinders = new ColumnValueParameterList(
				tableReference,
				ParameterUsage.RESTRICT,
				foreignKeyDescriptor.getJdbcTypeCount()
		);
		final List<ColumnValueBinding> keyRestrictionBindings = new ArrayList<>( foreignKeyDescriptor.getJdbcTypeCount() );
		foreignKeyDescriptor.getKeyPart().forEachSelectable( (selectionIndex, selectableMapping) -> {
			if ( !selectableMapping.isFormula() ) {
				final var parameter = parameterBinders.addColumValueParameter( selectableMapping );
				keyRestrictionBindings.add(
						new ColumnValueBinding(
								parameter.getColumnReference(),
								new ColumnWriteFragment( "?", parameter, selectableMapping )
						)
				);
			}
		} );

		return wrapShiftOperation( new TableUpdateCustomSql(
				tableReference,
				persister,
				collectionTableMapping.getDeleteDetails(),
				"one-shot delete for " + persister.getRolePath(),
				Collections.emptyList(),
				keyRestrictionBindings,
				null,
				parameterBinders
		).createMutationOperation( null, factory ), true );
	}

}
