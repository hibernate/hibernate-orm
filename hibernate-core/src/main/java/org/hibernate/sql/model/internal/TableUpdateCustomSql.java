/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.internal;

import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.ast.AbstractTableUpdate;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.ColumnValueParameter;
import org.hibernate.sql.model.ast.CustomSqlMutation;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.jdbc.JdbcMutationOperation;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

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
	private TableMapping.MutationDetails mutationDetails;

	public TableUpdateCustomSql(
			MutatingTableReference mutatingTable,
			MutationTarget<?> mutationTarget,
			String sqlComment,
			List<ColumnValueBinding> valueBindings,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings) {
		super( mutatingTable, mutationTarget, sqlComment, valueBindings, keyRestrictionBindings, optLockRestrictionBindings );
		this.mutationDetails = mutationTarget.getIdentifierTableMapping().getUpdateDetails();
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
		this.mutationDetails = mutationTarget.getIdentifierTableMapping().getUpdateDetails();
	}

	public TableUpdateCustomSql(
			MutatingTableReference mutatingTable,
			MutationTarget<?> mutationTarget,
			TableMapping.MutationDetails mutationDetails,
			String sqlComment,
			List<ColumnValueBinding> valueBindings,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings,
			List<ColumnValueParameter> parameters) {
		super( mutatingTable, mutationTarget, sqlComment, valueBindings, keyRestrictionBindings, optLockRestrictionBindings, parameters );
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
