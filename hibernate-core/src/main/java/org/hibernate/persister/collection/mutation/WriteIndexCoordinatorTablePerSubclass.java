package org.hibernate.persister.collection.mutation;

import jakarta.annotation.Nonnull;

import org.hibernate.action.queue.spi.decompose.collection.CollectionMutationTarget;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.jdbc.mutation.spi.MutationExecutorService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.OneToManyPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.spi.mutation.MutationType;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;

/**
 * OneToMany write-index coordinator if the element is a {@link org.hibernate.persister.entity.UnionSubclassEntityPersister}.
 * <p>
 * Writes the order column of each element to the table of its concrete subclass.
 */
public class WriteIndexCoordinatorTablePerSubclass implements WriteIndexCoordinator {
	private final OneToManyPersister mutationTarget;
	private final RowMutationOperations rowMutationOperations;
	private final MutationExecutorService mutationExecutorService;

	private final Map<EntityPersister, SubclassOperation> subclassOperations = new ConcurrentHashMap<>();

	public WriteIndexCoordinatorTablePerSubclass(
			@Nonnull OneToManyPersister mutationTarget,
			@Nonnull RowMutationOperations rowMutationOperations,
			@Nonnull SessionFactoryImplementor sessionFactory) {
		this.mutationTarget = mutationTarget;
		this.rowMutationOperations = rowMutationOperations;
		mutationExecutorService =
				sessionFactory.getServiceRegistry()
						.requireService( MutationExecutorService.class );
	}

	@Nonnull
	@Override
	public String toString() {
		return "WriteIndexCoordinator(" + mutationTarget.getRolePath() + ")";
	}

	@Nonnull
	@Override
	public CollectionMutationTarget getMutationTarget() {
		return mutationTarget;
	}

	@Override
	public void writeIndex(
			@Nonnull PersistentCollection<?> collection,
			@Nonnull Iterator<?> entries,
			@Nonnull Object key,
			boolean resetIndex,
			@Nonnull SharedSessionContractImplementor session) {
		final var updateRowValues = rowMutationOperations.getUpdateRowValues();
		final var updateRowRestrictions = rowMutationOperations.getUpdateRowRestrictions();
		if ( !entries.hasNext() || updateRowValues == null || updateRowRestrictions == null ) {
			return;
		}

		// one executor for the table of each concrete subclass
		final Map<EntityPersister, MutationExecutor> executors = new HashMap<>();
		try {
			int nextIndex = getBaseIndex() + ( resetIndex ? 0 : mutationTarget.getSize( key, session ) );
			while ( entries.hasNext() ) {
				final Object entry = entries.next();
				if ( entry != null && collection.entryExists( entry, nextIndex ) ) {
					final var mutationExecutor = executors.computeIfAbsent(
							mutationTarget.getConcreteElementPersister( collection.getElement( entry ), session ),
							elementPersister -> createExecutor( elementPersister, session )
					);
					final var jdbcValueBindings = mutationExecutor.getJdbcValueBindings();
					updateRowValues.applyValues(
							collection,
							key,
							entry,
							nextIndex,
							session,
							jdbcValueBindings
					);
					updateRowRestrictions.applyRestrictions(
							collection,
							key,
							entry,
							nextIndex,
							session,
							jdbcValueBindings
					);
					mutationExecutor.execute( collection, null, null, null, session );
				}
				// a null element occupies its position, leaving a gap in the order column
				nextIndex++;
			}
		}
		finally {
			executors.values().forEach( MutationExecutor::release );
		}
	}

	private int getBaseIndex() {
		final var indexMetadata = mutationTarget.getTargetPart().getIndexMetadata();
		return indexMetadata != null ? indexMetadata.getListIndexBase() : 0;
	}

	@Nonnull
	private MutationExecutor createExecutor(
			@Nonnull EntityPersister elementPersister,
			@Nonnull SharedSessionContractImplementor session) {
		final var subclassOperation =
				subclassOperations.computeIfAbsent( elementPersister, this::createSubclassOperation );
		return mutationExecutorService.createExecutor(
				subclassOperation::batchKey,
				subclassOperation.operationGroup(),
				session
		);
	}

	@Nonnull
	private SubclassOperation createSubclassOperation(@Nonnull EntityPersister elementPersister) {
		final var operation = rowMutationOperations.getUpdateRowOperation(
				new CollectionTableMapping(
						mutationTarget.getCollectionTableMapping(),
						elementPersister.getMappedTableDetails().getTableName()
				)
		);
		return new SubclassOperation(
				new BasicBatchKey( mutationTarget.getRolePath() + "#INDEX#" + elementPersister.getSubclassId() ),
				singleOperation( MutationType.UPDATE, mutationTarget, operation )
		);
	}

	private record SubclassOperation(
			@Nonnull BasicBatchKey batchKey,
			@Nonnull MutationOperationGroup operationGroup) {
	}
}
