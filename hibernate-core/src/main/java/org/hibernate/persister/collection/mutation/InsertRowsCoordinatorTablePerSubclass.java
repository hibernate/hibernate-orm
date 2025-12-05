/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;


import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.jdbc.mutation.spi.BatchKeyAccess;
import org.hibernate.engine.jdbc.mutation.spi.MutationExecutorService;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.OneToManyPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.MutationType;

import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;
import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;

/**
 * OneToMany insert coordinator if the element is a {@link org.hibernate.persister.entity.UnionSubclassEntityPersister}.
 */
public class InsertRowsCoordinatorTablePerSubclass implements InsertRowsCoordinator {
	private final CollectionMutationTarget mutationTarget;
	private final RowMutationOperations rowMutationOperations;

	private final SubclassEntry[] subclassEntries;
	private final MutationExecutorService mutationExecutorService;

	public InsertRowsCoordinatorTablePerSubclass(
			OneToManyPersister mutationTarget,
			RowMutationOperations rowMutationOperations,
			ServiceRegistry serviceRegistry) {
		this.mutationTarget = mutationTarget;
		this.rowMutationOperations = rowMutationOperations;
		subclassEntries =
				new SubclassEntry[mutationTarget.getElementPersister()
						.getRootEntityDescriptor()
						.getSubclassEntityNames()
						.size()];
		mutationExecutorService = serviceRegistry.getService( MutationExecutorService.class );
	}

	@Override
	public String toString() {
		return "InsertRowsCoordinator(" + mutationTarget.getRolePath() + ")";
	}

	@Override
	public CollectionMutationTarget getMutationTarget() {
		return mutationTarget;
	}

	@Override
	public void insertRows(
			PersistentCollection<?> collection,
			Object id,
			EntryFilter entryChecker,
			SharedSessionContractImplementor session) {
		if ( MODEL_MUTATION_LOGGER.isTraceEnabled() ) {
			MODEL_MUTATION_LOGGER.insertingNewCollectionRows( mutationTarget.getRolePath(), id );
		}

		final var pluralAttribute = mutationTarget.getTargetPart();
		final var collectionDescriptor = pluralAttribute.getCollectionDescriptor();

		final var entries = collection.entries( collectionDescriptor );
		collection.preInsert( collectionDescriptor );
		if ( !entries.hasNext() ) {
			MODEL_MUTATION_LOGGER.noCollectionRowsToInsert( mutationTarget.getRolePath(), id );
			return;
		}
		final var executors = new MutationExecutor[subclassEntries.length];
		try {
			int entryCount = 0;
			while ( entries.hasNext() ) {
				final Object entry = entries.next();

				if ( entryChecker == null || entryChecker.include( entry, entryCount, collection, pluralAttribute ) ) {
					final var entityEntry = session.getPersistenceContextInternal().getEntry( entry );
					final int subclassId = entityEntry.getPersister().getSubclassId();
					final MutationExecutor mutationExecutor;
					if ( executors[subclassId] == null ) {
						final var subclassEntry = getSubclassEntry( entityEntry.getPersister() );
						mutationExecutor = executors[subclassId] = mutationExecutorService.createExecutor(
								subclassEntry.batchKeySupplier,
								subclassEntry.operationGroup,
								session
						);
					}
					else {
						mutationExecutor = executors[subclassId];
					}
					// if the entry is included, perform the "insert"
					rowMutationOperations.getInsertRowValues().applyValues(
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

			MODEL_MUTATION_LOGGER.doneInsertingCollectionRows( entryCount, mutationTarget.getRolePath() );

		}
		finally {
			for ( var executor : executors ) {
				if ( executor != null ) {
					executor.release();
				}
			}
		}
	}

	private SubclassEntry getSubclassEntry(EntityPersister elementPersister) {
		final int subclassId = elementPersister.getSubclassId();
		final var subclassEntry = subclassEntries[subclassId];
		if ( subclassEntry != null ) {
			return subclassEntry;
		}
		final var basicBatchKey =
				new BasicBatchKey( mutationTarget.getRolePath() + "#INSERT#" + subclassId );
		return subclassEntries[subclassId] = new SubclassEntry(
				() -> basicBatchKey,
				createOperationGroup( elementPersister )
		);
	}

	private MutationOperationGroup createOperationGroup(EntityPersister elementPersister) {
		assert mutationTarget.getTargetPart() != null
			&& mutationTarget.getTargetPart().getKeyDescriptor() != null;

		final var collectionTableMapping = mutationTarget.getCollectionTableMapping();
		final var operation = rowMutationOperations.getInsertRowOperation(
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
		return singleOperation( MutationType.INSERT, mutationTarget, operation );
	}

	private static class SubclassEntry {

		private final BatchKeyAccess batchKeySupplier;

		private final MutationOperationGroup operationGroup;

		public SubclassEntry(BatchKeyAccess batchKeySupplier, MutationOperationGroup operationGroup) {
			this.batchKeySupplier = batchKeySupplier;
			this.operationGroup = operationGroup;
		}
	}
}
