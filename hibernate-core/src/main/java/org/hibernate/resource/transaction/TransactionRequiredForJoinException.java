/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
