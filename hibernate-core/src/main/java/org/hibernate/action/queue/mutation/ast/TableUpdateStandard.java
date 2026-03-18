/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.mutation.ast;

import org.hibernate.Incubating;
import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.action.queue.meta.TableDescriptor;
import org.hibernate.action.queue.mutation.GraphMutationTarget;
import org.hibernate.action.queue.mutation.jdbc.JdbcUpdate;
import org.hibernate.action.queue.mutation.jdbc.JdbcUpdateStandard;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.ColumnValueParameter;

import java.util.List;

/// Standard UPDATE mutation using {@link EntityTableDescriptor}.
///
/// Parallel to {@link org.hibernate.sql.model.internal.TableUpdateStandard}
/// but works with graph-based infrastructure.
///
/// @author Steve Ebersole
@Incubating
public class TableUpdateStandard
		extends AbstractAssigningTableMutation<JdbcUpdate>
		implements TableUpdate, Statement {

	private final List<ColumnValueBinding> keyRestrictionBindings;
	private final List<ColumnValueBinding> optLockRestrictionBindings;

	public TableUpdateStandard(
			TableDescriptor tableDescriptor,
			GraphMutationTarget<?> mutationTarget,
			List<ColumnValueBinding> valueBindings,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings,
			List<ColumnValueParameter> parameters) {
		this(
			tableDescriptor,
			mutationTarget,
			"update for " + mutationTarget.getRolePath(),
			valueBindings,
			keyRestrictionBindings,
			optLockRestrictionBindings,
			parameters
		);
	}

	public TableUpdateStandard(
			TableDescriptor tableDescriptor,
			GraphMutationTarget<?> mutationTarget,
			String sqlComment,
			List<ColumnValueBinding> valueBindings,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings,
			List<ColumnValueParameter> parameters) {
		super(
				tableDescriptor,
				MutationType.UPDATE,
				mutationTarget,
				sqlComment,
				parameters,
				valueBindings
		);
		this.keyRestrictionBindings = keyRestrictionBindings;
		this.optLockRestrictionBindings = optLockRestrictionBindings;
	}

	@Override
	protected String getLoggableName() {
		return "GraphTableUpdate";
	}

	@Override
	public List<ColumnValueBinding> getKeyRestrictionBindings() {
		return keyRestrictionBindings;
	}

	@Override
	public List<ColumnValueBinding> getOptLockRestrictionBindings() {
		return optLockRestrictionBindings;
	}

	@Override
	public JdbcUpdate createMutationOperation() {
		// Get expectation from TableDescriptor
		final var expectation = getTableDescriptor().updateDetails() != null
			? getTableDescriptor().updateDetails().getExpectation()
			: org.hibernate.jdbc.Expectations.NONE;

		// Generate SQL and create JDBC operation
		return new JdbcUpdateStandard(
			getTableDescriptor(),
			getMutationTarget(),
			expectation,
			valueBindings,
			keyRestrictionBindings,
			optLockRestrictionBindings,
			getParameters()
		);
	}

	@Override
	public void accept(SqlAstWalker walker) {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}
}
