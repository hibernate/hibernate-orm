// $Id: StatementExecutor.java 8631 2005-11-21 17:02:24Z steveebersole $
package org.hibernate.hql.ast.exec;

import org.hibernate.HibernateException;
import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.SessionImplementor;

/**
 * Encapsulates the strategy required to execute various types of update, delete,
 * and insert statements issued through HQL.
 *
 * @author Steve Ebersole
 */
public interface StatementExecutor {

	public String[] getSqlStatements();

	/**
	 * Execute the sql managed by this executor using the given parameters.
	 *
	 * @param parameters Essentially bind information for this processing.
	 * @param session The session originating the request.
	 * @return The number of entities updated/deleted.
	 * @throws HibernateException
	 */
	public int execute(QueryParameters parameters, SessionImplementor session) throws HibernateException;

}
