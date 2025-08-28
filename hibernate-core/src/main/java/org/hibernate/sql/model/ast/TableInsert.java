/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.ast;

import java.util.List;
import java.util.function.BiConsumer;

import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.model.jdbc.JdbcInsertMutation;

/**
 * Models an insert into a model (entity or collection) table,
 * triggered from flush
 *
 * @author Steve Ebersole
 */
public interface TableInsert extends TableMutation<JdbcInsertMutation> {
	/**
	 * The value bindings for each column, including table key(s)
	 */
	List<ColumnValueBinding> getValueBindings();

	/**
	 * The number of value bindings
	 *
	 * @see #getValueBindings()
	 */
	default int getNumberOfValueBindings() {
		return getValueBindings().size();
	}

	/**
	 * Visit each value binding
	 *
	 * @see #getValueBindings()
	 */
	void forEachValueBinding(BiConsumer<Integer, ColumnValueBinding> consumer);

	/**
	 * The columns to return from the insert.
	 */
	List<ColumnReference> getReturningColumns();

	/**
	 * The number of columns being returned
	 *
	 * @see #getReturningColumns
	 */
	default int getNumberOfReturningColumns() {
		final List<ColumnReference> returningColumns = getReturningColumns();
		return CollectionHelper.size( returningColumns );
	}

	/**
	 * Visit each return-column
	 *
	 * @see #getReturningColumns
	 */
	void forEachReturningColumn(BiConsumer<Integer,ColumnReference> consumer);
}
