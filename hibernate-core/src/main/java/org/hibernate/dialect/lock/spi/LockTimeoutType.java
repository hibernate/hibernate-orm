/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.spi;

/**
 * The way, if any, that a Dialect supports specifying lock timeouts.
 *
 * @author Steve Ebersole
 */
public enum LockTimeoutType {
	/**
	 * Lock timeouts are simply not supported.
	 */
	NONE,
	/**
	 * Lock timeouts are supported as part of the query; e.g. {@code for update ... wait 1000}.
	 */
	QUERY,
	/**
	 * Lock timeouts are supported on the JDBC Connection, typically through an {@code alter session} command.
	 *
	 * @see ConnectionLockTimeoutStrategy
	 */
	CONNECTION
}
