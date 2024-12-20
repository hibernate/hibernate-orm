/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.ast.builder;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;

/**
 * Common operations of {@link TableUpdateBuilder} and {@link TableInsertBuilder}.
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public interface ColumnValuesTableMutationBuilder {
	/**
	 * Add a column as part of the values list
	 */
	void addValueColumn(String columnName, String columnWriteFragment, JdbcMapping jdbcMapping, boolean isLob);
	/**
	 * Add a column as part of the values list
	 */
	default void addValueColumn(String columnName, String columnWriteFragment, JdbcMapping jdbcMapping) {
		addValueColumn( columnName, columnWriteFragment, jdbcMapping, jdbcMapping.getJdbcType().isLob() );
	}

	/**
	 * Add a column as part of the values list
	 */
	default void addValueColumn(SelectableMapping selectableMapping) {
		addValueColumn(
				selectableMapping.getSelectionExpression(),
				selectableMapping.getWriteExpression(),
				selectableMapping.getJdbcMapping(),
				selectableMapping.isLob()
		);
	}

	/**
	 * Add a key column
	 */
	void addKeyColumn(String columnName, String valueExpression, JdbcMapping jdbcMapping);

	/**
	 * Add a key column
	 */
	default void addKeyColumn(SelectableMapping selectableMapping) {
		addKeyColumn(
				selectableMapping.getSelectionExpression(),
				selectableMapping.getWriteExpression(),
				selectableMapping.getJdbcMapping()
		);
	}
}
