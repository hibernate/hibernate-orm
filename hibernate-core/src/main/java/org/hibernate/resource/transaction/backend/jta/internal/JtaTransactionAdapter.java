/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.transaction.backend.jta.internal;

import org.hibernate.resource.transaction.spi.TransactionStatus;

/**
 * Adapter for abstracting the physical means of interacting with JTA transactions.
 * <p>
 * JTA transactions can concretely be interacted with through {@link jakarta.transaction.UserTransaction}
 * or {@link jakarta.transaction.Transaction} depending on environment and situation.  This adapter hides
 * this difference.
 *
 * @author Steve Ebersole
 */
public interface JtaTransactionAdapter {
	/**
	 * Call begin on the underlying transaction object
	 */
	void begin();

	/**
	 * Call commit on the underlying transaction object
	 */
	void commit();

	/**
	 * Call rollback on the underlying transaction object
	 */
	void rollback();

	TransactionStatus getStatus();

	void markRollbackOnly();

	void setTimeOut(int seconds);
}
