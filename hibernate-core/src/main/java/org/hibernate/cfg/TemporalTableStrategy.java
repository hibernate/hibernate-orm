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
	VM_TIMESTAMP,
	/**
	 * Maintain two tables, one for current data and one for
	 * historical data. The table for current data has all the
	 * usual foreign key constraints that would exist for any
	 * non-temporal entity; the table for historical data has
	 * no foreign key constraints.
	 * <ul>
	 * <li>When a new entity instance is created, a row is
	 *    inserted in both tables.
	 * <li>When the instance is updated, its row in the table
	 *     for current data is updated; but in the table for
	 *     historical data, a new revision row is inserted and
	 *     the previous revision row is marked as superseded by
	 *     setting its
	 *     {@link org.hibernate.annotations.Temporal#rowEnd
	 *     row end} column to the current timestamp.
	 * <li>When an entity instance is removed, its row in the
	 *     table for current data is deleted; and the current
	 *     revision row in the table for historical data is
	 *     marked as superseded by setting its {@code row end}
	 *     column.
	 * </ul>
	 * <p>Queries for current data are run against the table
	 * for current data. Queries for historical data (that is,
	 * any query in a session created using
	 * {@link org.hibernate.engine.creation.CommonBuilder#asOf})
	 * are executed against the table for historical
	 * data.
	 */
	HISTORY
}
