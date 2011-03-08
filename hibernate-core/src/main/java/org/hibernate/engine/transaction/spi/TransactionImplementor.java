/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.transaction.spi;

import org.hibernate.Transaction;

/**
 * Additional contract for implementors of the Hibernate {@link Transaction} API.
 * 
 * @author Steve Ebersole
 */
public interface TransactionImplementor extends Transaction {
	/**
	 * Retrieve an isolation delegate appropriate for this transaction strategy.
	 *
	 * @return An isolation delegate.
	 */
	public IsolationDelegate createIsolationDelegate();

	/**
	 * Get the current state of this transaction's join status.
	 *
	 * @return The current join status
	 */
	public JoinStatus getJoinStatus();

	/**
	 * Mark a transaction as joinable
	 */
	public void markForJoin();

	/**
	 * Perform a join to the underlying transaction
	 */
	public void join();

	/**
	 * Reset this transaction's join status.
	 */
	public void resetJoinStatus();

	/**
	 * Make a best effort to mark the underlying transaction for rollback only.
	 */
	public void markRollbackOnly();

	/**
	 * Called after completion of the underlying transaction to signify the facade is no longer valid.
	 */
	public void invalidate();
}
