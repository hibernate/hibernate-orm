/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.mutation.jdbc;

import org.hibernate.Incubating;
import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.action.queue.meta.TableDescriptor;
import org.hibernate.action.queue.mutation.GraphMutationTarget;
import org.hibernate.jdbc.Expectation;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.ColumnValueParameter;

import java.util.List;

/// Standard JDBC INSERT mutation using {@link EntityTableDescriptor}.
///
/// Parallel to {@link org.hibernate.sql.model.jdbc.JdbcInsertMutation}
/// but generates SQL directly from TableDescriptor and ColumnValueBindings.
///
/// @author Steve Ebersole
@Incubating
public class JdbcInsertStandard
		extends AbstractJdbcOperation
		implements JdbcInsert {

	private final String sql;

	public JdbcInsertStandard(
			TableDescriptor tableDescriptor,
			GraphMutationTarget<?> mutationTarget,
			Expectation expectation,
			List<ColumnValueBinding> valueBindings,
			List<ColumnValueParameter> parameters) {
		super(tableDescriptor, mutationTarget, expectation, parameters);
		this.sql = generateSql(tableDescriptor, valueBindings);
		registerValueDescriptors(parameters);
	}

	private static String generateSql(
			TableDescriptor tableDescriptor,
			List<ColumnValueBinding> valueBindings) {
		final var sql = new StringBuilder("insert into ");
		sql.append(tableDescriptor.name());
		sql.append(" (");

		// Column names
		boolean first = true;
		for (var binding : valueBindings) {
			if (!first) {
				sql.append(", ");
			}
			first = false;
			sql.append(binding.getColumnReference().getColumnExpression());
		}

		sql.append(") values (");

		// Value placeholders - use write expressions if available
		first = true;
		for (var binding : valueBindings) {
			if (!first) {
				sql.append(", ");
			}
			first = false;

			// Use custom write expression if available, otherwise just ?
			final var writeExpression = binding.getValueExpression();
			if (writeExpression != null && writeExpression.getFragment() != null) {
				sql.append(writeExpression.getFragment());
			}
			else {
				sql.append("?");
			}
		}

		sql.append(")");
		return sql.toString();
	}

	@Override
	public MutationType getMutationType() {
		return MutationType.INSERT;
	}

	@Override
	public String getSqlString() {
		return sql;
	}

	@Override
	public boolean isCallable() {
		return false;
	}

	@Override
	public String toString() {
		return "GraphJdbcInsertMutation(" + getTableDescriptor().name() + ")";
	}
}
