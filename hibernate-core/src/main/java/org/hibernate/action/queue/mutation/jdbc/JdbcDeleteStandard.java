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

/// Standard JDBC DELETE mutation using {@link EntityTableDescriptor}.
///
/// Parallel to {@link org.hibernate.sql.model.jdbc.JdbcDeleteMutation}
/// but generates SQL directly from TableDescriptor and ColumnValueBindings.
///
/// @author Steve Ebersole
@Incubating
public class JdbcDeleteStandard
		extends AbstractJdbcOperation
		implements JdbcDelete {

	private final String sql;

	public JdbcDeleteStandard(
			TableDescriptor tableDescriptor,
			GraphMutationTarget<?> mutationTarget,
			Expectation expectation,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings,
			List<ColumnValueParameter> parameters) {
		super(tableDescriptor, mutationTarget, expectation, parameters);
		this.sql = generateSql(
				tableDescriptor,
				keyRestrictionBindings,
				optLockRestrictionBindings
		);
		registerValueDescriptors(parameters);
	}

	private static String generateSql(
			TableDescriptor tableDescriptor,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings) {
		final var sql = new StringBuilder("delete from ");
		sql.append(tableDescriptor.name());

		// WHERE clause - key restrictions
		sql.append(" where ");
		boolean first = true;
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
		return MutationType.DELETE;
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
		return "GraphJdbcDeleteMutation(" + getTableDescriptor().name() + ")";
	}
}
