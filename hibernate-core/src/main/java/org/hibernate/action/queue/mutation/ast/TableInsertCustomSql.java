/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.mutation.ast;

import org.hibernate.Incubating;
import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.action.queue.meta.TableDescriptor;
import org.hibernate.action.queue.mutation.GraphMutationTarget;
import org.hibernate.action.queue.mutation.jdbc.JdbcInsert;
import org.hibernate.action.queue.mutation.jdbc.JdbcInsertCustomSql;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.ColumnValueParameter;

import java.util.List;

/// Custom SQL INSERT mutation using {@link EntityTableDescriptor}.
///
/// Parallel to {@link org.hibernate.sql.model.internal.TableInsertCustomSql}
/// but works with graph-based infrastructure.
///
/// @see org.hibernate.annotations.SQLInsert
///
/// @author Steve Ebersole
@Incubating
public class TableInsertCustomSql
		extends AbstractAssigningTableMutation<JdbcInsert>
		implements TableInsert, CustomSqlMutation {

	public TableInsertCustomSql(
			TableDescriptor tableDescriptor,
			GraphMutationTarget<?> mutationTarget,
			String sqlComment,
			List<ColumnValueBinding> valueBindings,
			List<ColumnValueParameter> parameters) {
		super(
			tableDescriptor,
			MutationType.INSERT,
			mutationTarget,
			sqlComment,
			parameters,
			valueBindings
		);
	}

	@Override
	protected String getLoggableName() {
		return "GraphTableInsertCustomSql";
	}

	@Override
	public String getCustomSql() {
		return getTableDescriptor().insertDetails().getCustomSql();
	}

	@Override
	public boolean isCallable() {
		return getTableDescriptor().insertDetails().isCallable();
	}

	@Override
	public JdbcInsert createMutationOperation() {
		return new JdbcInsertCustomSql(
			getTableDescriptor(),
			getMutationTarget(),
			getTableDescriptor().insertDetails().getExpectation(),
			getValueBindings(),
			getParameters()
		);
	}
}
