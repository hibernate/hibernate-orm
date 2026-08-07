/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.mutation.internal;

import java.util.function.Consumer;

import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.mutation.TableInclusionChecker;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.values.GeneratedValuesMutationDelegate;
import org.hibernate.sql.model.PreparableMutationOperation;
import org.hibernate.sql.model.ValuesAnalysis;

/**
 * @author Steve Ebersole
 */
public class MutationExecutorSingleBatched extends AbstractSingleMutationExecutor {
	private final int batchSize;
	private final SharedSessionContractImplementor session;

	private final BatchKey batchKey;
	private final GeneratedValuesMutationDelegate generatedValuesDelegate;

	private Consumer<Object> generatedKeyConsumer;

	public MutationExecutorSingleBatched(
			PreparableMutationOperation mutationOperation,
			BatchKey batchKey,
			int batchSize,
			SharedSessionContractImplementor session) {
		this( mutationOperation, null, batchKey, batchSize, session );
	}

	public MutationExecutorSingleBatched(
			PreparableMutationOperation mutationOperation,
			GeneratedValuesMutationDelegate generatedValuesDelegate,
			BatchKey batchKey,
			int batchSize,
			SharedSessionContractImplementor session) {
		super( mutationOperation, session );

		this.batchSize = batchSize;
		this.session = session;

		this.batchKey = batchKey;
		this.generatedValuesDelegate = generatedValuesDelegate;

		// If the JDBC coordinator already holds a batch keyed differently from
		// ours, flush it now. This ensures any deferred work (notably batched
		// identity inserts) completes — and assigns its generated ids — before
		// we stage binding values that may depend on those ids.
		session.getJdbcCoordinator().conditionallyExecuteBatch( batchKey );
	}

	/**
	 * Register a consumer to receive the generated identifier once the
	 * batch is flushed. Must be called before
	 * {@link #execute} on this executor.
	 */
	public void setGeneratedKeyConsumer(Consumer<Object> generatedKeyConsumer) {
		this.generatedKeyConsumer = generatedKeyConsumer;
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
					() -> new PreparedStatementGroupSingleTable( getMutationOperation(), generatedValuesDelegate, session )
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
		if ( generatedKeyConsumer != null ) {
			resolveBatch().addToBatch( getJdbcValueBindings(), inclusionChecker, generatedKeyConsumer );
		}
		else {
			resolveBatch().addToBatch( getJdbcValueBindings(), inclusionChecker, staleStateMapper );
		}
	}

	@Override
	public void release() {
		// nothing to do
	}
}
