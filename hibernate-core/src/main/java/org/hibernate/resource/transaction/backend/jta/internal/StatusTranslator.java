/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) {DATE}, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
