/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;


import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.StatementShapeKey;
import org.hibernate.action.queue.bind.BindPlan;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.action.queue.plan.PlannedOperationGroup;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.jdbc.mutation.spi.MutationExecutorService;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.MutationType;

import java.util.List;

import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;
import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;

/**
 * @author Steve Ebersole
 */
public class InsertRowsCoordinatorStandard implements InsertRowsCoordinator {
	private final CollectionMutationTarget mutationTarget;
	private final RowMutationOperations rowMutationOperations;

	private final BasicBatchKey batchKey;
	private final MutationExecutorService mutationExecutorService;

	private MutationOperationGroup operationGroup;

	public InsertRowsCoordinatorStandard(
			CollectionMutationTarget mutationTarget,
			RowMutationOperations rowMutationOperations,
			ServiceRegistry serviceRegistry) {
		this.mutationTarget = mutationTarget;
		this.rowMutationOperations = rowMutationOperations;

		batchKey = new BasicBatchKey( mutationTarget.getRolePath() + "#INSERT" );
		mutationExecutorService = serviceRegistry.getService( MutationExecutorService.class );
	}

	@Override
	public String toString() {
		return "InsertRowsCoordinator(" + mutationTarget.getRolePath() + ")";
	}

	@Override
	public CollectionMutationTarget getMutationTarget() {
		return mutationTarget;
	}

	@Override
	public void insertRows(
			PersistentCollection<?> collection,
			Object id,
			EntryFilter entryChecker,
			SharedSessionContractImplementor session) {
		if ( operationGroup == null ) {
			operationGroup = createOperationGroup();
		}

		if ( MODEL_MUTATION_LOGGER.isTraceEnabled() ) {
			MODEL_MUTATION_LOGGER.insertingNewCollectionRows( mutationTarget.getRolePath(), id );
		}

		final var pluralAttribute = mutationTarget.getTargetPart();
		final var collectionDescriptor = pluralAttribute.getCollectionDescriptor();

		final var mutationExecutor = mutationExecutorService.createExecutor(
				() -> batchKey,
				operationGroup,
				session
		);
		final var jdbcValueBindings = mutationExecutor.getJdbcValueBindings();

		try {
			final var entries = collection.entries( collectionDescriptor );
			collection.preInsert( collectionDescriptor );
			if ( !entries.hasNext() ) {
				MODEL_MUTATION_LOGGER.noCollectionRowsToInsert( mutationTarget.getRolePath(), id );
				return;
			}


			int entryCount = 0;
			final var insertRowValues = rowMutationOperations.getInsertRowValues();

			while ( entries.hasNext() ) {
				final Object entry = entries.next();

				if ( entryChecker == null || entryChecker.include( entry, entryCount, collection, pluralAttribute ) ) {
					// if the entry is included, perform the "insert"
					insertRowValues.applyValues(
							collection,
							id,
							entry,
							entryCount,
							session,
							jdbcValueBindings
					);
					mutationExecutor.execute( entry, null, null, null, session );
				}

				entryCount++;
			}

			MODEL_MUTATION_LOGGER.doneInsertingCollectionRows( entryCount, mutationTarget.getRolePath() );

		}
		finally {
			mutationExecutor.release();
		}
	}

	private MutationOperationGroup createOperationGroup() {
		assert mutationTarget.getTargetPart() != null
			&& mutationTarget.getTargetPart().getKeyDescriptor() != null;

		final var operation = rowMutationOperations.getInsertRowOperation();
		return singleOperation( MutationType.INSERT, mutationTarget, operation );
	}

	@Override
	public List<PlannedOperationGroup> decomposeInsertRows(
			PersistentCollection<?> collection,
			Object key,
			EntryFilter entryFilter,
			int ordinalBase,
			SharedSessionContractImplementor session) {
		if ( operationGroup == null ) {
			operationGroup = createOperationGroup();
		}

		final var operation = rowMutationOperations.getInsertRowOperation();
		if ( operation == null ) {
			return List.of();
		}

		final var tableMapping = mutationTarget.getCollectionTableMapping();
		final String tableName = tableMapping.getTableName();

		// Create bind plan that delegates to this coordinator
		final BindPlan bindPlan = new InsertRowsBindPlan(
				collection,
				key,
				entryFilter,
				rowMutationOperations.getInsertRowValues(),
				mutationTarget
		);

		final PlannedOperation plannedOp = new PlannedOperation(
				tableName,
				MutationKind.INSERT,
				operation,
				bindPlan,
				ordinalBase * 1_000,
				"InsertRowsCoordinator(" + mutationTarget.getRolePath() + ")"
		);

		final List<PlannedOperation> operations = List.of( plannedOp );
		final PlannedOperationGroup group = new PlannedOperationGroup(
				tableName,
				MutationKind.INSERT,
				StatementShapeKey.forInsert( tableName, operations ),
				operations,
				false,
				ordinalBase * 1_000,
				"InsertRowsCoordinator(" + mutationTarget.getRolePath() + ")"
		);

		return List.of( group );
	}

	/**
	 * Bind plan for insert rows operations.
	 */
	private static class InsertRowsBindPlan implements BindPlan {
		private final PersistentCollection<?> collection;
		private final Object key;
		private final EntryFilter entryFilter;
		private final RowMutationOperations.Values insertRowValues;
		private final CollectionMutationTarget mutationTarget;

		public InsertRowsBindPlan(
				PersistentCollection<?> collection,
				Object key,
				EntryFilter entryFilter,
				RowMutationOperations.Values insertRowValues,
				CollectionMutationTarget mutationTarget) {
			this.collection = collection;
			this.key = key;
			this.entryFilter = entryFilter;
			this.insertRowValues = insertRowValues;
			this.mutationTarget = mutationTarget;
		}

		@Override
		public void bindAndMaybePatch(
				MutationExecutor executor,
				PlannedOperation operation,
				SharedSessionContractImplementor session) {
			// Binding happens in execute() since we iterate over entries
		}

		@Override
		public void execute(
				MutationExecutor executor,
				PlannedOperation operation,
				SharedSessionContractImplementor session) {
			final var pluralAttribute = mutationTarget.getTargetPart();
			final var collectionDescriptor = pluralAttribute.getCollectionDescriptor();
			final var entries = collection.entries( collectionDescriptor );
			final var jdbcValueBindings = executor.getJdbcValueBindings();

			collection.preInsert( collectionDescriptor );

			int entryCount = 0;
			while ( entries.hasNext() ) {
				final Object entry = entries.next();

				if ( entryFilter == null || entryFilter.include( entry, entryCount, collection, pluralAttribute ) ) {
					insertRowValues.applyValues(
							collection,
							key,
							entry,
							entryCount,
							session,
							jdbcValueBindings
					);
					executor.execute( entry, null, null, null, session );
				}

				entryCount++;
			}
		}
	}
}
