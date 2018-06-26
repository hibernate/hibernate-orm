/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc;

import static org.hibernate.internal.CoreLogging.messageLogger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.service.ServiceRegistry;

/**
 * A proxy for a ResultSet delegate, responsible for locally caching the columnName-to-columnIndex resolution that
 * has been found to be inefficient in a few vendor's drivers (i.e., Oracle and Postgres).
 *
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class ResultSetWrapperProxy implements InvocationHandler {
	private static final CoreMessageLogger LOG = messageLogger( ResultSetWrapperProxy.class );

	private static final SqlExceptionHelper SQL_EXCEPTION_HELPER = new SqlExceptionHelper( false );

	private static final Map<ResultSetMethodKey, Method> NAME_TO_INDEX_METHOD_MAPPING;

	private final ResultSet rs;
	private final ColumnNameCache columnNameCache;

	static {
		Map<ResultSetMethodKey, Method> nameToIndexMethodMapping = new HashMap<>();
		for ( Method method : ResultSet.class.getDeclaredMethods() ) {
			if ( isFirstArgColumnLabel( method ) ) {
				try {
					nameToIndexMethodMapping.put(
							new ResultSetMethodKey( method.getName(), method.getParameterTypes() ),
							locateCorrespondingColumnIndexMethod( method )
					);
				}
				catch (NoSuchMethodException e) {
					LOG.unableToSwitchToMethodUsingColumnIndex( method );
				}
			}
		}
		NAME_TO_INDEX_METHOD_MAPPING = Collections.unmodifiableMap( nameToIndexMethodMapping );
	}

	private ResultSetWrapperProxy(ResultSet rs, ColumnNameCache columnNameCache) {
		this.rs = rs;
		this.columnNameCache = columnNameCache;
	}

	/**
	 * Generates a proxy wrapping the ResultSet.
	 *
	 * @param resultSet The resultSet to wrap.
	 * @param columnNameCache The cache storing data for converting column names to column indexes.
	 * @param serviceRegistry Access to any needed services
	 *
	 * @return The generated proxy.
	 */
	public static ResultSet generateProxy(
			ResultSet resultSet,
			ColumnNameCache columnNameCache,
			ServiceRegistry serviceRegistry) {
		return serviceRegistry.getService( ClassLoaderService.class ).generateProxy(
				new ResultSetWrapperProxy( resultSet, columnNameCache ),
				ResultSet.class
		);
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if ( "findColumn".equals( method.getName() ) ) {
			return findColumn( (String) args[0] );
		}

		if ( isFirstArgColumnLabel( method ) ) {
			Method columnIndexMethod = NAME_TO_INDEX_METHOD_MAPPING.get( new ResultSetMethodKey( method.getName(), method.getParameterTypes() ) );
			if ( columnIndexMethod != null ) {
				try {
					final Integer columnIndex = findColumn( (String) args[0] );

					return invokeMethod( columnIndexMethod, buildColumnIndexMethodArgs( args, columnIndex ) );
				}
				catch ( SQLException ex ) {
					final String msg = "Exception getting column index for column: [" + args[0] +
							"].\nReverting to using: [" + args[0] +
							"] as first argument for method: [" + method + "]";
					SQL_EXCEPTION_HELPER.logExceptions( ex, msg );
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
	private Integer findColumn(String columnName) throws SQLException {
		return columnNameCache.getIndexForColumnName( columnName, rs );
	}

	private static boolean isFirstArgColumnLabel(Method method) {
		// method name should start with either get or update
		if ( ! ( method.getName().startsWith( "get" ) || method.getName().startsWith( "update" ) ) ) {
			return false;
		}

		// method should have at least one parameter
		if ( ! ( method.getParameterCount() > 0 ) ) {
			return false;
		}

		// The first parameter should be a String (the column name)
		if ( ! method.getParameterTypes()[0].equals( String.class ) ) {
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
	private static Method locateCorrespondingColumnIndexMethod(Method columnNameMethod) throws NoSuchMethodException {
		final Class<?>[] actualParameterTypes = new Class[columnNameMethod.getParameterCount()];
		actualParameterTypes[0] = int.class;
		System.arraycopy(
				columnNameMethod.getParameterTypes(),
				1,
				actualParameterTypes,
				1,
				columnNameMethod.getParameterCount() - 1
		);
		return columnNameMethod.getDeclaringClass().getMethod( columnNameMethod.getName(), actualParameterTypes );
	}

	private Object[] buildColumnIndexMethodArgs(Object[] incomingArgs, Integer columnIndex) {
		final Object[] actualArgs = new Object[incomingArgs.length];
		actualArgs[0] = columnIndex;
		System.arraycopy( incomingArgs, 1, actualArgs, 1, incomingArgs.length - 1 );
		return actualArgs;
	}

	private Object invokeMethod(Method method, Object[] args) throws Throwable {
		try {
			return method.invoke( rs, args );
		}
		catch ( InvocationTargetException e ) {
			throw e.getTargetException();
		}
	}

	private static class ResultSetMethodKey {

		private String methodName;

		private Class<?>[] parameterTypes;

		public ResultSetMethodKey(String methodName, Class<?>[] parameterTypes) {
			this.methodName = methodName;
			this.parameterTypes = parameterTypes;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + methodName.hashCode();
			result = prime * result + Arrays.hashCode( parameterTypes );
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			if ( obj == null ) {
				return false;
			}
			if ( getClass() != obj.getClass() ) {
				return false;
			}

			ResultSetMethodKey other = (ResultSetMethodKey) obj;
			if ( !methodName.equals( other.methodName ) ) {
				return false;
			}
			if ( !Arrays.equals( parameterTypes, other.parameterTypes ) ) {
				return false;
			}
			return true;
		}
	}
}
