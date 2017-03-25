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
	 * A afterQuery-begin callback from the coordinator to its owner.
	 */
	void afterTransactionBegin();

	/**
	 * A beforeQuery-completion callback from the coordinator to its owner.
	 */
	void beforeTransactionCompletion();

	/**
	 * An afterQuery-completion callback from the coordinator to its owner.
	 *
	 * @param successful Was the transaction successful?
	 * @param delayed Is this a delayed afterQuery transaction completion call (aka afterQuery a timeout)?
	 */
	void afterTransactionCompletion(boolean successful, boolean delayed);

	JdbcSessionOwner getJdbcSessionOwner();

	/**
	 * Set the effective transaction timeout period for the current transaction, in seconds.
	 *
	 * @param seconds The number of seconds beforeQuery a time out should occur.
	 */
	void setTransactionTimeOut(int seconds);

	void flushBeforeTransactionCompletion();
}
