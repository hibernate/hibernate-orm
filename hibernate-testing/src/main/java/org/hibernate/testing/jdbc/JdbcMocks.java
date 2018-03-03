/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.engine.jdbc.env.spi.AnsiSqlKeywords;
import org.hibernate.internal.util.StringHelper;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings({"UnnecessaryBoxing", "unused"})
public class JdbcMocks {

	public static class Options {
		private String databaseProductName;
		private int databaseMajorVersion = -9999;
		private int databaseMinorVersion = -9999;

		private String catalogName = "db1";

		private boolean supportsRefCursors = false;
		private boolean supportsNamedParameters = true;
		private boolean supportsResultSetType = true;
		private boolean supportsGetGeneratedKeys = true;
		private boolean supportsBatchUpdates = true;
		private boolean dataDefinitionIgnoredInTransactions = false;
		private boolean dataDefinitionCausesTransactionCommit = false;
		private String sqlKeywords = String.join( ",", AnsiSqlKeywords.INSTANCE.sql2003() );
		private int sqlStateType = DatabaseMetaData.sqlStateXOpen;
		private boolean locatorsUpdateCopy = false;
		private boolean storesLowerCaseIdentifiers = true;
		private boolean storesUpperCaseIdentifiers = false;
		private String catalogSeparator = ":";
		private boolean isCatalogAtStart = true;
	}

	public static class ConnectionBuilder {
		private final Options options;

		public ConnectionBuilder() {
			this( new Options() );
		}

		public ConnectionBuilder(Options options) {
			this.options = options;
		}

		public Connection buildConnection() {
			ConnectionHandler connectionHandler = new ConnectionHandler( options );
			return (Connection) Proxy.newProxyInstance(
					ClassLoader.getSystemClassLoader(),
					new Class[] { Connection.class },
					connectionHandler
			);
		}

		public ConnectionBuilder setDatabaseProductName(String databaseProductName) {
			this.options.databaseProductName = databaseProductName;
			return this;
		}

		public ConnectionBuilder setDatabaseMajorVersion(int databaseMajorVersion) {
			this.options.databaseMajorVersion = databaseMajorVersion;
			return this;
		}

		public ConnectionBuilder setDatabaseMinorVersion(int databaseMinorVersion) {
			this.options.databaseMinorVersion = databaseMinorVersion;
			return this;
		}

		public ConnectionBuilder setCatalogName(String catalogName) {
			this.options.catalogName = catalogName;
			return this;
		}

		public ConnectionBuilder setSupportsRefCursors(boolean supportsRefCursors) {
			this.options.supportsRefCursors = supportsRefCursors;
			return this;
		}

		public ConnectionBuilder setSupportsNamedParameters(boolean supportsNamedParameters) {
			this.options.supportsNamedParameters = supportsNamedParameters;
			return this;
		}

		public ConnectionBuilder setSupportsResultSetType(boolean supportsResultSetType) {
			this.options.supportsResultSetType = supportsResultSetType;
			return this;
		}

		public ConnectionBuilder setSupportsGetGeneratedKeys(boolean supportsGetGeneratedKeys) {
			this.options.supportsGetGeneratedKeys = supportsGetGeneratedKeys;
			return this;
		}

		public ConnectionBuilder setSupportsBatchUpdates(boolean supportsBatchUpdates) {
			this.options.supportsBatchUpdates = supportsBatchUpdates;
			return this;
		}

		public ConnectionBuilder setDataDefinitionIgnoredInTransactions(boolean dataDefinitionIgnoredInTransactions) {
			this.options.dataDefinitionIgnoredInTransactions = dataDefinitionIgnoredInTransactions;
			return this;
		}

		public ConnectionBuilder setDataDefinitionCausesTransactionCommit(boolean dataDefinitionCausesTransactionCommit) {
			this.options.dataDefinitionCausesTransactionCommit = dataDefinitionCausesTransactionCommit;
			return this;
		}

		public ConnectionBuilder setSqlKeywords(String sqlKeywords) {
			this.options.sqlKeywords = sqlKeywords;
			return this;
		}

		public ConnectionBuilder setSqlStateType(int sqlStateType) {
			this.options.sqlStateType = sqlStateType;
			return this;
		}

		public ConnectionBuilder setLocatorsUpdateCopy(boolean locatorsUpdateCopy) {
			this.options.locatorsUpdateCopy = locatorsUpdateCopy;
			return this;
		}

		public ConnectionBuilder setStoresLowerCaseIdentifiers(boolean storesLowerCaseIdentifiers) {
			this.options.storesLowerCaseIdentifiers = storesLowerCaseIdentifiers;
			return this;
		}

		public ConnectionBuilder setStoresUpperCaseIdentifiers(boolean storesUpperCaseIdentifiers) {
			this.options.storesUpperCaseIdentifiers = storesUpperCaseIdentifiers;
			return this;
		}

		public ConnectionBuilder setCatalogSeparator(String catalogSeparator) {
			this.options.catalogSeparator = catalogSeparator;
			return this;
		}

		public ConnectionBuilder setCatalogAtStart(boolean catalogAtStart) {
			this.options.isCatalogAtStart = catalogAtStart;
			return this;
		}
	}

