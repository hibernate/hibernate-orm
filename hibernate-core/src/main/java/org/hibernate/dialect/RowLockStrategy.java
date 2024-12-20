/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

/**
 * The strategy for rendering which row to lock with the {@code FOR UPDATE OF} clause.
 *
 * @author Christian Beikov
 */
public enum RowLockStrategy {
	/**
	 * Use a column name.
	 */
	COLUMN,
	/**
	 * Use a table alias.
	 */
	TABLE,
	/**
	 * No support for specifying rows to lock.
	 */
	NONE;
}
