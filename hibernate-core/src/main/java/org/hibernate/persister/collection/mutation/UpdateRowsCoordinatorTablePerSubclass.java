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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;

/**
 * OneToMany update coordinator if the element is a {@link org.hibernate.persister.entity.UnionSubclassEntityPersister}.
 */
public class UpdateRowsCoordinatorTablePerSubclass extends AbstractUpdateRowsCoordinator {
	private final OneToManyPersister persister;
	private final RowMutationOperations rowMutationOperations;

	private final Map<EntityPersister, SubclassEntry> deleteSubclassEntries = new ConcurrentHashMap<>();
	private final Map<EntityPersister, SubclassEntry> insertSubclassEntries = new ConcurrentHashMap<>();

	private final MutationExecutorService mutationExecutorService;

	public UpdateRowsCoordinatorTablePerSubclass(
			@Nonnull OneToManyPersister mutationTarget,
			@Nonnull RowMutationOperations rowMutationOperations,
			@Nonnull SessionFactoryImplementor sessionFactory) {
		super( mutationTarget, sessionFactory );
		this.persister = mutationTarget;
		this.rowMutationOperations = rowMutationOperations;
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
			final Map<EntityPersister, MutationExecutor> executors = new HashMap<>();
			try {
				int entryPosition = -1;
				while ( entries.hasNext() ) {
					final Object entry = entries.next();
					entryPosition++;
					if ( collection.needsUpdating( entry, entryPosition, attributeMapping ) ) {
						final Object entryToUpdate = collection.getSnapshotElement( entry, entryPosition );
						final var mutationExecutor = executors.computeIfAbsent(
								persister.getConcreteElementPersister( entryToUpdate, session ),
								elementPersister -> createExecutor( getDeleteSubclassEntry( elementPersister ), session )
						);
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
				executors.values().forEach( MutationExecutor::release );
			}
		}
	}

	@Nonnull
	private MutationExecutor createExecutor(
			@Nonnull SubclassEntry subclassEntry,
			@Nonnull SharedSessionContractImplementor session) {
		return mutationExecutorService.createExecutor(
				subclassEntry.batchKeySupplier,
				subclassEntry.operationGroup,
				session
		);
	}

	@Nonnull
	private SubclassEntry getDeleteSubclassEntry( @Nonnull EntityPersister elementPersister) {
		return deleteSubclassEntries.computeIfAbsent( elementPersister, persister -> {
			final var basicBatchKey =
					new BasicBatchKey( getMutationTarget().getRolePath() + "#UPDATE-DELETE#" + persister.getSubclassId() );
			return new SubclassEntry(
					() -> basicBatchKey,
					resolveDeleteGroup( persister )
			);
		} );
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

		final Map<EntityPersister, MutationExecutor> executors = new HashMap<>();
		try {
			int entryPosition = -1;
			while ( entries.hasNext() ) {
				final Object entry = entries.next();
				entryPosition++;
				if ( collection.needsUpdating( entry, entryPosition, attributeMapping ) ) {
					final var mutationExecutor = executors.computeIfAbsent(
							persister.getConcreteElementPersister( collection.getElement( entry ), session ),
							elementPersister -> createExecutor( getInsertSubclassEntry( elementPersister ), session )
					);
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
			executors.values().forEach( MutationExecutor::release );
		}
	}

	@Nonnull
	private SubclassEntry getInsertSubclassEntry( @Nonnull EntityPersister elementPersister) {
		return insertSubclassEntries.computeIfAbsent( elementPersister, persister -> {
			final var basicBatchKey =
					new BasicBatchKey( getMutationTarget().getRolePath() + "#UPDATE-INSERT#" + persister.getSubclassId() );
			return new SubclassEntry(
					() -> basicBatchKey,
					resolveInsertGroup( persister )
			);
		} );
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
