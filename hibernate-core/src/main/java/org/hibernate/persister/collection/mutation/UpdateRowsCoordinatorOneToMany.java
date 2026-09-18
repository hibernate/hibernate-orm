/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

import jakarta.annotation.Nullable;

import jakarta.annotation.Nonnull;


import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.mutation.spi.MutationExecutorService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.OneToManyPersister;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.spi.mutation.MutationType;

import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;

/**
 * @author Steve Ebersole
 */
public class UpdateRowsCoordinatorOneToMany extends AbstractUpdateRowsCoordinator {
	private final RowMutationOperations rowMutationOperations;
	private final MutationExecutorService mutationExecutorService;

	@Nullable
	private MutationOperationGroup deleteOperationGroup;
	@Nullable
	private MutationOperationGroup insertOperationGroup;

	public UpdateRowsCoordinatorOneToMany(
			@Nonnull OneToManyPersister mutationTarget,
			@Nonnull RowMutationOperations rowMutationOperations,
			@Nonnull SessionFactoryImplementor sessionFactory) {
		super( mutationTarget, sessionFactory );
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
			final var operationGroup = resolveDeleteGroup();
			final var mutationExecutor = mutationExecutorService.createExecutor(
					() -> new BasicBatchKey( getMutationTarget().getRolePath() + "#UPDATE-DELETE" ),
					operationGroup,
					session
			);

			try {
				final var jdbcValueBindings = mutationExecutor.getJdbcValueBindings();
				int entryPosition = -1;
				while ( entries.hasNext() ) {
					final Object entry = entries.next();
					entryPosition++;
					if ( collection.needsUpdating( entry, entryPosition, attributeMapping ) ) {
						final Object entryToUpdate = collection.getSnapshotElement( entry, entryPosition );
						castNonNull( rowMutationOperations.getDeleteRowRestrictions() ).applyRestrictions(
								collection,
								key,
								entryToUpdate,
								entryPosition,
								session,
								jdbcValueBindings
						);
						mutationExecutor.execute( entryToUpdate, null, null, null, session );
					}
				}
			}
			finally {
				mutationExecutor.release();
			}
		}
	}

	@Nonnull
	private MutationOperationGroup resolveDeleteGroup() {
		if ( deleteOperationGroup == null ) {
			final var operation = rowMutationOperations.getDeleteRowOperation();
			assert operation != null;
			deleteOperationGroup = singleOperation( MutationType.DELETE, getMutationTarget(), operation );
		}
		return deleteOperationGroup;
	}

	private int insertRows(@Nonnull Object key, @Nonnull PersistentCollection<?> collection, @Nonnull SharedSessionContractImplementor session) {
		final var attributeMapping = getMutationTarget().getTargetPart();
		final var collectionDescriptor = attributeMapping.getCollectionDescriptor();
		final var entries = collection.entries( collectionDescriptor );
		if ( !entries.hasNext() ) {
			return -1;
		}

		final var operationGroup = resolveInsertGroup();
		final var mutationExecutor = mutationExecutorService.createExecutor(
				() -> new BasicBatchKey( getMutationTarget().getRolePath() + "#UPDATE-INSERT" ),
				operationGroup,
				session
		);

		try {
			final var jdbcValueBindings = mutationExecutor.getJdbcValueBindings();
			int entryPosition = -1;
			while ( entries.hasNext() ) {
				final Object entry = entries.next();
				entryPosition++;
				if ( collection.needsUpdating( entry, entryPosition, attributeMapping ) ) {
					castNonNull( rowMutationOperations.getInsertRowValues() ).applyValues(
							collection,
							key,
							entry,
							entryPosition,
							session,
							jdbcValueBindings
					);

					mutationExecutor.execute( entry, null, null, null, session );
				}
			}
			return entryPosition;
		}
		finally {
			mutationExecutor.release();
		}
	}

	@Nonnull
	private MutationOperationGroup resolveInsertGroup() {
		if ( insertOperationGroup == null ) {
			final var operation = rowMutationOperations.getInsertRowOperation();
			assert operation != null;
			insertOperationGroup = singleOperation( MutationType.INSERT, getMutationTarget(), operation );
		}
		return insertOperationGroup;
	}
}
