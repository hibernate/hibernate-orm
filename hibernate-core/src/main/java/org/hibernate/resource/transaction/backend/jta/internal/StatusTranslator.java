/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.transaction.backend.jta.internal;

import jakarta.transaction.Status;

import org.hibernate.AssertionFailure;
import org.hibernate.resource.transaction.spi.TransactionStatus;

/**
 * @author Andrea Boriero
 */
public class StatusTranslator {

	public static TransactionStatus translate(int status) {
		return switch ( status ) {
			case Status.STATUS_ACTIVE -> TransactionStatus.ACTIVE;
			case Status.STATUS_PREPARED -> TransactionStatus.ACTIVE;
			case Status.STATUS_PREPARING -> TransactionStatus.ACTIVE;
			case Status.STATUS_COMMITTING -> TransactionStatus.COMMITTING;
			case Status.STATUS_ROLLING_BACK -> TransactionStatus.ROLLING_BACK;
			case Status.STATUS_NO_TRANSACTION -> TransactionStatus.NOT_ACTIVE;
			case Status.STATUS_COMMITTED -> TransactionStatus.COMMITTED;
			case Status.STATUS_ROLLEDBACK -> TransactionStatus.ROLLED_BACK;
			case Status.STATUS_MARKED_ROLLBACK -> TransactionStatus.MARKED_ROLLBACK;
			case Status.STATUS_UNKNOWN -> null;
			default -> throw new AssertionFailure( "Unknown transaction status code: " + status );
		};
	}

}
