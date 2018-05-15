/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * @author Steve Ebersole
 */
public class Mocks {

	public static Connection createConnection(String databaseName, int majorVersion) {
		return createConnection( databaseName, majorVersion, -9999 );
	}

	public static Connection createConnection(String databaseName, int majorVersion, int minorVersion) {
		DatabaseMetaDataHandler metadataHandler = new DatabaseMetaDataHandler( databaseName, majorVersion, minorVersion );
		ConnectionHandler connectionHandler = new ConnectionHandler();

		DatabaseMetaData metadataProxy = ( DatabaseMetaData ) Proxy.newProxyInstance(
				ClassLoader.getSystemClassLoader(),
				new Class[] { DatabaseMetaData.class },
				metadataHandler
		);

		Connection connectionProxy = ( Connection ) Proxy.newProxyInstance(
				ClassLoader.getSystemClassLoader(),
				new Class[] { Connection.class },
				connectionHandler
		);

		metadataHandler.setConnectionProxy( connectionProxy );
		connectionHandler.setMetadataProxy( metadataProxy );

		return connectionProxy;
	}

	private static class ConnectionHandler implements InvocationHandler {
		private DatabaseMetaData metadataProxy;

		public void setMetadataProxy(DatabaseMetaData metadataProxy) {
			this.metadataProxy = metadataProxy;
		}

		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			final String methodName = method.getName();
			if ( "getMetaData".equals( methodName ) ) {
				return metadataProxy;
			}

			if ( "toString".equals( methodName ) ) {
				return "Connection proxy [@" + hashCode() + "]";
			}

			if ( "hashCode".equals( methodName ) ) {
				return Integer.valueOf( this.hashCode() );
			}

			if ( canThrowSQLException( method ) ) {
				throw new SQLException();
			}
			else {
				throw new UnsupportedOperationException();
			}
		}
	}

	private static class DatabaseMetaDataHandler implements InvocationHandler {
		private final String databaseName;
		private final int majorVersion;
		private final int minorVersion;

		private Connection connectionProxy;

		public void setConnectionProxy(Connection connectionProxy) {
			this.connectionProxy = connectionProxy;
		}

		private DatabaseMetaDataHandler(String databaseName, int majorVersion) {
			this( databaseName, majorVersion, -9999 );
		}

		private DatabaseMetaDataHandler(String databaseName, int majorVersion, int minorVersion) {
			this.databaseName = databaseName;
			this.majorVersion = majorVersion;
			this.minorVersion = minorVersion;
		}

		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			final String methodName = method.getName();
			if ( "getDatabaseProductName".equals( methodName ) ) {
				return databaseName;
			}

			if ( "getDatabaseMajorVersion".equals( methodName ) ) {
				return Integer.valueOf( majorVersion );
			}

			if ( "getDatabaseMinorVersion".equals( methodName ) ) {
				return Integer.valueOf( minorVersion );
			}

			if ( "getConnection".equals( methodName ) ) {
				return connectionProxy;
			}

			if ( "toString".equals( methodName ) ) {
				return "DatabaseMetaData proxy [db-name=" + databaseName + ", version=" + majorVersion + "]";
			}

			if ( "hashCode".equals( methodName ) ) {
				return Integer.valueOf( this.hashCode() );
			}

			if ( canThrowSQLException( method ) ) {
				throw new SQLException();
			}
			else {
				throw new UnsupportedOperationException();
			}
		}
	}

	private static boolean canThrowSQLException(Method method) {
		final Class[] exceptions = method.getExceptionTypes();
		for ( Class exceptionType : exceptions ) {
			if ( SQLException.class.isAssignableFrom( exceptionType ) ) {
				return true;
			}
		}
		return false;
	}
}
