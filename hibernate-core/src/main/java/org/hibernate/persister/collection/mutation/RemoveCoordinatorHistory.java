/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

import jakarta.annotation.Nonnull;

import java.util.function.UnaryOperator;

import jakarta.annotation.Nullable;
import org.hibernate.action.queue.spi.decompose.collection.CollectionMutationTarget;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.mutation.spi.MutationExecutorService;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.spi.mutation.MutationType;
import org.hibernate.sql.ast.spi.model.MutatingTableReference;
import org.hibernate.sql.spi.mutation.jdbc.JdbcMutationOperation;

import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;
import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;

/**
 * {@link RemoveCoordinator} implementation for temporal collection tables
 * in the {@link org.hibernate.temporal.TemporalTableStrategy#HISTORY_TABLE}
 * temporal table mapping strategy.
 *
 * @author Gavin King
 */
public class RemoveCoordinatorHistory implements RemoveCoordinator {
	private final CollectionMutationTarget mutationTarget;
	private final OperationProducer operationProducer;
	private final MutationExecutorService mutationExecutorService;
	private final BasicBatchKey batchKey;
	private final BasicBatchKey historyBatchKey;
	@Nullable
	private final boolean[] indexColumnIsSettable;
	private final boolean[] elementColumnIsSettable;
	private final UnaryOperator<Object> indexIncrementer;

	@Nullable
	private MutationOperationGroup operationGroup;
	@Nullable
	private MutationOperationGroup historyOperationGroup;
	@Nullable
	private CollectionTableMapping historyTableMapping;
	@Nullable
	private HistoryCollectionRowMutationHelper rowMutationHelper;

	public RemoveCoordinatorHistory(
			@Nonnull CollectionMutationTarget mutationTarget,
			@Nonnull RowMutationOperations mutationOperations,
			@Nullable boolean[] indexColumnIsSettable,
			@Nonnull boolean[] elementColumnIsSettable,
			@Nonnull UnaryOperator<Object> indexIncrementer,
			@Nonnull ServiceRegistry serviceRegistry) {
		this.mutationTarget = mutationTarget;
		this.operationProducer = castNonNull( mutationOperations.getDeleteAllRowsOperationProducer() );
		this.indexColumnIsSettable = indexColumnIsSettable;
		this.elementColumnIsSettable = elementColumnIsSettable;
		this.indexIncrementer = indexIncrementer;
		this.batchKey = new BasicBatchKey( mutationTarget.getRolePath() + "#REMOVE" );
		this.historyBatchKey = new BasicBatchKey( mutationTarget.getRolePath() + "#HISTORY_REMOVE" );
		this.mutationExecutorService = serviceRegistry.getService( MutationExecutorService.class );
	}

	@Nonnull
	@Override
	public CollectionMutationTarget getMutationTarget() {
		return mutationTarget;
	}

	@Override
	public @Nullable String getSqlString() {
		if ( operationGroup == null ) {
			operationGroup = buildOperationGroup( mutationTarget.getCollectionTableMapping() );
		}
		final var operation = (JdbcMutationOperation) operationGroup.getSingleOperation();
		return operation.getSqlString();
	}

	@Override
	public void deleteAllRows(@Nonnull Object key, @Nonnull SharedSessionContractImplementor session) {
		if ( MODEL_MUTATION_LOGGER.isTraceEnabled() ) {
			MODEL_MUTATION_LOGGER.removingCollection( mutationTarget.getRolePath(), key );
		}

		if ( operationGroup == null ) {
			operationGroup = buildOperationGroup( mutationTarget.getCollectionTableMapping() );
		}
		if ( historyOperationGroup == null ) {
			historyOperationGroup = buildOperationGroup( getHistoryTableMapping() );
		}

		final var mutationExecutor = mutationExecutorService.createExecutor(
				() -> batchKey,
				operationGroup,
				session
		);
		final var historyExecutor = mutationExecutorService.createExecutor(
				() -> historyBatchKey,
				historyOperationGroup,
				session
		);

		try {
			final var jdbcValueBindings = mutationExecutor.getJdbcValueBindings();
			final var foreignKeyDescriptor = mutationTarget.getTargetPart().getKeyDescriptor();
			foreignKeyDescriptor.getKeyPart().decompose(
					key,
					0,
					jdbcValueBindings,
					null,
					RowMutationOperations.DEFAULT_RESTRICTOR,
					session
			);
			mutationExecutor.execute( key, null, null, null, session );

			getRowMutationHelper().bindDeleteAllRestrictions(
					key,
					session,
					historyExecutor.getJdbcValueBindings()
			);
			historyExecutor.execute( key, null, null, null, session );
		}
		finally {
			mutationExecutor.release();
			historyExecutor.release();
		}
	}

	@Nonnull
	private MutationOperationGroup buildOperationGroup(@Nonnull CollectionTableMapping tableMapping) {
		final var tableReference = new MutatingTableReference( tableMapping );
		return singleOperation( MutationType.DELETE, mutationTarget, operationProducer.createOperation( tableReference ) );
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
