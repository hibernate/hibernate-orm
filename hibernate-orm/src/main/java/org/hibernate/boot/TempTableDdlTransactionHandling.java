/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot;

/**
 * Enum describing how creation and dropping of temporary tables should be done in terms of
 * transaction handling.
 *
 * @author Steve Ebersole
 */
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
