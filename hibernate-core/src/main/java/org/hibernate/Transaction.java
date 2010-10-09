/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate;

import javax.transaction.Synchronization;

/**
 * Allows the application to define units of work, while
 * maintaining abstraction from the underlying transaction
 * implementation (eg. JTA, JDBC).<br>
 * <br>
 * A transaction is associated with a <tt>Session</tt> and is
 * usually instantiated by a call to <tt>Session.beginTransaction()</tt>.
 * A single session might span multiple transactions since
 * the notion of a session (a conversation between the application
 * and the datastore) is of coarser granularity than the notion of
 * a transaction. However, it is intended that there be at most one
 * uncommitted <tt>Transaction</tt> associated with a particular
 * <tt>Session</tt> at any time.<br>
 * <br>
 * Implementors are not intended to be threadsafe.
 *
 * @see Session#beginTransaction()
 * @see org.hibernate.transaction.TransactionFactory
 * @author Anton van Straaten
 */
public interface Transaction {
	
	/**
	 * Begin a new transaction.
	 */
	public void begin() throws HibernateException;

	/**
	 * Flush the associated <tt>Session</tt> and end the unit of work (unless
	 * we are in {@link FlushMode#MANUAL}.
	 * </p>
	 * This method will commit the underlying transaction if and only
	 * if the underlying transaction was initiated by this object.
	 *
	 * @throws HibernateException
	 */
	public void commit() throws HibernateException;

	/**
	 * Force the underlying transaction to roll back.
	 *
	 * @throws HibernateException
	 */
	public void rollback() throws HibernateException;

	/**
	 * Was this transaction rolled back or set to rollback only?
	 * <p/>
	 * This only accounts for actions initiated from this local transaction.
	 * If, for example, the underlying transaction is forced to rollback via
	 * some other means, this method still reports false because the rollback
	 * was not initiated from here.
	 *
	 * @return boolean True if the transaction was rolled back via this
	 * local transaction; false otherwise.
	 * @throws HibernateException
	 */
	public boolean wasRolledBack() throws HibernateException;

	/**
	 * Check if this transaction was successfully committed.
	 * <p/>
	 * This method could return <tt>false</tt> even after successful invocation
	 * of {@link #commit}.  As an example, JTA based strategies no-op on
	 * {@link #commit} calls if they did not start the transaction; in that case,
	 * they also report {@link #wasCommitted} as false.
	 *
	 * @return boolean True if the transaction was (unequivocally) committed
	 * via this local transaction; false otherwise.
	 * @throws HibernateException
	 */
	public boolean wasCommitted() throws HibernateException;
	
	/**
	 * Is this transaction still active?
	 * <p/>
	 * Again, this only returns information in relation to the
	 * local transaction, not the actual underlying transaction.
	 *
	 * @return boolean Treu if this local transaction is still active.
	 */
	public boolean isActive() throws HibernateException;

	/**
	 * Register a user synchronization callback for this transaction.
	 *
	 * @param synchronization The Synchronization callback to register.
	 * @throws HibernateException
	 */
	public void registerSynchronization(Synchronization synchronization) 
	throws HibernateException;

	/**
	 * Set the transaction timeout for any transaction started by
	 * a subsequent call to <tt>begin()</tt> on this instance.
	 *
	 * @param seconds The number of seconds before a timeout.
	 */
	public void setTimeout(int seconds);
}
