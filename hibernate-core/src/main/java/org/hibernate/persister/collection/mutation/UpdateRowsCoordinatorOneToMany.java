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
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.jdbc.JdbcMutationOperation;

/**
 * @author Steve Ebersole
 */
public class UpdateRowsCoordinatorOneToMany extends AbstractUpdateRowsCoordinator {
	private final RowMutationOperations rowMutationOperations;

	private MutationOperationGroup deleteOperationGroup;
	private MutationOperationGroup insertOperationGroup;

	public UpdateRowsCoordinatorOneToMany(
			CollectionMutationTarget mutationTarget,
			RowMutationOperations rowMutationOperations,
			SessionFactoryImplementor sessionFactory) {
		super( mutationTarget, sessionFactory );
		this.rowMutationOperations = rowMutationOperations;
	}

	@Override
	protected int doUpdate(Object key, PersistentCollection<?> collection, SharedSessionContractImplementor session) {
		if ( rowMutationOperations.hasDeleteRow() ) {
			deleteRows( key, collection, session );
		}

		if ( rowMutationOperations.hasInsertRow() ) {
			return insertRows( key, collection, session );
		}

		return 0;
	}

	private void deleteRows(Object key, PersistentCollection<?> collection, SharedSessionContractImplementor session) {
		final PluralAttributeMapping attributeMapping = getMutationTarget().getTargetPart();
		final CollectionPersister collectionDescriptor = attributeMapping.getCollectionDescriptor();
		final Iterator<?> entries = collection.entries( collectionDescriptor );
		if ( !entries.hasNext() ) {
			return;
		}

		final MutationOperationGroup operationGroup = resolveDeleteGroup();
		final MutationExecutor mutationExecutor = mutationExecutorService.createExecutor(
				() -> new BasicBatchKey( getMutationTarget().getRolePath() + "#UPDATE-DELETE" ),
				operationGroup,
				session
		);

		try {
			final JdbcValueBindings jdbcValueBindings = mutationExecutor.getJdbcValueBindings();

			int entryPosition = -1;

			while ( entries.hasNext() ) {
				final Object entry = entries.next();
				entryPosition++;

				if ( !collection.needsUpdating( entry, entryPosition, attributeMapping ) ) {
					continue;
				}
				final Object entryToUpdate = collection.getSnapshotElement( entry, entryPosition );

				rowMutationOperations.getDeleteRowRestrictions().applyRestrictions(
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
		finally {
			mutationExecutor.release();
		}
	}

	private MutationOperationGroup resolveDeleteGroup() {
		if ( deleteOperationGroup == null ) {
			final JdbcMutationOperation operation = rowMutationOperations.getDeleteRowOperation();
			assert operation != null;

			deleteOperationGroup = MutationOperationGroupFactory.singleOperation( MutationType.DELETE, getMutationTarget(), operation );
		}

		return deleteOperationGroup;
	}

	private int insertRows(Object key, PersistentCollection<?> collection, SharedSessionContractImplementor session) {
		final PluralAttributeMapping attributeMapping = getMutationTarget().getTargetPart();
		final CollectionPersister collectionDescriptor = attributeMapping.getCollectionDescriptor();
		final Iterator<?> entries = collection.entries( collectionDescriptor );
		if ( !entries.hasNext() ) {
			return -1;
		}

		final MutationOperationGroup operationGroup = resolveInsertGroup();
		final MutationExecutor mutationExecutor = mutationExecutorService.createExecutor(
				() -> new BasicBatchKey( getMutationTarget().getRolePath() + "#UPDATE-INSERT" ),
				operationGroup,
				session
		);

		try {
			final JdbcValueBindings jdbcValueBindings = mutationExecutor.getJdbcValueBindings();

			int entryPosition = -1;

			while ( entries.hasNext() ) {
				final Object entry = entries.next();
				entryPosition++;

				if ( !collection.needsUpdating( entry, entryPosition, attributeMapping ) ) {
					continue;
				}

				rowMutationOperations.getInsertRowValues().applyValues(
						collection,
						key,
						entry,
						entryPosition,
						session,
						jdbcValueBindings
				);

				mutationExecutor.execute( entry, null, null, null, session );
			}

			return entryPosition;
		}
		finally {
			mutationExecutor.release();
		}
	}

	private MutationOperationGroup resolveInsertGroup() {
		if ( insertOperationGroup == null ) {
			final JdbcMutationOperation operation = rowMutationOperations.getInsertRowOperation();
			assert operation != null;

			insertOperationGroup = MutationOperationGroupFactory.singleOperation( MutationType.INSERT, getMutationTarget(), operation );
		}

		return insertOperationGroup;
	}
}
