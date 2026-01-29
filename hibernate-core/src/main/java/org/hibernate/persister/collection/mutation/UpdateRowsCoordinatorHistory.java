/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.MutationType;

import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.noOperations;
import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;

/**
 * {@link UpdateRowsCoordinator} implementation for temporal collection tables
 * in the {@link org.hibernate.cfg.TemporalTableStrategy#HISTORY_TABLE} temporal
 * table mapping strategy.
 *
 * @author Gavin King
 */
public class UpdateRowsCoordinatorHistory extends AbstractUpdateRowsCoordinator implements UpdateRowsCoordinator {
	private final RowMutationOperations rowMutationOperations;
	private final BasicBatchKey historyDeleteBatchKey;
	private final BasicBatchKey historyInsertBatchKey;
	private final boolean[] indexColumnIsSettable;
	private final boolean[] elementColumnIsSettable;
	private final UnaryOperator<Object> indexIncrementer;

	private MutationOperationGroup updateOperationGroup;
	private MutationOperationGroup historyDeleteOperationGroup;
	private MutationOperationGroup historyInsertOperationGroup;
	private CollectionTableMapping historyTableMapping;
	private HistoryCollectionRowMutationHelper rowMutationHelper;

	public UpdateRowsCoordinatorHistory(
			CollectionMutationTarget mutationTarget,
			RowMutationOperations rowMutationOperations,
			SessionFactoryImplementor sessionFactory,
			boolean[] indexColumnIsSettable,
			boolean[] elementColumnIsSettable,
			UnaryOperator<Object> indexIncrementer) {
		super( mutationTarget, sessionFactory );
		this.rowMutationOperations = rowMutationOperations;
		this.indexColumnIsSettable = indexColumnIsSettable;
		this.elementColumnIsSettable = elementColumnIsSettable;
		this.indexIncrementer = indexIncrementer;
		this.historyDeleteBatchKey = new BasicBatchKey( mutationTarget.getRolePath() + "#HISTORY_DELETE" );
		this.historyInsertBatchKey = new BasicBatchKey( mutationTarget.getRolePath() + "#HISTORY_INSERT" );
	}

	@Override
	protected int doUpdate(Object key, PersistentCollection<?> collection, SharedSessionContractImplementor session) {
		if ( rowMutationOperations.getUpdateRowOperation() == null ) {
			return 0;
		}

		final var updateOperationGroup = getUpdateOperationGroup();
		final var historyDeleteGroup = getHistoryDeleteOperationGroup();
		final var historyInsertGroup = getHistoryInsertOperationGroup();

		final MutationExecutor updateExecutor = mutationExecutorService.createExecutor(
				() -> new BasicBatchKey( getMutationTarget().getRolePath() + "#UPDATE" ),
				updateOperationGroup,
				session
		);
		final MutationExecutor historyDeleteExecutor = historyDeleteGroup == null
				? null
				: mutationExecutorService.createExecutor(
						() -> historyDeleteBatchKey,
						historyDeleteGroup,
						session
				);
		final MutationExecutor historyInsertExecutor = historyInsertGroup == null
				? null
				: mutationExecutorService.createExecutor(
						() -> historyInsertBatchKey,
						historyInsertGroup,
						session
				);

		try {
			final var entries =
					collection.entries( getMutationTarget().getTargetPart().getCollectionDescriptor() );
			int count = 0;

			if ( collection.isElementRemoved() ) {
				final List<Object> elements = new ArrayList<>();
				while ( entries.hasNext() ) {
					elements.add( entries.next() );
				}
				for ( int i = elements.size() - 1; i >= 0; i-- ) {
					final Object entry = elements.get( i );
					final boolean updated = processRow(
							key,
							collection,
							entry,
							i,
							updateExecutor,
							historyDeleteExecutor,
							historyInsertExecutor,
							session
					);
					if ( updated ) {
						count++;
					}
				}
			}
			else {
				int position = 0;
				while ( entries.hasNext() ) {
					final Object entry = entries.next();
					final boolean updated = processRow(
							key,
							collection,
							entry,
							position++,
							updateExecutor,
							historyDeleteExecutor,
							historyInsertExecutor,
							session
					);
					if ( updated ) {
						count++;
					}
				}
			}

			return count;
		}
		finally {
			updateExecutor.release();
			if ( historyDeleteExecutor != null ) {
				historyDeleteExecutor.release();
			}
			if ( historyInsertExecutor != null ) {
				historyInsertExecutor.release();
			}
		}
	}

