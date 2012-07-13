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
package org.hibernate.engine.jdbc.internal.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.jboss.logging.Logger;

import org.hibernate.engine.jdbc.spi.JdbcResourceRegistry;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.engine.jdbc.spi.NonDurableConnectionObserver;
import org.hibernate.internal.CoreMessageLogger;

/**
 * The {@link InvocationHandler} for intercepting messages to {@link java.sql.Connection} proxies.
 *
 * @author Steve Ebersole
 */
public class ConnectionProxyHandler
		extends AbstractProxyHandler
		implements NonDurableConnectionObserver {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class,
                                                                       ConnectionProxyHandler.class.getName());

	private LogicalConnectionImplementor logicalConnection;

	public ConnectionProxyHandler(LogicalConnectionImplementor logicalConnection) {
		super( logicalConnection.hashCode() );
		this.logicalConnection = logicalConnection;
		this.logicalConnection.addObserver( this );
	}

	/**
	 * Access to our logical connection.
	 *
	 * @return the logical connection
	 */
	protected LogicalConnectionImplementor getLogicalConnection() {
		errorIfInvalid();
		return logicalConnection;
	}

	/**
	 * Get reference to physical connection.
	 * <p/>
	 * NOTE : be sure this handler is still valid before calling!
	 *
	 * @return The physical connection
	 */
	private Connection extractPhysicalConnection() {
		return logicalConnection.getConnection();
	}

	/**
	 * Provide access to JDBCServices.
	 * <p/>
	 * NOTE : package-protected
	 *
	 * @return JDBCServices
	 */
	JdbcServices getJdbcServices() {
		return logicalConnection.getJdbcServices();
	}

	/**
	 * Provide access to JDBCContainer.
	 * <p/>
	 * NOTE : package-protected
	 *
	 * @return JDBCContainer
	 */
	JdbcResourceRegistry getResourceRegistry() {
		return logicalConnection.getResourceRegistry();
	}

	@Override
	protected Object continueInvocation(Object proxy, Method method, Object[] args) throws Throwable {
		final String methodName = method.getName();
		LOG.tracev( "Handling invocation of connection method [{0}]", methodName );

		// other methods allowed while invalid ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		if ( "close".equals( methodName ) ) {
			explicitClose();
			return null;
		}

		if ( "isClosed".equals( methodName ) ) {
			return ! isValid();
		}

		errorIfInvalid();

		// handle the JDBC 4 Wrapper#isWrapperFor and Wrapper#unwrap calls
		//		these cause problems to the whole proxy scheme though as we need to return the raw objects
		if ( "isWrapperFor".equals( methodName ) && args.length == 1 ) {
			return method.invoke( extractPhysicalConnection(), args );
		}
		if ( "unwrap".equals( methodName ) && args.length == 1 ) {
			return method.invoke( extractPhysicalConnection(), args );
		}

		if ( "getWrappedObject".equals( methodName ) ) {
			return extractPhysicalConnection();
		}

		try {
			Object result = method.invoke( extractPhysicalConnection(), args );
			result = postProcess( result, proxy, method, args );

			return result;
		}
		catch( InvocationTargetException e ) {
			Throwable realException = e.getTargetException();
			if ( SQLException.class.isInstance( realException ) ) {
				throw logicalConnection.getJdbcServices().getSqlExceptionHelper()
						.convert( ( SQLException ) realException, realException.getMessage() );
			}
			else {
				throw realException;
			}
		}
	}

	private Object postProcess(Object result, Object proxy, Method method, Object[] args) throws SQLException {
		String methodName = method.getName();
		Object wrapped = result;
		if ( "createStatement".equals( methodName ) ) {
			wrapped = ProxyBuilder.buildStatement(
					(Statement) result,
					this,
					( Connection ) proxy
			);
			postProcessStatement( ( Statement ) wrapped );
		}
		else if ( "prepareStatement".equals( methodName ) ) {
			wrapped = ProxyBuilder.buildPreparedStatement(
					( String ) args[0],
					(PreparedStatement) result,
					this,
					( Connection ) proxy
			);
			postProcessPreparedStatement( ( Statement ) wrapped );
		}
		else if ( "prepareCall".equals( methodName ) ) {
			wrapped = ProxyBuilder.buildCallableStatement(
					( String ) args[0],
					(CallableStatement) result,
					this,
					( Connection ) proxy
			);
			postProcessPreparedStatement( ( Statement ) wrapped );
		}
		else if ( "getMetaData".equals( methodName ) ) {
			wrapped = ProxyBuilder.buildDatabaseMetaData( (DatabaseMetaData) result, this, ( Connection ) proxy );
		}
		return wrapped;
	}

	private void postProcessStatement(Statement statement) throws SQLException {
		getResourceRegistry().register( statement );
	}

	private void postProcessPreparedStatement(Statement statement) throws SQLException  {
		logicalConnection.notifyObserversStatementPrepared();
		postProcessStatement( statement );
	}

	private void explicitClose() {
		if ( isValid() ) {
			invalidateHandle();
		}
	}

	private void invalidateHandle() {
		LOG.trace( "Invalidating connection handle" );
		logicalConnection = null;
		invalidate();
	}

	// ConnectionObserver ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void physicalConnectionObtained(Connection connection) {
	}

	@Override
	public void physicalConnectionReleased() {
		LOG.logicalConnectionReleasingPhysicalConnection();
	}

	@Override
	public void logicalConnectionClosed() {
		LOG.logicalConnectionClosed();
		invalidateHandle();
	}

	@Override
	public void statementPrepared() {
		// N/A
	}
}
