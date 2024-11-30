/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.transaction.backend.jta.internal;

import jakarta.transaction.Status;

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
			throw new TransactionException( "TransactionManager reported transaction status as unknown" );
		}
		return transactionStatus;
	}

}
