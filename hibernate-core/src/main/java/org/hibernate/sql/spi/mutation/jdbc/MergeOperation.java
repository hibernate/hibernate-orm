/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.spi.mutation.jdbc;

import java.util.List;

import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.jdbc.Expectation;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.spi.mutation.MutationTarget;
import org.hibernate.sql.spi.mutation.MutationType;
import org.hibernate.sql.spi.mutation.TableMapping;

/**
 * {@link JdbcMutationOperation} implementation for MERGE handling
 *
 * @author Steve Ebersole
 */
public final class MergeOperation extends AbstractJdbcMutation {
	public MergeOperation(
			TableMapping tableDetails,
			MutationTarget mutationTarget,
			String sql,
			Expectation expectation,
			List<? extends JdbcParameterBinder> parameterBinders) {
		super( tableDetails, mutationTarget, sql, false, expectation, parameterBinders );
	}

	@Override
	public final MutationType getMutationType() {
		return MutationType.UPDATE;
	}

	@Override
	public boolean canBeBatched(BatchKey batchKey, int batchSize) {
		// When the mutating table is optional, we generated a delete part for the merge statement
		// which makes the statement non-idempotent and hence not retryable, so also not batchable
		return !getTableDetails().isOptional() && super.canBeBatched( batchKey, batchSize );
	}
}
