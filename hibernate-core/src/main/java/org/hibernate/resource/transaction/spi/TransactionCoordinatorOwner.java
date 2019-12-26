/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.transaction.spi;

import org.hibernate.resource.jdbc.spi.JdbcSessionOwner;

/**
 * Models an owner of a TransactionCoordinator.  Mainly used in 2 ways:<ul>
 *     <li>
 *         First to allow the coordinator to determine if its owner is still active (open, etc).
 *     </li>
 *     <li>
 *         Second is to allow the coordinator to dispatch before and after completion events to the owner
 *     </li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public interface TransactionCoordinatorOwner {
	/**
	 * Is the TransactionCoordinator owner considered active?
	 *
	 * @return {@code true} indicates the owner is still active; {@code false} indicates it is not.
	 */
	boolean isActive();

	/**
	 * Callback indicating recognition of entering into a transactional
	 * context whether that is explicitly via the Hibernate
	 * {@link org.hibernate.Transaction} API or via registration
	 * of Hibernate's JTA Synchronization impl with a JTA Transaction
	 */
	default void startTransactionBoundary() {
		getJdbcSessionOwner().startTransactionBoundary();
	}

	/**
	 * A after-begin callback from the coordinator to its owner.
	 */
	void afterTransactionBegin();

	/**
	 * A before-completion callback from the coordinator to its owner.
	 */
	void beforeTransactionCompletion();

	/**
	 * An after-completion callback from the coordinator to its owner.
	 *
	 * @param successful Was the transaction successful?
	 * @param delayed Is this delayed after transaction completion call (aka after a timeout)?
	 */
	void afterTransactionCompletion(boolean successful, boolean delayed);

	JdbcSessionOwner getJdbcSessionOwner();

	/**
	 * Set the effective transaction timeout period for the current transaction, in seconds.
	 *
	 * @param seconds The number of seconds before a time out should occur.
	 */
	void setTransactionTimeOut(int seconds);

	void flushBeforeTransactionCompletion();
}
