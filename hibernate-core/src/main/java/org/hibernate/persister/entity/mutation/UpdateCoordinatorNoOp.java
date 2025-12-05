/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.MutationType;

import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.noOperations;

/**
 * @author Steve Ebersole
 */
public class UpdateCoordinatorNoOp implements UpdateCoordinator {
	private final MutationOperationGroup operationGroup;

	public UpdateCoordinatorNoOp(EntityPersister entityPersister) {
		operationGroup = noOperations( MutationType.UPDATE, entityPersister );
	}

	@Override
	public MutationOperationGroup getStaticMutationOperationGroup() {
		return operationGroup;
	}

	@Override
	public GeneratedValues update(Object entity, Object id, Object rowId, Object[] values, Object oldVersion, Object[] incomingOldValues, int[] dirtyAttributeIndexes, boolean hasDirtyCollection, SharedSessionContractImplementor session) {
		// nothing to do
		return null;
	}

	@Override
	public void forceVersionIncrement(Object id, Object currentVersion, Object nextVersion, SharedSessionContractImplementor session) {
		// nothing to do
	}
}
