/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.dialect.lock;

import java.io.Serializable;

import org.hibernate.StaleObjectStateException;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * A strategy abstraction for how locks are obtained in the underlying database.
 * <p/>
 * All locking provided implementations assume the underlying database supports
 * (and that the connection is in) at least read-committed transaction isolation.
 * The most glaring exclusion to this is HSQLDB which only offers support for
 * READ_UNCOMMITTED isolation.
 *
 * @see org.hibernate.dialect.Dialect#getLockingStrategy
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
	 */
	public void lock(Serializable id, Object version, Object object, int timeout, SessionImplementor session)
			throws StaleObjectStateException, LockingStrategyException;
}
