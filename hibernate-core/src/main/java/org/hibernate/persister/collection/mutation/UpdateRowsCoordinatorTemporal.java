/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.MutationType;

import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;

/**
 * {@link UpdateRowsCoordinator} implementation for temporal collection tables
 * in the {@link org.hibernate.cfg.TemporalTableStrategy#SINGLE_TABLE} temporal
 * table mapping strategy.
 *
 * @author Gavin King
 */
public class UpdateRowsCoordinatorTemporal extends AbstractUpdateRowsCoordinator implements UpdateRowsCoordinator {

	private final RowMutationOperations rowMutationOperations;
	private final BasicBatchKey deleteBatchKey;
	private final BasicBatchKey insertBatchKey;

	private MutationOperationGroup deleteOperationGroup;
	private MutationOperationGroup insertOperationGroup;

	public UpdateRowsCoordinatorTemporal(
			CollectionMutationTarget mutationTarget,
			RowMutationOperations rowMutationOperations,
			SessionFactoryImplementor sessionFactory) {
		super( mutationTarget, sessionFactory );
		this.rowMutationOperations = rowMutationOperations;
		this.deleteBatchKey = new BasicBatchKey( mutationTarget.getRolePath() + "#DELETE" );
		this.insertBatchKey = new BasicBatchKey( mutationTarget.getRolePath() + "#INSERT" );
	}

	@Override
	protected int doUpdate(Object key, PersistentCollection<?> collection, SharedSessionContractImplementor session) {
		if ( rowMutationOperations.getDeleteRowOperation() == null
				|| rowMutationOperations.getInsertRowOperation() == null ) {
			return 0;
		}

		if ( deleteOperationGroup == null ) {
			deleteOperationGroup = singleOperation(
					MutationType.DELETE,
					getMutationTarget(),
					rowMutationOperations.getDeleteRowOperation()
			);
		}
		if ( insertOperationGroup == null ) {
			insertOperationGroup = singleOperation(
					MutationType.INSERT,
					getMutationTarget(),
					rowMutationOperations.getInsertRowOperation()
			);
		}

		final MutationExecutor deleteExecutor = mutationExecutorService.createExecutor(
				() -> deleteBatchKey,
				deleteOperationGroup,
				session
		);
		final MutationExecutor insertExecutor = mutationExecutorService.createExecutor(
				() -> insertBatchKey,
				insertOperationGroup,
				session
		);

		try {
			final var attribute = getMutationTarget().getTargetPart();
			final var collectionDescriptor = attribute.getCollectionDescriptor();
			final var entries = collection.entries( collectionDescriptor );

			int count = 0;
			if ( collection.isElementRemoved() ) {
				final List<Object> elements = new ArrayList<>();
				while ( entries.hasNext() ) {
					elements.add( entries.next() );
				}
				for ( int i = elements.size() - 1; i >= 0; i-- ) {
					final Object entry = elements.get( i );
					if ( processRow( key, collection, entry, i, deleteExecutor, insertExecutor, session ) ) {
						count++;
					}
				}
			}
			else {
				int position = 0;
				while ( entries.hasNext() ) {
					final Object entry = entries.next();
					if ( processRow( key, collection, entry, position++, deleteExecutor, insertExecutor, session ) ) {
						count++;
					}
				}
			}
			return count;
		}
		finally {
			deleteExecutor.release();
			insertExecutor.release();
		}
	}

	private boolean processRow(
			Object key,
			PersistentCollection<?> collection,
			Object entry,
			int entryPosition,
			MutationExecutor deleteExecutor,
			MutationExecutor insertExecutor,
			SharedSessionContractImplementor session) {
		final var attribute = getMutationTarget().getTargetPart();
		if ( !collection.needsUpdating( entry, entryPosition, attribute ) ) {
			return false;
		}

		final Object deleteRowValue = resolveDeleteRowValue( collection, entry, entryPosition );
		rowMutationOperations.getDeleteRowRestrictions().applyRestrictions(
				collection,
				key,
				deleteRowValue,
				entryPosition,
				session,
				deleteExecutor.getJdbcValueBindings()
		);
		deleteExecutor.execute( deleteRowValue, null, null, null, session );

		rowMutationOperations.getInsertRowValues().applyValues(
				collection,
				key,
				entry,
				entryPosition,
				session,
				insertExecutor.getJdbcValueBindings()
		);
		insertExecutor.execute( entry, null, null, null, session );

		return true;
	}
}
