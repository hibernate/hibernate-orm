/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.bind.BindPlan;
import org.hibernate.action.queue.plan.PlannedOperation;
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
public class DeleteRowsCoordinatorStandard implements DeleteRowsCoordinator {
	private final CollectionMutationTarget mutationTarget;
	private final RowMutationOperations rowMutationOperations;
	private final boolean deleteByIndex;

	private final BasicBatchKey batchKey;
	private final MutationExecutorService mutationExecutorService;

	private MutationOperationGroup operationGroup;

	public DeleteRowsCoordinatorStandard(
			CollectionMutationTarget mutationTarget,
			RowMutationOperations rowMutationOperations,
			boolean deleteByIndex,
			ServiceRegistry serviceRegistry) {
		this.mutationTarget = mutationTarget;
		this.rowMutationOperations = rowMutationOperations;
		this.deleteByIndex = deleteByIndex;

		batchKey = new BasicBatchKey( mutationTarget.getRolePath() + "#DELETE" );
		mutationExecutorService = serviceRegistry.getService( MutationExecutorService.class );
	}

	@Override
	public CollectionMutationTarget getMutationTarget() {
		return mutationTarget;
	}

	@Override
	public void deleteRows(PersistentCollection<?> collection, Object key, SharedSessionContractImplementor session) {
		if ( operationGroup == null ) {
			operationGroup = createOperationGroup();
		}

		if ( MODEL_MUTATION_LOGGER.isTraceEnabled() ) {
			MODEL_MUTATION_LOGGER.deletingRemovedCollectionRows( mutationTarget.getRolePath(), key );
		}

		final var mutationExecutor = mutationExecutorService.createExecutor(
				() -> batchKey,
				operationGroup,
				session
		);
		final var jdbcValueBindings = mutationExecutor.getJdbcValueBindings();

		try {
			final var pluralAttribute = mutationTarget.getTargetPart();
			final var collectionDescriptor = pluralAttribute.getCollectionDescriptor();

			final var deletes = collection.getDeletes( collectionDescriptor, !deleteByIndex );
			if ( !deletes.hasNext() ) {
				MODEL_MUTATION_LOGGER.noRowsToDelete();
			}
			else {

				int deletionCount = 0;

				final var restrictions = rowMutationOperations.getDeleteRowRestrictions();

				while ( deletes.hasNext() ) {
					final Object removal = deletes.next();

					restrictions.applyRestrictions(
							collection,
							key,
							removal,
							deletionCount,
							session,
							jdbcValueBindings
					);

					mutationExecutor.execute( removal, null, null, null, session );

					deletionCount++;

				}

				MODEL_MUTATION_LOGGER.doneDeletingCollectionRows( deletionCount, mutationTarget.getRolePath() );
			}
		}
		finally {
			mutationExecutor.release();
		}
	}

	private MutationOperationGroup createOperationGroup() {
		assert mutationTarget.getTargetPart() != null
			&& mutationTarget.getTargetPart().getKeyDescriptor() != null;

		return singleOperation( MutationType.DELETE, mutationTarget,
				rowMutationOperations.getDeleteRowOperation() );
	}

	@Override
	public List<PlannedOperation> decomposeDeleteRows(
			PersistentCollection<?> collection,
			Object key,
			int ordinalBase,
			SharedSessionContractImplementor session) {
		if ( operationGroup == null ) {
			operationGroup = createOperationGroup();
		}

		final var operation = rowMutationOperations.getDeleteRowOperation();
		if ( operation == null ) {
			return List.of();
		}

		final var tableMapping = mutationTarget.getCollectionTableMapping();
		final String tableName = tableMapping.getTableName();

		final BindPlan bindPlan = new DeleteRowsBindPlan(
				collection,
				key,
				deleteByIndex,
				rowMutationOperations.getDeleteRowRestrictions(),
				mutationTarget
		);

		final PlannedOperation plannedOp = new PlannedOperation(
				tableName,
				MutationKind.DELETE,
				operation,
				bindPlan,
				ordinalBase * 1_000,
				"DeleteRowsCoordinator(" + mutationTarget.getRolePath() + ")"
		);

		return List.of( plannedOp );
	}

	private static class DeleteRowsBindPlan implements BindPlan {
		private final PersistentCollection<?> collection;
		private final Object key;
		private final boolean deleteByIndex;
		private final RowMutationOperations.Restrictions deleteRowRestrictions;
		private final CollectionMutationTarget mutationTarget;

		public DeleteRowsBindPlan(
				PersistentCollection<?> collection,
				Object key,
				boolean deleteByIndex,
				RowMutationOperations.Restrictions deleteRowRestrictions,
				CollectionMutationTarget mutationTarget) {
			this.collection = collection;
			this.key = key;
			this.deleteByIndex = deleteByIndex;
			this.deleteRowRestrictions = deleteRowRestrictions;
			this.mutationTarget = mutationTarget;
		}

		@Override
		public void bindAndMaybePatch(
				MutationExecutor executor,
				PlannedOperation operation,
				SharedSessionContractImplementor session) {
			// Binding happens in execute()
		}

		@Override
		public void execute(
				MutationExecutor executor,
				PlannedOperation operation,
				SharedSessionContractImplementor session) {
			final var pluralAttribute = mutationTarget.getTargetPart();
			final var collectionDescriptor = pluralAttribute.getCollectionDescriptor();
			final var deletes = collection.getDeletes( collectionDescriptor, !deleteByIndex );
			final var jdbcValueBindings = executor.getJdbcValueBindings();

			int deletionCount = 0;
			while ( deletes.hasNext() ) {
				final Object removal = deletes.next();

				deleteRowRestrictions.applyRestrictions(
						collection,
						key,
						removal,
						deletionCount,
						session,
						jdbcValueBindings
				);

				executor.execute( removal, null, null, null, session );
				deletionCount++;
			}
		}
	}
}
