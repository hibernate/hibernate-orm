package org.hibernate.test.util.jdbc;

import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.util.List;

/**
 * @author Gail Badner
 */
public interface PreparedStatementObserver {

	/**
	 * Called after a PreparedStatement is created.
	 *
	 * @param preparedStatement The created PreparedStatement
	 * @param sql The SQL used to create the PreparedStatement
	 */
	void preparedStatementCreated(PreparedStatement preparedStatement, String sql);

	/**
	 * Called after the specified method was invoked on the specified PreparedStatement.
	 *
	 * @param preparedStatement The PreparedStatement to which the Method has been invoked.
	 * @param method The Method that was invoked.
	 * @param args The arguments passed to the Method.
	 * @param invocationReturnValue The value returned by the Method invocation.
	 * @return The return value from the invocation.
	 */
	void preparedStatementMethodInvoked(
			PreparedStatement preparedStatement,
			Method method,
			Object[] args,
			Object invocationReturnValue);

	/**
	 * Called after the ConnectionProvider is stopped. Clears the recorded PreparedStatements and associated data.
	 */
	void connectionProviderStopped();

	/**
	 * Get one and only one PreparedStatement associated to the given SQL statement.
	 *
	 * @param sql SQL statement.
	 *
	 * @return matching PreparedStatement.
	 *
	 * @throws IllegalArgumentException If there is no matching PreparedStatement or multiple instances, an exception is being thrown.
	 */
	PreparedStatement getPreparedStatement(String sql);

	/**
	 * Get the PreparedStatements that are associated to the following SQL statement.
	 *
	 * @param sql SQL statement.
	 *
	 * @return list of recorded PreparedStatements matching the SQL statement.
	 */
	List<PreparedStatement> getPreparedStatements(String sql);

	/**
	 * Get the PreparedStatements that were executed since the last clear operation.
	 *
	 * @return list of recorded PreparedStatements.
	 */
	List<PreparedStatement> getPreparedStatements();
}