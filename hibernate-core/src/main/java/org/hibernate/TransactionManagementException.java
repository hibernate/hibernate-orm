/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

/**
 * Thrown when user code causes the end of a transaction that is
 * being managed by the {@link SessionFactory}.
 *
 * @see SessionFactory#inTransaction
 * @see SessionFactory#fromTransaction
 */
public class TransactionManagementException extends RuntimeException {
	public TransactionManagementException(String message) {
		super( message );
	}
}
