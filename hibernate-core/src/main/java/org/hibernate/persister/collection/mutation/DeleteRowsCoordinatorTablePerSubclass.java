/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import java.util.Iterator;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.sql.model.internal.MutationOperationGroupFactory;
import org.hibernate.engine.jdbc.mutation.spi.BatchKeyAccess;
import org.hibernate.engine.jdbc.mutation.spi.MutationExecutorService;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.OneToManyPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.jdbc.JdbcMutationOperation;

import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;

/**
 * OneToMany delete coordinator if the element is a {@link org.hibernate.persister.entity.UnionSubclassEntityPersister}.
 */
public class DeleteRowsCoordinatorTablePerSubclass implements DeleteRowsCoordinator {
	private final CollectionMutationTarget mutationTarget;
	private final RowMutationOperations rowMutationOperations;
	private final boolean deleteByIndex;

	private final SubclassEntry[] subclassEntries;
	private final MutationExecutorService mutationExecutorService;

	public DeleteRowsCoordinatorTablePerSubclass(
			OneToManyPersister mutationTarget,
			RowMutationOperations rowMutationOperations,
			boolean deleteByIndex,
			ServiceRegistry serviceRegistry) {
		this.mutationTarget = mutationTarget;
		this.rowMutationOperations = rowMutationOperations;
		this.deleteByIndex = deleteByIndex;
		this.subclassEntries = new SubclassEntry[mutationTarget.getElementPersister().getRootEntityDescriptor().getSubclassEntityNames().size()];
		this.mutationExecutorService = serviceRegistry.getService( MutationExecutorService.class );
	}

	@Override
	public CollectionMutationTarget getMutationTarget() {
		return mutationTarget;
	}

	@Override
	public void deleteRows(PersistentCollection<?> collection, Object key, SharedSessionContractImplementor session) {
		if ( MODEL_MUTATION_LOGGER.isDebugEnabled() ) {
			MODEL_MUTATION_LOGGER.debugf(
					"Deleting removed collection rows - %s : %s",
					mutationTarget.getRolePath(),
					key
			);
		}

		final PluralAttributeMapping pluralAttribute = mutationTarget.getTargetPart();
		final CollectionPersister collectionDescriptor = pluralAttribute.getCollectionDescriptor();

		final Iterator<?> deletes = collection.getDeletes( collectionDescriptor, !deleteByIndex );
		if ( !deletes.hasNext() ) {
			MODEL_MUTATION_LOGGER.debug( "No rows to delete" );
			return;
		}
		final MutationExecutor[] executors = new MutationExecutor[subclassEntries.length];
		try {
			int deletionCount = 0;

			final RowMutationOperations.Restrictions restrictions = rowMutationOperations.getDeleteRowRestrictions();

			while ( deletes.hasNext() ) {
				final Object removal = deletes.next();
				final EntityEntry entityEntry = session.getPersistenceContextInternal().getEntry( removal );
				final int subclassId = entityEntry.getPersister().getSubclassId();
				final MutationExecutor mutationExecutor;
				if ( executors[subclassId] == null ) {
					final SubclassEntry subclassEntry = getSubclassEntry( entityEntry.getPersister() );
					mutationExecutor = executors[subclassId] = mutationExecutorService.createExecutor(
							subclassEntry.batchKeySupplier,
							subclassEntry.operationGroup,
							session
					);
				}
				else {
					mutationExecutor = executors[subclassId];
				}

				restrictions.applyRestrictions(
						collection,
						key,
						removal,
						deletionCount,
						session,
						mutationExecutor.getJdbcValueBindings()
				);

				mutationExecutor.execute( removal, null, null, null, session );

				deletionCount++;
			}

			MODEL_MUTATION_LOGGER.debugf( "Done deleting `%s` collection rows : %s", deletionCount, mutationTarget.getRolePath() );
		}
		finally {
			for ( MutationExecutor executor : executors ) {
				if ( executor != null ) {
					executor.release();
				}
			}
		}
	}

	private SubclassEntry getSubclassEntry(EntityPersister elementPersister) {
		final int subclassId = elementPersister.getSubclassId();
		final SubclassEntry subclassEntry = subclassEntries[subclassId];
		if ( subclassEntry != null ) {
			return subclassEntry;
		}
		final BasicBatchKey basicBatchKey = new BasicBatchKey( mutationTarget.getRolePath() + "#DELETE#" + subclassId );
		return subclassEntries[subclassId] = new SubclassEntry(
				() -> basicBatchKey,
				createOperationGroup( elementPersister )
		);
	}

	private MutationOperationGroup createOperationGroup(EntityPersister elementPersister) {
		assert mutationTarget.getTargetPart() != null;
		assert mutationTarget.getTargetPart().getKeyDescriptor() != null;

		final CollectionTableMapping collectionTableMapping = mutationTarget.getCollectionTableMapping();
		final JdbcMutationOperation operation = rowMutationOperations.getDeleteRowOperation(
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
		return MutationOperationGroupFactory.singleOperation( MutationType.DELETE, mutationTarget, operation );
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
