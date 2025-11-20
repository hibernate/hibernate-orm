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

import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.noOperations;
import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;

/**
 * UpdateRowsCoordinator implementation for cases with a separate collection table
 *
 * @see org.hibernate.persister.collection.BasicCollectionPersister
 *
 * @author Steve Ebersole
 */
public class UpdateRowsCoordinatorStandard extends AbstractUpdateRowsCoordinator implements UpdateRowsCoordinator {

	private final RowMutationOperations rowMutationOperations;
	private MutationOperationGroup operationGroup;

	public UpdateRowsCoordinatorStandard(
			CollectionMutationTarget mutationTarget,
			RowMutationOperations rowMutationOperations,
			SessionFactoryImplementor sessionFactory) {
		super( mutationTarget, sessionFactory );
		this.rowMutationOperations = rowMutationOperations;
	}

	@Override
	protected int doUpdate(Object key, PersistentCollection<?> collection, SharedSessionContractImplementor session) {
		final var operationGroup = getOperationGroup();

		final var mutationExecutor = mutationExecutorService.createExecutor(
				() -> new BasicBatchKey( getMutationTarget().getRolePath() + "#UPDATE" ),
				operationGroup,
				session
		);

		try {
			final var entries =
					collection.entries( getMutationTarget().getTargetPart().getCollectionDescriptor() );
			int count = 0;

			if ( collection.isElementRemoved() ) {
				// the update should be done starting from the end of the elements
				// 		- make a copy so that we can go in reverse
				final List<Object> elements = new ArrayList<>();
				while ( entries.hasNext() ) {
					elements.add( entries.next() );
				}

				for ( int i = elements.size() - 1; i >= 0; i-- ) {
					final Object entry = elements.get( i );
					final boolean updated = processRow(
							key,
							collection,
							entry,
							i,
							mutationExecutor,
							session
					);
					if ( updated ) {
						count++;
					}
				}
			}
			else {
				int position = 0;
				while ( entries.hasNext() ) {
					final Object entry = entries.next();
					final boolean updated = processRow(
							key,
							collection,
							entry,
							position++,
							mutationExecutor,
							session
					);
					if ( updated ) {
						count++;
					}
				}
			}

			return count;
		}
		finally {
			mutationExecutor.release();
		}
	}

	private boolean processRow(
			Object key,
			PersistentCollection<?> collection,
			Object entry,
			int entryPosition,
			MutationExecutor mutationExecutor,
			SharedSessionContractImplementor session) {
		if ( rowMutationOperations.getUpdateRowOperation() != null ) {
			final var attribute = getMutationTarget().getTargetPart();
			if ( !collection.needsUpdating( entry, entryPosition, attribute ) ) {
				return false;
			}

			rowMutationOperations.getUpdateRowValues().applyValues(
					collection,
					key,
					entry,
					entryPosition,
					session,
					mutationExecutor.getJdbcValueBindings()
			);

			rowMutationOperations.getUpdateRowRestrictions().applyRestrictions(
					collection,
					key,
					entry,
					entryPosition,
					session,
					mutationExecutor.getJdbcValueBindings()
			);

			mutationExecutor.execute( collection, null, null, null, session );
			return true;
		}
		else {
			return false;
		}
	}

	protected MutationOperationGroup getOperationGroup() {
		if ( operationGroup == null ) {
			final var updateRowOperation = rowMutationOperations.getUpdateRowOperation();
			operationGroup = updateRowOperation == null
					? noOperations( MutationType.UPDATE, getMutationTarget() )
					: singleOperation( MutationType.UPDATE, getMutationTarget(), updateRowOperation );
		}
		return operationGroup;
	}


}
