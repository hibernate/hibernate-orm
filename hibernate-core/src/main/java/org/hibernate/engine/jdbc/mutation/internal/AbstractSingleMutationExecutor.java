/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.mutation.internal;

import java.util.Locale;

import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.model.PreparableMutationOperation;
import org.hibernate.sql.model.jdbc.JdbcValueDescriptor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSingleMutationExecutor extends AbstractMutationExecutor implements JdbcValueBindingsImpl.JdbcValueDescriptorAccess {
	private final PreparableMutationOperation mutationOperation;
	private final JdbcValueBindingsImpl valueBindings;

	public AbstractSingleMutationExecutor(
			PreparableMutationOperation mutationOperation,
			SharedSessionContractImplementor session) {
		this.mutationOperation = mutationOperation;
		this.valueBindings = new JdbcValueBindingsImpl(
				mutationOperation.getMutationType(),
				mutationOperation.getMutationTarget(),
				this,
				session
		);
	}

	protected PreparableMutationOperation getMutationOperation() {
		return mutationOperation;
	}

	protected abstract PreparedStatementGroupSingleTable getStatementGroup();

	@Override
	public PreparedStatementDetails getPreparedStatementDetails(String tableName) {
		final PreparedStatementDetails statementDetails = getStatementGroup().getSingleStatementDetails();
		assert statementDetails.getMutatingTableDetails().getTableName().equals( tableName );
		return statementDetails;
	}

	@Override
	public String resolvePhysicalTableName(String tableName) {
		return mutationOperation.getTableDetails().getTableName();
	}

	@Override
	public JdbcValueDescriptor resolveValueDescriptor(String tableName, String columnName, ParameterUsage usage) {
		assert mutationOperation.getTableDetails().containsTableName( tableName )
				: String.format( Locale.ROOT, "table names did not match : `%s` & `%s`", tableName, mutationOperation.getTableDetails().getTableName()  );
		return mutationOperation.findValueDescriptor( columnName, usage );
	}

	@Override
	public JdbcValueBindings getJdbcValueBindings() {
		return valueBindings;
	}
}
