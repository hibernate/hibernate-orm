/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.audit.ModificationType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
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
		final var entityEntry = session.getPersistenceContextInternal().getEntry( entity );
		final var state = entityEntry.getLoadedState() != null
				? entityEntry.getLoadedState()
				: entityPersister().getValues( entity );

		currentDeleteCoordinator.delete( entity, id, version, session );

		session.getAuditWorkQueue().enqueue(
				entityEntry.getEntityKey(),
				entity,
				state,
				ModificationType.DEL,
				this,
				session
		);
	}
}
