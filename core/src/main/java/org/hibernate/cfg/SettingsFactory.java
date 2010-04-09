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
package org.hibernate.cfg;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.JdbcSupport;
import org.hibernate.engine.jdbc.JdbcSupportLoader;
import org.hibernate.bytecode.BytecodeProvider;
import org.hibernate.cache.QueryCacheFactory;
import org.hibernate.cache.RegionFactory;
import org.hibernate.cache.impl.NoCachingRegionFactory;
import org.hibernate.cache.impl.bridge.RegionFactoryCacheProviderBridge;
import org.hibernate.connection.ConnectionProvider;
import org.hibernate.connection.ConnectionProviderFactory;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.resolver.DialectFactory;
import org.hibernate.exception.SQLExceptionConverter;
import org.hibernate.exception.SQLExceptionConverterFactory;
import org.hibernate.hql.QueryTranslatorFactory;
import org.hibernate.jdbc.BatcherFactory;
import org.hibernate.jdbc.BatchingBatcherFactory;
import org.hibernate.jdbc.NonBatchingBatcherFactory;
import org.hibernate.jdbc.util.SQLStatementLogger;
import org.hibernate.transaction.TransactionFactory;
import org.hibernate.transaction.TransactionFactoryFactory;
import org.hibernate.transaction.TransactionManagerLookup;
import org.hibernate.transaction.TransactionManagerLookupFactory;
import org.hibernate.util.PropertiesHelper;
import org.hibernate.util.ReflectHelper;
import org.hibernate.util.StringHelper;

/**
 * Reads configuration properties and builds a {@link Settings} instance.
 *
 * @author Gavin King
 */
public class SettingsFactory implements Serializable {
	private static final Logger log = LoggerFactory.getLogger( SettingsFactory.class );
	private static final long serialVersionUID = -1194386144994524825L;

	public static final String DEF_CACHE_REG_FACTORY = NoCachingRegionFactory.class.getName();

	protected SettingsFactory() {
	}

