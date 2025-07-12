/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import java.util.Collection;

import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.sql.model.internal.MutationOperationGroupFactory;
import org.hibernate.engine.jdbc.mutation.spi.MutationExecutorService;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.persister.collection.OneToManyPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.MutatingTableReference;

import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;

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
		throw new UnsupportedOperationException();
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

		MutationOperationGroup[] operationGroups = this.operationGroups;
		if ( operationGroups == null ) {
			// delayed creation of the operation-group
			operationGroups = this.operationGroups = buildOperationGroups();
		}

		final ForeignKeyDescriptor fkDescriptor = mutationTarget.getTargetPart().getKeyDescriptor();

		for ( MutationOperationGroup operationGroup : operationGroups ) {
			final MutationExecutor mutationExecutor = mutationExecutorService.createExecutor(
					() -> null,
					operationGroup,
					session
			);

			try {
				fkDescriptor.getKeyPart().decompose(
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

	private MutationOperationGroup[] buildOperationGroups() {
		final Collection<EntityMappingType> subMappingTypes = mutationTarget.getElementPersister()
				.getRootEntityDescriptor()
				.getSubMappingTypes();
		final MutationOperationGroup[] operationGroups = new MutationOperationGroup[subMappingTypes.size()];
		int i = 0;
		for ( EntityMappingType subMappingType : subMappingTypes ) {
			operationGroups[i++] = buildOperationGroup( subMappingType.getEntityPersister() );
		}
		return operationGroups;
	}

	private MutationOperationGroup buildOperationGroup(EntityPersister elementPersister) {
		assert mutationTarget.getTargetPart() != null;
		assert mutationTarget.getTargetPart().getKeyDescriptor() != null;

		if ( MODEL_MUTATION_LOGGER.isTraceEnabled() ) {
			MODEL_MUTATION_LOGGER.tracef( "Starting RemoveCoordinator#buildOperationGroup - %s",
					mutationTarget.getRolePath() );
		}

		final CollectionTableMapping collectionTableMapping = mutationTarget.getCollectionTableMapping();
		final MutatingTableReference tableReference = new MutatingTableReference(
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

		return MutationOperationGroupFactory.singleOperation(
				MutationType.DELETE,
				mutationTarget,
				operationProducer.createOperation( tableReference )
		);
	}
}
