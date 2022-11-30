/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.sql.model.internal;

import java.util.List;

import org.hibernate.sql.ast.SqlAstWalker;
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
			List<ColumnValueParameter> parameters,
			List<ColumnValueBinding> valueBindings,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings) {
		super( mutatingTable, mutationTarget, parameters, valueBindings, keyRestrictionBindings, optLockRestrictionBindings );
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
	public void accept(SqlAstWalker walker) {
		walker.visitCustomTableUpdate( this );
	}
}
