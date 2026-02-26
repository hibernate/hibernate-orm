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
 * Update coordinator for audited entities.
 */
public class UpdateCoordinatorAudit extends AbstractAuditCoordinator implements UpdateCoordinator {
	private final UpdateCoordinator currentUpdateCoordinator;

	public UpdateCoordinatorAudit(
			EntityPersister entityPersister,
			SessionFactoryImplementor factory,
			UpdateCoordinator currentUpdateCoordinator) {
		super( entityPersister, factory );
		this.currentUpdateCoordinator = currentUpdateCoordinator;
	}

	@Override
	public MutationOperationGroup getStaticMutationOperationGroup() {
		return currentUpdateCoordinator.getStaticMutationOperationGroup();
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
		if ( shouldAuditUpdate( dirtyAttributeIndexes, hasDirtyCollection ) ) {
			insertAuditRow( entity, id, values, AuditStateManagement.ModificationType.MOD, session );
		}
		return generatedValues;
	}

	@Override
	public void forceVersionIncrement(
			Object id,
			Object currentVersion,
			Object nextVersion,
			SharedSessionContractImplementor session) {
		currentUpdateCoordinator.forceVersionIncrement( id, currentVersion, nextVersion, session );
	}

	private boolean shouldAuditUpdate(int[] dirtyAttributeIndexes, boolean hasDirtyCollection) {
		if ( dirtyAttributeIndexes == null || dirtyAttributeIndexes.length == 0 ) {
			return true;
		}
		else if ( hasDirtyCollection ) {
			return true;
		}
		else {
			for ( int dirtyIndex : dirtyAttributeIndexes ) {
				if ( auditedPropertyMask[dirtyIndex] ) {
					return true;
				}
			}
			return false;
		}
	}
}
