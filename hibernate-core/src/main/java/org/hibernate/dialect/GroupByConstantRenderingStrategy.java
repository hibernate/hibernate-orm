/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

/**
 * Strategies for rendering a constant in a group by.
 *
 * @author Christian Beikov
 */
public enum GroupByConstantRenderingStrategy {
	/**
	 * The strategy for ANSI SQL compliant DBs like e.g. PostgreSQL that renders `()` i.e. the empty grouping.
	 */
	EMPTY_GROUPING,
	/**
	 * Renders a constant e.g. `'0'`
	 */
	CONSTANT,
	/**
	 * Renders a constant expression e.g. `'0' || '0'`
	 */
	CONSTANT_EXPRESSION,
	/**
	 * Renders a subquery e.g. `(select 1)`
	 */
	SUBQUERY,
	/**
	 * Renders a column reference to a dummy table e.g. `, (select 1 x) dummy` and `dummy.x`
	 */
	COLUMN_REFERENCE;
}
