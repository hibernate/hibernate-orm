/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import jakarta.annotation.Nonnull;


import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.jdbc.mutation.spi.BatchKeyAccess;
import org.hibernate.engine.jdbc.mutation.spi.MutationExecutorService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.OneToManyPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.spi.mutation.MutationType;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;

/**
 * OneToMany update coordinator if the element is a {@link org.hibernate.persister.entity.UnionSubclassEntityPersister}.
 */
public class UpdateRowsCoordinatorTablePerSubclass extends AbstractUpdateRowsCoordinator {
	private final RowMutationOperations rowMutationOperations;

	private final SubclassEntry[] deleteSubclassEntries;
	private final SubclassEntry[] insertSubclassEntries;

	private final MutationExecutorService mutationExecutorService;

	public UpdateRowsCoordinatorTablePerSubclass(
			@Nonnull OneToManyPersister mutationTarget,
			@Nonnull RowMutationOperations rowMutationOperations,
			@Nonnull SessionFactoryImplementor sessionFactory) {
		super( mutationTarget, sessionFactory );
		this.rowMutationOperations = rowMutationOperations;
		final int size =
				castNonNull( mutationTarget.getElementPersister() ).getRootEntityDescriptor()
						.getSubclassEntityNames().size();
		deleteSubclassEntries = new SubclassEntry[size];
		insertSubclassEntries = new SubclassEntry[size];

		mutationExecutorService = sessionFactory.getServiceRegistry().requireService( MutationExecutorService.class );
	}

	@Override
	protected int doUpdate(@Nonnull Object key, @Nonnull PersistentCollection<?> collection, @Nonnull SharedSessionContractImplementor session) {
		if ( rowMutationOperations.hasDeleteRow() ) {
			deleteRows( key, collection, session );
		}

		if ( rowMutationOperations.hasInsertRow() ) {
			return insertRows( key, collection, session );
		}

		return 0;
	}

	private void deleteRows(@Nonnull Object key, @Nonnull PersistentCollection<?> collection, @Nonnull SharedSessionContractImplementor session) {
		final var attributeMapping = getMutationTarget().getTargetPart();
		final var collectionDescriptor = attributeMapping.getCollectionDescriptor();
		final var entries = collection.entries( collectionDescriptor );
		if ( entries.hasNext() ) {
			final var executors = new MutationExecutor[deleteSubclassEntries.length];
			try {
				int entryPosition = -1;
				while ( entries.hasNext() ) {
					final Object entry = entries.next();
					entryPosition++;
					if ( collection.needsUpdating( entry, entryPosition, attributeMapping ) ) {
						final Object entryToUpdate = collection.getSnapshotElement( entry, entryPosition );
						final var entityEntry = session.getPersistenceContextInternal().getEntry( entryToUpdate );
						final int subclassId = entityEntry.getPersister().getSubclassId();
						final MutationExecutor mutationExecutor;
						if ( executors[subclassId] == null ) {
							final var subclassEntry = getDeleteSubclassEntry( entityEntry.getPersister() );
							mutationExecutor = executors[subclassId] = mutationExecutorService.createExecutor(
									subclassEntry.batchKeySupplier,
									subclassEntry.operationGroup,
									session
							);
						}
						else {
							mutationExecutor = executors[subclassId];
						}
						castNonNull( rowMutationOperations.getDeleteRowRestrictions() ).applyRestrictions(
								collection,
								key,
								entryToUpdate,
								entryPosition,
								session,
								mutationExecutor.getJdbcValueBindings()
						);

						mutationExecutor.execute( entryToUpdate, null, null, null, session );
					}
				}
			}
			finally {
				for ( var executor : executors ) {
					if ( executor != null ) {
						executor.release();
					}
				}
			}
		}
	}

