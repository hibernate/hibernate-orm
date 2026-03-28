/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.internal;

import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.ast.AbstractTableDelete;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.ColumnValueParameter;
import org.hibernate.sql.model.ast.CustomSqlMutation;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.jdbc.JdbcDeleteMutation;

import java.util.List;

/**
 * Deletion defined using custom sql-delete
 *
 * @see org.hibernate.annotations.SQLDelete
 *
 * @author Steve Ebersole
 */
public class TableDeleteCustomSql extends AbstractTableDelete implements CustomSqlMutation<JdbcDeleteMutation> {
	private final TableMapping.MutationDetails mutationDetails;

	public TableDeleteCustomSql(
			MutatingTableReference mutatingTable,
			MutationTarget<?,?> mutationTarget,
			String sqlComment,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings,
			List<ColumnValueParameter> parameters) {
		this(
				mutatingTable,
				mutatingTable.getTableMapping().getDeleteDetails(),
				mutationTarget,
				sqlComment,
				keyRestrictionBindings,
				optLockRestrictionBindings,
				parameters
		);
	}

	public TableDeleteCustomSql(
			MutatingTableReference mutatingTable,
			TableMapping.MutationDetails mutationDetails,
			MutationTarget<?,?> mutationTarget,
			String sqlComment,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings,
			List<ColumnValueParameter> parameters) {
		super( mutatingTable, mutationTarget, sqlComment, keyRestrictionBindings,
				optLockRestrictionBindings, parameters );
		this.mutationDetails = mutationDetails;
	}

	@Override
	public boolean isCustomSql() {
		return true;
	}

	@Override
	public String getCustomSql() {
		return mutationDetails.getCustomSql();
	}

	@Override
	public boolean isCallable() {
		return mutationDetails.isCallable();
	}


	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitCustomTableDelete( this );
	}
}
