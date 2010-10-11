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
package org.hibernate.engine.jdbc.proxy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.service.jdbc.spi.JdbcResourceRegistry;
import org.hibernate.service.jdbc.spi.JdbcServices;
import org.hibernate.service.jdbc.spi.LogicalConnectionImplementor;

/**
 * Basic support for building {@link Statement}-based proxy handlers
 *
 * @author Steve Ebersole
 */
public abstract class AbstractStatementProxyHandler extends AbstractProxyHandler {
	private static final Logger log = LoggerFactory.getLogger( AbstractStatementProxyHandler.class );

	private ConnectionProxyHandler connectionProxyHandler;
	private Connection connectionProxy;
	private Statement statement;

	protected AbstractStatementProxyHandler(
			Statement statement,
			ConnectionProxyHandler connectionProxyHandler,
			Connection connectionProxy) {
		super( statement.hashCode() );
		this.statement = statement;
		this.connectionProxyHandler = connectionProxyHandler;
		this.connectionProxy = connectionProxy;
	}

	protected ConnectionProxyHandler getConnectionProxy() {
		errorIfInvalid();
		return connectionProxyHandler;
	}

	protected JdbcServices getJdbcServices() {
		return getConnectionProxy().getJdbcServices();
	}

	protected JdbcResourceRegistry getResourceRegistry() {
		return getConnectionProxy().getResourceRegistry();
	}

	protected Statement getStatement() {
		errorIfInvalid();
		return statement;
	}

	protected Statement getStatementWithoutChecks() {
		return statement;
	}

	protected Object continueInvocation(Object proxy, Method method, Object[] args) throws Throwable {
		String methodName = method.getName();
		log.trace( "Handling invocation of statement method [{}]", methodName );

		// other methods allowed while invalid ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		if ( "close".equals( methodName ) ) {
			explicitClose( ( Statement ) proxy );
			return null;
		}
		if ( "invalidate".equals( methodName ) ) {
			invalidateHandle();
			return null;
		}

		errorIfInvalid();

		// handle the JDBC 4 Wrapper#isWrapperFor and Wrapper#unwrap calls
		//		these cause problems to the whole proxy scheme though as we need to return the raw objects
		if ( "isWrapperFor".equals( methodName ) && args.length == 1 ) {
			return method.invoke( getStatementWithoutChecks(), args );
		}
		if ( "unwrap".equals( methodName ) && args.length == 1 ) {
			return method.invoke( getStatementWithoutChecks(), args );
		}

		if ( "getWrappedObject".equals( methodName ) ) {
			return getStatementWithoutChecks();
		}

		if ( "getConnection".equals( methodName ) ) {
			return connectionProxy;
		}

		beginningInvocationHandling( method, args );

		try {
			Object result = method.invoke( statement, args );
			result = wrapIfNecessary( result, proxy, method );
			return result;
		}
		catch ( InvocationTargetException e ) {
			Throwable realException = e.getTargetException();
			if ( SQLException.class.isInstance( realException ) ) {
				throw connectionProxyHandler.getJdbcServices().getSqlExceptionHelper()
						.convert( ( SQLException ) realException, realException.getMessage() );
			}
			else {
				throw realException;
			}
		}
	}

	private Object wrapIfNecessary(Object result, Object proxy, Method method) {
		if ( !( ResultSet.class.isAssignableFrom( method.getReturnType() ) ) ) {
			return result;
		}

		final ResultSet wrapper;
		if ( "getGeneratedKeys".equals( method.getName() ) ) {
			wrapper = ProxyBuilder.buildImplicitResultSet( ( ResultSet ) result, connectionProxyHandler, connectionProxy );
		}
		else {
			wrapper = ProxyBuilder.buildResultSet( ( ResultSet ) result, this, ( Statement ) proxy );
		}
		getResourceRegistry().register( wrapper );
		return wrapper;
	}

	protected void beginningInvocationHandling(Method method, Object[] args) {
	}

	private void explicitClose(Statement proxy) {
		if ( isValid() ) {
			LogicalConnectionImplementor lc = getConnectionProxy().getLogicalConnection();
			getResourceRegistry().release( proxy );
			lc.afterStatementExecution();
		}
	}

	private void invalidateHandle() {
		connectionProxyHandler = null;
		statement = null;
		invalidate();
	}
}
