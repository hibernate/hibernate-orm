/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi.model;

import java.util.List;
import java.util.function.BiConsumer;

import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.spi.mutation.MutationTarget;
import org.hibernate.sql.spi.mutation.jdbc.JdbcInsertMutation;

/**
 * @author Steve Ebersole
 */
public class TableInsertStandard extends AbstractTableInsert implements TableInsert, GeneratedMutation<JdbcInsertMutation> {
	private final List<ColumnReference> returningColumns;

	public TableInsertStandard(
			MutatingTableReference mutatingTable,
			MutationTarget mutationTarget,
			List<ColumnValueBinding> valueBindings,
			List<ColumnReference> returningColumns,
			List<ColumnValueParameter> parameters) {
		super( mutatingTable, mutationTarget, parameters, valueBindings );
		this.returningColumns = returningColumns;
	}

	@Override
	public boolean isCustomSql() {
		return false;
	}

	@Override
	public List<ColumnReference> getReturningColumns() {
		return returningColumns;
	}

	@Override
	public void forEachReturningColumn(BiConsumer<Integer,ColumnReference> consumer) {
		forEachThing( returningColumns, consumer );
	}

	@Override
	public boolean isCallable() {
		return false;
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitStandardTableInsert( this );
	}
}
