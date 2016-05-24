/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.jdbc.spi;

import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;

/**
 * Contract for something that controls a JdbcSessionContext.  The name comes from the
 * design idea of a JdbcSession which encapsulates this information, which we will hopefully
 * get back to later.
 *
 * The term "JDBC session" is taken from the SQL specification which calls a connection
 * and its associated transaction context a "session".
 *
 * @author Steve Ebersole
 */
public interface JdbcSessionOwner {

	JdbcSessionContext getJdbcSessionContext();

	JdbcConnectionAccess getJdbcConnectionAccess();

	/**
	 * Obtain the builder for TransactionCoordinator instances
	 *
	 * @return The TransactionCoordinatorBuilder
	 */
	TransactionCoordinator getTransactionCoordinator();

	/**
	 * A afterQuery-begin callback from the coordinator to its owner.
	 */
	void afterTransactionBegin();

	/**
	 * A beforeQuery-completion callback to the owner.
	 */
	void beforeTransactionCompletion();

	/**
	 * An afterQuery-completion callback to the owner.
	 *
	 * @param successful Was the transaction successful?
	 * @param delayed Is this a delayed afterQuery transaction completion call (aka afterQuery a timeout)?
	 */
	void afterTransactionCompletion(boolean successful, boolean delayed);

	void flushBeforeTransactionCompletion();

	/**
	 * Get the Session-level JDBC batch size.
	 * @return Session-level JDBC batch size
	 *
	 * @since 5.2
	 */
	Integer getJdbcBatchSize();
}
