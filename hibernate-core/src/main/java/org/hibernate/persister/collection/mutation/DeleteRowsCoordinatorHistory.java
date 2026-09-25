package org.hibernate.persister.collection.mutation;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

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

import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;
import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;

/**
 * {@link DeleteRowsCoordinator} implementation for temporal collection tables
 * in the {@link org.hibernate.temporal.TemporalTableStrategy#HISTORY_TABLE}
 * temporal table mapping strategy.
 *
 * @author Gavin King
 */
public class DeleteRowsCoordinatorHistory implements DeleteRowsCoordinator {
	private final CollectionMutationTarget mutationTarget;
	private final RowMutationOperations rowMutationOperations;
	private final boolean deleteByIndex;
	@Nullable
	private final boolean[] indexColumnIsSettable;
	private final boolean[] elementColumnIsSettable;
	private final UnaryOperator<Object> indexIncrementer;
	private final MutationExecutorService mutationExecutorService;
	private final BasicBatchKey deleteBatchKey;
	private final BasicBatchKey historyBatchKey;

	@Nullable
	private MutationOperationGroup deleteOperationGroup;
	@Nullable
	private MutationOperationGroup historyOperationGroup;
	@Nullable
	private CollectionTableMapping historyTableMapping;
	@Nullable
	private HistoryCollectionRowMutationHelper rowMutationHelper;

	public DeleteRowsCoordinatorHistory(
			@Nonnull CollectionMutationTarget mutationTarget,
			@Nonnull RowMutationOperations rowMutationOperations,
			boolean deleteByIndex,
			@Nullable boolean[] indexColumnIsSettable,
			@Nonnull boolean[] elementColumnIsSettable,
			@Nonnull UnaryOperator<Object> indexIncrementer,
			@Nonnull ServiceRegistry serviceRegistry) {
		this.mutationTarget = mutationTarget;
		this.rowMutationOperations = rowMutationOperations;
		this.deleteByIndex = deleteByIndex;
		this.indexColumnIsSettable = indexColumnIsSettable;
		this.elementColumnIsSettable = elementColumnIsSettable;
		this.indexIncrementer = indexIncrementer;
		this.deleteBatchKey = new BasicBatchKey( mutationTarget.getRolePath() + "#DELETE" );
		this.historyBatchKey = new BasicBatchKey( mutationTarget.getRolePath() + "#HISTORY_DELETE" );
		this.mutationExecutorService = serviceRegistry.getService( MutationExecutorService.class );
	}

	@Nonnull
	@Override
	public CollectionMutationTarget getMutationTarget() {
		return mutationTarget;
	}

	@Override
	public void deleteRows(@Nonnull PersistentCollection<?> collection, @Nonnull Object key, @Nonnull SharedSessionContractImplementor session) {
		if ( deleteOperationGroup == null ) {
			deleteOperationGroup = createOperationGroup();
		}
		if ( deleteOperationGroup == null ) {
			return;
		}
		if ( historyOperationGroup == null ) {
			historyOperationGroup = createHistoryOperationGroup();
		}

		if ( MODEL_MUTATION_LOGGER.isTraceEnabled() ) {
			MODEL_MUTATION_LOGGER.deletingRemovedCollectionRows( mutationTarget.getRolePath(), key );
		}

		final var mutationExecutor = mutationExecutorService.createExecutor(
				() -> deleteBatchKey,
				deleteOperationGroup,
				session
		);
		final var historyExecutor = historyOperationGroup == null ? null : mutationExecutorService.createExecutor(
				() -> historyBatchKey,
				historyOperationGroup,
				session
		);

		try {
			final var pluralAttribute = mutationTarget.getTargetPart();
			final var collectionDescriptor = pluralAttribute.getCollectionDescriptor();

			final var deletes = collection.getDeletes( collectionDescriptor, !deleteByIndex );
			if ( !deletes.hasNext() ) {
				MODEL_MUTATION_LOGGER.noRowsToDelete();
				return;
			}

			int deletionCount = 0;
			final var restrictions = castNonNull( rowMutationOperations.getDeleteRowRestrictions() );
			final var historyBindings = getRowMutationHelper();

			while ( deletes.hasNext() ) {
				final Object removal = deletes.next();

				restrictions.applyRestrictions(
						collection,
						key,
						removal,
						deletionCount,
						session,
						mutationExecutor.getJdbcValueBindings()
				);
				mutationExecutor.execute( removal, null, null, null, session );

				if ( historyExecutor != null ) {
					historyBindings.bindDeleteRowRestrictions(
							collection,
							key,
							removal,
							deletionCount,
							session,
							historyExecutor.getJdbcValueBindings()
					);
					historyExecutor.execute( removal, null, null, null, session );
				}

				deletionCount++;
			}

			MODEL_MUTATION_LOGGER.doneDeletingCollectionRows( deletionCount, mutationTarget.getRolePath() );
		}
		finally {
			mutationExecutor.release();
			if ( historyExecutor != null ) {
				historyExecutor.release();
			}
		}
	}

	@Nullable
	private MutationOperationGroup createOperationGroup() {
		final var operation = rowMutationOperations.getDeleteRowOperation();
		return operation == null ? null : singleOperation( MutationType.DELETE, mutationTarget, operation );
	}

	@Nullable
	private MutationOperationGroup createHistoryOperationGroup() {
		final var operation = rowMutationOperations.getDeleteRowOperation( getHistoryTableMapping() );
		return operation == null ? null : singleOperation( MutationType.DELETE, mutationTarget, operation );
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
