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

/// Custom SQL INSERT mutation using {@link EntityTableDescriptor}.
///
/// Parallel to custom SQL handling in legacy infrastructure but uses
/// pre-defined custom SQL from TableDescriptor.
///
/// @see org.hibernate.annotations.SQLInsert
///
/// @author Steve Ebersole
@Incubating
public class JdbcInsertCustomSql
		extends AbstractJdbcOperation
		implements JdbcInsert {

	private final String customSql;
	private final boolean callable;

	public JdbcInsertCustomSql(
			TableDescriptor tableDescriptor,
			GraphMutationTarget<?> mutationTarget,
			Expectation expectation,
			List<ColumnValueBinding> valueBindings,
			List<ColumnValueParameter> parameters) {
		super(tableDescriptor, mutationTarget, expectation, parameters);
		this.customSql = tableDescriptor.insertDetails().getCustomSql();
		this.callable = tableDescriptor.insertDetails().isCallable();
		registerValueDescriptors(parameters);
	}

	@Override
	public MutationType getMutationType() {
		return MutationType.INSERT;
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
		return "GraphJdbcInsertCustomSql(" + getTableDescriptor().name() + ")";
	}
}
