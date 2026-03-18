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
import org.hibernate.action.queue.mutation.jdbc.JdbcInsertStandard;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.ColumnValueParameter;

import java.util.List;

/// Standard INSERT mutation using {@link EntityTableDescriptor}.
///
/// Parallel to {@link org.hibernate.sql.model.internal.TableInsertStandard}
/// but works with graph-based infrastructure.
///
/// @author Steve Ebersole
@Incubating
public class TableInsertStandard
		extends AbstractAssigningTableMutation<JdbcInsert>
		implements TableInsert, Statement {

	public TableInsertStandard(
			TableDescriptor tableDescriptor,
			GraphMutationTarget<?> mutationTarget,
			List<ColumnValueBinding> valueBindings,
			List<ColumnValueParameter> parameters) {
		this(
			tableDescriptor,
			mutationTarget,
			"insert for " + mutationTarget.getRolePath(),
			valueBindings,
			parameters
		);
	}

	public TableInsertStandard(
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
		return "GraphTableInsert";
	}

	@Override
	public JdbcInsert createMutationOperation() {
		// Get expectation from TableDescriptor
		final var expectation = getTableDescriptor().insertDetails() != null
			? getTableDescriptor().insertDetails().getExpectation()
			: org.hibernate.jdbc.Expectations.NONE;

		// Generate SQL and create JDBC operation
		return new JdbcInsertStandard(
			getTableDescriptor(),
			getMutationTarget(),
			expectation,
			valueBindings,
			getParameters()
		);
	}

	@Override
	public void accept(SqlAstWalker walker) {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}
}
