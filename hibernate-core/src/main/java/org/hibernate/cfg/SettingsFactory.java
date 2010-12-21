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
package org.hibernate.cfg;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.bytecode.BytecodeProvider;
import org.hibernate.cache.QueryCacheFactory;
import org.hibernate.cache.RegionFactory;
import org.hibernate.cache.impl.NoCachingRegionFactory;
import org.hibernate.cache.impl.bridge.RegionFactoryCacheProviderBridge;
import org.hibernate.engine.jdbc.JdbcSupport;
import org.hibernate.engine.jdbc.batch.internal.BatchBuilder;
import org.hibernate.engine.jdbc.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.hql.QueryTranslatorFactory;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.jdbc.util.SQLStatementLogger;
import org.hibernate.transaction.TransactionFactory;
import org.hibernate.transaction.TransactionFactoryFactory;
import org.hibernate.transaction.TransactionManagerLookup;
import org.hibernate.transaction.TransactionManagerLookupFactory;
import org.hibernate.util.ReflectHelper;
import org.hibernate.util.StringHelper;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * Reads configuration properties and builds a {@link Settings} instance.
 *
 * @author Gavin King
 */
public class SettingsFactory implements Serializable {

    private static final long serialVersionUID = -1194386144994524825L;

    private static final Logger LOG = org.jboss.logging.Logger.getMessageLogger(Logger.class,
                                                                                SettingsFactory.class.getPackage().getName());

	public static final String DEF_CACHE_REG_FACTORY = NoCachingRegionFactory.class.getName();

	protected SettingsFactory() {
	}

