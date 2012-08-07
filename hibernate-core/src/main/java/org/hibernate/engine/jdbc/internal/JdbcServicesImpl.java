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
package org.hibernate.engine.jdbc.internal;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;

import org.hibernate.MultiTenancyStrategy;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jdbc.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.ResultSetWrapper;
import org.hibernate.engine.jdbc.spi.SchemaNameResolver;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.exception.internal.SQLExceptionTypeDelegate;
import org.hibernate.exception.internal.SQLStateConversionDelegate;
import org.hibernate.exception.internal.StandardSQLExceptionConverter;
import org.hibernate.exception.spi.SQLExceptionConverter;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.service.jdbc.dialect.spi.DialectFactory;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Standard implementation of the {@link JdbcServices} contract
 *
 * @author Steve Ebersole
 */
public class JdbcServicesImpl implements JdbcServices, ServiceRegistryAwareService, Configurable {
    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, JdbcServicesImpl.class.getName());

	private ServiceRegistryImplementor serviceRegistry;

	private Dialect dialect;
	private ConnectionProvider connectionProvider;
	private SqlStatementLogger sqlStatementLogger;
	private SqlExceptionHelper sqlExceptionHelper;
	private ExtractedDatabaseMetaData extractedMetaDataSupport;
	private LobCreatorBuilder lobCreatorBuilder;

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public void configure(Map configValues) {
		final JdbcConnectionAccess jdbcConnectionAccess = buildJdbcConnectionAccess( configValues );
		final DialectFactory dialectFactory = serviceRegistry.getService( DialectFactory.class );

		Dialect dialect = null;
		LobCreatorBuilder lobCreatorBuilder = null;

		boolean metaSupportsScrollable = false;
		boolean metaSupportsGetGeneratedKeys = false;
		boolean metaSupportsBatchUpdates = false;
		boolean metaReportsDDLCausesTxnCommit = false;
		boolean metaReportsDDLInTxnSupported = true;
		String extraKeywordsString = "";
		int sqlStateType = -1;
		boolean lobLocatorUpdateCopy = false;
		String catalogName = null;
		String schemaName = null;
		LinkedHashSet<TypeInfo> typeInfoSet = new LinkedHashSet<TypeInfo>();

		// 'hibernate.temp.use_jdbc_metadata_defaults' is a temporary magic value.
		// The need for it is intended to be alleviated with future development, thus it is
		// not defined as an Environment constant...
		//
		// it is used to control whether we should consult the JDBC metadata to determine
		// certain Settings default values; it is useful to *not* do this when the database
		// may not be available (mainly in tools usage).
		boolean useJdbcMetadata = ConfigurationHelper.getBoolean( "hibernate.temp.use_jdbc_metadata_defaults", configValues, true );
		if ( useJdbcMetadata ) {
			try {
				Connection connection = jdbcConnectionAccess.obtainConnection();
				try {
					DatabaseMetaData meta = connection.getMetaData();
					if(LOG.isDebugEnabled()) {
						LOG.debugf( "Database ->\n" + "       name : %s\n" + "    version : %s\n" + "      major : %s\n" + "      minor : %s",
									meta.getDatabaseProductName(),
									meta.getDatabaseProductVersion(),
									meta.getDatabaseMajorVersion(),
									meta.getDatabaseMinorVersion()
						);
						LOG.debugf( "Driver ->\n" + "       name : %s\n" + "    version : %s\n" + "      major : %s\n" + "      minor : %s",
									meta.getDriverName(),
									meta.getDriverVersion(),
									meta.getDriverMajorVersion(),
									meta.getDriverMinorVersion()
						);
						LOG.debugf( "JDBC version : %s.%s", meta.getJDBCMajorVersion(), meta.getJDBCMinorVersion() );
					}

					metaSupportsScrollable = meta.supportsResultSetType( ResultSet.TYPE_SCROLL_INSENSITIVE );
					metaSupportsBatchUpdates = meta.supportsBatchUpdates();
					metaReportsDDLCausesTxnCommit = meta.dataDefinitionCausesTransactionCommit();
					metaReportsDDLInTxnSupported = !meta.dataDefinitionIgnoredInTransactions();
					metaSupportsGetGeneratedKeys = meta.supportsGetGeneratedKeys();
					extraKeywordsString = meta.getSQLKeywords();
					sqlStateType = meta.getSQLStateType();
					lobLocatorUpdateCopy = meta.locatorsUpdateCopy();
					typeInfoSet.addAll( TypeInfoExtracter.extractTypeInfo( meta ) );

					dialect = dialectFactory.buildDialect( configValues, connection );

					catalogName = connection.getCatalog();
					SchemaNameResolver schemaNameResolver = determineExplicitSchemaNameResolver( configValues );
					if ( schemaNameResolver == null ) {
// todo : add dialect method
//						schemaNameResolver = dialect.getSchemaNameResolver();
					}
					if ( schemaNameResolver != null ) {
						schemaName = schemaNameResolver.resolveSchemaName( connection );
					}
					lobCreatorBuilder = new LobCreatorBuilder( configValues, connection );
				}
				catch ( SQLException sqle ) {
					LOG.unableToObtainConnectionMetadata( sqle.getMessage() );
				}
				finally {
					if ( connection != null ) {
						jdbcConnectionAccess.releaseConnection( connection );
					}
				}
			}
			catch ( SQLException sqle ) {
				LOG.unableToObtainConnectionToQueryMetadata( sqle.getMessage() );
				dialect = dialectFactory.buildDialect( configValues, null );
			}
			catch ( UnsupportedOperationException uoe ) {
				// user supplied JDBC connections
				dialect = dialectFactory.buildDialect( configValues, null );
			}
		}
		else {
			dialect = dialectFactory.buildDialect( configValues, null );
		}

		final boolean showSQL = ConfigurationHelper.getBoolean( Environment.SHOW_SQL, configValues, false );
		final boolean formatSQL = ConfigurationHelper.getBoolean( Environment.FORMAT_SQL, configValues, false );

		this.dialect = dialect;
		this.lobCreatorBuilder = (
				lobCreatorBuilder == null ?
						new LobCreatorBuilder( configValues, null ) :
						lobCreatorBuilder
		);

		this.sqlStatementLogger =  new SqlStatementLogger( showSQL, formatSQL );

		this.extractedMetaDataSupport = new ExtractedDatabaseMetaDataImpl(
				metaSupportsScrollable,
				metaSupportsGetGeneratedKeys,
				metaSupportsBatchUpdates,
				metaReportsDDLInTxnSupported,
				metaReportsDDLCausesTxnCommit,
				parseKeywords( extraKeywordsString ),
				parseSQLStateType( sqlStateType ),
				lobLocatorUpdateCopy,
				schemaName,
				catalogName,
				typeInfoSet
		);

		SQLExceptionConverter sqlExceptionConverter = dialect.buildSQLExceptionConverter();
		if ( sqlExceptionConverter == null ) {
			final StandardSQLExceptionConverter converter = new StandardSQLExceptionConverter();
			sqlExceptionConverter = converter;
			converter.addDelegate( dialect.buildSQLExceptionConversionDelegate() );
			converter.addDelegate( new SQLExceptionTypeDelegate( dialect ) );
			// todo : vary this based on extractedMetaDataSupport.getSqlStateType()
			converter.addDelegate( new SQLStateConversionDelegate( dialect ) );
		}
		this.sqlExceptionHelper = new SqlExceptionHelper( sqlExceptionConverter );
	}

	private JdbcConnectionAccess buildJdbcConnectionAccess(Map configValues) {
		final MultiTenancyStrategy multiTenancyStrategy = MultiTenancyStrategy.determineMultiTenancyStrategy( configValues );

		if ( MultiTenancyStrategy.NONE == multiTenancyStrategy ) {
			connectionProvider = serviceRegistry.getService( ConnectionProvider.class );
			return new ConnectionProviderJdbcConnectionAccess( connectionProvider );
		}
		else {
			connectionProvider = null;
			final MultiTenantConnectionProvider multiTenantConnectionProvider = serviceRegistry.getService( MultiTenantConnectionProvider.class );
			return new MultiTenantConnectionProviderJdbcConnectionAccess( multiTenantConnectionProvider );
		}
	}

	private static class ConnectionProviderJdbcConnectionAccess implements JdbcConnectionAccess {
		private final ConnectionProvider connectionProvider;

		public ConnectionProviderJdbcConnectionAccess(ConnectionProvider connectionProvider) {
			this.connectionProvider = connectionProvider;
		}

		@Override
		public Connection obtainConnection() throws SQLException {
			return connectionProvider.getConnection();
		}

		@Override
		public void releaseConnection(Connection connection) throws SQLException {
			connectionProvider.closeConnection( connection );
		}

		@Override
		public boolean supportsAggressiveRelease() {
			return connectionProvider.supportsAggressiveRelease();
		}
	}

	private static class MultiTenantConnectionProviderJdbcConnectionAccess implements JdbcConnectionAccess {
		private final MultiTenantConnectionProvider connectionProvider;

		public MultiTenantConnectionProviderJdbcConnectionAccess(MultiTenantConnectionProvider connectionProvider) {
			this.connectionProvider = connectionProvider;
		}

		@Override
		public Connection obtainConnection() throws SQLException {
			return connectionProvider.getAnyConnection();
		}

		@Override
		public void releaseConnection(Connection connection) throws SQLException {
			connectionProvider.releaseAnyConnection( connection );
		}

		@Override
		public boolean supportsAggressiveRelease() {
			return connectionProvider.supportsAggressiveRelease();
		}
	}


	// todo : add to Environment
	public static final String SCHEMA_NAME_RESOLVER = "hibernate.schema_name_resolver";

	private SchemaNameResolver determineExplicitSchemaNameResolver(Map configValues) {
		Object setting = configValues.get( SCHEMA_NAME_RESOLVER );
		if ( SchemaNameResolver.class.isInstance( setting ) ) {
			return (SchemaNameResolver) setting;
		}

		String resolverClassName = (String) setting;
		if ( resolverClassName != null ) {
			try {
				Class resolverClass = ReflectHelper.classForName( resolverClassName, getClass() );
				return (SchemaNameResolver) ReflectHelper.getDefaultConstructor( resolverClass ).newInstance();
			}
			catch ( ClassNotFoundException e ) {
				LOG.unableToLocateConfiguredSchemaNameResolver( resolverClassName, e.toString() );
			}
			catch ( InvocationTargetException e ) {
				LOG.unableToInstantiateConfiguredSchemaNameResolver( resolverClassName, e.getTargetException().toString() );
			}
			catch ( Exception e ) {
				LOG.unableToInstantiateConfiguredSchemaNameResolver( resolverClassName, e.toString() );
			}
		}
		return null;
	}

	private Set<String> parseKeywords(String extraKeywordsString) {
		Set<String> keywordSet = new HashSet<String>();
		keywordSet.addAll( Arrays.asList( extraKeywordsString.split( "," ) ) );
		return keywordSet;
	}

	private ExtractedDatabaseMetaData.SQLStateType parseSQLStateType(int sqlStateType) {
		switch ( sqlStateType ) {
			case DatabaseMetaData.sqlStateSQL99 : {
				return ExtractedDatabaseMetaData.SQLStateType.SQL99;
			}
			case DatabaseMetaData.sqlStateXOpen : {
				return ExtractedDatabaseMetaData.SQLStateType.XOpen;
			}
			default : {
				return ExtractedDatabaseMetaData.SQLStateType.UNKOWN;
			}
		}
	}

	private static class ExtractedDatabaseMetaDataImpl implements ExtractedDatabaseMetaData {
		private final boolean supportsScrollableResults;
		private final boolean supportsGetGeneratedKeys;
		private final boolean supportsBatchUpdates;
		private final boolean supportsDataDefinitionInTransaction;
		private final boolean doesDataDefinitionCauseTransactionCommit;
		private final Set<String> extraKeywords;
		private final SQLStateType sqlStateType;
		private final boolean lobLocatorUpdateCopy;
		private final String connectionSchemaName;
		private final String connectionCatalogName;
		private final LinkedHashSet<TypeInfo> typeInfoSet;

		private ExtractedDatabaseMetaDataImpl(
				boolean supportsScrollableResults,
				boolean supportsGetGeneratedKeys,
				boolean supportsBatchUpdates,
				boolean supportsDataDefinitionInTransaction,
				boolean doesDataDefinitionCauseTransactionCommit,
				Set<String> extraKeywords,
				SQLStateType sqlStateType,
				boolean lobLocatorUpdateCopy,
				String connectionSchemaName,
				String connectionCatalogName,
				LinkedHashSet<TypeInfo> typeInfoSet) {
			this.supportsScrollableResults = supportsScrollableResults;
			this.supportsGetGeneratedKeys = supportsGetGeneratedKeys;
			this.supportsBatchUpdates = supportsBatchUpdates;
			this.supportsDataDefinitionInTransaction = supportsDataDefinitionInTransaction;
			this.doesDataDefinitionCauseTransactionCommit = doesDataDefinitionCauseTransactionCommit;
			this.extraKeywords = extraKeywords;
			this.sqlStateType = sqlStateType;
			this.lobLocatorUpdateCopy = lobLocatorUpdateCopy;
			this.connectionSchemaName = connectionSchemaName;
			this.connectionCatalogName = connectionCatalogName;
			this.typeInfoSet = typeInfoSet;
		}

		@Override
		public boolean supportsScrollableResults() {
			return supportsScrollableResults;
		}

		@Override
		public boolean supportsGetGeneratedKeys() {
			return supportsGetGeneratedKeys;
		}

		@Override
		public boolean supportsBatchUpdates() {
			return supportsBatchUpdates;
		}

		@Override
		public boolean supportsDataDefinitionInTransaction() {
			return supportsDataDefinitionInTransaction;
		}

		@Override
		public boolean doesDataDefinitionCauseTransactionCommit() {
			return doesDataDefinitionCauseTransactionCommit;
		}

		@Override
		public Set<String> getExtraKeywords() {
			return extraKeywords;
		}

		@Override
		public SQLStateType getSqlStateType() {
			return sqlStateType;
		}

		@Override
		public boolean doesLobLocatorUpdateCopy() {
			return lobLocatorUpdateCopy;
		}

		@Override
		public String getConnectionSchemaName() {
			return connectionSchemaName;
		}

		@Override
		public String getConnectionCatalogName() {
			return connectionCatalogName;
		}

		@Override
		public LinkedHashSet<TypeInfo> getTypeInfoSet() {
			return typeInfoSet;
		}
	}

	@Override
	public ConnectionProvider getConnectionProvider() {
		return connectionProvider;
	}

	@Override
	public SqlStatementLogger getSqlStatementLogger() {
		return sqlStatementLogger;
	}

	@Override
	public SqlExceptionHelper getSqlExceptionHelper() {
		return sqlExceptionHelper;
	}

	@Override
	public Dialect getDialect() {
		return dialect;
	}

	@Override
	public ExtractedDatabaseMetaData getExtractedMetaDataSupport() {
		return extractedMetaDataSupport;
	}

	@Override
	public LobCreator getLobCreator(LobCreationContext lobCreationContext) {
		return lobCreatorBuilder.buildLobCreator( lobCreationContext );
	}

	@Override
	public ResultSetWrapper getResultSetWrapper() {
		return ResultSetWrapperImpl.INSTANCE;
	}
}
