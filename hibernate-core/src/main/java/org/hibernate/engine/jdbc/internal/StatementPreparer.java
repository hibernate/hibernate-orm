/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.engine.jdbc.internal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.Logger;
import org.hibernate.ScrollMode;
import org.hibernate.TransactionException;
import org.hibernate.cfg.Settings;
import org.hibernate.engine.jdbc.internal.proxy.ProxyBuilder;
import org.hibernate.engine.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.engine.jdbc.spi.SQLExceptionHelper;

/**
 * Prepares statements.
 *
 * @author Gail Badner
 */
public class StatementPreparer {

    private static final Logger LOG = org.jboss.logging.Logger.getMessageLogger(Logger.class, Logger.class.getPackage().getName());

	// TODO: Move JDBC settings into a different object...
	private final Settings settings;
	private final Connection proxiedConnection;
	private final SQLExceptionHelper sqlExceptionHelper;

	private long transactionTimeout = -1;
	boolean isTransactionTimeoutSet;

	/**
	 * Constructs a StatementPreparer object
	 * @param logicalConnection - the logical connection
	 * @param settings - contains settings configured for preparing statements
	 */
	public StatementPreparer(LogicalConnectionImplementor logicalConnection, Settings settings) {
		this.settings = settings;
		proxiedConnection = ProxyBuilder.buildConnection( logicalConnection );
		sqlExceptionHelper = logicalConnection.getJdbcServices().getSqlExceptionHelper();
	}

	private abstract class StatementPreparation {
		private final String sql;
		protected abstract PreparedStatement doPrepare() throws SQLException;
		public StatementPreparation(String sql) {
			this.sql = sql;
		}
		public String getSql() {
			return sql;
		}
		public void postProcess(PreparedStatement preparedStatement) throws SQLException {
			setStatementTimeout( preparedStatement );
		}
		public PreparedStatement prepareAndPostProcess() {
			try {
				PreparedStatement ps = doPrepare();
				postProcess( ps );
				return ps;
			}
			catch ( SQLException sqle ) {
                LOG.sqlExceptionEscapedProxy(sqle);
				throw sqlExceptionHelper.convert(
						sqle, "could not prepare statement", sql
				);
			}
		}
	}

	private abstract class QueryStatementPreparation extends StatementPreparation {
		QueryStatementPreparation(String sql) {
			super( sql );
		}
		@Override
        public void postProcess(PreparedStatement preparedStatement) throws SQLException {
			super.postProcess( preparedStatement );
			setStatementFetchSize( preparedStatement );
		}
	}

	public void close() {
		try {
			proxiedConnection.close();
		}
		catch (SQLException sqle) {
            LOG.sqlExceptionEscapedProxy(sqle);
			throw sqlExceptionHelper.convert( sqle, "could not close connection proxy" );
		}
	}

	/**
	 * Prepare a statement. If configured, the query timeout is set.
	 * <p/>
	 * If not explicitly closed via {@link java.sql.PreparedStatement#close()},
	 * it will be released when the session is closed or disconnected.
	 *
	 * @param sql - the SQL for the statement to be prepared
	 * @param isCallable - true, if a callable statement is to be prepared
	 * @return the prepared statement
	 */
	public PreparedStatement prepareStatement(String sql, final boolean isCallable) {
		StatementPreparation statementPreparation = new StatementPreparation( sql ) {
			@Override
            public PreparedStatement doPrepare() throws SQLException {
				return isCallable ?
						proxiedConnection.prepareCall( getSql() ) :
						proxiedConnection.prepareStatement( getSql() );
			}
		};
		return statementPreparation.prepareAndPostProcess();
	}

	/**
	 * Get a prepared statement to use for inserting / deleting / updating,
	 * using JDBC3 getGeneratedKeys ({@link java.sql.Connection#prepareStatement(String, int)}).
	 * If configured, the query timeout is set.
	 * <p/>
	 * If not explicitly closed via {@link java.sql.PreparedStatement#close()},
	 * it will be released when the session is closed or disconnected.

	 * @param sql - the SQL for the statement to be prepared
	 * @param autoGeneratedKeys - a flag indicating whether auto-generated
	 *        keys should be returned; one of
     *        <code>PreparedStatement.RETURN_GENERATED_KEYS</code> or
     *	      <code>Statement.NO_GENERATED_KEYS</code>
	 * @return the prepared statement
	 */
	public PreparedStatement prepareStatement(String sql, final int autoGeneratedKeys)
			throws HibernateException {
		if ( autoGeneratedKeys == Statement.RETURN_GENERATED_KEYS ) {
			checkAutoGeneratedKeysSupportEnabled();
		}
		StatementPreparation statementPreparation = new StatementPreparation( sql ) {
			@Override
            public PreparedStatement doPrepare() throws SQLException {
				return proxiedConnection.prepareStatement( getSql(), autoGeneratedKeys );
			}
		};
		return statementPreparation.prepareAndPostProcess();
	}

