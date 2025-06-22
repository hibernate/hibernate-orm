/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import org.hibernate.Incubating;

/**
 * A special expression for the definition of columns within the {@code json_table} function.
 * @since 7.0
 */
@Incubating
public interface JpaJsonTableColumnsNode {

	/**
	 * Like {@link #existsColumn(String, String)}, but uses the column name as JSON path expression.
	 *
	 * @return The {@link JpaJsonExistsNode} for the column
	 */
	JpaJsonExistsNode existsColumn(String columnName);

	/**
	 * Defines a boolean column on the result type with the given name for which the value can be obtained
	 * by invoking {@code json_exists} with the given JSON path.
	 *
	 * @return The {@link JpaJsonExistsNode} for the column
	 */
	JpaJsonExistsNode existsColumn(String columnName, String jsonPath);

	/**
	 * Like {@link #queryColumn(String, String)}, but uses the column name as JSON path expression.
	 *
	 * @return The {@link JpaJsonQueryNode} for the column
	 */
	JpaJsonQueryNode queryColumn(String columnName);

	/**
	 * Defines a string column on the result type with the given name for which the value can be obtained
	 * by invoking {@code json_query} with the given JSON path.
	 *
	 * @return The {@link JpaJsonQueryNode} for the column
	 */
	JpaJsonQueryNode queryColumn(String columnName, String jsonPath);

	/**
	 * Like {@link #valueColumn(String, Class, String)} but uses the column name as JSON path expression.
	 *
	 * @return The {@link JpaJsonValueNode} for the column
	 */
	<T> JpaJsonValueNode<T> valueColumn(String columnName, Class<T> type);

	/**
	 * Defines a column on the result type with the given name and type for which the value can be obtained by the given JSON path expression.
	 *
	 * @return The {@link JpaJsonValueNode} for the column
	 */
	<T> JpaJsonValueNode<T> valueColumn(String columnName, Class<T> type, String jsonPath);

	/**
	 * Like {@link #valueColumn(String, Class, String)} but uses the column name as JSON path expression.
	 *
	 * @return The {@link JpaJsonValueNode} for the column
	 */
	<T> JpaJsonValueNode<T> valueColumn(String columnName, JpaCastTarget<T> type);

	/**
	 * Defines a column on the result type with the given name and type for which the value can be obtained by the given JSON path expression.
	 *
	 * @return The {@link JpaJsonValueNode} for the column
	 */
	<T> JpaJsonValueNode<T> valueColumn(String columnName, JpaCastTarget<T> type, String jsonPath);

	/**
	 * Defines nested columns that are accessible by the given JSON path.
	 *
	 * @return a new columns node for the nested JSON path
	 */
	JpaJsonTableColumnsNode nested(String jsonPath);

	/**
	 * Defines a long typed column on the result type with the given name which is set to the ordinality i.e.
	 * the 1-based position of the processed element. Ordinality starts again at 1 within nested paths.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaJsonTableColumnsNode ordinalityColumn(String columnName);
}