	public Settings buildSettings(Properties props) {
		Settings settings = new Settings();

		//SessionFactory name:

		String sessionFactoryName = props.getProperty(Environment.SESSION_FACTORY_NAME);
		settings.setSessionFactoryName(sessionFactoryName);

		//JDBC and connection settings:

		ConnectionProvider connections = createConnectionProvider(props);
		settings.setConnectionProvider(connections);

		//Interrogate JDBC metadata

		boolean metaSupportsScrollable = false;
		boolean metaSupportsGetGeneratedKeys = false;
		boolean metaSupportsBatchUpdates = false;
		boolean metaReportsDDLCausesTxnCommit = false;
		boolean metaReportsDDLInTxnSupported = true;
		Dialect dialect = null;
		JdbcSupport jdbcSupport = null;

		// 'hibernate.temp.use_jdbc_metadata_defaults' is a temporary magic value.
		// The need for it is intended to be alleviated with future development, thus it is
		// not defined as an Environment constant...
		//
		// it is used to control whether we should consult the JDBC metadata to determine
		// certain Settings default values; it is useful to *not* do this when the database
		// may not be available (mainly in tools usage).
		boolean useJdbcMetadata = PropertiesHelper.getBoolean( "hibernate.temp.use_jdbc_metadata_defaults", props, true );
		if ( useJdbcMetadata ) {
			try {
				Connection conn = connections.getConnection();
				try {
					DatabaseMetaData meta = conn.getMetaData();
					log.info( "RDBMS: " + meta.getDatabaseProductName() + ", version: " + meta.getDatabaseProductVersion() );
					log.info( "JDBC driver: " + meta.getDriverName() + ", version: " + meta.getDriverVersion() );

					dialect = DialectFactory.buildDialect( props, conn );
					jdbcSupport = JdbcSupportLoader.loadJdbcSupport( conn );

					metaSupportsScrollable = meta.supportsResultSetType( ResultSet.TYPE_SCROLL_INSENSITIVE );
					metaSupportsBatchUpdates = meta.supportsBatchUpdates();
					metaReportsDDLCausesTxnCommit = meta.dataDefinitionCausesTransactionCommit();
					metaReportsDDLInTxnSupported = !meta.dataDefinitionIgnoredInTransactions();
					metaSupportsGetGeneratedKeys = meta.supportsGetGeneratedKeys();
				}
				catch ( SQLException sqle ) {
					log.warn( "Could not obtain connection metadata", sqle );
				}
				finally {
					connections.closeConnection( conn );
				}
			}
			catch ( SQLException sqle ) {
				log.warn( "Could not obtain connection to query metadata", sqle );
				dialect = DialectFactory.buildDialect( props );
			}
			catch ( UnsupportedOperationException uoe ) {
				// user supplied JDBC connections
				dialect = DialectFactory.buildDialect( props );
			}
		}
		else {
			dialect = DialectFactory.buildDialect( props );
		}

		settings.setDataDefinitionImplicitCommit( metaReportsDDLCausesTxnCommit );
		settings.setDataDefinitionInTransactionSupported( metaReportsDDLInTxnSupported );
		settings.setDialect( dialect );
		if ( jdbcSupport == null ) {
			jdbcSupport = JdbcSupportLoader.loadJdbcSupport( null );
		}
		settings.setJdbcSupport( jdbcSupport );

		//use dialect default properties
		final Properties properties = new Properties();
		properties.putAll( dialect.getDefaultProperties() );
		properties.putAll( props );

		// Transaction settings:

		TransactionFactory transactionFactory = createTransactionFactory(properties);
		settings.setTransactionFactory(transactionFactory);
		settings.setTransactionManagerLookup( createTransactionManagerLookup(properties) );

		boolean flushBeforeCompletion = PropertiesHelper.getBoolean(Environment.FLUSH_BEFORE_COMPLETION, properties);
		log.info("Automatic flush during beforeCompletion(): " + enabledDisabled(flushBeforeCompletion) );
		settings.setFlushBeforeCompletionEnabled(flushBeforeCompletion);

		boolean autoCloseSession = PropertiesHelper.getBoolean(Environment.AUTO_CLOSE_SESSION, properties);
		log.info("Automatic session close at end of transaction: " + enabledDisabled(autoCloseSession) );
		settings.setAutoCloseSessionEnabled(autoCloseSession);

		//JDBC and connection settings:

		int batchSize = PropertiesHelper.getInt(Environment.STATEMENT_BATCH_SIZE, properties, 0);
		if ( !metaSupportsBatchUpdates ) batchSize = 0;
		if (batchSize>0) log.info("JDBC batch size: " + batchSize);
		settings.setJdbcBatchSize(batchSize);
		boolean jdbcBatchVersionedData = PropertiesHelper.getBoolean(Environment.BATCH_VERSIONED_DATA, properties, false);
		if (batchSize>0) log.info("JDBC batch updates for versioned data: " + enabledDisabled(jdbcBatchVersionedData) );
		settings.setJdbcBatchVersionedData(jdbcBatchVersionedData);
		settings.setBatcherFactory( createBatcherFactory(properties, batchSize) );

		boolean useScrollableResultSets = PropertiesHelper.getBoolean(Environment.USE_SCROLLABLE_RESULTSET, properties, metaSupportsScrollable);
		log.info("Scrollable result sets: " + enabledDisabled(useScrollableResultSets) );
		settings.setScrollableResultSetsEnabled(useScrollableResultSets);

		boolean wrapResultSets = PropertiesHelper.getBoolean(Environment.WRAP_RESULT_SETS, properties, false);
		log.debug( "Wrap result sets: " + enabledDisabled(wrapResultSets) );
		settings.setWrapResultSetsEnabled(wrapResultSets);

		boolean useGetGeneratedKeys = PropertiesHelper.getBoolean(Environment.USE_GET_GENERATED_KEYS, properties, metaSupportsGetGeneratedKeys);
		log.info("JDBC3 getGeneratedKeys(): " + enabledDisabled(useGetGeneratedKeys) );
		settings.setGetGeneratedKeysEnabled(useGetGeneratedKeys);

		Integer statementFetchSize = PropertiesHelper.getInteger(Environment.STATEMENT_FETCH_SIZE, properties);
		if (statementFetchSize!=null) log.info("JDBC result set fetch size: " + statementFetchSize);
		settings.setJdbcFetchSize(statementFetchSize);

		String releaseModeName = PropertiesHelper.getString( Environment.RELEASE_CONNECTIONS, properties, "auto" );
		log.info( "Connection release mode: " + releaseModeName );
		ConnectionReleaseMode releaseMode;
		if ( "auto".equals(releaseModeName) ) {
			releaseMode = transactionFactory.getDefaultReleaseMode();
		}
		else {
			releaseMode = ConnectionReleaseMode.parse( releaseModeName );
			if ( releaseMode == ConnectionReleaseMode.AFTER_STATEMENT && !connections.supportsAggressiveRelease() ) {
				log.warn( "Overriding release mode as connection provider does not support 'after_statement'" );
				releaseMode = ConnectionReleaseMode.AFTER_TRANSACTION;
			}
		}
		settings.setConnectionReleaseMode( releaseMode );

		//SQL Generation settings:

		String defaultSchema = properties.getProperty(Environment.DEFAULT_SCHEMA);
		String defaultCatalog = properties.getProperty(Environment.DEFAULT_CATALOG);
		if (defaultSchema!=null) log.info("Default schema: " + defaultSchema);
		if (defaultCatalog!=null) log.info("Default catalog: " + defaultCatalog);
		settings.setDefaultSchemaName(defaultSchema);
		settings.setDefaultCatalogName(defaultCatalog);

		Integer maxFetchDepth = PropertiesHelper.getInteger(Environment.MAX_FETCH_DEPTH, properties);
		if (maxFetchDepth!=null) log.info("Maximum outer join fetch depth: " + maxFetchDepth);
		settings.setMaximumFetchDepth(maxFetchDepth);
		int batchFetchSize = PropertiesHelper.getInt(Environment.DEFAULT_BATCH_FETCH_SIZE, properties, 1);
		log.info("Default batch fetch size: " + batchFetchSize);
		settings.setDefaultBatchFetchSize(batchFetchSize);

		boolean comments = PropertiesHelper.getBoolean(Environment.USE_SQL_COMMENTS, properties);
		log.info( "Generate SQL with comments: " + enabledDisabled(comments) );
		settings.setCommentsEnabled(comments);

		boolean orderUpdates = PropertiesHelper.getBoolean(Environment.ORDER_UPDATES, properties);
		log.info( "Order SQL updates by primary key: " + enabledDisabled(orderUpdates) );
		settings.setOrderUpdatesEnabled(orderUpdates);

		boolean orderInserts = PropertiesHelper.getBoolean(Environment.ORDER_INSERTS, properties);
		log.info( "Order SQL inserts for batching: " + enabledDisabled( orderInserts ) );
		settings.setOrderInsertsEnabled( orderInserts );

		//Query parser settings:

		settings.setQueryTranslatorFactory( createQueryTranslatorFactory(properties) );

		Map querySubstitutions = PropertiesHelper.toMap(Environment.QUERY_SUBSTITUTIONS, " ,=;:\n\t\r\f", properties);
		log.info("Query language substitutions: " + querySubstitutions);
		settings.setQuerySubstitutions(querySubstitutions);

		boolean jpaqlCompliance = PropertiesHelper.getBoolean( Environment.JPAQL_STRICT_COMPLIANCE, properties, false );
		settings.setStrictJPAQLCompliance( jpaqlCompliance );
		log.info( "JPA-QL strict compliance: " + enabledDisabled( jpaqlCompliance ) );

		// Second-level / query cache:

		boolean useSecondLevelCache = PropertiesHelper.getBoolean(Environment.USE_SECOND_LEVEL_CACHE, properties, true);
		log.info( "Second-level cache: " + enabledDisabled(useSecondLevelCache) );
		settings.setSecondLevelCacheEnabled(useSecondLevelCache);

		boolean useQueryCache = PropertiesHelper.getBoolean(Environment.USE_QUERY_CACHE, properties);
		log.info( "Query cache: " + enabledDisabled(useQueryCache) );
		settings.setQueryCacheEnabled(useQueryCache);

		// The cache provider is needed when we either have second-level cache enabled
		// or query cache enabled.  Note that useSecondLevelCache is enabled by default
		settings.setRegionFactory( createRegionFactory( properties, ( useSecondLevelCache || useQueryCache ) ) );

		boolean useMinimalPuts = PropertiesHelper.getBoolean(
				Environment.USE_MINIMAL_PUTS, properties, settings.getRegionFactory().isMinimalPutsEnabledByDefault()
		);
		log.info( "Optimize cache for minimal puts: " + enabledDisabled(useMinimalPuts) );
		settings.setMinimalPutsEnabled(useMinimalPuts);

		String prefix = properties.getProperty(Environment.CACHE_REGION_PREFIX);
		if ( StringHelper.isEmpty(prefix) ) prefix=null;
		if (prefix!=null) log.info("Cache region prefix: "+ prefix);
		settings.setCacheRegionPrefix(prefix);

		boolean useStructuredCacheEntries = PropertiesHelper.getBoolean(Environment.USE_STRUCTURED_CACHE, properties, false);
		log.info( "Structured second-level cache entries: " + enabledDisabled(useStructuredCacheEntries) );
		settings.setStructuredCacheEntriesEnabled(useStructuredCacheEntries);

		if (useQueryCache) settings.setQueryCacheFactory( createQueryCacheFactory(properties) );

		//SQL Exception converter:

		SQLExceptionConverter sqlExceptionConverter;
		try {
			sqlExceptionConverter = SQLExceptionConverterFactory.buildSQLExceptionConverter( dialect, properties );
		}
		catch(HibernateException e) {
			log.warn("Error building SQLExceptionConverter; using minimal converter");
			sqlExceptionConverter = SQLExceptionConverterFactory.buildMinimalSQLExceptionConverter();
		}
		settings.setSQLExceptionConverter(sqlExceptionConverter);

		//Statistics and logging:

		boolean showSql = PropertiesHelper.getBoolean(Environment.SHOW_SQL, properties);
		if (showSql) log.info("Echoing all SQL to stdout");
//		settings.setShowSqlEnabled(showSql);

		boolean formatSql = PropertiesHelper.getBoolean(Environment.FORMAT_SQL, properties);
//		settings.setFormatSqlEnabled(formatSql);

		settings.setSqlStatementLogger( new SQLStatementLogger( showSql, formatSql ) );

		boolean useStatistics = PropertiesHelper.getBoolean(Environment.GENERATE_STATISTICS, properties);
		log.info( "Statistics: " + enabledDisabled(useStatistics) );
		settings.setStatisticsEnabled(useStatistics);

		boolean useIdentifierRollback = PropertiesHelper.getBoolean(Environment.USE_IDENTIFIER_ROLLBACK, properties);
		log.info( "Deleted entity synthetic identifier rollback: " + enabledDisabled(useIdentifierRollback) );
		settings.setIdentifierRollbackEnabled(useIdentifierRollback);

		//Schema export:

		String autoSchemaExport = properties.getProperty(Environment.HBM2DDL_AUTO);
		if ( "validate".equals(autoSchemaExport) ) settings.setAutoValidateSchema(true);
		if ( "update".equals(autoSchemaExport) ) settings.setAutoUpdateSchema(true);
		if ( "create".equals(autoSchemaExport) ) settings.setAutoCreateSchema(true);
		if ( "create-drop".equals(autoSchemaExport) ) {
			settings.setAutoCreateSchema(true);
			settings.setAutoDropSchema(true);
		}

		EntityMode defaultEntityMode = EntityMode.parse( properties.getProperty( Environment.DEFAULT_ENTITY_MODE ) );
		log.info( "Default entity-mode: " + defaultEntityMode );
		settings.setDefaultEntityMode( defaultEntityMode );

		boolean namedQueryChecking = PropertiesHelper.getBoolean( Environment.QUERY_STARTUP_CHECKING, properties, true );
		log.info( "Named query checking : " + enabledDisabled( namedQueryChecking ) );
		settings.setNamedQueryStartupCheckingEnabled( namedQueryChecking );

		boolean checkNullability = PropertiesHelper.getBoolean(Environment.CHECK_NULLABILITY, properties, true);
		log.info( "Check Nullability in Core (should be disabled when Bean Validation is on): " + enabledDisabled(checkNullability) );
		settings.setCheckNullability(checkNullability);


//		String provider = properties.getProperty( Environment.BYTECODE_PROVIDER );
//		log.info( "Bytecode provider name : " + provider );
//		BytecodeProvider bytecodeProvider = buildBytecodeProvider( provider );
//		settings.setBytecodeProvider( bytecodeProvider );

		return settings;

	}

