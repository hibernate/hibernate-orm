/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import java.util.function.UnaryOperator;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.mutation.spi.MutationExecutorService;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.MutationType;

import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;

/**
 * {@link InsertRowsCoordinator} implementation for temporal collection tables
 * in the {@link org.hibernate.cfg.TemporalTableStrategy#HISTORY_TABLE} temporal
 * table mapping strategy.
 *
 * @author Gavin King
 */
public class InsertRowsCoordinatorHistory implements InsertRowsCoordinator {
	private final CollectionMutationTarget mutationTarget;
	private final RowMutationOperations rowMutationOperations;
	private final InsertRowsCoordinator currentInsertCoordinator;
	private final MutationExecutorService mutationExecutorService;
	private final BasicBatchKey historyBatchKey;
	private final boolean[] indexColumnIsSettable;
	private final boolean[] elementColumnIsSettable;
	private final UnaryOperator<Object> indexIncrementer;

	private MutationOperationGroup historyOperationGroup;
	private CollectionTableMapping historyTableMapping;
	private HistoryCollectionRowMutationHelper rowMutationHelper;

	public InsertRowsCoordinatorHistory(
			CollectionMutationTarget mutationTarget,
			RowMutationOperations rowMutationOperations,
			InsertRowsCoordinator currentInsertCoordinator,
			boolean[] indexColumnIsSettable,
			boolean[] elementColumnIsSettable,
			UnaryOperator<Object> indexIncrementer,
			ServiceRegistry serviceRegistry) {
		this.mutationTarget = mutationTarget;
		this.rowMutationOperations = rowMutationOperations;
		this.currentInsertCoordinator = currentInsertCoordinator;
		this.indexColumnIsSettable = indexColumnIsSettable;
		this.elementColumnIsSettable = elementColumnIsSettable;
		this.indexIncrementer = indexIncrementer;
		this.historyBatchKey = new BasicBatchKey( mutationTarget.getRolePath() + "#HISTORY_INSERT" );
		this.mutationExecutorService = serviceRegistry.getService( MutationExecutorService.class );
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
		currentInsertCoordinator.insertRows( collection, id, entryChecker, session );

		if ( historyOperationGroup == null ) {
			historyOperationGroup = createHistoryOperationGroup();
		}
		if ( historyOperationGroup == null ) {
			return;
		}

		final var pluralAttribute = mutationTarget.getTargetPart();
		final var collectionDescriptor = pluralAttribute.getCollectionDescriptor();
		final var entries = collection.entries( collectionDescriptor );
		if ( !entries.hasNext() ) {
			return;
		}

		final var mutationExecutor = mutationExecutorService.createExecutor(
				() -> historyBatchKey,
				historyOperationGroup,
				session
		);

		try {
			int entryCount = 0;
			final var historyBindings = getRowMutationHelper();
			while ( entries.hasNext() ) {
				final Object entry = entries.next();
				if ( entryChecker == null || entryChecker.include( entry, entryCount, collection, pluralAttribute ) ) {
					historyBindings.bindInsertValues(
							collection,
							id,
							entry,
							entryCount,
							session,
							mutationExecutor.getJdbcValueBindings()
					);
					mutationExecutor.execute( entry, null, null, null, session );
				}
				entryCount++;
			}
		}
		finally {
			mutationExecutor.release();
		}
	}

	private MutationOperationGroup createHistoryOperationGroup() {
		final var operation = rowMutationOperations.getInsertRowOperation( getHistoryTableMapping() );
		return operation == null ? null : singleOperation( MutationType.INSERT, mutationTarget, operation );
	}

	private CollectionTableMapping getHistoryTableMapping() {
		if ( historyTableMapping == null ) {
			final var temporalMapping = mutationTarget.getTargetPart().getTemporalMapping();
			historyTableMapping =
					new CollectionTableMapping( mutationTarget.getCollectionTableMapping(),
							temporalMapping.getTableName() );
		}
		return historyTableMapping;
	}

	private HistoryCollectionRowMutationHelper getRowMutationHelper() {
		if ( rowMutationHelper == null ) {
			rowMutationHelper = new HistoryCollectionRowMutationHelper(
					mutationTarget,
					getHistoryTableMapping().getTableName(),
					indexColumnIsSettable,
					elementColumnIsSettable,
					indexIncrementer
			);
		}
		return rowMutationHelper;
	}
}
