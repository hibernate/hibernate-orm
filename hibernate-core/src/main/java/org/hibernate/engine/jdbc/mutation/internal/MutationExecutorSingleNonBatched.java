/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.engine.jdbc.mutation.internal;

import org.hibernate.engine.jdbc.mutation.OperationResultChecker;
import org.hibernate.engine.jdbc.mutation.TableInclusionChecker;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.model.PreparableMutationOperation;
import org.hibernate.sql.model.ValuesAnalysis;

/**
 * @author Steve Ebersole
 */
public class MutationExecutorSingleNonBatched extends AbstractSingleMutationExecutor {
	private final PreparedStatementGroupSingleTable statementGroup;

	public MutationExecutorSingleNonBatched(
			PreparableMutationOperation mutationOperation,
			SharedSessionContractImplementor session) {
		super( mutationOperation, session );
		this.statementGroup = new PreparedStatementGroupSingleTable( mutationOperation, session );
		prepareForNonBatchedWork( null, session );
	}

	@Override
	protected PreparedStatementGroupSingleTable getStatementGroup() {
		return statementGroup;
	}

	@Override
	protected void performNonBatchedOperations(
			ValuesAnalysis valuesAnalysis,
			TableInclusionChecker inclusionChecker,
			OperationResultChecker resultChecker,
			SharedSessionContractImplementor session) {
		performNonBatchedMutation(
				statementGroup.getSingleStatementDetails(),
				getJdbcValueBindings(),
				inclusionChecker,
				resultChecker,
				session
		);
	}

	@Override
	public void release() {
		// nothing to do - `#performNonBatchedMutation` already releases the statement
	}
}
