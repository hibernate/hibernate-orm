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
	public boolean isActive();

	/**
	 * A after-begin callback from the coordinator to its owner.
	 */
	public void afterTransactionBegin();

	/**
	 * A before-completion callback from the coordinator to its owner.
	 */
	public void beforeTransactionCompletion();

	/**
	 * An after-completion callback from the coordinator to its owner.
	 *
	 * @param successful Was the transaction successful?
	 */
	public void afterTransactionCompletion(boolean successful);

	public JdbcSessionOwner getJdbcSessionOwner();

	/**
	 * Set the effective transaction timeout period for the current transaction, in seconds.
	 *
	 * @param seconds The number of seconds before a time out should occur.
	 */
	public void setTransactionTimeOut(int seconds);

}
