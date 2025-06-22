/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.persistence.criteria.Expression;
import org.hibernate.Incubating;

/**
 * A special node for column defined for a {@code xmltable} function.
 * @since 7.0
 */
@Incubating
public interface JpaXmlTableColumnNode<T> {

	/**
	 * Specifies the default value to use if resolving the XPath expression doesn't produce results.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaXmlTableColumnNode<T> defaultValue(T value);

	/**
	 * Specifies the default value to use if resolving the XPath expression doesn't produce results.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaXmlTableColumnNode<T> defaultExpression(Expression<T> expression);
}
