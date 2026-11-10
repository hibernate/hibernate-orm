package org.hibernate.engine.jdbc.mutation.internal;

import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.batch.spi.GroupedBatch;
import org.hibernate.engine.jdbc.batch.spi.StaleStateMapper;
import org.hibernate.engine.jdbc.mutation.TableInclusionChecker;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.spi.mutation.jdbc.PreparableMutationOperation;
import org.hibernate.sql.spi.mutation.ValuesAnalysis;

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

	private GroupedBatch resolveBatch() {
		// The coordinator owns the batch lifetime. Another executor may have switched
		// keys and executed/released our previous batch since this executor last ran.
		return session.getJdbcCoordinator().getGroupedBatch(
				batchKey,
				batchSize,
				getMutationOperation()
		);
	}

	@Override
	protected void performBatchedOperations(
			ValuesAnalysis valuesAnalysis,
			TableInclusionChecker inclusionChecker,
			StaleStateMapper staleStateMapper) {
		resolveBatch().addToBatch( getJdbcValueBindings(), inclusionChecker, staleStateMapper );
	}

	@Override
	public void release() {
		// nothing to do
	}
}
