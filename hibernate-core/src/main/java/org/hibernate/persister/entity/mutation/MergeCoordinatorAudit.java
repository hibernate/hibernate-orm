/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

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
public class MergeCoordinatorAudit extends UpdateCoordinatorAudit {
	public MergeCoordinatorAudit(
			EntityPersister entityPersister,
			SessionFactoryImplementor factory,
			UpdateCoordinator currentUpdateCoordinator) {
		super( entityPersister, factory, currentUpdateCoordinator );
	}

	@Override
	public GeneratedValues update(
			Object entity,
			Object id,
			Object rowId,
			Object[] values,
			Object oldVersion,
			Object[] incomingOldValues,
			int[] dirtyAttributeIndexes,
			boolean hasDirtyCollection,
			SharedSessionContractImplementor session) {
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