	/**
	 * Get a prepared statement to use for inserting / deleting / updating.
	 * using JDBC3 getGeneratedKeys ({@link java.sql.Connection#prepareStatement(String, String[])}).
	 * If configured, the query timeout is set.
	 * <p/>
	 * If not explicitly closed via {@link java.sql.PreparedStatement#close()},
	 * it will be released when the session is closed or disconnected.
	 */
	public PreparedStatement prepareStatement(String sql, final String[] columnNames) {
		checkAutoGeneratedKeysSupportEnabled();
		StatementPreparation preparation = new StatementPreparation( sql ) {
			@Override
            public PreparedStatement doPrepare() throws SQLException {
				return proxiedConnection.prepareStatement( getSql(), columnNames );
			}
		};
		return preparation.prepareAndPostProcess();
	}

	private void checkAutoGeneratedKeysSupportEnabled() {
		if ( ! settings.isGetGeneratedKeysEnabled() ) {
			throw new AssertionFailure("getGeneratedKeys() support is not enabled");
		}
	}

	/**
	 * Get a prepared statement for use in loading / querying.
	 * If configured, the query timeout and statement fetch size are set.
	 * <p/>
	 * If not explicitly closed via {@link java.sql.PreparedStatement#close()},
	 * it will be released when the session is closed or disconnected.
	 */
	public PreparedStatement prepareQueryStatement(
			String sql,
			final boolean isCallable
	) {
		StatementPreparation prep = new QueryStatementPreparation( sql ) {
			@Override
            public PreparedStatement doPrepare() throws SQLException {
				return isCallable ?
						proxiedConnection.prepareCall( getSql() ) :
						proxiedConnection.prepareStatement( getSql() );
			}
		};
		return prep.prepareAndPostProcess();
	}

	/**
	 * Get a scrollable prepared statement for use in loading / querying.
	 * If configured, the query timeout and statement fetch size are set.
	 * <p/>
	 * If not explicitly closed via {@link java.sql.PreparedStatement#close()},
	 * it will be released when the session is closed or disconnected.
	 */
	public PreparedStatement prepareScrollableQueryStatement(
			String sql,
			final ScrollMode scrollMode,
			final boolean isCallable
	) {
		if ( ! settings.isScrollableResultSetsEnabled() ) {
			throw new AssertionFailure("scrollable result sets are not enabled");
		}
		StatementPreparation prep = new QueryStatementPreparation( sql ) {
			@Override
            public PreparedStatement doPrepare() throws SQLException {
					return isCallable ?
							proxiedConnection.prepareCall(
									getSql(), scrollMode.toResultSetType(), ResultSet.CONCUR_READ_ONLY
							) :
							proxiedConnection.prepareStatement(
									getSql(), scrollMode.toResultSetType(), ResultSet.CONCUR_READ_ONLY
					);
			}
		};
		return prep.prepareAndPostProcess();
	}

	/**
	 * Sets the transaction timeout.
	 * @param seconds - number of seconds until the the transaction times out.
	 */
	public void setTransactionTimeout(int seconds) {
		isTransactionTimeoutSet = true;
		transactionTimeout = System.currentTimeMillis() / 1000 + seconds;
	}

	/**
	 * Unset the transaction timeout, called after the end of a
	 * transaction.
	 */
	public void unsetTransactionTimeout() {
		isTransactionTimeoutSet = false;
	}

	private void setStatementTimeout(PreparedStatement preparedStatement) throws SQLException {
		if ( isTransactionTimeoutSet ) {
			int timeout = (int) ( transactionTimeout - ( System.currentTimeMillis() / 1000 ) );
			if ( timeout <=  0) {
				throw new TransactionException("transaction timeout expired");
			}
			else {
				preparedStatement.setQueryTimeout(timeout);
			}
		}
	}

	private void setStatementFetchSize(PreparedStatement statement) throws SQLException {
		if ( settings.getJdbcFetchSize() != null ) {
			statement.setFetchSize( settings.getJdbcFetchSize() );
		}
	}
}
