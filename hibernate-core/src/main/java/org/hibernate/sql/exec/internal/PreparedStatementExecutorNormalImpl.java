/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.internal;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.exec.spi.PreparedStatementExecutor;

/**
 * Normal PreparedStatement execution which:<ol>
 *     <li>calls {@link PreparedStatement#executeQuery()}</li>
 *     <li>immediately reads all the rows in the ResultSet returning a List of the transformed results</li>
 * </ol>
 *
 * @author Steve Ebersole
 */
public class PreparedStatementExecutorNormalImpl implements PreparedStatementExecutor {
	/**
	 * Singleton access
	 */
	public static final PreparedStatementExecutorNormalImpl INSTANCE = new PreparedStatementExecutorNormalImpl();

	@Override
	public ResultSet execute(
			PreparedStatement preparedStatement,
			QueryOptions queryOptions,
			SharedSessionContractImplementor session) throws SQLException {
		return preparedStatement.executeQuery();
	}
}
