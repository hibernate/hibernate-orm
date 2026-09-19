/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.audit.ModificationType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Merge coordinator for audited entities.
 * <p>
 * When the update affects audited properties, the entity table
 * is checked via {@link EntityPersister#getDatabaseSnapshot}
 * before the merge executes: if the entity does not yet exist
 * the upsert is an insert ({@code ADD}), otherwise it is an
 * update ({@code MOD}).
 */
@org.hibernate.Internal
public class MergeCoordinatorAudit extends UpdateCoordinatorAudit {
	public MergeCoordinatorAudit(
			@Nonnull EntityPersister entityPersister,
			@Nonnull SessionFactoryImplementor factory,
			@Nonnull UpdateCoordinator currentUpdateCoordinator) {
		super( entityPersister, factory, currentUpdateCoordinator );
	}

	@Nullable
	@Override
	public GeneratedValues update(
			@Nonnull Object entity,
			@Nonnull Object id,
			@Nullable Object rowId,
			@Nonnull Object[] values,
			@Nullable Object oldVersion,
			@Nullable Object[] incomingOldValues,
			@Nullable int[] dirtyAttributeIndexes,
			boolean hasDirtyCollection,
			@Nonnull SharedSessionContractImplementor session) {
		final boolean shouldAudit = shouldAuditUpdate( dirtyAttributeIndexes, hasDirtyCollection );
		final boolean entityExists = shouldAudit && entityPersister().getDatabaseSnapshot( id, session ) != null;
		final var generatedValues = currentUpdateCoordinator.update(
				entity,
				id,
				rowId,
				values,
				oldVersion,
				incomingOldValues,
				dirtyAttributeIndexes,
				hasDirtyCollection,
				session
		);
		if ( shouldAudit ) {
			enqueueAuditEntry(
					resolveEntityKey( entity, id, session ),
					entity,
					values,
					entityExists ? ModificationType.MOD : ModificationType.ADD,
					session
			);
		}
		return generatedValues;
	}
}
