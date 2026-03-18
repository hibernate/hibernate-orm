/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.mutation.jdbc;

import org.hibernate.Incubating;
import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.action.queue.meta.TableDescriptor;
import org.hibernate.action.queue.mutation.GraphMutationTarget;
import org.hibernate.action.queue.op.JdbcValueDescriptorImpl;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.jdbc.Expectation;
import org.hibernate.sql.model.ast.ColumnValueParameter;
import org.hibernate.sql.model.jdbc.JdbcValueDescriptor;

import java.util.ArrayList;
import java.util.List;

/// Base support for graph-based JDBC mutations.
///
/// Parallel to {@link org.hibernate.sql.model.jdbc.AbstractJdbcMutation}
/// but works with {@link EntityTableDescriptor} instead of
/// {@link org.hibernate.sql.model.TableMapping}.
///
/// @author Steve Ebersole
@Incubating
public abstract class AbstractJdbcOperation implements JdbcOperation {
	private final TableDescriptor tableDescriptor;
	private final GraphMutationTarget<?> mutationTarget;
	private final Expectation expectation;
	private final List<JdbcValueDescriptor> jdbcValueDescriptors;
	protected final List<ColumnValueParameter> parameters;

	protected AbstractJdbcOperation(
			TableDescriptor tableDescriptor,
			GraphMutationTarget<?> mutationTarget,
			Expectation expectation,
			List<ColumnValueParameter> parameters) {
		this.tableDescriptor = tableDescriptor;
		this.mutationTarget = mutationTarget;
		this.expectation = expectation;
		this.jdbcValueDescriptors = new ArrayList<>();
		this.parameters = parameters;
	}

	protected void registerValueDescriptors(List<ColumnValueParameter> parameters) {
		int position = expectation.getNumberOfParametersUsed() + 1;
		for (var parameter : parameters) {
			final String columnName = parameter.getColumnReference().getColumnExpression();
			final var descriptor = new JdbcValueDescriptorImpl(
					columnName,
					parameter.getColumnReference().getJdbcMapping(),
					parameter.getUsage(),
					position++
			);
			jdbcValueDescriptors.add(descriptor);
		}
	}

	@Override
	public TableDescriptor getTableDescriptor() {
		return tableDescriptor;
	}

	@Override
	public GraphMutationTarget<?> getMutationTarget() {
		return mutationTarget;
	}

	@Override
	public Expectation getExpectation() {
		return expectation;
	}

	public List<ColumnValueParameter> getParameters() {
		return parameters;
	}

	@Override
	public JdbcValueDescriptor findValueDescriptor(String columnName, ParameterUsage usage) {
		for (var descriptor : jdbcValueDescriptors) {
			if (descriptor.getColumnName().equals(columnName)
					&& descriptor.getUsage() == usage) {
				return descriptor;
			}
		}
		return null;
	}
}