	protected BytecodeProvider buildBytecodeProvider(String providerName) {
		if ( "javassist".equals( providerName ) ) {
			return new org.hibernate.bytecode.javassist.BytecodeProviderImpl();
		}
		else if ( "cglib".equals( providerName ) ) {
			return new org.hibernate.bytecode.cglib.BytecodeProviderImpl();
		}
		else {
			log.debug( "using cglib as bytecode provider by default" );
			return new org.hibernate.bytecode.cglib.BytecodeProviderImpl();
		}
	}

	private static String enabledDisabled(boolean value) {
		return value ? "enabled" : "disabled";
	}

	protected QueryCacheFactory createQueryCacheFactory(Properties properties) {
		String queryCacheFactoryClassName = PropertiesHelper.getString(
				Environment.QUERY_CACHE_FACTORY, properties, "org.hibernate.cache.StandardQueryCacheFactory"
		);
		log.info("Query cache factory: " + queryCacheFactoryClassName);
		try {
			return (QueryCacheFactory) ReflectHelper.classForName(queryCacheFactoryClassName).newInstance();
		}
		catch (Exception cnfe) {
			throw new HibernateException("could not instantiate QueryCacheFactory: " + queryCacheFactoryClassName, cnfe);
		}
	}

	public static RegionFactory createRegionFactory(Properties properties, boolean cachingEnabled) {
		String regionFactoryClassName = PropertiesHelper.getString( Environment.CACHE_REGION_FACTORY, properties, null );
		if ( regionFactoryClassName == null && cachingEnabled ) {
			String providerClassName = PropertiesHelper.getString( Environment.CACHE_PROVIDER, properties, null );
			if ( providerClassName != null ) {
				// legacy behavior, apply the bridge...
				regionFactoryClassName = RegionFactoryCacheProviderBridge.class.getName();
			}
		}
		if ( regionFactoryClassName == null ) {
			regionFactoryClassName = DEF_CACHE_REG_FACTORY;
		}
		log.info( "Cache region factory : " + regionFactoryClassName );
		try {
			return ( RegionFactory ) ReflectHelper.classForName( regionFactoryClassName )
					.getConstructor( new Class[] { Properties.class } )
					.newInstance( new Object[] { properties } );
		}
		catch ( Exception e ) {
			throw new HibernateException( "could not instantiate RegionFactory [" + regionFactoryClassName + "]", e );
		}
	}

