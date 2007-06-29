package org.hibernate.engine.transaction;

import org.hibernate.HibernateException;

import java.sql.Connection;

/**
 * Represents work that needs to be performed in a manner
 * which isolates it from any current application unit of
 * work transaction.
 *
 * @author Steve Ebersole
 */
public interface IsolatedWork {
	/**
	 * Perform the actual work to be done.
	 *
	 * @param connection The JDBC connection to use.
	 * @throws HibernateException
	 */
	public void doWork(Connection connection) throws HibernateException;
}
