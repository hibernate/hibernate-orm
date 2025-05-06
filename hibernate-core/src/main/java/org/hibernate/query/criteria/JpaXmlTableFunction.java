/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import org.hibernate.Incubating;

/**
 * A special expression for the {@code xmltable} function.
 * @since 7.0
 */
@Incubating
public interface JpaXmlTableFunction {

	/**
	 * Like {@link #queryColumn(String, String)}, but uses the column name as XPath expression.
	 *
	 * @return The {@link JpaXmlTableColumnNode} for the column
	 */
	JpaXmlTableColumnNode<String> queryColumn(String columnName);

	/**
	 * Defines a string column on the result type with the given name for which the value can be obtained
	 * by evaluating {@code xmlquery} with the given XPath expression on the XML document.
	 *
	 * @return The {@link JpaXmlTableColumnNode} for the column
	 */
	JpaXmlTableColumnNode<String> queryColumn(String columnName, String xpath);

	/**
	 * Like {@link #valueColumn(String, Class, String)} but uses the column name as XPath expression.
	 *
	 * @return The {@link JpaXmlTableColumnNode} for the column
	 */
	<X> JpaXmlTableColumnNode<X> valueColumn(String columnName, Class<X> type);

	/**
	 * Like {@link #valueColumn(String, JpaCastTarget, String)} but uses the column name as XPath expression.
	 *
	 * @return The {@link JpaXmlTableColumnNode} for the column
	 */
	<X> JpaXmlTableColumnNode<X> valueColumn(String columnName, JpaCastTarget<X> castTarget);

	/**
	 * Like {@link #valueColumn(String, JpaCastTarget, String)}, but converting the {@link Class}
	 * to {@link JpaCastTarget} via {@link HibernateCriteriaBuilder#castTarget(Class)}.
	 *
	 * @return The {@link JpaXmlTableColumnNode} for the column
	 */
	<X> JpaXmlTableColumnNode<X> valueColumn(String columnName, Class<X> type, String xpath);

	/**
	 * Defines an column on the result type with the given name and type for which the value can be obtained by the given XPath path expression.
	 *
	 * @return The {@link JpaXmlTableColumnNode} for the column
	 */
	<X> JpaXmlTableColumnNode<X> valueColumn(String columnName, JpaCastTarget<X> castTarget, String xpath);

	/**
	 * Defines a long column on the result type with the given name which is set to the ordinality i.e.
	 * the 1-based position of the processed element. Ordinality starts again at 1 within nested paths.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaXmlTableFunction ordinalityColumn(String columnName);
}
