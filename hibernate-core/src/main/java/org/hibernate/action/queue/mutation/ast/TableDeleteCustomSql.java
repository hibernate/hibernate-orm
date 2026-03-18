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
import org.hibernate.action.queue.mutation.jdbc.JdbcDeleteCustomSql;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.ColumnValueParameter;

import java.util.List;

/// Custom SQL DELETE mutation using {@link EntityTableDescriptor}.
///
/// Parallel to {@link org.hibernate.sql.model.internal.TableDeleteCustomSql}
/// but works with graph-based infrastructure.
///
/// @see org.hibernate.annotations.SQLDelete
///
/// @author Steve Ebersole
@Incubating
public class TableDeleteCustomSql
		extends AbstractTableMutation<JdbcDelete>
		implements TableDelete, CustomSqlMutation {

	private final List<ColumnValueBinding> keyRestrictionBindings;
	private final List<ColumnValueBinding> optLockRestrictionBindings;

	public TableDeleteCustomSql(
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

	@Override
	protected String getLoggableName() {
		return "GraphTableDeleteCustomSql";
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
	public String getCustomSql() {
		return getTableDescriptor().deleteDetails().getCustomSql();
	}

	@Override
	public boolean isCallable() {
		return getTableDescriptor().deleteDetails().isCallable();
	}

	@Override
	public JdbcDelete createMutationOperation() {
		return new JdbcDeleteCustomSql(
			getTableDescriptor(),
			getMutationTarget(),
			getTableDescriptor().deleteDetails().getExpectation(),
			keyRestrictionBindings,
			optLockRestrictionBindings,
			getParameters()
		);
	}
}
