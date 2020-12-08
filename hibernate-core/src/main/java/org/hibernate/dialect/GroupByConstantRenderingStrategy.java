/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
