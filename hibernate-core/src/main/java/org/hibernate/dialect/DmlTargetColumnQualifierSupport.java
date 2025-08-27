/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
	NONE
}
