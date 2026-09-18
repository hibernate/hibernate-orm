/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.Incubating;

/**
 * A special expression for the {@code xmltable} function.
 * @since 7.0
 */
@Incubating(since = "6.3")
public interface JpaXmlTableFunction {

	/**
	 * Like {@link #queryColumn(String, String)}, but uses the column name as XPath expression.
	 *
	 * @return The {@link JpaXmlTableColumnNode} for the column
	 */
	@Nonnull
	JpaXmlTableColumnNode<String> queryColumn(@Nonnull String columnName);

	/**
	 * Defines a string column on the result type with the given name for which the value can be obtained
	 * by evaluating {@code xmlquery} with the given XPath expression on the XML document.
	 *
	 * @return The {@link JpaXmlTableColumnNode} for the column
	 */
	@Nonnull
	JpaXmlTableColumnNode<String> queryColumn(@Nonnull String columnName, @Nullable String xpath);

	/**
	 * Like {@link #valueColumn(String, Class, String)} but uses the column name as XPath expression.
	 *
	 * @return The {@link JpaXmlTableColumnNode} for the column
	 */
	@Nonnull
	<X> JpaXmlTableColumnNode<X> valueColumn(@Nonnull String columnName, @Nonnull Class<X> type);

	/**
	 * Like {@link #valueColumn(String, JpaCastTarget, String)} but uses the column name as XPath expression.
	 *
	 * @return The {@link JpaXmlTableColumnNode} for the column
	 */
	@Nonnull
	<X> JpaXmlTableColumnNode<X> valueColumn(@Nonnull String columnName, @Nonnull JpaCastTarget<X> castTarget);

	/**
	 * Like {@link #valueColumn(String, JpaCastTarget, String)}, but converting the {@link Class}
	 * to {@link JpaCastTarget} via {@link HibernateCriteriaBuilder#castTarget(Class)}.
	 *
	 * @return The {@link JpaXmlTableColumnNode} for the column
	 */
	@Nonnull
	<X> JpaXmlTableColumnNode<X> valueColumn(@Nonnull String columnName, @Nonnull Class<X> type, @Nullable String xpath);

	/**
	 * Defines an column on the result type with the given name and type for which the value can be obtained by the given XPath path expression.
	 *
	 * @return The {@link JpaXmlTableColumnNode} for the column
	 */
	@Nonnull
	<X> JpaXmlTableColumnNode<X> valueColumn(@Nonnull String columnName, @Nonnull JpaCastTarget<X> castTarget, @Nullable String xpath);

	/**
	 * Defines a long column on the result type with the given name which is set to the ordinality i.e.
	 * the 1-based position of the processed element. Ordinality starts again at 1 within nested paths.
	 *
	 * @return {@code this} for method chaining
	 */
	@Nonnull
	JpaXmlTableFunction ordinalityColumn(@Nonnull String columnName);
}
