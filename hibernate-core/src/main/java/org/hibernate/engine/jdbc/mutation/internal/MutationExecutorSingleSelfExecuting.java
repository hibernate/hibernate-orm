/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.mutation.internal;

import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.TableInclusionChecker;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.model.SelfExecutingUpdateOperation;
import org.hibernate.sql.model.ValuesAnalysis;
import org.hibernate.sql.model.jdbc.JdbcValueDescriptor;

/**
 * @author Steve Ebersole
 */
public class MutationExecutorSingleSelfExecuting extends AbstractMutationExecutor implements JdbcValueBindingsImpl.JdbcValueDescriptorAccess {
	private final SelfExecutingUpdateOperation operation;
	private final JdbcValueBindingsImpl valueBindings;

	public MutationExecutorSingleSelfExecuting(
			SelfExecutingUpdateOperation operation,
			SharedSessionContractImplementor session) {
		this.operation = operation;

		this.valueBindings = new JdbcValueBindingsImpl(
				operation.getMutationType(),
				operation.getMutationTarget(),
				this,
				session
		);

		prepareForNonBatchedWork( null, session );
	}

	@Override
	public JdbcValueDescriptor resolveValueDescriptor(String tableName, String columnName, ParameterUsage usage) {
		return operation.findValueDescriptor( columnName, usage );
	}

	@Override
	public JdbcValueBindings getJdbcValueBindings() {
		return valueBindings;
	}

	@Override
	public PreparedStatementDetails getPreparedStatementDetails(String tableName) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void performSelfExecutingOperations(ValuesAnalysis valuesAnalysis, TableInclusionChecker inclusionChecker, SharedSessionContractImplementor session) {
		if ( inclusionChecker.include( operation.getTableDetails() ) ) {
			operation.performMutation( valueBindings, valuesAnalysis, session );
		}
	}

	@Override
	public void release() {
		// todo (mutation) :implement
	}
}
