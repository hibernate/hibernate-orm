/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.transaction.backend.jta.internal.synchronization;

/**
 * Defines "inflow" for JTA transactions from the perspective of Hibernate's registered JTA Synchronization
 * back into the TransactionCoordinator by means of the SynchronizationCallbackCoordinator.
 * <p>
 * That's a mouthful, :).  The way it works is like this...<ul>
 *     <li>
 *         Hibernate will register a JTA {@link jakarta.transaction.Synchronization} implementation
 *         ({@link RegisteredSynchronization}) which allows
 *         it to listen for completion of the JTA transaction.
 *     </li>
 *     <li>
 *         That RegisteredSynchronization is given a SynchronizationCallbackCoordinator which it uses
 *         to route the transaction completion calls back into Hibernate.  The SynchronizationCallbackCoordinator
 *         contract applies various behaviors around this process.  See the impls for details.
 *     </li>
 *     <li>
 *         The SynchronizationCallbackCoordinator is handed a SynchronizationCallbackTarget which is the specific
 *         means for it to "route the transaction completion calls back into Hibernate".  The SynchronizationCallbackTarget
 *         is most often the TransactionCoordinator impl or a direct delegate of the TransactionCoordinator impl.  In
 *         that sense, SynchronizationCallbackTarget is the contract between the SynchronizationCallbackCoordinator
 *         and the TransactionCoordinator.
 *     </li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public interface SynchronizationCallbackTarget {
	/**
	 * Is the callback target still active?  Generally this is checked by the caller prior to calling
	 * {@link #beforeCompletion} or {@link #afterCompletion}
	 *
	 * @return {@code true} indicates the target is active; {@code false} indicates it is not.
	 */
	boolean isActive();

	/**
	 * Callback of before-completion.
	 *
	 * @see jakarta.transaction.Synchronization#beforeCompletion
	 */
	void beforeCompletion();

	/**
	 * Callback of after-completion.
	 *
	 * @param successful Was the transaction successful?
	 *
	 * @see jakarta.transaction.Synchronization#afterCompletion
	 */
	void afterCompletion(boolean successful, boolean delayed);
}
