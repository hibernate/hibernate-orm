/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock;

import org.hibernate.StaleObjectStateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.EventSource;

/**
 * A strategy abstraction for how locks are obtained in the underlying database.
 * <p>
 * All built-in implementations assume the underlying database supports at least
 * {@linkplain java.sql.Connection#TRANSACTION_READ_COMMITTED read-committed}
 * transaction isolation, and that the JDBC connection was obtained with at least
 * this isolation level.
 *
 * @see org.hibernate.dialect.Dialect#getLockingStrategy
 * @see org.hibernate.cfg.JdbcSettings#ISOLATION
 * @since 3.2
 *
 * @author Steve Ebersole
 */
public interface LockingStrategy {
	/**
	 * Acquire an appropriate type of lock on the underlying data that will
	 * endure until the end of the current transaction.
	 *
	 * @param id The id of the row to be locked
	 * @param version The current version (or null if not versioned)
	 * @param object The object logically being locked (currently not used)
	 * @param timeout timeout in milliseconds, 0 = no wait, -1 = wait indefinitely
	 * @param session The session from which the lock request originated
	 *
	 * @throws StaleObjectStateException Indicates an inability to locate the database row as part of acquiring
	 * the requested lock.
	 * @throws LockingStrategyException Indicates a failure in the lock attempt
	 *
	 * @deprecated Use {@link #lock(Object, Object, Object, int, SharedSessionContractImplementor)}
	 */
	@Deprecated(since = "7")
	default void lock(Object id, Object version, Object object, int timeout, EventSource session)
			throws StaleObjectStateException, LockingStrategyException {
		lock( id, version, object, timeout, (SharedSessionContractImplementor) session );
	}

	/**
	 * Acquire an appropriate type of lock on the underlying data that will
	 * endure until the end of the current transaction.
	 *
	 * @param id The id of the row to be locked
	 * @param version The current version (or null if not versioned)
	 * @param object The object logically being locked (currently not used)
	 * @param timeout timeout in milliseconds, 0 = no wait, -1 = wait indefinitely
	 * @param session The session from which the lock request originated
	 *
	 * @throws StaleObjectStateException Indicates an inability to locate the database row as part of acquiring
	 * the requested lock.
	 * @throws LockingStrategyException Indicates a failure in the lock attempt
	 */
	default void lock(Object id, Object version, Object object, int timeout, SharedSessionContractImplementor session)
			throws StaleObjectStateException, LockingStrategyException {
		if ( session instanceof EventSource eventSource ) {
			lock( id, version, object, timeout, eventSource );
		}
		else {
			throw new UnsupportedOperationException( "Optimistic locking strategies not supported in stateless session" );
		}
	}
}
