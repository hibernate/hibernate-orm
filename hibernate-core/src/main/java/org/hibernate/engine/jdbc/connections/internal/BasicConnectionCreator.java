/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.connections.internal;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.exception.internal.SQLStateConversionDelegate;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.service.spi.ServiceException;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Template (as in template pattern) support for {@link ConnectionCreator} implementors.
 *
 * @author Steve Ebersole
 */
public abstract class BasicConnectionCreator implements ConnectionCreator {
	private final ServiceRegistryImplementor serviceRegistry;

	private final String url;
	private final Properties connectionProps;

	private final boolean autoCommit;
	private final Integer isolation;
	private final String initSql;

	public BasicConnectionCreator(
			ServiceRegistryImplementor serviceRegistry,
			String url,
			Properties connectionProps,
			boolean autocommit,
			Integer isolation,
			String initSql) {
		this.serviceRegistry = serviceRegistry;
		this.url = url;
		this.connectionProps = connectionProps;
		this.autoCommit = autocommit;
		this.isolation = isolation;
		this.initSql = initSql;
	}

	@Override
	public String getUrl() {
		return url;
	}

	@Override
	public Connection createConnection() {
		final Connection conn = makeConnection( url, connectionProps );
		if ( conn == null ) {
			throw new HibernateException( "Unable to make JDBC Connection [" + url + "]" );
		}

		try {
			try {
				if ( isolation != null ) {
					conn.setTransactionIsolation( isolation );
				}
			}
			catch (SQLException e) {
				throw convertSqlException( "Unable to set transaction isolation (" + isolation + ")", e );
			}

			try {
				if ( conn.getAutoCommit() != autoCommit ) {
					conn.setAutoCommit( autoCommit );
				}
			}
			catch (SQLException e) {
				throw convertSqlException( "Unable to set auto-commit (" + autoCommit + ")", e );
			}

			if ( initSql != null && !initSql.trim().isEmpty() ) {
				try (Statement s = conn.createStatement()) {
					s.execute( initSql );
				}
				catch (SQLException e) {
					throw convertSqlException( "Unable to execute initSql (" + initSql + ")", e );
				}
			}
		}
		catch (RuntimeException | Error e) {
			try {
				conn.close();
			}
			catch (SQLException ex) {
				e.addSuppressed( ex );
			}
			throw e;
		}

		return conn;
	}

	private final ValueHolder<SQLExceptionConversionDelegate> simpleConverterAccess =
			new ValueHolder<>( () -> new SQLExceptionConversionDelegate() {
				private final SQLStateConversionDelegate sqlStateDelegate = new SQLStateConversionDelegate(
						() -> {
							// this should never happen...
							throw new HibernateException( "Unexpected call to ConversionContext.getViolatedConstraintNameExtractor" );
						}
				);

				@Override
				public JDBCException convert(SQLException sqlException, String message, String sql) {
					JDBCException exception = sqlStateDelegate.convert( sqlException, message, sql );
					if ( exception == null ) {
						// assume this is either a set-up problem or a problem connecting, which we will
						// categorize the same here.
						exception = new JDBCConnectionException( message, sqlException, sql );
					}
					return exception;
				}
			}
	);

	protected JDBCException convertSqlException(String message, SQLException e) {
		final String fullMessage = message + " [" + e.getMessage() + "]";
		try {
			// if JdbcServices#getSqlExceptionHelper is available, use it...
			final JdbcServices jdbcServices = serviceRegistry.getService( JdbcServices.class );
			if ( jdbcServices != null && jdbcServices.getSqlExceptionHelper() != null ) {
				return jdbcServices.getSqlExceptionHelper().convert( e, fullMessage );
			}
		}
		catch (ServiceException se) {
			//swallow it because we're in the process of initializing JdbcServices
		}

		// likely we are still in the process of initializing the ServiceRegistry, so use the simplified
		// SQLException conversion
		return simpleConverterAccess.getValue().convert( e, fullMessage, null );
	}

	protected abstract Connection makeConnection(String url, Properties connectionProps);

	/**
	 * Exposed for testing purposes only.
	 */
	public Properties getConnectionProperties() {
		return new Properties( connectionProps );
	}

}
