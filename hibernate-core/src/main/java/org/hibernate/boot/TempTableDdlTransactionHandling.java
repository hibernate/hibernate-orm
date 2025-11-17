/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot;

/**
 * Enum describing how creation and dropping of temporary tables should be done in terms of
 * transaction handling.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.dialect.Dialect#getTemporaryTableDdlTransactionHandling
 *
 * @deprecated All dialects currently use {@link #NONE}, so it's obsolete.
 */
@Deprecated(since = "7.0")
public enum TempTableDdlTransactionHandling {
	/**
	 * No handling of transactions is needed
	 */
	NONE,
	/**
	 * Execution of the DDL must be isolated from any ongoing transaction
	 */
	ISOLATE,
	/**
	 * As with {@link #ISOLATE} the execution of the DDL must be isolated from any ongoing transaction.
	 * However, here the "isolation" will also be transacted.  Some databases require that the DDL
	 * happen within a transaction.  This value covers such cases.
	 */
	ISOLATE_AND_TRANSACT
}
