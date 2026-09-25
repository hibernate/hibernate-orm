package org.hibernate.persister.collection.mutation;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

import jakarta.annotation.Nullable;

import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.jdbc.mutation.spi.MutationExecutorService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.AbstractCollectionPersister;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.spi.mutation.MutationType;

import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.noOperations;
import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;

/**
 * {@link UpdateRowsCoordinator} implementation for temporal collection tables
 * in the {@link org.hibernate.temporal.TemporalTableStrategy#HISTORY_TABLE}
 * temporal table mapping strategy.
 *
 * @author Gavin King
 */
public class UpdateRowsCoordinatorHistory extends AbstractUpdateRowsCoordinator implements UpdateRowsCoordinator {
	private final RowMutationOperations rowMutationOperations;
	private final BasicBatchKey historyDeleteBatchKey;
	private final BasicBatchKey historyInsertBatchKey;
	@Nullable
	private final boolean[] indexColumnIsSettable;
	private final boolean[] elementColumnIsSettable;
	private final UnaryOperator<Object> indexIncrementer;

	@Nullable
	private MutationOperationGroup updateOperationGroup;
	@Nullable
	private MutationOperationGroup historyDeleteOperationGroup;
	@Nullable
	private MutationOperationGroup historyInsertOperationGroup;
	@Nullable
	private CollectionTableMapping historyTableMapping;
	@Nullable
	private HistoryCollectionRowMutationHelper rowMutationHelper;
	protected final MutationExecutorService mutationExecutorService;

	public UpdateRowsCoordinatorHistory(
			@Nonnull AbstractCollectionPersister mutationTarget,
			@Nonnull RowMutationOperations rowMutationOperations,
			@Nonnull SessionFactoryImplementor sessionFactory,
			@Nullable boolean[] indexColumnIsSettable,
			@Nonnull boolean[] elementColumnIsSettable,
			@Nonnull UnaryOperator<Object> indexIncrementer) {
		super( mutationTarget, sessionFactory );
		this.rowMutationOperations = rowMutationOperations;
		this.indexColumnIsSettable = indexColumnIsSettable;
		this.elementColumnIsSettable = elementColumnIsSettable;
		this.indexIncrementer = indexIncrementer;
		this.historyDeleteBatchKey = new BasicBatchKey( mutationTarget.getRolePath() + "#HISTORY_DELETE" );
		this.historyInsertBatchKey = new BasicBatchKey( mutationTarget.getRolePath() + "#HISTORY_INSERT" );
		mutationExecutorService = sessionFactory.getServiceRegistry().getService( MutationExecutorService.class );
	}

	@Override
	protected int doUpdate(@Nonnull Object key, @Nonnull PersistentCollection<?> collection, @Nonnull SharedSessionContractImplementor session) {
		if ( rowMutationOperations.getUpdateRowOperation() == null ) {
			return 0;
		}

		final var updateOperationGroup = getUpdateOperationGroup();
		final var historyDeleteGroup = getHistoryDeleteOperationGroup();
		final var historyInsertGroup = getHistoryInsertOperationGroup();

		final var updateExecutor = mutationExecutorService.createExecutor(
				() -> new BasicBatchKey( getMutationTarget().getRolePath() + "#UPDATE" ),
				updateOperationGroup,
				session
		);
		final var historyDeleteExecutor = historyDeleteGroup == null
				? null
				: mutationExecutorService.createExecutor(
						() -> historyDeleteBatchKey,
						historyDeleteGroup,
						session
				);
		final var historyInsertExecutor = historyInsertGroup == null
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
			@Nonnull Object key,
			@Nonnull PersistentCollection<?> collection,
			@Nonnull Object entry,
			int entryPosition,
			@Nonnull MutationExecutor updateExecutor,
			@Nullable MutationExecutor historyDeleteExecutor,
			@Nullable MutationExecutor historyInsertExecutor,
			@Nonnull SharedSessionContractImplementor session) {
		final var attribute = getMutationTarget().getTargetPart();
		if ( !collection.needsUpdating( entry, entryPosition, attribute ) ) {
			return false;
		}

		final Object deleteRowValue = resolveDeleteRowValue( collection, entry, entryPosition );
		castNonNull( rowMutationOperations.getUpdateRowValues() ).applyValues(
				collection,
				key,
				entry,
				entryPosition,
				session,
				updateExecutor.getJdbcValueBindings()
		);
		castNonNull( rowMutationOperations.getUpdateRowRestrictions() ).applyRestrictions(
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

	@Nonnull
	private MutationOperationGroup getUpdateOperationGroup() {
		if ( updateOperationGroup == null ) {
			final var updateRowOperation = rowMutationOperations.getUpdateRowOperation();
			final var mutationTarget = getMutationTarget();
			updateOperationGroup = updateRowOperation == null
					? noOperations( MutationType.UPDATE, mutationTarget )
					: singleOperation( MutationType.UPDATE, mutationTarget, updateRowOperation );
		}
		return updateOperationGroup;
	}

	@Nullable
	private MutationOperationGroup getHistoryDeleteOperationGroup() {
		if ( historyDeleteOperationGroup == null ) {
			final var operation = rowMutationOperations.getDeleteRowOperation( getHistoryTableMapping() );
			historyDeleteOperationGroup = operation == null
					? null
					: singleOperation( MutationType.DELETE, getMutationTarget(), operation );
		}
		return historyDeleteOperationGroup;
	}

	@Nullable
	private MutationOperationGroup getHistoryInsertOperationGroup() {
		if ( historyInsertOperationGroup == null ) {
			final var operation = rowMutationOperations.getInsertRowOperation( getHistoryTableMapping() );
			historyInsertOperationGroup = operation == null
					? null
					: singleOperation( MutationType.INSERT, getMutationTarget(), operation );
		}
		return historyInsertOperationGroup;
	}

	@Nonnull
	private CollectionTableMapping getHistoryTableMapping() {
		if ( historyTableMapping == null ) {
			final var mutationTarget = getMutationTarget();
			historyTableMapping =
					new CollectionTableMapping( mutationTarget.getCollectionTableMapping(),
							mutationTarget.getTargetPart().getTemporalMapping().getTableName() );
		}
		return historyTableMapping;
	}

	@Nonnull
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
