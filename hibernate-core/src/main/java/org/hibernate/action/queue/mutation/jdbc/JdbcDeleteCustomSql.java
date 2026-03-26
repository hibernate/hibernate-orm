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

/// Custom SQL DELETE mutation using {@link EntityTableDescriptor}.
///
/// Parallel to custom SQL handling in legacy infrastructure but uses
/// pre-defined custom SQL from TableDescriptor.
///
/// @see org.hibernate.annotations.SQLDelete
///
/// @author Steve Ebersole
@Incubating
public class JdbcDeleteCustomSql
		extends AbstractJdbcOperation
		implements JdbcDelete {

	private final String customSql;
	private final boolean callable;

	public JdbcDeleteCustomSql(
			TableDescriptor tableDescriptor,
			GraphMutationTarget<?> mutationTarget,
			Expectation expectation,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings,
			List<ColumnValueParameter> parameters) {
		this(
				tableDescriptor,
				mutationTarget,
				tableDescriptor.deleteDetails().getCustomSql(),
				tableDescriptor.deleteDetails().isCallable(),
				expectation,
				keyRestrictionBindings,
				optLockRestrictionBindings,
				parameters
		);
	}

	public JdbcDeleteCustomSql(
			TableDescriptor tableDescriptor,
			GraphMutationTarget<?> mutationTarget,
			String customSql,
			boolean callable,
			Expectation expectation,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings,
			List<ColumnValueParameter> parameters) {
		super(tableDescriptor, mutationTarget, expectation, parameters);
		this.customSql = customSql;
		this.callable = callable;
		registerValueDescriptors(parameters);
	}

	@Override
	public MutationType getMutationType() {
		return MutationType.DELETE;
	}

	@Override
	public String getSqlString() {
		return customSql;
	}

	@Override
	public boolean isCallable() {
		return callable;
	}

	@Override
	public String toString() {
		return "GraphJdbcDeleteCustomSql(" + getTableDescriptor().name() + ")";
	}
}
