/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.AbstractCollectionPersister;
import org.hibernate.sql.model.MutationOperationGroup;


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
			AbstractCollectionPersister mutationTarget,
			RowMutationOperations rowMutationOperations,
			SessionFactoryImplementor sessionFactory) {
		super( mutationTarget, sessionFactory );
		this.rowMutationOperations = rowMutationOperations;
	}
	@Override
	protected int doUpdate(Object key, PersistentCollection<?> collection, SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException( "This operation is not supported yet!" );

//		final var entries =
//				collection.entries( getMutationTarget().getTargetPart().getCollectionDescriptor() );
//		int count = 0;
//
//		if ( collection.isElementRemoved() ) {
//			// the update should be done starting from the end of the elements
//			// 		- make a copy so that we can go in reverse
//			final List<Object> elements = new ArrayList<>();
//			while ( entries.hasNext() ) {
//				elements.add( entries.next() );
//			}
//
//			for ( int i = elements.size() - 1; i >= 0; i-- ) {
//				final Object entry = elements.get( i );
//				final boolean updated = processRow(
//						key,
//						collection,
//						entry,
//						i,
//						session
//				);
//				if ( updated ) {
//					count++;
//				}
//			}
//		}
//		else {
//			int position = 0;
//			while ( entries.hasNext() ) {
//				final Object entry = entries.next();
//				final boolean updated = processRow(
//						key,
//						collection,
//						entry,
//						position++,
//						session
//				);
//				if ( updated ) {
//					count++;
//				}
//			}
//		}
//
//		return count;
	}

//	private boolean processRow(
//			Object key,
//			PersistentCollection<?> collection,
//			Object entry,
//			int entryPosition,
//			SharedSessionContractImplementor session) {
//		if ( jdbcOperations.getUpdateRowOperation() != null ) {
//			final var attribute = getMutationTarget().getTargetPart();
//			if ( !collection.needsUpdating( entry, entryPosition, attribute ) ) {
//				return false;
//			}
//
//			jdbcOperations.getUpdateRowValues().applyValues(
//					collection,
//					key,
//					entry,
//					entryPosition,
//					session,
//					mutationExecutor.getJdbcValueBindings()
//			);
//
//			jdbcOperations.getUpdateRowRestrictions().applyRestrictions(
//					collection,
//					key,
//					entry,
//					entryPosition,
//					session,
//					mutationExecutor.getJdbcValueBindings()
//			);
//
//			mutationExecutor.execute( collection, null, null, null, session );
//			return true;
//		}
//		else {
//			return false;
//		}
//	}
//
//	protected MutationOperationGroup getOperationGroup() {
//		if ( operationGroup == null ) {
//			final var updateRowOperation = jdbcOperations.getUpdateRowOperation();
//			operationGroup = updateRowOperation == null
//					? noOperations( MutationType.UPDATE, getMutationTarget() )
//					: singleOperation( MutationType.UPDATE, getMutationTarget(), updateRowOperation );
//		}
//		return operationGroup;
//	}

//	@Override
//	public List<PlannedOperation> decomposeUpdateRows(
//			PersistentCollection<?> collection,
//			Object key,
//			int ordinalBase,
//			SharedSessionContractImplementor session) {
//		final var jdbcOperation = collectionJdbcOperations.getUpdateRowOperation();
//		if ( jdbcOperation == null ) {
//			return List.of();
//		}
//
//		final var mutationTarget = getMutationTarget();
//		final var tableDescriptor = mutationTarget.getCollectionTableDescriptor();
//
//		final var pluralAttribute = mutationTarget.getTargetPart();
//		final var collectionDescriptor = pluralAttribute.getCollectionDescriptor();
//		final var entries = collection.entries( collectionDescriptor );
//
//		if ( !entries.hasNext() ) {
//			return List.of();
//		}
//
//		final List<PlannedOperation> operations = new ArrayList<>();
//		final var updateRowValues = rowJdbcOperations.getUpdateRowValues();
//		final var updateRowRestrictions = rowJdbcOperations.getUpdateRowRestrictions();
//
//		int entryCount = 0;
//		while ( entries.hasNext() ) {
//			final Object entry = entries.next();
//
//			if ( collection.needsUpdating( entry, entryCount, pluralAttribute ) ) {
//				// Create ONE PlannedOperation per row update
//				final BindPlan bindPlan = new SingleRowUpdateBindPlan(
//						collection,
//						key,
//						entry,
//						entryCount,
//						updateRowValues,
//						updateRowRestrictions,
//						mutationTarget
//				);
//
//				final PlannedOperation plannedOp = new PlannedOperation(
//						tableDescriptor,
//						MutationKind.UPDATE,
//						jdbcOperation,
//						bindPlan,
//						ordinalBase * 1_000 + entryCount,
//						"UpdateRow[" + entryCount + "](" + mutationTarget.getRolePath() + ")"
//				);
//
//				operations.add( plannedOp );
//			}
//
//			entryCount++;
//		}
//
//		return operations;
//	}

}
