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
 *
 */
package org.hibernate.dialect;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class Mocks {

	public static Connection createConnection(String dbName, int version) {
		DatabaseMetaDataHandler metadataHandler = new DatabaseMetaDataHandler( dbName, version );
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
				return new Integer( this.hashCode() );
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

		private Connection connectionProxy;

		public void setConnectionProxy(Connection connectionProxy) {
			this.connectionProxy = connectionProxy;
		}

		private DatabaseMetaDataHandler(String databaseName, int majorVersion) {
			this.databaseName = databaseName;
			this.majorVersion = majorVersion;
		}

		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			final String methodName = method.getName();
			if ( "getDatabaseProductName".equals( methodName ) ) {
				return databaseName;
			}

			if ( "getDatabaseMajorVersion".equals( methodName ) ) {
				return new Integer( majorVersion );
			}

			if ( "getConnection".equals( methodName ) ) {
				return connectionProxy;
			}

			if ( "toString".equals( methodName ) ) {
				return "DatabaseMetaData proxy [db-name=" + databaseName + ", version=" + majorVersion + "]";
			}

			if ( "hashCode".equals( methodName ) ) {
				return new Integer( this.hashCode() );
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
		for ( int i = 0; i < exceptions.length; i++ ) {
			if ( SQLException.class.isAssignableFrom( exceptions[i] ) ) {
				return true;
			}
		}
		return false;
	}
}
