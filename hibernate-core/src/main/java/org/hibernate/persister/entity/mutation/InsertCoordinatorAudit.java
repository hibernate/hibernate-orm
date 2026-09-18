/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.Internal;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.audit.ModificationType;
import org.hibernate.sql.model.MutationOperationGroup;

/**
 * Insert coordinator for audited entities.
 */
@Internal
public class InsertCoordinatorAudit extends AbstractAuditCoordinator implements InsertCoordinator {
	private final InsertCoordinator currentInsertCoordinator;

	public InsertCoordinatorAudit(
			@Nonnull EntityPersister entityPersister,
			@Nonnull SessionFactoryImplementor factory,
			@Nonnull InsertCoordinator currentInsertCoordinator) {
		super( entityPersister, factory );
		this.currentInsertCoordinator = currentInsertCoordinator;
	}

	@Nullable
	@Override
	public MutationOperationGroup getStaticMutationOperationGroup() {
		return currentInsertCoordinator.getStaticMutationOperationGroup();
	}

	@Nullable
	@Override
	public GeneratedValues insert(
			@Nonnull Object entity,
			@Nonnull Object[] values,
			@Nonnull SharedSessionContractImplementor session) {
		final var generatedValues = currentInsertCoordinator.insert( entity, values, session );
		final var entityEntry = session.getPersistenceContextInternal().getEntry( entity );
		final var entityKey = entityEntry != null
				? entityEntry.getEntityKey()
				: new EntityKey( resolveInsertedIdentifier( entity, null, generatedValues, session ), entityPersister() );
		enqueueAuditEntry( entityKey, entity, values, ModificationType.ADD, session );
		return generatedValues;
	}

	@Nullable
	@Override
	public GeneratedValues insert(
			@Nonnull Object entity,
			@Nullable Object id,
			@Nonnull Object[] values,
			@Nonnull SharedSessionContractImplementor session) {
		final var generatedValues = currentInsertCoordinator.insert( entity, id, values, session );
		enqueueAuditEntry( resolveEntityKey( entity, resolveInsertedIdentifier( entity, id, generatedValues, session ), session ), entity, values, ModificationType.ADD, session );
		return generatedValues;
	}
}
