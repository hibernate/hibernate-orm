/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate;

import javax.transaction.Synchronization;

import org.hibernate.engine.transaction.spi.LocalStatus;

/**
 * Defines the contract for abstracting applications from the configured underlying means of transaction management.
 * Allows the application to define units of work, while maintaining abstraction from the underlying transaction
 * implementation (eg. JTA, JDBC).
 * <p/>
 * A transaction is associated with a {@link Session} and is usually initiated by a call to
 * {@link org.hibernate.Session#beginTransaction()}.  A single session might span multiple transactions since
 * the notion of a session (a conversation between the application and the datastore) is of coarser granularity than
 * the notion of a transaction.  However, it is intended that there be at most one uncommitted transaction associated
 * with a particular {@link Session} at any time.
 * <p/>
 * Implementers are not intended to be thread-safe.
 *
 * @author Anton van Straaten
 * @author Steve Ebersole
 */
public interface Transaction {
	/**
	 * Is this transaction the initiator of any underlying transaction?
	 *
	 * @return {@code true} if this transaction initiated the underlying transaction; {@code false} otherwise.
	 */
	public boolean isInitiator();

	/**
	 * Begin this transaction.  No-op if the transaction has already been begun.  Note that this is not necessarily
	 * symmetrical since usually multiple calls to {@link #commit} or {@link #rollback} will error.
	 *
	 * @throws HibernateException Indicates a problem beginning the transaction.
	 */
	public void begin();

	/**
	 * Commit this transaction.  This might entail a number of things depending on the context:<ul>
	 *     <li>
	 *         If this transaction is the {@link #isInitiator initiator}, {@link Session#flush} the {@link Session}
	 *         with which it is associated (unless {@link Session} is in {@link FlushMode#MANUAL}).
	 *     </li>
	 *     <li>
	 *         If this transaction is the {@link #isInitiator initiator}, commit the underlying transaction.
	 *     </li>
	 *     <li>
	 *         Coordinate various callbacks
	 *     </li>
	 * </ul>
	 *
	 * @throws HibernateException Indicates a problem committing the transaction.
	 */
	public void commit();

	/**
	 * Rollback this transaction.  Either rolls back the underlying transaction or ensures it cannot later commit
	 * (depending on the actual underlying strategy).
	 *
	 * @throws HibernateException Indicates a problem rolling back the transaction.
	 */
	public void rollback();

	/**
	 * Get the current local status of this transaction.
	 * <p/>
	 * This only accounts for the local view of the transaction status.  In other words it does not check the status
	 * of the actual underlying transaction.
	 *
	 * @return The current local status.
	 */
	public LocalStatus getLocalStatus();

	/**
	 * Is this transaction still active?
	 * <p/>
	 * Answers on a best effort basis.  For example, in the case of JDBC based transactions we cannot know that a
	 * transaction is active when it is initiated directly through the JDBC {@link java.sql.Connection}, only when
	 * it is initiated from here.
	 *
	 * @return {@code true} if the transaction is still active; {@code false} otherwise.
	 *
	 * @throws HibernateException Indicates a problem checking the transaction status.
	 */
	public boolean isActive();

	/**
	 * Is Hibernate participating in the underlying transaction?
	 * <p/>
	 * Generally speaking this will be the same as {@link #isActive()}.
	 * 
	 * @return {@code true} if Hibernate is known to be participating in the underlying transaction; {@code false}
	 * otherwise.
	 */
	public boolean isParticipating();

	/**
	 * Was this transaction committed?
	 * <p/>
	 * Answers on a best effort basis.  For example, in the case of JDBC based transactions we cannot know that a
	 * transaction was committed when the commit was performed directly through the JDBC {@link java.sql.Connection},
	 * only when the commit was done from this.
	 *
	 * @return {@code true} if the transaction is rolled back; {@code false} otherwise.
	 *
	 * @throws HibernateException Indicates a problem checking the transaction status.
	 */
	@SuppressWarnings( {"UnusedDeclaration"})
	public boolean wasCommitted();

	/**
	 * Was this transaction rolled back or set to rollback only?
	 * <p/>
	 * Answers on a best effort basis.  For example, in the case of JDBC based transactions we cannot know that a
	 * transaction was rolled back when rollback was performed directly through the JDBC {@link java.sql.Connection},
	 * only when it was rolled back  from here.
	 *
	 * @return {@literal true} if the transaction is rolled back; {@literal false} otherwise.
	 *
	 * @throws HibernateException Indicates a problem checking the transaction status.
	 */
	@SuppressWarnings( {"UnusedDeclaration"})
	public boolean wasRolledBack();

	/**
	 * Register a user synchronization callback for this transaction.
	 *
	 * @param synchronization The Synchronization callback to register.
	 *
	 * @throws HibernateException Indicates a problem registering the synchronization.
	 */
	public void registerSynchronization(Synchronization synchronization) throws HibernateException;

	/**
	 * Set the transaction timeout for any transaction started by a subsequent call to {@link #begin} on this instance.
	 *
	 * @param seconds The number of seconds before a timeout.
	 */
	public void setTimeout(int seconds);

	/**
	 * Retrieve the transaction timeout set for this transaction.  A negative indicates no timeout has been set.
	 *
	 * @return The timeout, in seconds.
	 */
	public int getTimeout();
}