	public Settings buildSettings(Properties props, JdbcServices jdbcServices) {
		Settings settings = new Settings();

		//SessionFactory name:

		String sessionFactoryName = props.getProperty(Environment.SESSION_FACTORY_NAME);
		settings.setSessionFactoryName(sessionFactoryName);

		//JDBC and connection settings:

		//Interrogate JDBC metadata
		ExtractedDatabaseMetaData meta = jdbcServices.getExtractedMetaDataSupport();

		settings.setDataDefinitionImplicitCommit( meta.doesDataDefinitionCauseTransactionCommit() );
		settings.setDataDefinitionInTransactionSupported( meta.supportsDataDefinitionInTransaction() );

		//use dialect default properties
		final Properties properties = new Properties();
		properties.putAll( jdbcServices.getDialect().getDefaultProperties() );
		properties.putAll( props );

		settings.setJdbcSupport( new JdbcSupport( ! ConfigurationHelper.getBoolean( Environment.NON_CONTEXTUAL_LOB_CREATION, properties ) ) );

		// Transaction settings:

		TransactionFactory transactionFactory = createTransactionFactory(properties);
		settings.setTransactionFactory(transactionFactory);
		settings.setTransactionManagerLookup( createTransactionManagerLookup(properties) );

		boolean flushBeforeCompletion = ConfigurationHelper.getBoolean(Environment.FLUSH_BEFORE_COMPLETION, properties);
        LOG.autoFlush(enabledDisabled(flushBeforeCompletion));
		settings.setFlushBeforeCompletionEnabled(flushBeforeCompletion);

		boolean autoCloseSession = ConfigurationHelper.getBoolean(Environment.AUTO_CLOSE_SESSION, properties);
        LOG.autoSessionClose(enabledDisabled(autoCloseSession));
		settings.setAutoCloseSessionEnabled(autoCloseSession);

		//JDBC and connection settings:

		int batchSize = ConfigurationHelper.getInt(Environment.STATEMENT_BATCH_SIZE, properties, 0);
		if ( !meta.supportsBatchUpdates() ) batchSize = 0;
		if (batchSize>0) LOG.jdbcBatchSize(batchSize);
		settings.setJdbcBatchSize(batchSize);
		boolean jdbcBatchVersionedData = ConfigurationHelper.getBoolean(Environment.BATCH_VERSIONED_DATA, properties, false);
        if (batchSize > 0) LOG.jdbcBatchUpdates(enabledDisabled(jdbcBatchVersionedData));
		settings.setJdbcBatchVersionedData(jdbcBatchVersionedData);
		settings.setBatcherBuilder( createBatchBuilder(properties, batchSize) );

		boolean useScrollableResultSets = ConfigurationHelper.getBoolean(Environment.USE_SCROLLABLE_RESULTSET, properties, meta.supportsScrollableResults());
        LOG.scrollabelResultSets(enabledDisabled(useScrollableResultSets));
		settings.setScrollableResultSetsEnabled(useScrollableResultSets);

		boolean wrapResultSets = ConfigurationHelper.getBoolean(Environment.WRAP_RESULT_SETS, properties, false);
        LOG.wrapResultSets(enabledDisabled(wrapResultSets));
		settings.setWrapResultSetsEnabled(wrapResultSets);

		boolean useGetGeneratedKeys = ConfigurationHelper.getBoolean(Environment.USE_GET_GENERATED_KEYS, properties, meta.supportsGetGeneratedKeys());
        LOG.jdbc3GeneratedKeys(enabledDisabled(useGetGeneratedKeys));
		settings.setGetGeneratedKeysEnabled(useGetGeneratedKeys);

		Integer statementFetchSize = ConfigurationHelper.getInteger(Environment.STATEMENT_FETCH_SIZE, properties);
        if (statementFetchSize != null) LOG.jdbcResultSetFetchSize(statementFetchSize);
		settings.setJdbcFetchSize(statementFetchSize);

		String releaseModeName = ConfigurationHelper.getString( Environment.RELEASE_CONNECTIONS, properties, "auto" );
        LOG.connectionReleaseMode(releaseModeName);
		ConnectionReleaseMode releaseMode;
		if ( "auto".equals(releaseModeName) ) {
			releaseMode = transactionFactory.getDefaultReleaseMode();
		}
		else {
			releaseMode = ConnectionReleaseMode.parse( releaseModeName );
			if ( releaseMode == ConnectionReleaseMode.AFTER_STATEMENT &&
					! jdbcServices.getConnectionProvider().supportsAggressiveRelease() ) {
                LOG.unsupportedAfterStatement();
				releaseMode = ConnectionReleaseMode.AFTER_TRANSACTION;
			}
		}
		settings.setConnectionReleaseMode( releaseMode );

		//SQL Generation settings:

		String defaultSchema = properties.getProperty(Environment.DEFAULT_SCHEMA);
		String defaultCatalog = properties.getProperty(Environment.DEFAULT_CATALOG);
        if (defaultSchema != null) LOG.defaultSchema(defaultSchema);
        if (defaultCatalog != null) LOG.defaultCatalog(defaultCatalog);
		settings.setDefaultSchemaName(defaultSchema);
		settings.setDefaultCatalogName(defaultCatalog);

		Integer maxFetchDepth = ConfigurationHelper.getInteger(Environment.MAX_FETCH_DEPTH, properties);
        if (maxFetchDepth != null) LOG.maxOuterJoinFetchDepth(maxFetchDepth);
		settings.setMaximumFetchDepth(maxFetchDepth);
		int batchFetchSize = ConfigurationHelper.getInt(Environment.DEFAULT_BATCH_FETCH_SIZE, properties, 1);
        LOG.defaultBatchFetchSize(batchFetchSize);
		settings.setDefaultBatchFetchSize(batchFetchSize);

		boolean comments = ConfigurationHelper.getBoolean(Environment.USE_SQL_COMMENTS, properties);
        LOG.generateSqlWithComments(enabledDisabled(comments));
		settings.setCommentsEnabled(comments);

		boolean orderUpdates = ConfigurationHelper.getBoolean(Environment.ORDER_UPDATES, properties);
        LOG.orderSqlUpdatesByPrimaryKey(enabledDisabled(orderUpdates));
		settings.setOrderUpdatesEnabled(orderUpdates);

		boolean orderInserts = ConfigurationHelper.getBoolean(Environment.ORDER_INSERTS, properties);
        LOG.orderSqlInsertsForBatching(enabledDisabled(orderInserts));
		settings.setOrderInsertsEnabled( orderInserts );

		//Query parser settings:

		settings.setQueryTranslatorFactory( createQueryTranslatorFactory(properties) );

        Map querySubstitutions = ConfigurationHelper.toMap(Environment.QUERY_SUBSTITUTIONS, " ,=;:\n\t\r\f", properties);
        LOG.queryLanguageSubstitutions(querySubstitutions);
		settings.setQuerySubstitutions(querySubstitutions);

		boolean jpaqlCompliance = ConfigurationHelper.getBoolean( Environment.JPAQL_STRICT_COMPLIANCE, properties, false );
		settings.setStrictJPAQLCompliance( jpaqlCompliance );
        LOG.jpaQlStrictCompliance(enabledDisabled(jpaqlCompliance));

		// Second-level / query cache:

		boolean useSecondLevelCache = ConfigurationHelper.getBoolean(Environment.USE_SECOND_LEVEL_CACHE, properties, true);
        LOG.secondLevelCache(enabledDisabled(useSecondLevelCache));
		settings.setSecondLevelCacheEnabled(useSecondLevelCache);

		boolean useQueryCache = ConfigurationHelper.getBoolean(Environment.USE_QUERY_CACHE, properties);
        LOG.queryCache(enabledDisabled(useQueryCache));
		settings.setQueryCacheEnabled(useQueryCache);

		// The cache provider is needed when we either have second-level cache enabled
		// or query cache enabled.  Note that useSecondLevelCache is enabled by default
		settings.setRegionFactory( createRegionFactory( properties, ( useSecondLevelCache || useQueryCache ) ) );

		boolean useMinimalPuts = ConfigurationHelper.getBoolean(
				Environment.USE_MINIMAL_PUTS, properties, settings.getRegionFactory().isMinimalPutsEnabledByDefault()
		);
        LOG.optimizeCacheForMinimalInputs(enabledDisabled(useMinimalPuts));
		settings.setMinimalPutsEnabled(useMinimalPuts);

		String prefix = properties.getProperty(Environment.CACHE_REGION_PREFIX);
		if ( StringHelper.isEmpty(prefix) ) prefix=null;
        if (prefix != null) LOG.cacheRegionPrefix(prefix);
		settings.setCacheRegionPrefix(prefix);

		boolean useStructuredCacheEntries = ConfigurationHelper.getBoolean(Environment.USE_STRUCTURED_CACHE, properties, false);
        LOG.structuredSecondLevelCacheEntries(enabledDisabled(useStructuredCacheEntries));
		settings.setStructuredCacheEntriesEnabled(useStructuredCacheEntries);

		if (useQueryCache) settings.setQueryCacheFactory( createQueryCacheFactory(properties) );

		//Statistics and logging:

		boolean showSql = ConfigurationHelper.getBoolean(Environment.SHOW_SQL, properties);
		if (showSql) LOG.echoingSql();
//		settings.setShowSqlEnabled(showSql);

		boolean formatSql = ConfigurationHelper.getBoolean(Environment.FORMAT_SQL, properties);
//		settings.setFormatSqlEnabled(formatSql);

		settings.setSqlStatementLogger( new SQLStatementLogger( showSql, formatSql ) );

		boolean useStatistics = ConfigurationHelper.getBoolean(Environment.GENERATE_STATISTICS, properties);
		LOG.statistics( enabledDisabled(useStatistics) );
		settings.setStatisticsEnabled(useStatistics);

		boolean useIdentifierRollback = ConfigurationHelper.getBoolean(Environment.USE_IDENTIFIER_ROLLBACK, properties);
        LOG.deletedEntitySyntheticIdentifierRollback(enabledDisabled(useIdentifierRollback));
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
		settings.setImportFiles( properties.getProperty( Environment.HBM2DDL_IMPORT_FILES ) );

		EntityMode defaultEntityMode = EntityMode.parse( properties.getProperty( Environment.DEFAULT_ENTITY_MODE ) );
        LOG.defaultEntityMode(defaultEntityMode);
		settings.setDefaultEntityMode( defaultEntityMode );

		boolean namedQueryChecking = ConfigurationHelper.getBoolean( Environment.QUERY_STARTUP_CHECKING, properties, true );
        LOG.namedQueryChecking(enabledDisabled(namedQueryChecking));
		settings.setNamedQueryStartupCheckingEnabled( namedQueryChecking );

		boolean checkNullability = ConfigurationHelper.getBoolean(Environment.CHECK_NULLABILITY, properties, true);
        LOG.checkNullability(enabledDisabled(checkNullability));
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
            LOG.usingJavassist();
			return new org.hibernate.bytecode.javassist.BytecodeProviderImpl();
		}
	}

	private static String enabledDisabled(boolean value) {
		return value ? "enabled" : "disabled";
	}

	protected QueryCacheFactory createQueryCacheFactory(Properties properties) {
		String queryCacheFactoryClassName = ConfigurationHelper.getString(
				Environment.QUERY_CACHE_FACTORY, properties, "org.hibernate.cache.StandardQueryCacheFactory"
		);
        LOG.queryCacheFactory(queryCacheFactoryClassName);
		try {
			return (QueryCacheFactory) ReflectHelper.classForName(queryCacheFactoryClassName).newInstance();
		}
		catch (Exception cnfe) {
			throw new HibernateException("could not instantiate QueryCacheFactory: " + queryCacheFactoryClassName, cnfe);
		}
	}

	public static RegionFactory createRegionFactory(Properties properties, boolean cachingEnabled) {
		String regionFactoryClassName = ConfigurationHelper.getString( Environment.CACHE_REGION_FACTORY, properties, null );
		if ( regionFactoryClassName == null && cachingEnabled ) {
			String providerClassName = ConfigurationHelper.getString( Environment.CACHE_PROVIDER, properties, null );
			if ( providerClassName != null ) {
				// legacy behavior, apply the bridge...
				regionFactoryClassName = RegionFactoryCacheProviderBridge.class.getName();
			}
		}
		if ( regionFactoryClassName == null ) {
			regionFactoryClassName = DEF_CACHE_REG_FACTORY;
		}
        LOG.cacheRegionFactory(regionFactoryClassName);
		try {
			try {
				return (RegionFactory) ReflectHelper.classForName( regionFactoryClassName )
						.getConstructor( Properties.class )
						.newInstance( properties );
			}
			catch ( NoSuchMethodException nsme ) {
				// no constructor accepting Properties found, try no arg constructor
                LOG.constructorWithPropertiesNotFound(regionFactoryClassName);
				return (RegionFactory) ReflectHelper.classForName( regionFactoryClassName ).newInstance();
			}
		}
		catch ( Exception e ) {
			throw new HibernateException( "could not instantiate RegionFactory [" + regionFactoryClassName + "]", e );
		}
	}

	protected QueryTranslatorFactory createQueryTranslatorFactory(Properties properties) {
		String className = ConfigurationHelper.getString(
				Environment.QUERY_TRANSLATOR, properties, "org.hibernate.hql.ast.ASTQueryTranslatorFactory"
		);
        LOG.queryTranslator(className);
		try {
			return (QueryTranslatorFactory) ReflectHelper.classForName(className).newInstance();
		}
		catch (Exception cnfe) {
			throw new HibernateException("could not instantiate QueryTranslatorFactory: " + className, cnfe);
		}
	}

	protected BatchBuilder createBatchBuilder(Properties properties, int batchSize) {
		String batchBuilderClass = properties.getProperty(Environment.BATCH_STRATEGY);
		BatchBuilder batchBuilder;
		if (batchBuilderClass==null) {
			batchBuilder = batchSize > 0
					? new BatchBuilder( batchSize )
					: new BatchBuilder();
		}
		else {
            LOG.batcherFactory(batchBuilderClass);
			try {
				batchBuilder = (BatchBuilder) ReflectHelper.classForName(batchBuilderClass).newInstance();
			}
			catch (Exception cnfe) {
				throw new HibernateException("could not instantiate BatchBuilder: " + batchBuilderClass, cnfe);
			}
		}
		batchBuilder.setJdbcBatchSize( batchSize );
		return batchBuilder;
	}

	protected TransactionFactory createTransactionFactory(Properties properties) {
		return TransactionFactoryFactory.buildTransactionFactory(properties);
	}

	protected TransactionManagerLookup createTransactionManagerLookup(Properties properties) {
		return TransactionManagerLookupFactory.getTransactionManagerLookup(properties);
	}

    /**
     * Interface defining messages that may be logged by the outer class
     */
    @MessageLogger
    interface Logger extends BasicLogger {

        @LogMessage( level = INFO )
        @Message( value = "Automatic flush during beforeCompletion(): %s" )
        void autoFlush( String enabledDisabled );

        @LogMessage( level = INFO )
        @Message( value = "Automatic session close at end of transaction: %s" )
        void autoSessionClose( String enabledDisabled );

        @LogMessage( level = INFO )
        @Message( value = "Batcher factory: %s" )
        void batcherFactory( String batcherClass );

        @LogMessage( level = INFO )
        @Message( value = "Cache region factory : %s" )
        void cacheRegionFactory( String regionFactoryClassName );

        @LogMessage( level = INFO )
        @Message( value = "Cache region prefix: %s" )
        void cacheRegionPrefix( String prefix );

        @LogMessage( level = INFO )
        @Message( value = "Check Nullability in Core (should be disabled when Bean Validation is on): %s" )
        void checkNullability( String enabledDisabled );

        @LogMessage( level = INFO )
        @Message( value = "Connection release mode: %s" )
        void connectionReleaseMode( String releaseModeName );

        @LogMessage( level = INFO )
        @Message( value = "%s did not provide constructor accepting java.util.Properties; attempting no-arg constructor." )
        void constructorWithPropertiesNotFound( String regionFactoryClassName );

        @LogMessage( level = INFO )
        // @formatter:off
        @Message( value = "Database ->\n" +
                          "       name : %s\n" +
                          "    version : %s\n" +
                          "      major : %s\n" +
                          "      minor : %s" )
        // @formatter:on
        void database( String databaseProductName,
                       String databaseProductVersion,
                       int databaseMajorVersion,
                       int databaseMinorVersion );

        @LogMessage( level = INFO )
        @Message( value = "Default batch fetch size: %s" )
        void defaultBatchFetchSize( int batchFetchSize );

        @LogMessage( level = INFO )
        @Message( value = "Default catalog: %s" )
        void defaultCatalog( String defaultCatalog );

        @LogMessage( level = INFO )
        @Message( value = "Default entity-mode: %s" )
        void defaultEntityMode( EntityMode defaultEntityMode );

        @LogMessage( level = INFO )
        @Message( value = "Default schema: %s" )
        void defaultSchema( String defaultSchema );

        @LogMessage( level = INFO )
        @Message( value = "Deleted entity synthetic identifier rollback: %s" )
        void deletedEntitySyntheticIdentifierRollback( String enabledDisabled );

        @LogMessage( level = INFO )
        // @formatter:off
        @Message( value = "Driver ->\n" +
                          "       name : %s\n" +
                          "    version : %s\n" +
                          "      major : %s\n" +
                          "      minor : %s" )
        // @formatter:on
        void driver( String driverProductName,
                     String driverProductVersion,
                     int driverMajorVersion,
                     int driverMinorVersion );

        @LogMessage( level = INFO )
        @Message( value = "Echoing all SQL to stdout" )
        void echoingSql();

        @LogMessage( level = INFO )
        @Message( value = "Generate SQL with comments: %s" )
        void generateSqlWithComments( String enabledDisabled );

        @LogMessage( level = INFO )
        @Message( value = "JDBC3 getGeneratedKeys(): %s" )
        void jdbc3GeneratedKeys( String enabledDisabled );

        @LogMessage( level = INFO )
        @Message( value = "JDBC batch size: %s" )
        void jdbcBatchSize( int batchSize );

        @LogMessage( level = INFO )
        @Message( value = "JDBC batch updates for versioned data: %s" )
        void jdbcBatchUpdates( String enabledDisabled );

        @LogMessage( level = INFO )
        @Message( value = "JDBC result set fetch size: %s" )
        void jdbcResultSetFetchSize( Integer statementFetchSize );

        @LogMessage( level = INFO )
        @Message( value = "JPA-QL strict compliance: %s" )
        void jpaQlStrictCompliance( String enabledDisabled );

        @LogMessage( level = INFO )
        @Message( value = "Maximum outer join fetch depth: %s" )
        void maxOuterJoinFetchDepth( Integer maxFetchDepth );

        @LogMessage( level = INFO )
        @Message( value = "Named query checking : %s" )
        void namedQueryChecking( String enabledDisabled );

        @LogMessage( level = INFO )
        @Message( value = "Optimize cache for minimal puts: %s" )
        void optimizeCacheForMinimalInputs( String enabledDisabled );

        @LogMessage( level = INFO )
        @Message( value = "Order SQL inserts for batching: %s" )
        void orderSqlInsertsForBatching( String enabledDisabled );

        @LogMessage( level = INFO )
        @Message( value = "Order SQL updates by primary key: %s" )
        void orderSqlUpdatesByPrimaryKey( String enabledDisabled );

        @LogMessage( level = INFO )
        @Message( value = "Query cache: %s" )
        void queryCache( String enabledDisabled );

        @LogMessage( level = INFO )
        @Message( value = "Query cache factory: %s" )
        void queryCacheFactory( String queryCacheFactoryClassName );

        @LogMessage( level = INFO )
        @Message( value = "Query language substitutions: %s" )
        void queryLanguageSubstitutions( Map<String, String> querySubstitutions );

        @LogMessage( level = INFO )
        @Message( value = "Query translator: %s" )
        void queryTranslator( String className );

        @LogMessage( level = INFO )
        @Message( value = "Scrollable result sets: %s" )
        void scrollabelResultSets( String enabledDisabled );

        @LogMessage( level = INFO )
        @Message( value = "Second-level cache: %s" )
        void secondLevelCache( String enabledDisabled );

        @LogMessage( level = INFO )
        @Message( value = "Statistics: %s" )
        void statistics( String enabledDisabled );

        @LogMessage( level = INFO )
        @Message( value = "Structured second-level cache entries: %s" )
        void structuredSecondLevelCacheEntries( String enabledDisabled );

        @LogMessage( level = WARN )
        @Message( value = "Could not obtain connection metadata: %s" )
        void unableToObjectConnectionMetadata( SQLException error );

        @LogMessage( level = WARN )
        @Message( value = "Could not obtain connection to query metadata: %s" )
        void unableToObjectConnectionToQueryMetadata( SQLException error );

        @LogMessage( level = WARN )
        @Message( value = "Overriding release mode as connection provider does not support 'after_statement'" )
        void unsupportedAfterStatement();

        @LogMessage( level = DEBUG )
        @Message( value = "Using javassist as bytecode provider by default" )
        void usingJavassist();

        @LogMessage( level = WARN )
        @Message( value = "Error building SQLExceptionConverter; using minimal converter" )
        void usingMinimalConverter();

        @LogMessage( level = INFO )
        @Message( value = "Wrap result sets: %s" )
        void wrapResultSets( String enabledDisabled );
    }
}
