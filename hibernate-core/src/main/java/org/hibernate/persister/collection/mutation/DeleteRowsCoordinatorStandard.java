/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import java.util.Iterator;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.sql.model.internal.MutationOperationGroupFactory;
import org.hibernate.engine.jdbc.mutation.spi.MutationExecutorService;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.jdbc.JdbcMutationOperation;

import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;

/**
 * @author Steve Ebersole
 */
public class DeleteRowsCoordinatorStandard implements DeleteRowsCoordinator {
	private final CollectionMutationTarget mutationTarget;
	private final RowMutationOperations rowMutationOperations;
	private final boolean deleteByIndex;

	private final BasicBatchKey batchKey;
	private final MutationExecutorService mutationExecutorService;

	private MutationOperationGroup operationGroup;

	public DeleteRowsCoordinatorStandard(
			CollectionMutationTarget mutationTarget,
			RowMutationOperations rowMutationOperations,
			boolean deleteByIndex,
			ServiceRegistry serviceRegistry) {
		this.mutationTarget = mutationTarget;
		this.rowMutationOperations = rowMutationOperations;
		this.deleteByIndex = deleteByIndex;

		this.batchKey = new BasicBatchKey( mutationTarget.getRolePath() + "#DELETE" );
		this.mutationExecutorService = serviceRegistry.getService( MutationExecutorService.class );
	}

	@Override
	public CollectionMutationTarget getMutationTarget() {
		return mutationTarget;
	}

	@Override
	public void deleteRows(PersistentCollection<?> collection, Object key, SharedSessionContractImplementor session) {
		if ( operationGroup == null ) {
			operationGroup = createOperationGroup();
		}

		if ( MODEL_MUTATION_LOGGER.isTraceEnabled() ) {
			MODEL_MUTATION_LOGGER.deletingRemovedCollectionRows( mutationTarget.getRolePath(), key );
		}

		final MutationExecutor mutationExecutor = mutationExecutorService.createExecutor(
				() -> batchKey,
				operationGroup,
				session
		);
		final JdbcValueBindings jdbcValueBindings = mutationExecutor.getJdbcValueBindings();

		try {
			final PluralAttributeMapping pluralAttribute = mutationTarget.getTargetPart();
			final CollectionPersister collectionDescriptor = pluralAttribute.getCollectionDescriptor();

			final Iterator<?> deletes = collection.getDeletes( collectionDescriptor, !deleteByIndex );
			if ( !deletes.hasNext() ) {
				MODEL_MUTATION_LOGGER.noRowsToDelete();
				return;
			}

			int deletionCount = 0;

			final RowMutationOperations.Restrictions restrictions = rowMutationOperations.getDeleteRowRestrictions();

			while ( deletes.hasNext() ) {
				final Object removal = deletes.next();

				restrictions.applyRestrictions(
						collection,
						key,
						removal,
						deletionCount,
						session,
						jdbcValueBindings
				);

				mutationExecutor.execute( removal, null, null, null, session );

				deletionCount++;
			}

			MODEL_MUTATION_LOGGER.doneDeletingCollectionRows( deletionCount, mutationTarget.getRolePath() );
		}
		finally {
			mutationExecutor.release();
		}
	}

	private MutationOperationGroup createOperationGroup() {
		assert mutationTarget.getTargetPart() != null;
		assert mutationTarget.getTargetPart().getKeyDescriptor() != null;

		final JdbcMutationOperation operation = rowMutationOperations.getDeleteRowOperation();
		return MutationOperationGroupFactory.singleOperation( MutationType.DELETE, mutationTarget, operation );
	}
}
