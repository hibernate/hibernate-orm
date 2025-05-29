/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

/**
 * The strategy for rendering which row to lock with the {@code FOR UPDATE OF} clause.
 *
 * @author Christian Beikov
 * @author Steve Ebersole
 */
public enum RowLockStrategy {
	/**
	 * Use the column reference (column name qualified by the table alias).
	 */
	COLUMN,
	/**
	 * Use the table alias.
	 */
	TABLE,
	/**
	 * No support for specifying rows to lock.
	 */
	NONE
}