	private boolean processRow(
			Object key,
			PersistentCollection<?> collection,
			Object entry,
			int entryPosition,
			MutationExecutor updateExecutor,
			MutationExecutor historyDeleteExecutor,
			MutationExecutor historyInsertExecutor,
			SharedSessionContractImplementor session) {
		final var attribute = getMutationTarget().getTargetPart();
		if ( !collection.needsUpdating( entry, entryPosition, attribute ) ) {
			return false;
		}

		final Object deleteRowValue = resolveDeleteRowValue( collection, entry, entryPosition );
		rowMutationOperations.getUpdateRowValues().applyValues(
				collection,
				key,
				entry,
				entryPosition,
				session,
				updateExecutor.getJdbcValueBindings()
		);
		rowMutationOperations.getUpdateRowRestrictions().applyRestrictions(
				collection,
				key,
				entry,
				entryPosition,
				session,
				updateExecutor.getJdbcValueBindings()
		);
		updateExecutor.execute( collection, null, null, null, session );

		if ( historyDeleteExecutor != null && historyInsertExecutor != null ) {
			final var historyBindings = getRowMutationHelper();
			historyBindings.bindDeleteRowRestrictions(
					collection,
					key,
					deleteRowValue,
					entryPosition,
					session,
					historyDeleteExecutor.getJdbcValueBindings()
			);
			historyDeleteExecutor.execute( deleteRowValue, null, null, null, session );

			historyBindings.bindInsertValues(
					collection,
					key,
					entry,
					entryPosition,
					session,
					historyInsertExecutor.getJdbcValueBindings()
			);
			historyInsertExecutor.execute( entry, null, null, null, session );
		}

		return true;
	}

	private MutationOperationGroup getUpdateOperationGroup() {
		if ( updateOperationGroup == null ) {
			final var updateRowOperation = rowMutationOperations.getUpdateRowOperation();
			updateOperationGroup = updateRowOperation == null
					? noOperations( MutationType.UPDATE, getMutationTarget() )
					: singleOperation( MutationType.UPDATE, getMutationTarget(), updateRowOperation );
		}
		return updateOperationGroup;
	}

	private MutationOperationGroup getHistoryDeleteOperationGroup() {
		if ( historyDeleteOperationGroup == null ) {
			final var operation = rowMutationOperations.getDeleteRowOperation( getHistoryTableMapping() );
			historyDeleteOperationGroup = operation == null
					? null
					: singleOperation( MutationType.DELETE, getMutationTarget(), operation );
		}
		return historyDeleteOperationGroup;
	}

	private MutationOperationGroup getHistoryInsertOperationGroup() {
		if ( historyInsertOperationGroup == null ) {
			final var operation = rowMutationOperations.getInsertRowOperation( getHistoryTableMapping() );
			historyInsertOperationGroup = operation == null
					? null
					: singleOperation( MutationType.INSERT, getMutationTarget(), operation );
		}
		return historyInsertOperationGroup;
	}

	private CollectionTableMapping getHistoryTableMapping() {
		if ( historyTableMapping == null ) {
			final var temporalMapping = getMutationTarget().getTargetPart().getTemporalMapping();
			historyTableMapping = HistoryCollectionTableMappingHelper.createHistoryTableMapping(
					getMutationTarget().getCollectionTableMapping(),
					temporalMapping.getTableName()
			);
		}
		return historyTableMapping;
	}

	private HistoryCollectionRowMutationHelper getRowMutationHelper() {
		if ( rowMutationHelper == null ) {
			rowMutationHelper = new HistoryCollectionRowMutationHelper(
					getMutationTarget(),
					getHistoryTableMapping().getTableName(),
					indexColumnIsSettable,
					elementColumnIsSettable,
					indexIncrementer
			);
		}
		return rowMutationHelper;
	}
}
