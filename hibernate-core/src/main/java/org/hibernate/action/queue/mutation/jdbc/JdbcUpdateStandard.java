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

/// Standard JDBC UPDATE mutation using {@link EntityTableDescriptor}.
///
/// Parallel to {@link org.hibernate.sql.model.jdbc.JdbcUpdateMutation}
/// but generates SQL directly from TableDescriptor and ColumnValueBindings.
///
/// @author Steve Ebersole
@Incubating
public class JdbcUpdateStandard
		extends AbstractJdbcOperation
		implements JdbcUpdate, PreparableJdbcOperation {

	private final String sql;

	public JdbcUpdateStandard(
			TableDescriptor tableDescriptor,
			GraphMutationTarget<?> mutationTarget,
			Expectation expectation,
			List<ColumnValueBinding> valueBindings,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings,
			List<ColumnValueParameter> parameters) {
		super(tableDescriptor, mutationTarget, expectation, parameters);
		this.sql = generateSql(
				tableDescriptor,
				valueBindings,
				keyRestrictionBindings,
				optLockRestrictionBindings
		);
		registerValueDescriptors(parameters);
	}

	private static String generateSql(
			TableDescriptor tableDescriptor,
			List<ColumnValueBinding> valueBindings,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings) {
		final var sql = new StringBuilder("update ");
		sql.append(tableDescriptor.name());
		sql.append(" set ");

		// SET clause
		boolean first = true;
		for (var binding : valueBindings) {
			if (!first) {
				sql.append(", ");
			}
			first = false;
			sql.append(binding.getColumnReference().getColumnExpression());
			sql.append("=");

			// Use custom write expression if available, otherwise just ?
			final var writeExpression = binding.getValueExpression();
			if (writeExpression != null && writeExpression.getFragment() != null) {
				sql.append(writeExpression.getFragment());
			}
			else {
				sql.append("?");
			}
		}

		// WHERE clause - key restrictions
		sql.append(" where ");
		first = true;
		for (var binding : keyRestrictionBindings) {
			if (!first) {
				sql.append(" and ");
			}
			first = false;
			sql.append(binding.getColumnReference().getColumnExpression());
			sql.append("=?");
		}

		// WHERE clause - optimistic lock restrictions
		if (optLockRestrictionBindings != null && !optLockRestrictionBindings.isEmpty()) {
			for (var binding : optLockRestrictionBindings) {
				sql.append(" and ");
				sql.append(binding.getColumnReference().getColumnExpression());
				sql.append("=?");
			}
		}

		return sql.toString();
	}

	@Override
	public MutationType getMutationType() {
		return MutationType.UPDATE;
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
		return "GraphJdbcUpdateMutation(" + getTableDescriptor().name() + ")";
	}
}
