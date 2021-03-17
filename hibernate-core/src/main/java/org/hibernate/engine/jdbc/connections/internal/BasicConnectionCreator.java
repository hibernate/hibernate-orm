/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.connections.internal;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.exception.internal.SQLStateConversionDelegate;
import org.hibernate.exception.spi.ConversionContext;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.ViolatedConstraintNameExtracter;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Template (as in template pattern) support for ConnectionCreator implementors.
 *
 * @author Steve Ebersole
 */
public abstract class BasicConnectionCreator implements ConnectionCreator {
	private final ServiceRegistryImplementor serviceRegistry;

	private final String url;
	private final Properties connectionProps;

	private final boolean autoCommit;
	private final Integer isolation;

	public BasicConnectionCreator(
			ServiceRegistryImplementor serviceRegistry,
			String url,
			Properties connectionProps,
			boolean autocommit,
			Integer isolation) {
		this.serviceRegistry = serviceRegistry;
		this.url = url;
		this.connectionProps = connectionProps;
		this.autoCommit = autocommit;
		this.isolation = isolation;
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

		return conn;
	}

	private ValueHolder<SQLExceptionConversionDelegate> simpleConverterAccess = new ValueHolder<SQLExceptionConversionDelegate>(
			new ValueHolder.DeferredInitializer<SQLExceptionConversionDelegate>() {
				@Override
				public SQLExceptionConversionDelegate initialize() {
					return new SQLExceptionConversionDelegate() {
						private final SQLStateConversionDelegate sqlStateDelegate = new SQLStateConversionDelegate(
								new ConversionContext() {
									@Override
									public ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter() {
										// this should never happen...
										throw new HibernateException( "Unexpected call to org.hibernate.exception.spi.ConversionContext.getViolatedConstraintNameExtracter" );
									}
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
					};
				}
			}
	);

	protected JDBCException convertSqlException(String message, SQLException e) {
		// if JdbcServices#getSqlExceptionHelper is available, use it...
		final JdbcServices jdbcServices = serviceRegistry.getService( JdbcServices.class );
		if ( jdbcServices != null && jdbcServices.getSqlExceptionHelper() != null ) {
			return jdbcServices.getSqlExceptionHelper().convert( e, message, null );
		}

		// likely we are still in the process of initializing the ServiceRegistry, so use the simplified
		// SQLException conversion
		return simpleConverterAccess.getValue().convert( e, message, null );
	}

	protected abstract Connection makeConnection(String url, Properties connectionProps);

	/**
	 * Exposed for testing purposes only.
	 * @return
	 */
	public Properties getConnectionProperties() {
		return new Properties( connectionProps );
	}

}
