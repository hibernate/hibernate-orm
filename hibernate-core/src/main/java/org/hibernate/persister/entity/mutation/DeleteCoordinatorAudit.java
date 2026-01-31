/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.state.internal.AuditStateManagement;
import org.hibernate.sql.model.MutationOperationGroup;

/**
 * Delete coordinator for audited entities.
 */
public class DeleteCoordinatorAudit extends AbstractAuditCoordinator implements DeleteCoordinator {
	private final DeleteCoordinator currentDeleteCoordinator;

	public DeleteCoordinatorAudit(
			EntityPersister entityPersister,
			SessionFactoryImplementor factory,
			DeleteCoordinator currentDeleteCoordinator) {
		super( entityPersister, factory );
		this.currentDeleteCoordinator = currentDeleteCoordinator;
	}

	@Override
	public MutationOperationGroup getStaticMutationOperationGroup() {
		return currentDeleteCoordinator.getStaticMutationOperationGroup();
	}

	@Override
	public void delete(
			Object entity,
			Object id,
			Object version,
			SharedSessionContractImplementor session) {
		currentDeleteCoordinator.delete( entity, id, version, session );
		final var state = resolveDeleteState( entity, session );
		if ( state != null ) {
			insertAuditRow( entity, id, state, AuditStateManagement.ModificationType.DEL, session );
		}
	}

	private Object[] resolveDeleteState(Object entity, SharedSessionContractImplementor session) {
		if ( entity == null ) {
			return null;
		}
		else {
			final var persistenceContext = session.getPersistenceContextInternal();
			final var entry = persistenceContext.getEntry( entity );
			return entry != null
				&& entry.getLoadedState() != null
					? entry.getLoadedState()
					: entityPersister().getValues( entity );
		}
	}
}
