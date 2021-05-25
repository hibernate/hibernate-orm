/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

/**
 * The strategy for rendering which row to lock with the FOR UPDATE OF clause.
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
