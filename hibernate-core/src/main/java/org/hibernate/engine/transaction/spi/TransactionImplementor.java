/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.transaction.spi;

import org.hibernate.HibernateException;
import org.hibernate.Transaction;
import org.hibernate.resource.transaction.spi.TransactionStatus;

/**
 * Defines the "internal contract" for an implementation of {@link Transaction}.
 *
 * @author Steve Ebersole
 */
public interface TransactionImplementor extends Transaction {
	/**
	 * Indicate whether a resource transaction is in progress.
	 *
	 * @param isMarkedRollbackConsideredActive whether to consider {@link TransactionStatus#MARKED_ROLLBACK} as active.
	 *
	 * @return boolean indicating whether transaction is in progress
	 * @throws HibernateException if an unexpected error condition is encountered.
	 */
	boolean isActive(boolean isMarkedRollbackConsideredActive);
}
