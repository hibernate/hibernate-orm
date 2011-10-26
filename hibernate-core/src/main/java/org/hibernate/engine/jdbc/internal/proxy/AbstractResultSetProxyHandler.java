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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.jboss.logging.Logger;

import org.hibernate.engine.jdbc.spi.JdbcResourceRegistry;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.internal.CoreMessageLogger;

/**
 * Basic support for building {@link ResultSet}-based proxy handlers
 *
 * @author Steve Ebersole
 */
public abstract class AbstractResultSetProxyHandler extends AbstractProxyHandler {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class,
                                                                       AbstractResultSetProxyHandler.class.getName());

	private ResultSet resultSet;

	public AbstractResultSetProxyHandler(ResultSet resultSet) {
		super( resultSet.hashCode() );
		this.resultSet = resultSet;
	}

	protected abstract JdbcServices getJdbcServices();

	protected abstract JdbcResourceRegistry getResourceRegistry();

	protected abstract Statement getExposableStatement();

	protected final ResultSet getResultSet() {
		errorIfInvalid();
		return resultSet;
	}

	protected final ResultSet getResultSetWithoutChecks() {
		return resultSet;
	}

	@Override
	protected Object continueInvocation(Object proxy, Method method, Object[] args) throws Throwable {
		final String methodName = method.getName();
		LOG.tracev( "Handling invocation of ResultSet method [{0}]", methodName );

		// other methods allowed while invalid ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		if ( "close".equals( methodName ) ) {
			explicitClose( ( ResultSet ) proxy );
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
			return method.invoke( getResultSetWithoutChecks(), args );
		}
		if ( "unwrap".equals( methodName ) && args.length == 1 ) {
			return method.invoke( getResultSetWithoutChecks(), args );
		}

		if ( "getWrappedObject".equals( methodName ) ) {
			return getResultSetWithoutChecks();
		}

		if ( "getStatement".equals( methodName ) ) {
			return getExposableStatement();
		}

		try {
			return method.invoke( resultSet, args );
		}
		catch ( InvocationTargetException e ) {
			Throwable realException = e.getTargetException();
            if (SQLException.class.isInstance(realException)) throw getJdbcServices().getSqlExceptionHelper().convert((SQLException)realException,
                                                                                                                      realException.getMessage());
            throw realException;
		}
	}

	private void explicitClose(ResultSet proxy) {
		if ( isValid() ) {
			getResourceRegistry().release( proxy );
		}
	}

	protected void invalidateHandle() {
		resultSet = null;
		invalidate();
	}
}
