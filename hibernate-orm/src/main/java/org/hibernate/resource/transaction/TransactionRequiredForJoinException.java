/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.transaction;

import org.hibernate.HibernateException;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;

/**
 * Indicates a call to {@link TransactionCoordinator#explicitJoin()} that requires an
 * active transaction where there currently is none.
 *
 * @author Steve Ebersole
 */
public class TransactionRequiredForJoinException extends HibernateException {
	public TransactionRequiredForJoinException(String message) {
		super( message );
	}

	public TransactionRequiredForJoinException(String message, Throwable cause) {
		super( message, cause );
	}
}
