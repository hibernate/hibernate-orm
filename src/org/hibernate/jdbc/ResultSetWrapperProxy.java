//$Id: $
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hibernate.util.JDBCExceptionReporter;

/**
 * A proxy for a ResultSet delegate, responsible for locally caching the
 * columnName-to-columnIndex resolution that has been found to be inefficient
 * in a few vendor's drivers (i.e., Oracle and Postgres).
 *
 * @author Gail Badner
 */
public class ResultSetWrapperProxy implements InvocationHandler {

	private static final Class[] PROXY_INTERFACES = new Class[] { ResultSet.class };
	private static final Log log = LogFactory.getLog( ResultSetWrapperProxy.class );

	private final ResultSet rs;
	private ColumnNameCache columnNameCache;

	/**
	 * Generates a proxy wrapping the ResultSet.
	 *
	 * @param resultSet The resultSet to wrap.
	 * @param columnNameCache The cache storing data for converting column names to column indexes.
	 * @return The generated proxy.
	 */
	public static ResultSet generateProxy(ResultSet resultSet, ColumnNameCache columnNameCache) {
		return ( ResultSet ) Proxy.newProxyInstance(
				getProxyClassLoader(),
				PROXY_INTERFACES,
				new ResultSetWrapperProxy( resultSet, columnNameCache )
		);
	}

	private ResultSetWrapperProxy(ResultSet resultSet, ColumnNameCache columnNameCache) {
		this.rs = resultSet;
		this.columnNameCache = columnNameCache;
	}

	/**
	 * Overridden version to utilize local caching of the column indexes by name
	 * to improve performance for those drivers which are known to not support
	 * such caching by themselves.
	 * <p/>
	 * This implementation performs the caching based on the upper case version
	 * of the given column name.
	 *
	 * @param columnName The column name to resolve into an index.
	 * @return The column index corresponding to the given column name.
	 * @throws java.sql.SQLException - if the ResultSet object does not contain
	 * columnName or a database access error occurs
	 */
	public int findColumn(String columnName) throws SQLException {
		return columnNameCache.getIndexForColumnName( columnName, rs );
	}

	/**
	 * {@inheritDoc}
	 */
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if ( isFirstArgColumnLabel( method, args ) ) {
			try {
				int columnIndex = findColumn( ( String ) args[0] );
				return invokeMethod( getMethodUsingColumnIndex( method ), getArgsUsingColumnIndex( args, columnIndex ) );
			}
			catch ( SQLException ex ) {
				StringBuffer buf = new StringBuffer()
						.append( "Exception getting column index for column: [" )
						.append( args[0] )
						.append( "].\nReverting to using: [" )
						.append( args[ 0 ] )
						.append( "] as first argument for method: [" )
						.append( method )
						.append( "]" );
				JDBCExceptionReporter.logExceptions( ex, buf.toString() );
			}
			catch ( NoSuchMethodException ex ) {
				StringBuffer buf = new StringBuffer()
						.append( "Exception switching from method: [" )
						.append( method )
						.append( "] to a method using the column index. Reverting to using: [" )
						.append( method )
						.append( "]" );
				if ( log.isWarnEnabled() ) {
					log.warn( buf.toString() );
				}
			}
		}
		return invokeMethod( method, args );
	}

	private boolean isFirstArgColumnLabel(Method method, Object args[]) {
		return method.getParameterTypes().length > 0 &&
				method.getParameterTypes()[ 0 ].equals( String.class ) &&
				args.length == method.getParameterTypes().length &&
				String.class.isInstance( args[ 0 ] ) &&
				( method.getName().startsWith( "get" ) || method.getName().startsWith( "update" ) );
	}

	private Method getMethodUsingColumnIndex(Method method) throws NoSuchMethodException {
		Class actualParameterTypes[] = new Class[ method.getParameterTypes().length ];
		actualParameterTypes[0] = int.class;
		System.arraycopy( method.getParameterTypes(), 1, actualParameterTypes, 1, method.getParameterTypes().length - 1 );
		return method.getDeclaringClass().getMethod( method.getName(), actualParameterTypes );
	}

	private Object[] getArgsUsingColumnIndex( Object[] args, int columnIndex ) {
		Object actualArgs[] = new Object[ args.length ];
		actualArgs[0] = new Integer( columnIndex );
		System.arraycopy( args, 1, actualArgs, 1, args.length - 1 );
	    return actualArgs;
	}

	private Object invokeMethod( Method method, Object args[] ) throws Throwable {
		try {
			return method.invoke( rs, args );
		}
		catch( InvocationTargetException e ) {
			throw e.getTargetException();
		}
	}

	/**
	 * Determines the appropriate class loader to which the generated proxy
	 * should be scoped.
	 *
	 * @return The class loader appropriate for proxy construction.
	 */
	public static ClassLoader getProxyClassLoader() {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		if ( cl == null ) {
			cl = ResultSet.class.getClassLoader();
		}
		return cl;
	}
}