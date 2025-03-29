/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import java.util.List;

import org.hibernate.Incubating;

import jakarta.persistence.criteria.Expression;

/**
 * A special expression for the {@code xmlelement} function.
 * @since 7.0
 */
@Incubating
public interface JpaXmlElementExpression extends JpaExpression<String> {

	/**
	 * Passes the given {@link Expression} as value for the XML attribute with the given name.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaXmlElementExpression attribute(String attributeName, Expression<?> expression);

	/**
	 * Passes the given {@link Expression}s as value for the XML content of this element.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaXmlElementExpression content(List<? extends Expression<?>> expressions);

	/**
	 * Passes the given {@link Expression}s as value for the XML content of this element.
	 *
	 * @return {@code this} for method chaining
	 */
	JpaXmlElementExpression content(Expression<?>... expressions);
}
