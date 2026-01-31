/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.state.internal.AuditStateManagement;
import org.hibernate.sql.model.MutationOperationGroup;

/**
 * Insert coordinator for audited entities.
 */
public class InsertCoordinatorAudit extends AbstractAuditCoordinator implements InsertCoordinator {
	private final InsertCoordinator currentInsertCoordinator;

	public InsertCoordinatorAudit(
			EntityPersister entityPersister,
			SessionFactoryImplementor factory,
			InsertCoordinator currentInsertCoordinator) {
		super( entityPersister, factory );
		this.currentInsertCoordinator = currentInsertCoordinator;
	}

	@Override
	public MutationOperationGroup getStaticMutationOperationGroup() {
		return currentInsertCoordinator.getStaticMutationOperationGroup();
	}

	@Override
	public GeneratedValues insert(
			Object entity,
			Object[] values,
			SharedSessionContractImplementor session) {
		final var generatedValues = currentInsertCoordinator.insert( entity, values, session );
		insertAuditRow( entity, null, values, AuditStateManagement.ModificationType.ADD, session );
		return generatedValues;
	}

	@Override
	public GeneratedValues insert(
			Object entity,
			Object id,
			Object[] values,
			SharedSessionContractImplementor session) {
		final var generatedValues = currentInsertCoordinator.insert( entity, id, values, session );
		insertAuditRow( entity, id, values, AuditStateManagement.ModificationType.ADD, session );
		return generatedValues;
	}
}
