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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.hibernate.HibernateLogger;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.ResultSetWrapper;
import org.hibernate.engine.jdbc.spi.SchemaNameResolver;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.jdbc.dialect.spi.DialectFactory;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.InjectService;
import org.jboss.logging.Logger;

/**
 * Standard implementation of the {@link JdbcServices} contract
 *
 * @author Steve Ebersole
 */
public class JdbcServicesImpl implements JdbcServices, Configurable {

    private static final HibernateLogger LOG = Logger.getMessageLogger(HibernateLogger.class, JdbcServicesImpl.class.getName());

	private ConnectionProvider connectionProvider;

	@InjectService
	public void setConnectionProvider(ConnectionProvider connectionProvider) {
		this.connectionProvider = connectionProvider;
	}

	private DialectFactory dialectFactory;

	@InjectService
	public void setDialectFactory(DialectFactory dialectFactory) {
		this.dialectFactory = dialectFactory;
	}

	private Dialect dialect;
	private SqlStatementLogger sqlStatementLogger;
	private SqlExceptionHelper sqlExceptionHelper;
	private ExtractedDatabaseMetaData extractedMetaDataSupport;
	private LobCreatorBuilder lobCreatorBuilder;

	public void configure(Map configValues) {
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
				Connection conn = connectionProvider.getConnection();
				try {
					DatabaseMetaData meta = conn.getMetaData();
                    LOG.database(meta.getDatabaseProductName(),
                                 meta.getDatabaseProductVersion(),
                                 meta.getDatabaseMajorVersion(),
                                 meta.getDatabaseMinorVersion());
                    LOG.driver(meta.getDriverName(),
                               meta.getDriverVersion(),
                               meta.getDriverMajorVersion(),
                               meta.getDriverMinorVersion());
                    LOG.jdbcVersion(meta.getJDBCMajorVersion(), meta.getJDBCMinorVersion());

					metaSupportsScrollable = meta.supportsResultSetType( ResultSet.TYPE_SCROLL_INSENSITIVE );
					metaSupportsBatchUpdates = meta.supportsBatchUpdates();
					metaReportsDDLCausesTxnCommit = meta.dataDefinitionCausesTransactionCommit();
					metaReportsDDLInTxnSupported = !meta.dataDefinitionIgnoredInTransactions();
					metaSupportsGetGeneratedKeys = meta.supportsGetGeneratedKeys();
					extraKeywordsString = meta.getSQLKeywords();
					sqlStateType = meta.getSQLStateType();
					lobLocatorUpdateCopy = meta.locatorsUpdateCopy();
					typeInfoSet.addAll( TypeInfoExtracter.extractTypeInfo( meta ) );

					dialect = dialectFactory.buildDialect( configValues, conn );

					catalogName = conn.getCatalog();
					SchemaNameResolver schemaNameResolver = determineExplicitSchemaNameResolver( configValues );
					if ( schemaNameResolver == null ) {
// todo : add dialect method
//						schemaNameResolver = dialect.getSchemaNameResolver();
					}
					if ( schemaNameResolver != null ) {
						schemaName = schemaNameResolver.resolveSchemaName( conn );
					}
					lobCreatorBuilder = new LobCreatorBuilder( configValues, conn );
				}
				catch ( SQLException sqle ) {
                    LOG.unableToObtainConnectionMetadata(sqle.getMessage());
				}
				finally {
					connectionProvider.closeConnection( conn );
				}
			}
			catch ( SQLException sqle ) {
                LOG.unableToObtainConnectionToQueryMetadata(sqle.getMessage());
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
		this.sqlExceptionHelper = new SqlExceptionHelper( dialect.buildSQLExceptionConverter() );
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
                LOG.unableToLocateConfiguredSchemaNameResolver(resolverClassName, e.toString());
			}
			catch ( InvocationTargetException e ) {
                LOG.unableToInstantiateConfiguredSchemaNameResolver(resolverClassName, e.getTargetException().toString());
			}
			catch ( Exception e ) {
                LOG.unableToInstantiateConfiguredSchemaNameResolver(resolverClassName, e.toString());
			}
		}
		return null;
	}

	private Set<String> parseKeywords(String extraKeywordsString) {
		Set<String> keywordSet = new HashSet<String>();
		for ( String keyword : extraKeywordsString.split( "," ) ) {
			keywordSet.add( keyword );
		}
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

		public boolean supportsScrollableResults() {
			return supportsScrollableResults;
		}

		public boolean supportsGetGeneratedKeys() {
			return supportsGetGeneratedKeys;
		}

		public boolean supportsBatchUpdates() {
			return supportsBatchUpdates;
		}

		public boolean supportsDataDefinitionInTransaction() {
			return supportsDataDefinitionInTransaction;
		}

		public boolean doesDataDefinitionCauseTransactionCommit() {
			return doesDataDefinitionCauseTransactionCommit;
		}

		public Set<String> getExtraKeywords() {
			return extraKeywords;
		}

		public SQLStateType getSqlStateType() {
			return sqlStateType;
		}

		public boolean doesLobLocatorUpdateCopy() {
			return lobLocatorUpdateCopy;
		}

		public String getConnectionSchemaName() {
			return connectionSchemaName;
		}

		public String getConnectionCatalogName() {
			return connectionCatalogName;
		}

		public LinkedHashSet<TypeInfo> getTypeInfoSet() {
			return typeInfoSet;
		}
	}

	public ConnectionProvider getConnectionProvider() {
		return connectionProvider;
	}

	public SqlStatementLogger getSqlStatementLogger() {
		return sqlStatementLogger;
	}

	public SqlExceptionHelper getSqlExceptionHelper() {
		return sqlExceptionHelper;
	}

	public Dialect getDialect() {
		return dialect;
	}

	public ExtractedDatabaseMetaData getExtractedMetaDataSupport() {
		return extractedMetaDataSupport;
	}

	/**
	 * {@inheritDoc}
	 */
	public LobCreator getLobCreator(LobCreationContext lobCreationContext) {
		return lobCreatorBuilder.buildLobCreator( lobCreationContext );
	}

	/**
	 * {@inheritDoc}
	 */
	public ResultSetWrapper getResultSetWrapper() {
		return ResultSetWrapperImpl.INSTANCE;
	}
}
