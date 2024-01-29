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
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.generator.values.GeneratedValuesMutationDelegate;
import org.hibernate.sql.model.PreparableMutationOperation;
import org.hibernate.sql.model.ValuesAnalysis;

/**
 * @author Steve Ebersole
 */
public class MutationExecutorSingleNonBatched extends AbstractSingleMutationExecutor {
	private final PreparedStatementGroupSingleTable statementGroup;
	private final GeneratedValuesMutationDelegate generatedValuesDelegate;

	public MutationExecutorSingleNonBatched(
			PreparableMutationOperation mutationOperation,
			GeneratedValuesMutationDelegate generatedValuesDelegate,
			SharedSessionContractImplementor session) {
		super( mutationOperation, session );
		this.generatedValuesDelegate = generatedValuesDelegate;
		this.statementGroup = new PreparedStatementGroupSingleTable( mutationOperation, generatedValuesDelegate, session );
		prepareForNonBatchedWork( null, session );
	}

	@Override
	protected PreparedStatementGroupSingleTable getStatementGroup() {
		return statementGroup;
	}

	@Override
	protected GeneratedValues performNonBatchedOperations(
			Object modelReference,
			ValuesAnalysis valuesAnalysis,
			TableInclusionChecker inclusionChecker,
			OperationResultChecker resultChecker,
			SharedSessionContractImplementor session) {
		if ( generatedValuesDelegate != null ) {
			return generatedValuesDelegate.performMutation(
					statementGroup.getSingleStatementDetails(),
					getJdbcValueBindings(),
					modelReference,
					session
			);
		}
		else {
			performNonBatchedMutation(
					statementGroup.getSingleStatementDetails(),
					null,
					getJdbcValueBindings(),
					inclusionChecker,
					resultChecker,
					session
			);
			return null;
		}
	}

	@Override
	public void release() {
		// nothing to do - `#performNonBatchedMutation` already releases the statement
		assert statementGroup.getSingleStatementDetails().getStatement() == null;
	}
}
