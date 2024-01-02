/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.sql.model.internal;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.ast.AbstractTableUpdate;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.ColumnValueParameter;
import org.hibernate.sql.model.ast.CustomSqlMutation;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.jdbc.JdbcMutationOperation;

/**
 * Update defined using custom sql-update
 *
 * @see org.hibernate.annotations.SQLUpdate
 *
 * @author Steve Ebersole
 */
public class TableUpdateCustomSql
		extends AbstractTableUpdate<JdbcMutationOperation>
		implements CustomSqlMutation<JdbcMutationOperation> {
	public TableUpdateCustomSql(
			MutatingTableReference mutatingTable,
			MutationTarget<?> mutationTarget,
			String sqlComment,
			List<ColumnValueBinding> valueBindings,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings) {
		super( mutatingTable, mutationTarget, sqlComment, valueBindings, keyRestrictionBindings, optLockRestrictionBindings );
	}

	public TableUpdateCustomSql(
			MutatingTableReference mutatingTable,
			MutationTarget<?> mutationTarget,
			String sqlComment,
			List<ColumnValueBinding> valueBindings,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings,
			List<ColumnValueParameter> parameters) {
		super( mutatingTable, mutationTarget, sqlComment, valueBindings, keyRestrictionBindings, optLockRestrictionBindings, parameters );
	}

	@Override
	public boolean isCustomSql() {
		return true;
	}

	@Override
	public String getCustomSql() {
		return getMutatingTable().getTableMapping().getUpdateDetails().getCustomSql();
	}

	@Override
	public boolean isCallable() {
		return getMutatingTable().getTableMapping().getUpdateDetails().isCallable();
	}

	@Override
	public List<ColumnReference> getReturningColumns() {
		return Collections.emptyList();
	}

	@Override
	public void forEachReturningColumn(BiConsumer<Integer, ColumnReference> consumer) {
		// nothing to do
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitCustomTableUpdate( this );
	}
}
