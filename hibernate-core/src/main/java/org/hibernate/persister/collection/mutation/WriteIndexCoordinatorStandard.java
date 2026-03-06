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
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.sql.model.MutationType;

import java.util.Iterator;
import java.util.List;

import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;

/**
 * Standard WriteIndexCoordinator implementation for collections with {@code @OrderColumn}.
 * <p>
 * Handles updating the order column values (list indices) for indexed collections.
 *
 * @author Steve Ebersole
 */
public class WriteIndexCoordinatorStandard implements WriteIndexCoordinator {
	private final CollectionMutationTarget mutationTarget;
	private final RowMutationOperations rowMutationOperations;
	private final MutationExecutorService mutationExecutorService;

	public WriteIndexCoordinatorStandard(
			CollectionMutationTarget mutationTarget,
			RowMutationOperations rowMutationOperations,
			SessionFactoryImplementor sessionFactory) {
		this.mutationTarget = mutationTarget;
		this.rowMutationOperations = rowMutationOperations;
		this.mutationExecutorService = sessionFactory.getServiceRegistry()
				.requireService( MutationExecutorService.class );
	}

	@Override
	public String toString() {
		return "WriteIndexCoordinator(" + mutationTarget.getRolePath() + ")";
	}

	@Override
	public CollectionMutationTarget getMutationTarget() {
		return mutationTarget;
	}

	@Override
	public void writeIndex(
			PersistentCollection<?> collection,
			Iterator<?> entries,
			Object key,
			boolean resetIndex,
			SharedSessionContractImplementor session) {
		// See HHH-5732 and HHH-18830.
		// Claim: "If one-to-many and inverse, still need to create the index."
		// In fact this is wrong: JPA is very clear that bidirectional associations
		// are persisted from the owning side. However, since this is a very ancient
		// mistake, I have fixed it in a backward-compatible way, by writing to the
		// order column if there is no mapping at all for it on the other side.
		// But if the owning entity does have a mapping for the order column, don't
		// do superfluous SQL UPDATEs here from the unowned side, no matter how many
		// users complain.
		if ( !entries.hasNext() ) {
			return;
		}

		final var updateRowOperation = rowMutationOperations.getUpdateRowOperation();
		final var updateRowValues = rowMutationOperations.getUpdateRowValues();
		final var updateRowRestrictions = rowMutationOperations.getUpdateRowRestrictions();
		if ( updateRowOperation == null || updateRowValues == null || updateRowRestrictions == null ) {
			return;
		}

		final var mutationExecutor = mutationExecutorService.createExecutor(
				() -> new BasicBatchKey( mutationTarget.getRolePath() + "#INDEX" ),
				singleOperation( MutationType.UPDATE, mutationTarget, updateRowOperation ),
				session
		);

		final var jdbcValueBindings = mutationExecutor.getJdbcValueBindings();
		try {
			final CollectionPersister persister = mutationTarget.getTargetPart().getCollectionDescriptor();
			int nextIndex = getBaseIndex() + ( resetIndex ? 0 : persister.getSize( key, session ) );
			while ( entries.hasNext() ) {
				final Object entry = entries.next();
				if ( entry != null && collection.entryExists( entry, nextIndex ) ) {
					updateRowValues.applyValues(
							collection,
							key,
							entry,
							nextIndex,
							session,
							jdbcValueBindings
					);
					updateRowRestrictions.applyRestrictions(
							collection,
							key,
							entry,
							nextIndex,
							session,
							jdbcValueBindings
					);
					mutationExecutor.execute( collection, null, null, null, session );
					nextIndex++;
				}
			}
		}
		finally {
			mutationExecutor.release();
		}
	}

	@Override
	public List<PlannedOperation> decomposeWriteIndex(
			PersistentCollection<?> collection,
			Iterator<?> entries,
			Object key,
			boolean resetIndex,
			int ordinalBase,
			SharedSessionContractImplementor session) {
		final var updateRowOperation = rowMutationOperations.getUpdateRowOperation();
		if ( updateRowOperation == null || !entries.hasNext() ) {
			return List.of();
		}

		final var tableMapping = mutationTarget.getCollectionTableMapping();
		final String tableName = tableMapping.getTableName();

		final BindPlan bindPlan = new WriteIndexBindPlan(
				collection,
				key,
				resetIndex,
				entries,
				rowMutationOperations.getUpdateRowValues(),
				rowMutationOperations.getUpdateRowRestrictions(),
				mutationTarget
		);

		final PlannedOperation plannedOp = new PlannedOperation(
				tableName,
				MutationKind.UPDATE,
				updateRowOperation,
				bindPlan,
				ordinalBase * 1_000,
				"WriteIndexCoordinator(" + mutationTarget.getRolePath() + ")"
		);

		return List.of( plannedOp );
	}

	private int getBaseIndex() {
		return getBaseIndex(mutationTarget);
	}

	private static int getBaseIndex(CollectionMutationTarget mutationTarget) {
		final var indexMetadata = mutationTarget.getTargetPart().getIndexMetadata();
		return indexMetadata != null ? indexMetadata.getListIndexBase() : 0;
	}

	private static class WriteIndexBindPlan implements BindPlan {
		private final PersistentCollection<?> collection;
		private final Object key;
		private final boolean resetIndex;
		private final Iterator<?> entries;
		private final RowMutationOperations.Values updateRowValues;
		private final RowMutationOperations.Restrictions updateRowRestrictions;
		private final CollectionMutationTarget mutationTarget;

		public WriteIndexBindPlan(
				PersistentCollection<?> collection,
				Object key,
				boolean resetIndex,
				Iterator<?> entries,
				RowMutationOperations.Values updateRowValues,
				RowMutationOperations.Restrictions updateRowRestrictions,
				CollectionMutationTarget mutationTarget) {
			this.collection = collection;
			this.key = key;
			this.resetIndex = resetIndex;
			this.entries = entries;
			this.updateRowValues = updateRowValues;
			this.updateRowRestrictions = updateRowRestrictions;
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
			final var jdbcValueBindings = executor.getJdbcValueBindings();
			final CollectionPersister persister = mutationTarget.getTargetPart().getCollectionDescriptor();
			final int baseIndex = getBaseIndex(mutationTarget);
			int nextIndex = baseIndex + ( resetIndex ? 0 : persister.getSize( key, session ) );

			while ( entries.hasNext() ) {
				final Object entry = entries.next();
				if ( entry != null && collection.entryExists( entry, nextIndex ) ) {
					updateRowValues.applyValues(
							collection,
							key,
							entry,
							nextIndex,
							session,
							jdbcValueBindings
					);
					updateRowRestrictions.applyRestrictions(
							collection,
							key,
							entry,
							nextIndex,
							session,
							jdbcValueBindings
					);
					executor.execute( collection, null, null, null, session );
					nextIndex++;
				}
			}
		}
	}
}
