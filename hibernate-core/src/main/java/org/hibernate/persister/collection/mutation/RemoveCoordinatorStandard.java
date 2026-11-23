package org.hibernate.persister.collection.mutation;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

import jakarta.annotation.Nonnull;

import jakarta.annotation.Nullable;
import org.hibernate.action.queue.spi.decompose.collection.CollectionMutationTarget;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.mutation.spi.MutationExecutorService;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.AbstractCollectionPersister;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.spi.mutation.MutationType;
import org.hibernate.sql.ast.spi.model.MutatingTableReference;
import org.hibernate.sql.spi.mutation.jdbc.JdbcMutationOperation;

import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;
import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;

/**
 * Handles complete removal of a collection by its key
 *
 * @author Steve Ebersole
 */
public class RemoveCoordinatorStandard implements RemoveCoordinator {
	private final AbstractCollectionPersister mutationTarget;
	private final OperationProducer operationProducer;
	private final RowMutationOperations.DeleteAllRowsBindings bindings;
	private final BasicBatchKey batchKey;
	private final MutationExecutorService mutationExecutorService;

	@Nullable
	private MutationOperationGroup operationGroup;

	/**
	 * Creates the coordinator.
	 *
	 * @implNote We pass a Supplier here and lazily create the operation-group because
	 * of timing (chicken-egg) back on the persister.
	 */
	public RemoveCoordinatorStandard(
			@Nonnull AbstractCollectionPersister mutationTarget,
			@Nonnull RowMutationOperations mutationOperations,
			@Nonnull ServiceRegistry serviceRegistry) {
		this.mutationTarget = mutationTarget;
		this.operationProducer = castNonNull( mutationOperations.getDeleteAllRowsOperationProducer() );
		this.bindings = castNonNull( mutationOperations.getDeleteAllRowsBindings() );

		batchKey = new BasicBatchKey( mutationTarget.getRolePath() + "#REMOVE" );
		mutationExecutorService = serviceRegistry.getService( MutationExecutorService.class );
	}

	@Nonnull
	@Override
	public String toString() {
		return "RemoveCoordinator(" + mutationTarget.getRolePath() + ")";
	}

	@Nonnull
	@Override
	public CollectionMutationTarget getMutationTarget() {
		return mutationTarget;
	}

	@Override
	public @Nullable String getSqlString() {
		if ( operationGroup == null ) {
			// delayed creation of the operation-group
			operationGroup = buildOperationGroup();
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
			// delayed creation of the operation-group
			operationGroup = buildOperationGroup();
		}

		final var mutationExecutor = mutationExecutorService.createExecutor(
				() -> batchKey,
				operationGroup,
				session
		);

		try {
			bindings.bind( key, session, mutationExecutor.getJdbcValueBindings() );
			mutationExecutor.execute(
					key,
					null,
					null,
					null,
					session
			);
		}
		finally {
			mutationExecutor.release();
		}
	}

	@Nonnull
	private MutationOperationGroup buildOperationGroup() {
		assert mutationTarget.getTargetPart() != null
			&& mutationTarget.getTargetPart().getKeyDescriptor() != null;

//		if ( MODEL_MUTATION_LOGGER.isTraceEnabled() ) {
//			MODEL_MUTATION_LOGGER.tracef( "Starting RemoveCoordinator#buildOperationGroup - %s",
//					mutationTarget.getRolePath() );
//		}

		final var tableMapping = mutationTarget.getCollectionTableMapping();
		final var tableReference = new MutatingTableReference( tableMapping );
		return singleOperation( MutationType.DELETE, mutationTarget,
				operationProducer.createOperation( tableReference ) );
	}
}
