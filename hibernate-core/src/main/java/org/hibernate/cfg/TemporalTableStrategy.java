/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cfg;

import org.hibernate.Incubating;

/**
 * Enumerates the implementation strategies for
 * {@linkplain org.hibernate.annotations.Temporal
 * temporal} tables.
 *
 * @see org.hibernate.annotations.Temporal
 *
 * @author Gavin King
 */
@Incubating
public enum TemporalTableStrategy {
	/**
	 * Use native ANSI SQL 2011-style temporal tables where they
	 * are supported (Maria, SQL Server, Db2).
	 */
	NATIVE,
	/**
	 * Use timestamps generated on the database server using the
	 * {@link org.hibernate.dialect.Dialect#currentTimestamp
	 * current_timestamp} function to initialize effective and
	 * superseded columns.
	 * <p>Not recommended on database platforms where there is no
	 * way to obtain the timestamp of the start of the current
	 * transaction (MySQL, Maria).
	 */
	SERVER_TIMESTAMP,
	/**
	 * Use timestamps {@linkplain java.time.Instant#now generated}
	 * in the Java Virtual Machine to initialize effective and
	 * superseded columns. This is the default.
	 */
	VM_TIMESTAMP
}
