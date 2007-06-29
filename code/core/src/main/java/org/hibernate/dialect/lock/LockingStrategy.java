package org.hibernate.dialect.lock;

import org.hibernate.engine.SessionImplementor;
import org.hibernate.StaleObjectStateException;
import org.hibernate.JDBCException;

import java.io.Serializable;

/**
 * A strategy abstraction for how locks are obtained in the underlying database.
 * <p/>
 * All locking provided implemenations assume the underlying database supports
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
	 * @param session The session from which the lock request originated
	 * @throws StaleObjectStateException Indicates an optimisitic lock failure
	 * as part of acquiring the requested database lock.
	 * @throws JDBCException
	 */
	public void lock(Serializable id, Object version, Object object, SessionImplementor session)
	throws StaleObjectStateException, JDBCException;
}
