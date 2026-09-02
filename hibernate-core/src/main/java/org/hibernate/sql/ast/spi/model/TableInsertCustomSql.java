/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi.model;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.spi.mutation.MutationTarget;
import org.hibernate.sql.spi.mutation.jdbc.JdbcInsertMutation;

/**
 * Insertion defined using custom sql-insert
 *
 * @see org.hibernate.annotations.SQLInsert
 *
 * @author Steve Ebersole
 */
public class TableInsertCustomSql extends AbstractTableInsert implements CustomSqlMutation<JdbcInsertMutation> {

	public TableInsertCustomSql(
			MutatingTableReference mutatingTable,
			MutationTarget mutationTarget,
			List<ColumnValueBinding> valueBindings,
			List<ColumnValueParameter> parameters) {
		super( mutatingTable, mutationTarget, parameters, valueBindings );
	}

	@Override
	public boolean isCustomSql() {
		return true;
	}

	@Override
	public String getCustomSql() {
		return getMutatingTable().getTableMapping().getInsertDetails().getCustomSql();
	}

	@Override
	public boolean isCallable() {
		return getMutatingTable().getTableMapping().getInsertDetails().isCallable();
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
		walker.visitCustomTableInsert( this );
	}
}
