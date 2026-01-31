/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.state.internal;

import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.mutation.DeleteRowsCoordinator;
import org.hibernate.persister.collection.mutation.DeleteRowsCoordinatorNoOp;
import org.hibernate.persister.collection.mutation.DeleteRowsCoordinatorAudit;
import org.hibernate.persister.collection.mutation.InsertRowsCoordinator;
import org.hibernate.persister.collection.mutation.InsertRowsCoordinatorAudit;
import org.hibernate.persister.collection.mutation.InsertRowsCoordinatorNoOp;
import org.hibernate.persister.collection.mutation.RemoveCoordinator;
import org.hibernate.persister.collection.mutation.RemoveCoordinatorNoOp;
import org.hibernate.persister.collection.mutation.UpdateRowsCoordinator;
import org.hibernate.persister.collection.mutation.UpdateRowsCoordinatorAudit;
import org.hibernate.persister.collection.mutation.UpdateRowsCoordinatorNoOp;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.DeleteCoordinator;
import org.hibernate.persister.entity.mutation.DeleteCoordinatorAudit;
import org.hibernate.persister.entity.mutation.InsertCoordinator;
import org.hibernate.persister.entity.mutation.InsertCoordinatorAudit;
import org.hibernate.persister.entity.mutation.MergeCoordinatorAudit;
import org.hibernate.persister.entity.mutation.UpdateCoordinator;
import org.hibernate.persister.entity.mutation.UpdateCoordinatorAudit;
import org.hibernate.persister.state.StateManagement;

/**
 * State management for {@linkplain org.hibernate.annotations.Audited audited}
 * entities and collections.
 *
 * @author Gavin King
 */
public class AuditStateManagement implements StateManagement {
	public static final AuditStateManagement INSTANCE = new AuditStateManagement();

	private AuditStateManagement() {
	}

	/**
	 * The modification type stored in the
	 * {@linkplain org.hibernate.annotations.Audited#modificationType
	 * modification type column}.
	 */
	public enum ModificationType {
		/** Creation, encoded as 0 */
		ADD,
		/** Modification, encoded as 1 */
		MOD,
		/** Deletion, encoded as 2 */
		DEL
	}

	@Override
	public InsertCoordinator createInsertCoordinator(EntityPersister persister) {
		return new InsertCoordinatorAudit( persister, persister.getFactory(),
				StandardStateManagement.INSTANCE.createInsertCoordinator( persister ) );
	}

	@Override
	public UpdateCoordinator createUpdateCoordinator(EntityPersister persister) {
		return new UpdateCoordinatorAudit( persister, persister.getFactory(),
				StandardStateManagement.INSTANCE.createUpdateCoordinator( persister ) );
	}

	@Override
	public UpdateCoordinator createMergeCoordinator(EntityPersister persister) {
		return new MergeCoordinatorAudit( persister, persister.getFactory(),
				StandardStateManagement.INSTANCE.createMergeCoordinator( persister ) );
	}

	@Override
	public DeleteCoordinator createDeleteCoordinator(EntityPersister persister) {
		return new DeleteCoordinatorAudit( persister, persister.getFactory(),
				StandardStateManagement.INSTANCE.createDeleteCoordinator( persister ) );
	}

	@Override
	public InsertRowsCoordinator createInsertRowsCoordinator(CollectionPersister persister) {
		final var mutationTarget = AbstractStateManagement.resolveMutationTarget( persister );
		if ( !AbstractStateManagement.isInsertAllowed( persister ) ) {
			return new InsertRowsCoordinatorNoOp( mutationTarget );
		}
		else if ( persister.isOneToMany() ) {
			throw new UnsupportedOperationException();
		}
		else {
			return new InsertRowsCoordinatorAudit(
					mutationTarget,
					persister.getRowMutationOperations(),
					StandardStateManagement.INSTANCE.createInsertRowsCoordinator( persister ),
					persister.getIndexColumnIsSettable(),
					persister.getElementColumnIsSettable(),
					persister.getIndexIncrementer(),
					persister.getFactory()
			);
		}
	}

	@Override
	public UpdateRowsCoordinator createUpdateRowsCoordinator(CollectionPersister persister) {
		final var mutationTarget = AbstractStateManagement.resolveMutationTarget( persister );
		if ( !AbstractStateManagement.isUpdatePossible( persister ) ) {
			return new UpdateRowsCoordinatorNoOp( mutationTarget );
		}
		else if ( persister.isOneToMany() ) {
			throw new UnsupportedOperationException();
		}
		else {
			return new UpdateRowsCoordinatorAudit(
					mutationTarget,
					StandardStateManagement.INSTANCE.createUpdateRowsCoordinator( persister ),
					persister.getIndexColumnIsSettable(),
					persister.getElementColumnIsSettable(),
					persister.getIndexIncrementer(),
					persister.getFactory()
			);
		}
	}

	@Override
	public DeleteRowsCoordinator createDeleteRowsCoordinator(CollectionPersister persister) {
		final var mutationTarget = AbstractStateManagement.resolveMutationTarget( persister );
		if ( !persister.needsRemove() ) {
			return new DeleteRowsCoordinatorNoOp( mutationTarget );
		}
		else if ( persister.isOneToMany() ) {
			throw new UnsupportedOperationException();
		}
		else {
			return new DeleteRowsCoordinatorAudit(
					mutationTarget,
					StandardStateManagement.INSTANCE.createDeleteRowsCoordinator( persister ),
					mutationTarget.hasPhysicalIndexColumn(),
					persister.getIndexColumnIsSettable(),
					persister.getElementColumnIsSettable(),
					persister.getIndexIncrementer(),
					persister.getFactory()
			);
		}
	}

	@Override
	public RemoveCoordinator createRemoveCoordinator(CollectionPersister persister) {
		final var mutationTarget = AbstractStateManagement.resolveMutationTarget( persister );
		if ( !persister.needsRemove() ) {
			return new RemoveCoordinatorNoOp( mutationTarget );
		}
		else if ( persister.isOneToMany() ) {
			throw new UnsupportedOperationException();
		}
		else {
			return StandardStateManagement.INSTANCE.createRemoveCoordinator( persister );
		}
	}
}
