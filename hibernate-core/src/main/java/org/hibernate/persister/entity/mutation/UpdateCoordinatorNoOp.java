/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.sql.model.internal.MutationOperationGroupFactory;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.MutationType;

/**
 * @author Steve Ebersole
 */
public class UpdateCoordinatorNoOp implements UpdateCoordinator {
	private final MutationOperationGroup operationGroup;

	public UpdateCoordinatorNoOp(AbstractEntityPersister entityPersister) {
		operationGroup = MutationOperationGroupFactory.noOperations( MutationType.UPDATE, entityPersister );
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
