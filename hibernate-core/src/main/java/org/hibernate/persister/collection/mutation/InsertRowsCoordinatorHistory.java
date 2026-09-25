package org.hibernate.persister.collection.mutation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.UnaryOperator;

import org.hibernate.action.queue.spi.decompose.collection.CollectionMutationTarget;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.mutation.spi.MutationExecutorService;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.spi.mutation.MutationType;

import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;

/**
 * {@link InsertRowsCoordinator} implementation for temporal collection tables
 * in the {@link org.hibernate.temporal.TemporalTableStrategy#HISTORY_TABLE}
 * temporal table mapping strategy.
 *
 * @author Gavin King
 */
public class InsertRowsCoordinatorHistory implements InsertRowsCoordinator {
	private final CollectionMutationTarget mutationTarget;
	private final RowMutationOperations rowMutationOperations;
	private final InsertRowsCoordinator currentInsertCoordinator;
	private final MutationExecutorService mutationExecutorService;
	private final BasicBatchKey historyBatchKey;
	@Nullable
	private final boolean[] indexColumnIsSettable;
	private final boolean[] elementColumnIsSettable;
	private final UnaryOperator<Object> indexIncrementer;

	@Nullable
	private MutationOperationGroup historyOperationGroup;
	@Nullable
	private CollectionTableMapping historyTableMapping;
	@Nullable
	private HistoryCollectionRowMutationHelper rowMutationHelper;

	public InsertRowsCoordinatorHistory(
			@Nonnull CollectionMutationTarget mutationTarget,
			@Nonnull RowMutationOperations rowMutationOperations,
			@Nonnull InsertRowsCoordinator currentInsertCoordinator,
			@Nullable boolean[] indexColumnIsSettable,
			@Nonnull boolean[] elementColumnIsSettable,
			@Nonnull UnaryOperator<Object> indexIncrementer,
			@Nonnull ServiceRegistry serviceRegistry) {
		this.mutationTarget = mutationTarget;
		this.rowMutationOperations = rowMutationOperations;
		this.currentInsertCoordinator = currentInsertCoordinator;
		this.indexColumnIsSettable = indexColumnIsSettable;
		this.elementColumnIsSettable = elementColumnIsSettable;
		this.indexIncrementer = indexIncrementer;
		this.historyBatchKey = new BasicBatchKey( mutationTarget.getRolePath() + "#HISTORY_INSERT" );
		this.mutationExecutorService = serviceRegistry.getService( MutationExecutorService.class );
	}

	@Nonnull
	@Override
	public CollectionMutationTarget getMutationTarget() {
		return mutationTarget;
	}

	@Override
	public void insertRows(
			@Nonnull PersistentCollection<?> collection,
			@Nonnull Object id,
			@Nullable EntryFilter entryChecker,
			@Nonnull SharedSessionContractImplementor session) {
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

	@Nullable
	private MutationOperationGroup createHistoryOperationGroup() {
		final var operation = rowMutationOperations.getInsertRowOperation( getHistoryTableMapping() );
		return operation == null ? null : singleOperation( MutationType.INSERT, mutationTarget, operation );
	}

	@Nonnull
	private CollectionTableMapping getHistoryTableMapping() {
		if ( historyTableMapping == null ) {
			final var temporalMapping = mutationTarget.getTargetPart().getTemporalMapping();
			historyTableMapping =
					new CollectionTableMapping( mutationTarget.getCollectionTableMapping(),
							temporalMapping.getTableName() );
		}
		return historyTableMapping;
	}

	@Nonnull
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