	protected QueryTranslatorFactory createQueryTranslatorFactory(Properties properties) {
		String className = PropertiesHelper.getString(
				Environment.QUERY_TRANSLATOR, properties, "org.hibernate.hql.ast.ASTQueryTranslatorFactory"
		);
		log.info("Query translator: " + className);
		try {
			return (QueryTranslatorFactory) ReflectHelper.classForName(className).newInstance();
		}
		catch (Exception cnfe) {
			throw new HibernateException("could not instantiate QueryTranslatorFactory: " + className, cnfe);
		}
	}

	protected BatcherFactory createBatcherFactory(Properties properties, int batchSize) {
		String batcherClass = properties.getProperty(Environment.BATCH_STRATEGY);
		if (batcherClass==null) {
			return batchSize==0 ?
					(BatcherFactory) new NonBatchingBatcherFactory() :
					(BatcherFactory) new BatchingBatcherFactory();
		}
		else {
			log.info("Batcher factory: " + batcherClass);
			try {
				return (BatcherFactory) ReflectHelper.classForName(batcherClass).newInstance();
			}
			catch (Exception cnfe) {
				throw new HibernateException("could not instantiate BatcherFactory: " + batcherClass, cnfe);
			}
		}
	}

	protected ConnectionProvider createConnectionProvider(Properties properties) {
		return ConnectionProviderFactory.newConnectionProvider(properties);
	}

	protected TransactionFactory createTransactionFactory(Properties properties) {
		return TransactionFactoryFactory.buildTransactionFactory(properties);
	}

	protected TransactionManagerLookup createTransactionManagerLookup(Properties properties) {
		return TransactionManagerLookupFactory.getTransactionManagerLookup(properties);
	}

}
