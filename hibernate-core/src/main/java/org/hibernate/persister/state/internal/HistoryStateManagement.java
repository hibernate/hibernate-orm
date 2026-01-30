/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.state.internal;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.Internal;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.mutation.DeleteRowsCoordinator;
import org.hibernate.persister.collection.mutation.DeleteRowsCoordinatorHistory;
import org.hibernate.persister.collection.mutation.DeleteRowsCoordinatorNoOp;
import org.hibernate.persister.collection.mutation.InsertRowsCoordinator;
import org.hibernate.persister.collection.mutation.InsertRowsCoordinatorHistory;
import org.hibernate.persister.collection.mutation.InsertRowsCoordinatorNoOp;
import org.hibernate.persister.collection.mutation.RemoveCoordinator;
import org.hibernate.persister.collection.mutation.RemoveCoordinatorHistory;
import org.hibernate.persister.collection.mutation.RemoveCoordinatorNoOp;
import org.hibernate.persister.collection.mutation.UpdateRowsCoordinator;
import org.hibernate.persister.collection.mutation.UpdateRowsCoordinatorHistory;
import org.hibernate.persister.collection.mutation.UpdateRowsCoordinatorNoOp;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.DeleteCoordinator;
import org.hibernate.persister.entity.mutation.DeleteCoordinatorHistory;
import org.hibernate.persister.entity.mutation.InsertCoordinator;
import org.hibernate.persister.entity.mutation.InsertCoordinatorHistory;
import org.hibernate.persister.entity.mutation.UpdateCoordinator;
import org.hibernate.persister.entity.mutation.UpdateCoordinatorHistory;
import org.hibernate.persister.state.StateManagement;
import org.hibernate.sql.model.MutationOperationGroup;

import static org.hibernate.persister.state.internal.AbstractStateManagement.isInsertAllowed;
import static org.hibernate.persister.state.internal.AbstractStateManagement.isUpdatePossible;
import static org.hibernate.persister.state.internal.AbstractStateManagement.resolveMutationTarget;

/**
 * @author Gavin King
 */
@Internal
public final class HistoryStateManagement implements StateManagement {
	public static final HistoryStateManagement INSTANCE = new HistoryStateManagement();

	private HistoryStateManagement() {
	}

	@Override
	public UpdateCoordinator createMergeCoordinator(EntityPersister persister) {
		//TODO: fix this!
		return new UpdateCoordinator() {
			@Override
			public @Nullable GeneratedValues update(Object entity, Object id, Object rowId, Object[] values, Object oldVersion, Object[] incomingOldValues, int[] dirtyAttributeIndexes, boolean hasDirtyCollection, SharedSessionContractImplementor session) {
				throw new UnsupportedOperationException( "upsert() not supported for history tables" );
			}

			@Override
			public void forceVersionIncrement(Object id, Object currentVersion, Object nextVersion, SharedSessionContractImplementor session) {
				throw new UnsupportedOperationException( "upsert() not supported for history tables" );

			}

			@Override
			public MutationOperationGroup getStaticMutationOperationGroup() {
				throw new UnsupportedOperationException( "upsert() not supported for history tables" );

			}
		};
	}

	@Override
	public InsertCoordinator createInsertCoordinator(EntityPersister persister) {
		return new InsertCoordinatorHistory( persister, persister.getFactory() );
	}

	@Override
	public UpdateCoordinator createUpdateCoordinator(EntityPersister persister) {
		return new UpdateCoordinatorHistory(
				persister,
				persister.getFactory(),
				StandardStateManagement.INSTANCE.createUpdateCoordinator( persister )
		);
	}

	@Override
	public DeleteCoordinator createDeleteCoordinator(EntityPersister persister) {
		return new DeleteCoordinatorHistory(
				persister,
				persister.getFactory(),
				StandardStateManagement.INSTANCE.createDeleteCoordinator( persister )
		);
	}

	@Override
	public InsertRowsCoordinator createInsertRowsCoordinator(CollectionPersister persister) {
		final var mutationTarget = resolveMutationTarget( persister );
		if ( !isInsertAllowed( persister ) ) {
			return new InsertRowsCoordinatorNoOp( mutationTarget );
		}
		else if ( persister.isOneToMany() ) {
			throw new UnsupportedOperationException();
		}
		else {
			return new InsertRowsCoordinatorHistory(
					mutationTarget,
					persister.getRowMutationOperations(),
					StandardStateManagement.INSTANCE.createInsertRowsCoordinator( persister ),
					persister.getIndexColumnIsSettable(),
					persister.getElementColumnIsSettable(),
					persister.getIndexIncrementer(),
					persister.getFactory().getServiceRegistry()
			);
		}
	}

	@Override
	public UpdateRowsCoordinator createUpdateRowsCoordinator(CollectionPersister persister) {
		final var mutationTarget = resolveMutationTarget( persister );
		if ( !isUpdatePossible( persister ) ) {
			return new UpdateRowsCoordinatorNoOp( mutationTarget );
		}
		else if ( persister.isOneToMany() ) {
			throw new UnsupportedOperationException();
		}
		else {
			return new UpdateRowsCoordinatorHistory(
					mutationTarget,
					persister.getRowMutationOperations(),
					persister.getFactory(),
					persister.getIndexColumnIsSettable(),
					persister.getElementColumnIsSettable(),
					persister.getIndexIncrementer()
			);
		}
	}

	@Override
	public DeleteRowsCoordinator createDeleteRowsCoordinator(CollectionPersister persister) {
		final var mutationTarget = resolveMutationTarget( persister );
		if ( !persister.needsRemove() ) {
			return new DeleteRowsCoordinatorNoOp( mutationTarget );
		}
		else if ( persister.isOneToMany() ) {
			throw new UnsupportedOperationException();
		}
		else {
			return new DeleteRowsCoordinatorHistory(
					mutationTarget,
					persister.getRowMutationOperations(),
					mutationTarget.hasPhysicalIndexColumn(),
					persister.getIndexColumnIsSettable(),
					persister.getElementColumnIsSettable(),
					persister.getIndexIncrementer(),
					persister.getFactory().getServiceRegistry()
			);
		}
	}

	@Override
	public RemoveCoordinator createRemoveCoordinator(CollectionPersister persister) {
		final var mutationTarget = resolveMutationTarget( persister );
		if ( !persister.needsRemove() ) {
			return new RemoveCoordinatorNoOp( mutationTarget );
		}
		else if ( persister.isOneToMany() ) {
			throw new UnsupportedOperationException();
		}
		else {
			return new RemoveCoordinatorHistory(
					mutationTarget,
					persister.getRowMutationOperations(),
					persister.getIndexColumnIsSettable(),
					persister.getElementColumnIsSettable(),
					persister.getIndexIncrementer(),
					persister.getFactory().getServiceRegistry()
			);
		}
	}
}
