/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.spi;

import java.io.Serializable;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * A strategy abstraction for how locks are obtained in the underlying database.
 * <p/>
 * All provided locking implementations assume the underlying database supports
 * (and that the connection is in) at least read-committed transaction isolation.
 * The most glaring exclusion to this is HSQLDB which only offers support for
 * READ_UNCOMMITTED isolation.
 *
 * @since 6.0 - largely copied from legacy org.hibernate.dialect.lock.LockingStrategy
 * which has been around since 3.2
 *
 * @author Steve Ebersole
 */
public interface EntityLocker {
	// todo (6.0) - any additional Options info?
	//		- "scope"?
	//		- LockMode-by-alias map?
	//		- other flags (skip-locked, etc)?

	interface Options {
		/**
		 * Obtain the query timeout, in milliseconds.  0 = no wait; -1 = wait indefinitely
		 */
		int getTimeout();
	}

	/**
	 * Acquire an appropriate type of lock on the underlying data that will
	 * endure until the end of the current transaction.
	 *
	 * @param id The id of the row to be locked
	 * @param version The current version (or null if not versioned)
	 * @param object The object logically being locked (currently not used)
	 * @param session The session from which the lock request originated
	 */
	void lock(Serializable id, Object version, Object object, SharedSessionContractImplementor session, Options options);
}
