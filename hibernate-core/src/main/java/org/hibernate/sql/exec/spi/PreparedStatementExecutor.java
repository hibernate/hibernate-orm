/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.spi;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.QueryOptions;

/**
 * General contract for executing a PreparedStatement and consuming the "results" of that
 * execution.  That might mean reading the rows of a {@link ResultSet} obtained from executing
 * a query.  Or in the case of a {@link org.hibernate.ScrollableResults} e.g. it might just
 * mean holding the results open and handing them to the ScrollableResults object.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface PreparedStatementExecutor {
	// todo : ideally this also caters to consuming ProcedureCall executions as well.
	//		another option would be a CallableStatementExecutor (corollary to PreparedStatementExecutor)
	// 		and a ProcedureCallExecutor (corollary to SqlTreeExecutor)

	/**
	 * Do the consumption
	 *
	 * @param ps The PreparedStatement that the ResultSet was obtained from (mainly
	 * used to interact with the ResourceRegistry)
	 * @param queryOptions
	 * @param session
	 *
	 * @return
	 *
	 * @throws SQLException
	 */
	ResultSet execute(
			PreparedStatement ps,
			QueryOptions queryOptions,
			SharedSessionContractImplementor session) throws SQLException;
}
