/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Merge coordinator for
 * {@link org.hibernate.temporal.TemporalTableStrategy#HISTORY_TABLE}
 * temporal strategy.
 *
 * @author Gavin King
 */
public class MergeCoordinatorHistory extends UpdateCoordinatorHistory {
	public MergeCoordinatorHistory(
			EntityPersister entityPersister,
			SessionFactoryImplementor factory,
			UpdateCoordinator currentMergeCoordinator) {
		super( entityPersister, factory, currentMergeCoordinator );
	}

	@Override
	boolean resultCheck(Object id, PreparedStatementDetails statementDetails, int affectedRowCount, int batchPosition) {
		return affectedRowCount != 0
			&& super.resultCheck( id, statementDetails, affectedRowCount, batchPosition );
	}
}
