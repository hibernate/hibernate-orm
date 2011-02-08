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
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * The InvocationHandler for intercepting messages to {@link java.sql.DatabaseMetaData} proxies.
 * <p/>
 * Mainly we need to intercept the methods defined on {@link java.sql.DatabaseMetaData} which expose
 * {@link java.sql.ResultSet} instances, which in turn expose {@link java.sql.Statement}
 * instances, which in turn...
 *
 * @author Steve Ebersole
 */
public class DatabaseMetaDataProxyHandler extends AbstractProxyHandler {
	private ConnectionProxyHandler connectionProxyHandler;
	private Connection connectionProxy;
	private DatabaseMetaData databaseMetaData;

	public DatabaseMetaDataProxyHandler(DatabaseMetaData databaseMetaData, ConnectionProxyHandler connectionProxyHandler, Connection connectionProxy) {
		super( databaseMetaData.hashCode() );
		this.connectionProxyHandler = connectionProxyHandler;
		this.connectionProxy = connectionProxy;
		this.databaseMetaData = databaseMetaData;
	}

	protected Object continueInvocation(Object proxy, Method method, Object[] args) throws Throwable {
		// handle the JDBC 4 Wrapper#isWrapperFor and Wrapper#unwrap calls
		//		these cause problems to the whole proxy scheme though as we need to return the raw objects
		if ( "isWrapperFor".equals( method.getName() ) && args.length == 1 ) {
			return method.invoke( databaseMetaData, args );
		}
		if ( "unwrap".equals( method.getName() ) && args.length == 1 ) {
			return method.invoke( databaseMetaData, args );
		}

		try {
			boolean exposingResultSet = doesMethodExposeResultSet( method );

			Object result = method.invoke( databaseMetaData, args );

			if ( exposingResultSet ) {
				result = ProxyBuilder.buildImplicitResultSet( (ResultSet) result, connectionProxyHandler, connectionProxy );
				connectionProxyHandler.getResourceRegistry().register( ( ResultSet ) result );
			}

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

	protected boolean doesMethodExposeResultSet(Method method) {
		return ResultSet.class.isAssignableFrom( method.getReturnType() );
	}

}
