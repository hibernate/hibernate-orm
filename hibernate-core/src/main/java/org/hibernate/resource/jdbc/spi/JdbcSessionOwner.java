/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.jdbc.spi;

import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.resource.transaction.TransactionCoordinatorBuilder;

/**
 * @author Steve Ebersole
 */
public interface JdbcSessionOwner {
	/**
	 * Obtain the builder for TransactionCoordinator instances
	 *
	 * @return The TransactionCoordinatorBuilder
	 */
	public TransactionCoordinatorBuilder getTransactionCoordinatorBuilder();

	public JdbcSessionContext getJdbcSessionContext();

	public JdbcConnectionAccess getJdbcConnectionAccess();

	/**
	 * A after-begin callback from the coordinator to its owner.
	 */
	public void afterTransactionBegin();

	/**
	 * A before-completion callback to the owner.
	 */
	public void beforeTransactionCompletion();

	/**
	 * An after-completion callback to the owner.
	 *
	 * @param successful Was the transaction successful?
	 * @param delayed Is this a delayed after transaction completion call (aka after a timeout)?
	 */
	public void afterTransactionCompletion(boolean successful, boolean delayed);

	public void flushBeforeTransactionCompletion();
}
