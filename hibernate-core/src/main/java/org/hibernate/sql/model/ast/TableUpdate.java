/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.ast;

import java.util.List;
import java.util.function.BiConsumer;

import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.model.MutationOperation;

/**
 * Models an update to a model (entity or collection) table,
 * triggered from flush
 *
 * @apiNote I
 *
 * @author Steve Ebersole
 */
public interface TableUpdate<O extends MutationOperation> extends LogicalTableUpdate<O> {
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
		return CollectionHelper.size( getReturningColumns() );
	}

	/**
	 * Visit each return-column
	 *
	 * @see #getReturningColumns
	 */
	void forEachReturningColumn(BiConsumer<Integer,ColumnReference> consumer);
}
