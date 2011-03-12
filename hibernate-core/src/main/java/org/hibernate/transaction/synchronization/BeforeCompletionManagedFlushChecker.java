/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.transaction.synchronization;

import javax.transaction.SystemException;
import javax.transaction.Transaction;

import org.hibernate.transaction.TransactionFactory;

/**
 * Contract for checking whether to perform a managed flush in a
 * {@link javax.transaction.Synchronization#beforeCompletion()} callback
 *
 * @author Steve Ebersole
 */
public interface BeforeCompletionManagedFlushChecker {
	/**
	 * Check whether we should perform the managed flush
	 *
	 * @param ctx The Hibernate "transaction context"
	 * @param jtaTransaction The JTA transaction
	 *
	 * @return True to indicate to perform the managed flush; false otherwise.
	 *
	 * @throws SystemException Can be thrown while accessing the JTA transaction; will result in transaction being
	 * marked for rollback (best effort).
	 */
	public boolean shouldDoManagedFlush(TransactionFactory.Context ctx, Transaction jtaTransaction)
			throws SystemException;
}
