/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.internal;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.ResultSetReturn;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;

/**
 * Standard implementation of the ResultSetReturn contract
 *
 * @author Brett Meyer
 */
public class ResultSetReturnImpl implements ResultSetReturn {
	private final JdbcCoordinator jdbcCoordinator;

	private final Dialect dialect;
	private final SqlStatementLogger sqlStatementLogger;
	private final SqlExceptionHelper sqlExceptionHelper;

	private boolean isJdbc4 = true;

	/**
	 * Constructs a ResultSetReturnImpl
	 *
	 * @param jdbcCoordinator The JdbcCoordinator
	 */
	public ResultSetReturnImpl(JdbcCoordinator jdbcCoordinator) {
		this.jdbcCoordinator = jdbcCoordinator;

		final JdbcServices jdbcServices = jdbcCoordinator.getJdbcSessionOwner()
				.getJdbcSessionContext()
				.getServiceRegistry()
				.getService( JdbcServices.class );

		this.dialect = jdbcServices.getDialect();

		this.sqlStatementLogger = jdbcServices.getSqlStatementLogger();
		this.sqlExceptionHelper = jdbcServices.getSqlExceptionHelper();
	}

	@Override
	public ResultSet extract(PreparedStatement statement) {
		// IMPL NOTE : SQL logged by caller
		if ( isTypeOf( statement, CallableStatement.class ) ) {
			// We actually need to extract from Callable statement.  Although
			// this seems needless, Oracle can return an
			// OracleCallableStatementWrapper that finds its way to this method,
			// rather than extract(CallableStatement).  See HHH-8022.
			final CallableStatement callableStatement = (CallableStatement) statement;
			return extract( callableStatement );
		}
		try {
			final ResultSet rs;
			try {
				jdbcExecuteStatementStart();
				rs = statement.executeQuery();
			}
			finally {
				jdbcExecuteStatementEnd();
			}
			postExtract( rs, statement );
			return rs;
		}
		catch (SQLException e) {
			throw sqlExceptionHelper.convert( e, "could not extract ResultSet" );
		}
	}

	private void jdbcExecuteStatementEnd() {
		jdbcCoordinator.getJdbcSessionOwner().getJdbcSessionContext().getObserver().jdbcExecuteStatementEnd();
	}

	private void jdbcExecuteStatementStart() {
		jdbcCoordinator.getJdbcSessionOwner().getJdbcSessionContext().getObserver().jdbcExecuteStatementStart();
	}

	private boolean isTypeOf(final Statement statement, final Class<? extends Statement> type) {
		if ( isJdbc4 ) {
			try {
				// This is "more correct" than #isInstance, but not always supported.
				return statement.isWrapperFor( type );
			}
			catch (SQLException e) {
				// No operation
			}
			catch (Throwable e) {
				// No operation. Note that this catches more than just SQLException to
				// cover edge cases where a driver might throw an UnsupportedOperationException, AbstractMethodError,
				// etc.  If so, skip permanently.
				isJdbc4 = false;
			}
		}
		return type.isInstance( statement );
	}

	@Override
	public ResultSet extract(CallableStatement callableStatement) {
		// IMPL NOTE : SQL logged by caller
		try {
			final ResultSet rs;
			try {
				jdbcExecuteStatementStart();
				rs = dialect.getResultSet( callableStatement );
			}
			finally {
				jdbcExecuteStatementEnd();
			}
			postExtract( rs, callableStatement );
			return rs;
		}
		catch (SQLException e) {
			throw sqlExceptionHelper.convert( e, "could not extract ResultSet" );
		}
	}

	@Override
	public ResultSet extract(Statement statement, String sql) {
		sqlStatementLogger.logStatement( sql );
		try {
			final ResultSet rs;
			try {
				jdbcExecuteStatementStart();
				rs = statement.executeQuery( sql );
			}
			finally {
				jdbcExecuteStatementEnd();
			}
			postExtract( rs, statement );
			return rs;
		}
		catch (SQLException e) {
			throw sqlExceptionHelper.convert( e, "could not extract ResultSet" );
		}
	}

	@Override
	public ResultSet execute(PreparedStatement statement) {
		// sql logged by StatementPreparerImpl
		try {
			final ResultSet rs;
			try {
				jdbcExecuteStatementStart();
				if ( !statement.execute() ) {
					while ( !statement.getMoreResults() && statement.getUpdateCount() != -1 ) {
						// do nothing until we hit the resultset
					}
				}
				rs = statement.getResultSet();
			}
			finally {
				jdbcExecuteStatementEnd();
			}
			postExtract( rs, statement );
			return rs;
		}
		catch (SQLException e) {
			throw sqlExceptionHelper.convert( e, "could not execute statement" );
		}
	}

	@Override
	public ResultSet execute(Statement statement, String sql) {
		sqlStatementLogger.logStatement( sql );
		try {
			final ResultSet rs;
			try {
				jdbcExecuteStatementStart();
				if ( !statement.execute( sql ) ) {
					while ( !statement.getMoreResults() && statement.getUpdateCount() != -1 ) {
						// do nothing until we hit the resultset
					}
				}
				rs = statement.getResultSet();
			}
			finally {
				jdbcExecuteStatementEnd();
			}
			postExtract( rs, statement );
			return rs;
		}
		catch (SQLException e) {
			throw sqlExceptionHelper.convert( e, "could not execute statement" );
		}
	}

	@Override
	public int executeUpdate(PreparedStatement statement) {
		try {
			jdbcExecuteStatementStart();
			return statement.executeUpdate();
		}
		catch (SQLException e) {
			throw sqlExceptionHelper.convert( e, "could not execute statement" );
		}
		finally {
			jdbcExecuteStatementEnd();
		}
	}

	@Override
	public int executeUpdate(Statement statement, String sql) {
		sqlStatementLogger.logStatement( sql );
		try {
			jdbcExecuteStatementStart();
			return statement.executeUpdate( sql );
		}
		catch (SQLException e) {
			throw sqlExceptionHelper.convert( e, "could not execute statement" );
		}
		finally {
			jdbcExecuteStatementEnd();
		}
	}

	private void postExtract(ResultSet rs, Statement st) {
		if ( rs != null ) {
			jdbcCoordinator.getResourceRegistry().register( rs, st );
		}
	}

}
