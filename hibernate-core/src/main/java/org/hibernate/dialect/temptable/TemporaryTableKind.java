/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temptable;

import org.hibernate.dialect.Dialect;

/**
 * Classifies the kinds of temporary table implementations.
 *
 * @since 6.0
 */
public enum TemporaryTableKind {
	/**
	 * Modeled as a regular table with a special {@link TemporaryTableSessionUidColumn},
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
	 * though it is possible to control if Hibernate should drop it explicitly through {@link Dialect#getTemporaryTableAfterUseAction()}
	 * and {@value org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableStrategy#DROP_ID_TABLES}.
	 */
	LOCAL,
	/**
	 * Modeled as what the SQL standard calls a global temporary table, which is a table that is defined once per schema,
	 * but its data is scoped to a transaction where data is usually deleted automatically on transaction commit,
	 * though it is possible to control whether Hibernate should delete data or not through {@link Dialect#getTemporaryTableAfterUseAction()}.
	 * <p>
	 * The table is created once on application startup, unless {@value org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableStrategy#CREATE_ID_TABLES}
	 * is disabled and dropped on application startup unless {@value org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableStrategy#CREATE_ID_TABLES}
	 * or {@value org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableStrategy#DROP_ID_TABLES} are
	 * disabled.
	 */
	GLOBAL
}
