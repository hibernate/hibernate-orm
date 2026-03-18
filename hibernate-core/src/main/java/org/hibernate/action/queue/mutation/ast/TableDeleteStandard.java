/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.mutation.ast;

import org.hibernate.Incubating;
import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.action.queue.meta.TableDescriptor;
import org.hibernate.action.queue.mutation.GraphMutationTarget;
import org.hibernate.action.queue.mutation.jdbc.JdbcDelete;
import org.hibernate.action.queue.mutation.jdbc.JdbcDeleteStandard;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.ColumnValueParameter;

import java.util.List;

/// Standard DELETE mutation using {@link EntityTableDescriptor}.
///
/// Parallel to {@link org.hibernate.sql.model.internal.TableDeleteStandard}
/// but works with graph-based infrastructure.
///
/// @author Steve Ebersole
@Incubating
public class TableDeleteStandard
		extends AbstractTableMutation<JdbcDelete>
		implements TableDelete, Statement {

	private final List<ColumnValueBinding> keyRestrictionBindings;
	private final List<ColumnValueBinding> optLockRestrictionBindings;

	public TableDeleteStandard(
			TableDescriptor tableDescriptor,
			GraphMutationTarget<?> mutationTarget,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings,
			List<ColumnValueParameter> parameters) {
		this(
			tableDescriptor,
			mutationTarget,
			"delete for " + mutationTarget.getRolePath(),
			keyRestrictionBindings,
			optLockRestrictionBindings,
			parameters
		);
	}

	public TableDeleteStandard(
			TableDescriptor tableDescriptor,
			GraphMutationTarget<?> mutationTarget,
			String sqlComment,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings,
			List<ColumnValueParameter> parameters) {
		super(
			tableDescriptor,
			MutationType.DELETE,
			mutationTarget,
			sqlComment,
			parameters
		);
		this.keyRestrictionBindings = keyRestrictionBindings;
		this.optLockRestrictionBindings = optLockRestrictionBindings;
	}

	private static List<ColumnValueBinding> combineBindings(
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings) {
		final var combined = new java.util.ArrayList<ColumnValueBinding>();
		if (keyRestrictionBindings != null) {
			combined.addAll(keyRestrictionBindings);
		}
		if (optLockRestrictionBindings != null) {
			combined.addAll(optLockRestrictionBindings);
		}
		return combined;
	}

	@Override
	protected String getLoggableName() {
		return "GraphTableDelete";
	}

	@Override
	public List<ColumnValueBinding> getValueBindings() {
		return CollectionHelper.combine( keyRestrictionBindings, optLockRestrictionBindings );
	}

	public List<ColumnValueBinding> getKeyRestrictionBindings() {
		return keyRestrictionBindings;
	}

	public List<ColumnValueBinding> getOptLockRestrictionBindings() {
		return optLockRestrictionBindings;
	}

	@Override
	public JdbcDelete createMutationOperation() {
		// Get expectation from TableDescriptor
		final var expectation = getTableDescriptor().deleteDetails() != null
			? getTableDescriptor().deleteDetails().getExpectation()
			: org.hibernate.jdbc.Expectations.NONE;

		// Generate SQL and create JDBC operation
		return new JdbcDeleteStandard(
			getTableDescriptor(),
			getMutationTarget(),
			expectation,
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
