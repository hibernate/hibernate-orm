/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.transaction.backend.jta.internal;

import javax.transaction.Status;

import org.hibernate.TransactionException;
import org.hibernate.resource.transaction.spi.TransactionStatus;

/**
 * @author Andrea Boriero
 */
public class StatusTranslator {

	public static TransactionStatus translate(int status) {
		TransactionStatus transactionStatus = null;
		switch ( status ) {
			case Status.STATUS_ACTIVE:
				transactionStatus = TransactionStatus.ACTIVE;
				break;
			case Status.STATUS_PREPARED:
				transactionStatus = TransactionStatus.ACTIVE;
				break;
			case Status.STATUS_PREPARING:
				transactionStatus = TransactionStatus.ACTIVE;
				break;
			case Status.STATUS_COMMITTING:
				transactionStatus = TransactionStatus.COMMITTING;
				break;
			case Status.STATUS_ROLLING_BACK:
				transactionStatus = TransactionStatus.ROLLING_BACK;
				break;
			case Status.STATUS_NO_TRANSACTION:
				transactionStatus = TransactionStatus.NOT_ACTIVE;
				break;
			case Status.STATUS_COMMITTED:
				transactionStatus = TransactionStatus.COMMITTED;
				break;
			case Status.STATUS_ROLLEDBACK:
				transactionStatus = TransactionStatus.ROLLED_BACK;
				break;
			case Status.STATUS_MARKED_ROLLBACK:
				transactionStatus = TransactionStatus.MARKED_ROLLBACK;
				break;
			default:
				break;
		}
		if ( transactionStatus == null ) {
			throw new TransactionException( "TransactionManager reported transaction status as unknwon" );
		}
		return transactionStatus;
	}

}
