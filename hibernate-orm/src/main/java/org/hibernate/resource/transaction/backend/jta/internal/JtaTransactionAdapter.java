/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.transaction.backend.jta.internal;

import org.hibernate.resource.transaction.spi.TransactionStatus;

/**
 * Adapter for abstracting the physical means of interacting with JTA transactions.
 * <p/>
 * JTA transactions can concretely be interacted with through {@link javax.transaction.UserTransaction}
 * or {@link javax.transaction.Transaction} depending on environment and situation.  This adapter hides
 * this difference.
 *
 * @author Steve Ebersole
 */
public interface JtaTransactionAdapter {
	/**
	 * Call begin on the underlying transaction object
	 */
	public void begin();

	/**
	 * Call commit on the underlying transaction object
	 */
	public void commit();

	/**
	 * Call rollback on the underlying transaction object
	 */
	public void rollback();

	public TransactionStatus getStatus();

	public void markRollbackOnly();

	public void setTimeOut(int seconds);
}
