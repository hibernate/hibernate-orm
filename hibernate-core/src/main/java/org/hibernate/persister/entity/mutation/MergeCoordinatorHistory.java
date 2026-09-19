/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import jakarta.annotation.Nonnull;

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
@org.hibernate.Internal
public class MergeCoordinatorHistory extends UpdateCoordinatorHistory {
	public MergeCoordinatorHistory(
			@Nonnull EntityPersister entityPersister,
			@Nonnull SessionFactoryImplementor factory,
			@Nonnull UpdateCoordinator currentMergeCoordinator) {
		super( entityPersister, factory, currentMergeCoordinator );
	}

	@Override
	boolean resultCheck(@Nonnull Object id, @Nonnull PreparedStatementDetails statementDetails, int affectedRowCount, int batchPosition) {
		return affectedRowCount != 0
			&& super.resultCheck( id, statementDetails, affectedRowCount, batchPosition );
	}
}
