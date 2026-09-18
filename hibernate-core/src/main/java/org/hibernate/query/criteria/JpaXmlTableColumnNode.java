/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import jakarta.persistence.criteria.Expression;
import org.hibernate.Incubating;

/**
 * A special node for column defined for a {@code xmltable} function.
 * @since 7.0
 */
@Incubating(since = "6.3")
public interface JpaXmlTableColumnNode<T> {

	/**
	 * Specifies the default value to use if resolving the XPath expression doesn't produce results.
	 *
	 * @return {@code this} for method chaining
	 */
	@Nonnull
	JpaXmlTableColumnNode<T> defaultValue(@Nullable T value);

	/**
	 * Specifies the default value to use if resolving the XPath expression doesn't produce results.
	 *
	 * @return {@code this} for method chaining
	 */
	@Nonnull
	JpaXmlTableColumnNode<T> defaultExpression(@Nonnull Expression<T> expression);
}
