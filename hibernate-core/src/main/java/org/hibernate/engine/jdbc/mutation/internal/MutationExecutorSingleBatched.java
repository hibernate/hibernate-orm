/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.mutation.internal;

import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.mutation.TableInclusionChecker;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.model.PreparableMutationOperation;
import org.hibernate.sql.model.ValuesAnalysis;

/**
 * @author Steve Ebersole
 */
public class MutationExecutorSingleBatched extends AbstractSingleMutationExecutor {
	private final int batchSize;
	private final SharedSessionContractImplementor session;

	private final BatchKey batchKey;

	public MutationExecutorSingleBatched(
			PreparableMutationOperation mutationOperation,
			BatchKey batchKey,
			int batchSize,
			SharedSessionContractImplementor session) {
		super( mutationOperation, session );

		this.batchSize = batchSize;
		this.session = session;

		this.batchKey = batchKey;
	}

	@Override
	protected PreparedStatementGroupSingleTable getStatementGroup() {
		return (PreparedStatementGroupSingleTable) resolveBatch().getStatementGroup();
	}

	private Batch batch;

	private Batch resolveBatch() {
		if ( batch == null ) {
			batch = session.getJdbcCoordinator().getBatch(
					batchKey,
					batchSize,
					() -> new PreparedStatementGroupSingleTable( getMutationOperation(), session )
			);
			assert batch != null;
		}

		return batch;
	}

	@Override
	protected void performBatchedOperations(
			ValuesAnalysis valuesAnalysis,
			TableInclusionChecker inclusionChecker,
			Batch.StaleStateMapper staleStateMapper) {
		resolveBatch().addToBatch( getJdbcValueBindings(), inclusionChecker, staleStateMapper );
	}

	@Override
	public void release() {
		// nothing to do
	}
}
