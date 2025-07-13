/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.sql.model.internal.MutationOperationGroupFactory;
import org.hibernate.engine.jdbc.mutation.spi.MutationExecutorService;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.jdbc.JdbcMutationOperation;

import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;

/**
 * Handles complete removal of a collection by its key
 *
 * @author Steve Ebersole
 */
public class RemoveCoordinatorStandard implements RemoveCoordinator {
	private final CollectionMutationTarget mutationTarget;
	private final OperationProducer operationProducer;
	private final BasicBatchKey batchKey;
	private final MutationExecutorService mutationExecutorService;

	private MutationOperationGroup operationGroup;

	/**
	 * Creates the coordinator.
	 *
	 * @implNote We pass a Supplier here and lazily create the operation-group because
	 * of timing (chicken-egg) back on the persister.
	 */
	public RemoveCoordinatorStandard(
			CollectionMutationTarget mutationTarget,
			OperationProducer operationProducer,
			ServiceRegistry serviceRegistry) {
		this.mutationTarget = mutationTarget;
		this.operationProducer = operationProducer;

		this.batchKey = new BasicBatchKey( mutationTarget.getRolePath() + "#REMOVE" );
		this.mutationExecutorService = serviceRegistry.getService( MutationExecutorService.class );
	}

	@Override
	public String toString() {
		return "RemoveCoordinator(" + mutationTarget.getRolePath() + ")";
	}

	@Override
	public CollectionMutationTarget getMutationTarget() {
		return mutationTarget;
	}

	@Override
	public String getSqlString() {
		if ( operationGroup == null ) {
			// delayed creation of the operation-group
			operationGroup = buildOperationGroup();
		}

		final JdbcMutationOperation operation = (JdbcMutationOperation) operationGroup.getSingleOperation();
		return operation.getSqlString();
	}

	@Override
	public void deleteAllRows(Object key, SharedSessionContractImplementor session) {
		if ( MODEL_MUTATION_LOGGER.isTraceEnabled() ) {
			MODEL_MUTATION_LOGGER.tracef(
					"Deleting collection - %s : %s",
					mutationTarget.getRolePath(),
					key
			);
		}

		if ( operationGroup == null ) {
			// delayed creation of the operation-group
			operationGroup = buildOperationGroup();
		}

		final MutationExecutor mutationExecutor = mutationExecutorService.createExecutor(
				() -> batchKey,
				operationGroup,
				session
		);

		try {
			final JdbcValueBindings jdbcValueBindings = mutationExecutor.getJdbcValueBindings();
			final ForeignKeyDescriptor fkDescriptor = mutationTarget.getTargetPart().getKeyDescriptor();
			fkDescriptor.getKeyPart().decompose(
					key,
					0,
					jdbcValueBindings,
					null,
					RowMutationOperations.DEFAULT_RESTRICTOR,
					session
			);

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

	private MutationOperationGroup buildOperationGroup() {
		assert mutationTarget.getTargetPart() != null;
		assert mutationTarget.getTargetPart().getKeyDescriptor() != null;

//		if ( MODEL_MUTATION_LOGGER.isTraceEnabled() ) {
//			MODEL_MUTATION_LOGGER.tracef( "Starting RemoveCoordinator#buildOperationGroup - %s",
//					mutationTarget.getRolePath() );
//		}

		final CollectionTableMapping tableMapping = mutationTarget.getCollectionTableMapping();
		final MutatingTableReference tableReference = new MutatingTableReference( tableMapping );

		return MutationOperationGroupFactory.singleOperation(
				MutationType.DELETE,
				mutationTarget,
				operationProducer.createOperation( tableReference )
		);
	}
}
