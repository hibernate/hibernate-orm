/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

/**
 * Indicates the level of qualifier support used by
 * the dialect when referencing a column.
 *
 * @author Marco Belladelli
 */
public enum DmlTargetColumnQualifierSupport {
	/**
	 * Qualify the column using the table expression,
	 * ignoring a possible table alias.
	 */
	TABLE_EXPRESSION,

	/**
	 * Qualify the column using the table alias, whenever available,
	 * and fallback to the table expression.
	 */
	TABLE_ALIAS,

	/**
	 * No need to explicitly qualify the column.
	 */
	NONE;
}
