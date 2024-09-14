/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.exec.spi;

/**
 * The strategy to use for applying locks to a {@link JdbcOperationQuerySelect}.
 *
 * @author Christian Beikov
 */
public enum JdbcLockStrategy {

	/**
	 * Use a dialect specific check to determine how to apply locks.
	 */
	AUTO,
	/**
	 * Use follow-on locking.
	 */
	FOLLOW_ON,
	/**
	 * Do not apply locks.
	 */
	NONE;
}
