/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.spi;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Contract for extracting {@link ResultSet}s from {@link Statement}s, executing the statements,
 * managing resources, and logging statement calls.
 * <p>
 * Generally the methods here for dealing with {@link CallableStatement} are extremely limited
 *
 * @author Brett Meyer
 * @author Steve Ebersole
 *
 * @see JdbcCoordinator#getResultSetReturn()
 */
public interface ResultSetReturn {

	/**
	 * Extract the {@link ResultSet} from the {@link PreparedStatement}.
	 * <p>
	 * If client passes {@link CallableStatement} reference, this method calls {@link #extract(CallableStatement)}
	 * internally.  Otherwise, {@link PreparedStatement#executeQuery()} is called.
	 *
	 * @param statement The {@link PreparedStatement} from which to extract the {@link ResultSet}
	 *
	 * @return The extracted ResultSet
	 *
	 * @deprecated Use {@link #extract(PreparedStatement, String)} instead
	 */
	@Deprecated(forRemoval = true)
	ResultSet extract(PreparedStatement statement);

	/**
	 * Extract the {@link ResultSet} from the {@link PreparedStatement}.
	 * <p>
	 * If client passes {@link CallableStatement} reference, this method calls {@link #extract(CallableStatement)}
	 * internally.  Otherwise, {@link PreparedStatement#executeQuery()} is called.
	 *
	 * @param statement The {@link PreparedStatement} from which to extract the {@link ResultSet}
	 *
	 * @return The extracted {@link ResultSet}
	 */
	ResultSet extract(PreparedStatement statement, String sql);

	/**
	 * Extract the {@link ResultSet}  from the {@link CallableStatement}.  Note that this is the limited legacy
	 * form which delegates to {@link org.hibernate.dialect.Dialect#getResultSet}.  Better option is to integrate
	 * {@link org.hibernate.procedure.ProcedureCall}-like hooks
	 *
	 * @param callableStatement The {@link CallableStatement} from which to extract the {@link ResultSet}
	 *
	 * @return The extracted {@link ResultSet}
	 *
	 * @deprecated Use {@link #extract(PreparedStatement, String)} instead
	 */
	@Deprecated(forRemoval = true)
	ResultSet extract(CallableStatement callableStatement);
	
	/**
	 * Performs the given SQL statement, expecting a {@link ResultSet} in return
	 *
	 * @param statement The JDBC {@link Statement} object to use
	 * @param sql The SQL to execute
	 *
	 * @return The resulting {@link ResultSet}
	 */
	ResultSet extract(Statement statement, String sql);
	
	/**
	 * Execute the {@link PreparedStatement} return its first {@link ResultSet}, if any.
	 * If there is no {@link ResultSet}, returns {@code null}
	 *
	 * @param statement The {@link PreparedStatement} to execute
	 *
	 * @return The extracted {@link ResultSet}, or {@code null}
	 *
	 * @deprecated Use {@link #execute(PreparedStatement, String)} instead
	 */
	@Deprecated(forRemoval = true)
	ResultSet execute(PreparedStatement statement);

	/**
	 * Execute the {@link PreparedStatement} return its first {@link ResultSet}, if any.
	 * If there is no {@link ResultSet}, returns {@code null}
	 *
	 * @param statement The {@link PreparedStatement} to execute
	 * @param sql For error reporting
	 *
	 * @return The extracted {@link ResultSet}, or {@code null}
	 */
	ResultSet execute(PreparedStatement statement, String sql);

	/**
	 * Performs the given SQL statement, returning its first {@link ResultSet}, if any.
	 * If there is no {@link ResultSet}, returns {@code null}
	 *
	 * @param statement The JDBC {@link Statement} object to use
	 * @param sql The SQL to execute
	 *
	 * @return The extracted {@link ResultSet}, or {@code null}
	 */
	ResultSet execute(Statement statement, String sql);
	
	/**
	 * Execute the {@link PreparedStatement}, returning its "affected row count".
	 *
	 * @param statement The {@link PreparedStatement} to execute
	 *
	 * @return The {@link PreparedStatement#executeUpdate()} result
	 *
	 * @deprecated Use {@link #executeUpdate(PreparedStatement, String)} instead
	 */
	@Deprecated(forRemoval = true)
	int executeUpdate(PreparedStatement statement);

	/**
	 * Execute the {@link PreparedStatement}, returning its "affected row count".
	 *
	 * @param statement The {@link PreparedStatement} to execute
	 * @param sql For error reporting
	 *
	 * @return The {@link PreparedStatement#executeUpdate()} result
	 */
	int executeUpdate(PreparedStatement statement, String sql);
	
	/**
	 * Execute the given SQL statement returning its "affected row count".
	 *
	 * @param statement The JDBC {@link Statement} object to use
	 * @param sql The SQL to execute
	 *
	 * @return The {@link PreparedStatement#executeUpdate(String)} result
	 */
	int executeUpdate(Statement statement, String sql);
}
