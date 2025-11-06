/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.ast.builder;

import org.hibernate.Incubating;
import org.hibernate.Internal;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.TableMutation;

/**
 * Common operations of {@link TableUpdateBuilder} and {@link TableInsertBuilder}.
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public interface ColumnValuesTableMutationBuilder<M extends TableMutation<?>> extends TableMutationBuilder<M> {
	/**
	 * Adds a restriction, which is assumed to be based on a table key.
	 *
	 * @apiNote Be sure you know what you are doing before using this method.  Generally
	 * prefer any of the other methods here for adding key restrictions.
	 */
	@Internal
	@Incubating
	void addValueColumn(ColumnValueBinding valueBinding);

	/**
	 * Add a column as part of the values list
	 */
	void addValueColumn(String columnWriteFragment, SelectableMapping selectableMapping);

	/**
	 * Add a column as part of the values list
	 */
	default void addValueColumn(SelectableMapping selectableMapping) {
		addValueColumn(
				selectableMapping.getWriteExpression(),
				selectableMapping
		);
	}

	/**
	 * Add a key column
	 */
	void addKeyColumn(String valueExpression, SelectableMapping selectableMapping);

	/**
	 * Add a key column
	 */
	default void addKeyColumn(SelectableMapping selectableMapping) {
		addKeyColumn(
				selectableMapping.getWriteExpression(),
				selectableMapping
		);
	}

	/**
	 * Add a key column
	 */
	default void addKeyColumn(int index, SelectableMapping selectableMapping) {
		addKeyColumn(
				selectableMapping.getWriteExpression(),
				selectableMapping
		);
	}
}
