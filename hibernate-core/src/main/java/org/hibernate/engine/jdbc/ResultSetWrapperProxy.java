/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
package org.hibernate.engine.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.util.JDBCExceptionReporter;

/**
 * A proxy for a ResultSet delegate, responsible for locally caching the columnName-to-columnIndex resolution that
 * has been found to be inefficient in a few vendor's drivers (i.e., Oracle and Postgres).
 *
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class ResultSetWrapperProxy implements InvocationHandler {
	private static final Logger log = LoggerFactory.getLogger( ResultSetWrapperProxy.class );
	private static final Class[] PROXY_INTERFACES = new Class[] { ResultSet.class };

	private final ResultSet rs;
	private final ColumnNameCache columnNameCache;

	private ResultSetWrapperProxy(ResultSet rs, ColumnNameCache columnNameCache) {
		this.rs = rs;
		this.columnNameCache = columnNameCache;
	}

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

	/**
	 * {@inheritDoc}
	 */
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if ( "findColumn".equals( method.getName() ) ) {
			return new Integer( findColumn( ( String ) args[0] ) );
		}

		if ( isFirstArgColumnLabel( method, args ) ) {
			try {
				int columnIndex = findColumn( ( String ) args[0] );
				return invokeMethod(
						locateCorrespondingColumnIndexMethod( method ), buildColumnIndexMethodArgs( args, columnIndex )
				);
			}
			catch ( SQLException ex ) {
				StringBuffer buf = new StringBuffer()
						.append( "Exception getting column index for column: [" )
						.append( args[0] )
						.append( "].\nReverting to using: [" )
						.append( args[0] )
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

	/**
	 * Locate the column index corresponding to the given column name via the cache.
	 *
	 * @param columnName The column name to resolve into an index.
	 * @return The column index corresponding to the given column name.
	 * @throws SQLException if the ResultSet object does not contain columnName or a database access error occurs
	 */
	private int findColumn(String columnName) throws SQLException {
		return columnNameCache.getIndexForColumnName( columnName, rs );
	}

	private boolean isFirstArgColumnLabel(Method method, Object args[]) {
		// method name should start with either get or update
		if ( ! ( method.getName().startsWith( "get" ) || method.getName().startsWith( "update" ) ) ) {
			return false;
		}

		// method should have arguments, and have same number as incoming arguments
		if ( ! ( method.getParameterTypes().length > 0 && args.length == method.getParameterTypes().length ) ) {
			return false;
		}

		// The first argument should be a String (the column name)
		//noinspection RedundantIfStatement
		if ( ! ( String.class.isInstance( args[0] ) && method.getParameterTypes()[0].equals( String.class ) ) ) {
			return false;
		}

		return true;
	}

	/**
	 * For a given {@link ResultSet} method passed a column name, locate the corresponding method passed the same
	 * parameters but the column index.
	 *
	 * @param columnNameMethod The method passed the column name
	 * @return The corresponding method passed the column index.
	 * @throws NoSuchMethodException Should never happen, but...
	 */
	private Method locateCorrespondingColumnIndexMethod(Method columnNameMethod) throws NoSuchMethodException {
		Class actualParameterTypes[] = new Class[columnNameMethod.getParameterTypes().length];
		actualParameterTypes[0] = int.class;
		System.arraycopy(
				columnNameMethod.getParameterTypes(),
				1,
				actualParameterTypes,
				1,
				columnNameMethod.getParameterTypes().length - 1
		);
		return columnNameMethod.getDeclaringClass().getMethod( columnNameMethod.getName(), actualParameterTypes );
	}

	private Object[] buildColumnIndexMethodArgs(Object[] incomingArgs, int columnIndex) {
		Object actualArgs[] = new Object[incomingArgs.length];
		actualArgs[0] = new Integer( columnIndex );
		System.arraycopy( incomingArgs, 1, actualArgs, 1, incomingArgs.length - 1 );
		return actualArgs;
	}

	private Object invokeMethod(Method method, Object args[]) throws Throwable {
		try {
			return method.invoke( rs, args );
		}
		catch ( InvocationTargetException e ) {
			throw e.getTargetException();
		}
	}
}