	public static Connection createConnection(String databaseName, int majorVersion) {
		return new ConnectionBuilder()
				.setDatabaseProductName( databaseName )
				.setDatabaseMajorVersion( majorVersion )
				.buildConnection();
	}

	public static Connection createConnection(String databaseName, int majorVersion, int minorVersion) {
		return new ConnectionBuilder()
				.setDatabaseProductName( databaseName )
				.setDatabaseMajorVersion( majorVersion )
				.setDatabaseMinorVersion( minorVersion )
				.buildConnection();
	}

	private static class ConnectionHandler implements InvocationHandler {
		private final Options options;
		private DatabaseMetaData metadataProxy;

		public ConnectionHandler(Options options) {
			this.options = options;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			final String methodName = method.getName();
			if ( "getMetaData".equals( methodName ) ) {
				return getMetadataProxy( (Connection) proxy );
			}

			if ( "toString".equals( methodName ) ) {
				return "Connection proxy [@" + hashCode() + "]";
			}

			if ( "hashCode".equals( methodName ) ) {
				return Integer.valueOf( this.hashCode() );
			}

			if ( "getCatalog".equals( methodName ) ) {
				return options.catalogName;
			}

			if ( "supportsRefCursors".equals( methodName ) ) {
				return options.supportsRefCursors;
			}

			if ( canThrowSQLException( method ) ) {
				throw new SQLException();
			}
			else {
				throw new UnsupportedOperationException();
			}
		}

		private DatabaseMetaData getMetadataProxy(Connection connectionProxy) {
			if ( metadataProxy == null ) {
				// we need to make it
				final DatabaseMetaDataHandler metadataHandler = new DatabaseMetaDataHandler( options, connectionProxy );
				metadataProxy = (DatabaseMetaData) Proxy.newProxyInstance(
						ClassLoader.getSystemClassLoader(),
						new Class[] {DatabaseMetaData.class},
						metadataHandler
				);
			}
			return metadataProxy;
		}
	}

	private static class DatabaseMetaDataHandler implements InvocationHandler {
		private final Options options;
		private final Connection connectionProxy;

		public DatabaseMetaDataHandler(Options options, Connection connectionProxy) {
			this.options = options;
			this.connectionProxy = connectionProxy;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			final String methodName = method.getName();
			if ( "getDatabaseProductName".equals( methodName ) ) {
				return options.databaseProductName;
			}

			if ( "getDatabaseMajorVersion".equals( methodName ) ) {
				return Integer.valueOf( options.databaseMajorVersion );
			}

			if ( "getDatabaseMinorVersion".equals( methodName ) ) {
				return Integer.valueOf( options.databaseMinorVersion );
			}

			if ( "getConnection".equals( methodName ) ) {
				return connectionProxy;
			}

			if ( "toString".equals( methodName ) ) {
				return "DatabaseMetaData proxy [db-name=" + options.databaseProductName + ", version=" + options.databaseMajorVersion + "]";
			}

			if ( "hashCode".equals( methodName ) ) {
				return Integer.valueOf( this.hashCode() );
			}

			if ( "supportsNamedParameters".equals( methodName ) ) {
				return options.supportsNamedParameters;
			}

			if ( "supportsResultSetType".equals( methodName ) ) {
				return options.supportsResultSetType;
			}

			if ( "supportsGetGeneratedKeys".equals( methodName ) ) {
				return options.supportsGetGeneratedKeys;
			}

			if ( "supportsBatchUpdates".equals( methodName ) ) {
				return options.supportsBatchUpdates;
			}

			if ( "dataDefinitionIgnoredInTransactions".equals( methodName ) ) {
				return options.dataDefinitionIgnoredInTransactions;
			}

			if ( "dataDefinitionCausesTransactionCommit".equals( methodName ) ) {
				return options.dataDefinitionCausesTransactionCommit;
			}

			if ( "getSQLKeywords".equals( methodName ) ) {
				return options.sqlKeywords;
			}

			if ( "getSQLStateType".equals( methodName ) ) {
				return options.sqlStateType;
			}

			if ( "locatorsUpdateCopy".equals( methodName ) ) {
				return options.locatorsUpdateCopy;
			}

			if ( "getTypeInfo".equals( methodName ) ) {
				return EmptyResultSetHandler.makeProxy();
			}

			if ( "storesLowerCaseIdentifiers".equals( methodName ) ) {
				return options.storesLowerCaseIdentifiers;
			}

			if ( "storesUpperCaseIdentifiers".equals( methodName ) ) {
				return options.storesUpperCaseIdentifiers;
			}

			if ( "getCatalogSeparator".equals( methodName ) ) {
				return options.catalogSeparator;
			}

			if ( "isCatalogAtStart".equals( methodName ) ) {
				return options.isCatalogAtStart;
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

	public static class EmptyResultSetHandler implements InvocationHandler {
		public static ResultSet makeProxy() {
			final EmptyResultSetHandler handler = new EmptyResultSetHandler();
			return (ResultSet) Proxy.newProxyInstance(
					ClassLoader.getSystemClassLoader(),
					new Class[] {ResultSet.class},
					handler
			);

		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if ( method.getName().equals( "next" ) ) {
				return Boolean.FALSE;
			}

			if ( canThrowSQLException( method ) ) {
				throw new SQLException();
			}
			else {
				throw new UnsupportedOperationException();
			}
		}
	}
}
