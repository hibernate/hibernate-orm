/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.spi;

/**
 * The strategy to use for applying locks to a {@link JdbcOperationQuerySelect}.
 *
 * @author Christian Beikov
 */
public enum JdbcLockStrategy {

	/**
	 * Use a dialect-specific check to determine how to apply locks.
	 */
	AUTO,
	/**
	 * Use follow-on locking.
	 */
	FOLLOW_ON,
	/**
	 * Do not apply locks.
	 */
	NONE
}
