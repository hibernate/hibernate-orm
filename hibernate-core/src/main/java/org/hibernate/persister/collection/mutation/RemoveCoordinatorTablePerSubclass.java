/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;


import org.hibernate.engine.jdbc.mutation.spi.MutationExecutorService;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.OneToManyPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.jdbc.JdbcMutationOperation;

import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;
import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;

/**
 * OneToMany remove coordinator if the element is a {@link org.hibernate.persister.entity.UnionSubclassEntityPersister}.
 */
public class RemoveCoordinatorTablePerSubclass implements RemoveCoordinator {
	private final OneToManyPersister mutationTarget;
	private final OperationProducer operationProducer;
	private final MutationExecutorService mutationExecutorService;

	private MutationOperationGroup[] operationGroups;

	/**
	 * Creates the coordinator.
	 *
	 * @implNote We pass a Supplier here and lazily create the operation-group because
	 * of timing (chicken-egg) back on the persister.
	 */
	public RemoveCoordinatorTablePerSubclass(
			OneToManyPersister mutationTarget,
			OperationProducer operationProducer,
			ServiceRegistry serviceRegistry) {
		this.mutationTarget = mutationTarget;
		this.operationProducer = operationProducer;
		mutationExecutorService = serviceRegistry.getService( MutationExecutorService.class );
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
		final var operationGroups = getMutationOperationGroups();
		final var operation = (JdbcMutationOperation) operationGroups[0].getSingleOperation();
		return operation.getSqlString();
	}

	@Override
	public void deleteAllRows(Object key, SharedSessionContractImplementor session) {
		if ( MODEL_MUTATION_LOGGER.isTraceEnabled() ) {
			MODEL_MUTATION_LOGGER.removingCollection( mutationTarget.getRolePath(), key );
		}

		final var foreignKeyDescriptor = mutationTarget.getTargetPart().getKeyDescriptor();

		for ( var operationGroup : getMutationOperationGroups() ) {
			final var mutationExecutor = mutationExecutorService.createExecutor(
					() -> null,
					operationGroup,
					session
			);

			try {
				foreignKeyDescriptor.getKeyPart().decompose(
						key,
						0,
						mutationExecutor.getJdbcValueBindings(),
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
	}

	private MutationOperationGroup[] getMutationOperationGroups() {
		final var operationGroups = this.operationGroups;
		// delayed creation of the operation-group
		return operationGroups == null
				? (this.operationGroups = buildOperationGroups())
				: operationGroups;
	}

	private MutationOperationGroup[] buildOperationGroups() {
		final var subMappingTypes =
				mutationTarget.getElementPersister()
						.getRootEntityDescriptor()
						.getSubMappingTypes();
		final var operationGroups = new MutationOperationGroup[subMappingTypes.size()];
		int i = 0;
		for ( var subMappingType : subMappingTypes ) {
			operationGroups[i++] = buildOperationGroup( subMappingType.getEntityPersister() );
		}
		return operationGroups;
	}

	private MutationOperationGroup buildOperationGroup(EntityPersister elementPersister) {
		assert mutationTarget.getTargetPart() != null
			&& mutationTarget.getTargetPart().getKeyDescriptor() != null;

//		if ( MODEL_MUTATION_LOGGER.isTraceEnabled() ) {
//			MODEL_MUTATION_LOGGER.tracef( "Starting RemoveCoordinator#buildOperationGroup - %s",
//					mutationTarget.getRolePath() );
//		}

		final var collectionTableMapping = mutationTarget.getCollectionTableMapping();
		final var tableReference = new MutatingTableReference(
				new CollectionTableMapping(
						elementPersister.getMappedTableDetails().getTableName(),
						collectionTableMapping.getSpaces(),
						collectionTableMapping.isJoinTable(),
						collectionTableMapping.isInverse(),
						collectionTableMapping.getInsertDetails(),
						collectionTableMapping.getUpdateDetails(),
						collectionTableMapping.isCascadeDeleteEnabled(),
						collectionTableMapping.getDeleteDetails(),
						collectionTableMapping.getDeleteRowDetails()
				)
		);

		return singleOperation( MutationType.DELETE, mutationTarget,
				mutationTarget.getFactory().getStoredProcedureHelper()
						.maybeWrapOperation( operationProducer.createOperation( tableReference ) ) );
	}
}
