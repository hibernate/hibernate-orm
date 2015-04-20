/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.resource.transaction.backend.jta.internal.synchronization;

/**
 * Defines "inflow" for JTA transactions from the perspective of Hibernate's registered JTA Synchronization
 * back into the TransactionCoordinator by means of the SynchronizationCallbackCoordinator.
 * <p/>
 * That's a mouthful :)  The way it works is like this...<ul>
 *     <li>
 *         Hibernate will register a JTA {@link javax.transaction.Synchronization} implementation
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
	 * {@link #beforeCompletion()} or {@link #afterCompletion(boolean)}
	 *
	 * @return {@code true} indicates the target is active; {@code false} indicates it is not.
	 */
	boolean isActive();

	/**
	 * Callback of before-completion.
	 *
	 * @see javax.transaction.Synchronization#beforeCompletion
	 */
	void beforeCompletion();

	/**
	 * Callback of after-completion.
	 *
	 * @param successful Was the transaction successful?
	 *
	 * @see javax.transaction.Synchronization#afterCompletion
	 */
	void afterCompletion(boolean successful);
}
