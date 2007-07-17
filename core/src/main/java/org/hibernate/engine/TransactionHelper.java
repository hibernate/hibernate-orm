//$Id: TransactionHelper.java 9056 2006-01-13 19:40:15Z steveebersole $
package org.hibernate.engine;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.engine.transaction.IsolatedWork;
import org.hibernate.engine.transaction.Isolater;
import org.hibernate.exception.JDBCExceptionHelper;

/**
 * Allows work to be done outside the current transaction, by suspending it,
 * and performing work in a new transaction
 * 
 * @author Emmanuel Bernard
 */
public abstract class TransactionHelper {

	// todo : remove this and just have subclasses use Isolater/IsolatedWork directly...

	/**
	 * The work to be done
	 */
	protected abstract Serializable doWorkInCurrentTransaction(Connection conn, String sql) throws SQLException;

	/**
	 * Suspend the current transaction and perform work in a new transaction
	 */
	public Serializable doWorkInNewTransaction(final SessionImplementor session)
	throws HibernateException {
		class Work implements IsolatedWork {
			Serializable generatedValue;
			public void doWork(Connection connection) throws HibernateException {
				String sql = null;
				try {
					generatedValue = doWorkInCurrentTransaction( connection, sql );
				}
				catch( SQLException sqle ) {
					throw JDBCExceptionHelper.convert(
							session.getFactory().getSQLExceptionConverter(),
							sqle,
							"could not get or update next value",
							sql
						);
				}
			}
		}
		Work work = new Work();
		Isolater.doIsolatedWork( work, session );
		return work.generatedValue;
	}
}
