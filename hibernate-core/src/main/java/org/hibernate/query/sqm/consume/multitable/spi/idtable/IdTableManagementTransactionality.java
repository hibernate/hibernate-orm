/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.multitable.spi.idtable;

/**
 * @author Steve Ebersole
 */
public enum IdTableManagementTransactionality {
	/**
	 * No handling of transactions is needed
	 */
	NONE,
	/**
	 * Execution of the DDL must be isolated from any ongoing transaction.  Isolation
	 * is needed when the database implicitly commints any active transaction when
	 * DDL is performed.
	 */
	ISOLATE,
	/**
	 * As with {@link #ISOLATE} the execution of the DDL must be isolated from any ongoing transaction.
	 * However, here the "isolation" will also be transacted.  Some databases require that the DDL
	 * happen within a transaction.  This value covers such cases.
	 */
	ISOLATE_AND_TRANSACT
}