	@Nonnull
	private SubclassEntry getDeleteSubclassEntry( @Nonnull EntityPersister elementPersister) {
		final int subclassId = elementPersister.getSubclassId();
		final var subclassEntry = deleteSubclassEntries[subclassId];
		if ( subclassEntry != null ) {
			return subclassEntry;
		}
		final var basicBatchKey = new BasicBatchKey( getMutationTarget().getRolePath() + "#UPDATE-DELETE#" + subclassId );
		return deleteSubclassEntries[subclassId] = new SubclassEntry(
				() -> basicBatchKey,
				resolveDeleteGroup( elementPersister )
		);
	}

	@Nonnull
	private MutationOperationGroup resolveDeleteGroup(@Nonnull EntityPersister elementPersister) {
		final var collectionTableMapping = getMutationTarget().getCollectionTableMapping();
		final var operation = rowMutationOperations.getDeleteRowOperation(
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

		return singleOperation( MutationType.DELETE, getMutationTarget(), operation );
	}

	private int insertRows(@Nonnull Object key, @Nonnull PersistentCollection<?> collection, @Nonnull SharedSessionContractImplementor session) {
		final var attributeMapping = getMutationTarget().getTargetPart();
		final var collectionDescriptor = attributeMapping.getCollectionDescriptor();
		final var entries = collection.entries( collectionDescriptor );
		if ( !entries.hasNext() ) {
			return -1;
		}

		final var executors = new MutationExecutor[insertSubclassEntries.length];
		try {
			int entryPosition = -1;
			while ( entries.hasNext() ) {
				final Object entry = entries.next();
				entryPosition++;
				if ( collection.needsUpdating( entry, entryPosition, attributeMapping ) ) {
					final var entityEntry = session.getPersistenceContextInternal().getEntry( entry );
					final int subclassId = entityEntry.getPersister().getSubclassId();
					final MutationExecutor mutationExecutor;
					if ( executors[subclassId] == null ) {
						final var subclassEntry = getInsertSubclassEntry( entityEntry.getPersister() );
						mutationExecutor = executors[subclassId] = mutationExecutorService.createExecutor(
								subclassEntry.batchKeySupplier,
								subclassEntry.operationGroup,
								session
						);
					}
					else {
						mutationExecutor = executors[subclassId];
					}
					castNonNull( rowMutationOperations.getInsertRowValues() ).applyValues(
							collection,
							key,
							entry,
							entryPosition,
							session,
							mutationExecutor.getJdbcValueBindings()
					);
					mutationExecutor.execute( entry, null, null, null, session );
				}
			}
			return entryPosition;
		}
		finally {
			for ( var executor : executors ) {
				if ( executor != null ) {
					executor.release();
				}
			}
		}
	}

	@Nonnull
	private SubclassEntry getInsertSubclassEntry( @Nonnull EntityPersister elementPersister) {
		final int subclassId = elementPersister.getSubclassId();
		final var subclassEntry = insertSubclassEntries[subclassId];
		if ( subclassEntry != null ) {
			return subclassEntry;
		}
		else {
			final var basicBatchKey =
					new BasicBatchKey( getMutationTarget().getRolePath() + "#UPDATE-INSERT#" + subclassId );
			return insertSubclassEntries[subclassId] = new SubclassEntry(
					() -> basicBatchKey,
					resolveInsertGroup( elementPersister )
			);
		}
	}

	@Nonnull
	private MutationOperationGroup resolveInsertGroup(@Nonnull EntityPersister elementPersister) {
		final var collectionTableMapping = getMutationTarget().getCollectionTableMapping();
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

		return singleOperation( MutationType.INSERT, getMutationTarget(), operation );
	}

	private static class SubclassEntry {

		private final BatchKeyAccess batchKeySupplier;

		private final MutationOperationGroup operationGroup;

		public SubclassEntry(@Nonnull BatchKeyAccess batchKeySupplier, @Nonnull MutationOperationGroup operationGroup) {
			this.batchKeySupplier = batchKeySupplier;
			this.operationGroup = operationGroup;
		}
	}
}
