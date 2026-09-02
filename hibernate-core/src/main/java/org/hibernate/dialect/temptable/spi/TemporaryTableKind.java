/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temptable.spi;

import org.hibernate.SPI;

/**
 * Classifies the kinds of temporary table implementations.
 *
 * @since 6.0
 */
@SPI
public enum TemporaryTableKind {
	/**
	 * Modeled as a regular table with a special session-identifier column,
	 * which is explicitly deleted from at the end of a transaction.
	 * <p>
	 * The table is created once on application startup, unless {@value org.hibernate.query.sqm.mutation.internal.temptable.PersistentTableStrategy#CREATE_ID_TABLES}
	 * is disabled and dropped on application startup unless {@value org.hibernate.query.sqm.mutation.internal.temptable.PersistentTableStrategy#CREATE_ID_TABLES}
	 * or {@value org.hibernate.query.sqm.mutation.internal.temptable.PersistentTableStrategy#DROP_ID_TABLES} are
	 * disabled.
	 */
	PERSISTENT,
	/**
	 * Modeled as what the SQL standard calls a local temporary table, which is a table that is defined per connection.
	 * <p>
	 * Usually, the table is created when needed in a transaction and databases usually drop it on transaction commit,
	 * though the selected strategy controls whether Hibernate should drop it explicitly
	 * and {@value org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableStrategy#DROP_ID_TABLES}.
	 */
	LOCAL,
	/**
	 * Modeled as what the SQL standard calls a global temporary table, which is a table that is defined once per schema,
	 * but its data is scoped to a transaction where data is usually deleted automatically on transaction commit,
	 * though the selected strategy controls whether Hibernate should delete data.
	 * <p>
	 * The table is created once on application startup, unless {@value org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableStrategy#CREATE_ID_TABLES}
	 * is disabled and dropped on application startup unless {@value org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableStrategy#CREATE_ID_TABLES}
	 * or {@value org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableStrategy#DROP_ID_TABLES} are
	 * disabled.
	 */
	GLOBAL
}
