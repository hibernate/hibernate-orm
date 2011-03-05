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

import java.io.Serializable;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.JdbcSupport;
import org.hibernate.bytecode.BytecodeProvider;
import org.hibernate.cache.QueryCacheFactory;
import org.hibernate.cache.RegionFactory;
import org.hibernate.cache.impl.NoCachingRegionFactory;
import org.hibernate.cache.impl.bridge.RegionFactoryCacheProviderBridge;
import org.hibernate.engine.jdbc.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.transaction.spi.TransactionFactory;
import org.hibernate.hql.QueryTranslatorFactory;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.jta.platform.spi.JtaPlatform;
import org.hibernate.service.spi.ServiceRegistry;
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

	public Settings buildSettings(Properties props, ServiceRegistry serviceRegistry) {
		final JdbcServices jdbcServices = serviceRegistry.getService( JdbcServices.class );
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
		settings.setJtaPlatform( serviceRegistry.getService( JtaPlatform.class ) );

		boolean flushBeforeCompletion = ConfigurationHelper.getBoolean(Environment.FLUSH_BEFORE_COMPLETION, properties);
		log.info("Automatic flush during beforeCompletion(): " + enabledDisabled(flushBeforeCompletion) );
		settings.setFlushBeforeCompletionEnabled(flushBeforeCompletion);

		boolean autoCloseSession = ConfigurationHelper.getBoolean(Environment.AUTO_CLOSE_SESSION, properties);
		log.info("Automatic session close at end of transaction: " + enabledDisabled(autoCloseSession) );
		settings.setAutoCloseSessionEnabled(autoCloseSession);

		//JDBC and connection settings:

		int batchSize = ConfigurationHelper.getInt(Environment.STATEMENT_BATCH_SIZE, properties, 0);
		if ( !meta.supportsBatchUpdates() ) batchSize = 0;
		if (batchSize>0) log.info("JDBC batch size: " + batchSize);
		settings.setJdbcBatchSize(batchSize);
		boolean jdbcBatchVersionedData = ConfigurationHelper.getBoolean(Environment.BATCH_VERSIONED_DATA, properties, false);
		if (batchSize>0) log.info("JDBC batch updates for versioned data: " + enabledDisabled(jdbcBatchVersionedData) );
		settings.setJdbcBatchVersionedData(jdbcBatchVersionedData);

		boolean useScrollableResultSets = ConfigurationHelper.getBoolean(Environment.USE_SCROLLABLE_RESULTSET, properties, meta.supportsScrollableResults());
		log.info("Scrollable result sets: " + enabledDisabled(useScrollableResultSets) );
		settings.setScrollableResultSetsEnabled(useScrollableResultSets);

		boolean wrapResultSets = ConfigurationHelper.getBoolean(Environment.WRAP_RESULT_SETS, properties, false);
		log.debug( "Wrap result sets: " + enabledDisabled(wrapResultSets) );
		settings.setWrapResultSetsEnabled(wrapResultSets);

		boolean useGetGeneratedKeys = ConfigurationHelper.getBoolean(Environment.USE_GET_GENERATED_KEYS, properties, meta.supportsGetGeneratedKeys());
		log.info("JDBC3 getGeneratedKeys(): " + enabledDisabled(useGetGeneratedKeys) );
		settings.setGetGeneratedKeysEnabled(useGetGeneratedKeys);

		Integer statementFetchSize = ConfigurationHelper.getInteger(Environment.STATEMENT_FETCH_SIZE, properties);
		if (statementFetchSize!=null) log.info("JDBC result set fetch size: " + statementFetchSize);
		settings.setJdbcFetchSize(statementFetchSize);

		String releaseModeName = ConfigurationHelper.getString( Environment.RELEASE_CONNECTIONS, properties, "auto" );
		log.info( "Connection release mode: " + releaseModeName );
		ConnectionReleaseMode releaseMode;
		if ( "auto".equals(releaseModeName) ) {
			releaseMode = serviceRegistry.getService( TransactionFactory.class ).getDefaultReleaseMode();
		}
		else {
			releaseMode = ConnectionReleaseMode.parse( releaseModeName );
			if ( releaseMode == ConnectionReleaseMode.AFTER_STATEMENT &&
					! jdbcServices.getConnectionProvider().supportsAggressiveRelease() ) {			
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

		Integer maxFetchDepth = ConfigurationHelper.getInteger(Environment.MAX_FETCH_DEPTH, properties);
		if (maxFetchDepth!=null) log.info("Maximum outer join fetch depth: " + maxFetchDepth);
		settings.setMaximumFetchDepth(maxFetchDepth);
		int batchFetchSize = ConfigurationHelper.getInt(Environment.DEFAULT_BATCH_FETCH_SIZE, properties, 1);
		log.info("Default batch fetch size: " + batchFetchSize);
		settings.setDefaultBatchFetchSize(batchFetchSize);

		boolean comments = ConfigurationHelper.getBoolean(Environment.USE_SQL_COMMENTS, properties);
		log.info( "Generate SQL with comments: " + enabledDisabled(comments) );
		settings.setCommentsEnabled(comments);

		boolean orderUpdates = ConfigurationHelper.getBoolean(Environment.ORDER_UPDATES, properties);
		log.info( "Order SQL updates by primary key: " + enabledDisabled(orderUpdates) );
		settings.setOrderUpdatesEnabled(orderUpdates);

		boolean orderInserts = ConfigurationHelper.getBoolean(Environment.ORDER_INSERTS, properties);
		log.info( "Order SQL inserts for batching: " + enabledDisabled( orderInserts ) );
		settings.setOrderInsertsEnabled( orderInserts );

		//Query parser settings:

		settings.setQueryTranslatorFactory( createQueryTranslatorFactory(properties) );

		Map querySubstitutions = ConfigurationHelper.toMap(Environment.QUERY_SUBSTITUTIONS, " ,=;:\n\t\r\f", properties);
		log.info("Query language substitutions: " + querySubstitutions);
		settings.setQuerySubstitutions(querySubstitutions);

		boolean jpaqlCompliance = ConfigurationHelper.getBoolean( Environment.JPAQL_STRICT_COMPLIANCE, properties, false );
		settings.setStrictJPAQLCompliance( jpaqlCompliance );
		log.info( "JPA-QL strict compliance: " + enabledDisabled( jpaqlCompliance ) );

		// Second-level / query cache:

		boolean useSecondLevelCache = ConfigurationHelper.getBoolean(Environment.USE_SECOND_LEVEL_CACHE, properties, true);
		log.info( "Second-level cache: " + enabledDisabled(useSecondLevelCache) );
		settings.setSecondLevelCacheEnabled(useSecondLevelCache);

		boolean useQueryCache = ConfigurationHelper.getBoolean(Environment.USE_QUERY_CACHE, properties);
		log.info( "Query cache: " + enabledDisabled(useQueryCache) );
		settings.setQueryCacheEnabled(useQueryCache);

		// The cache provider is needed when we either have second-level cache enabled
		// or query cache enabled.  Note that useSecondLevelCache is enabled by default
		settings.setRegionFactory( createRegionFactory( properties, ( useSecondLevelCache || useQueryCache ) ) );

		boolean useMinimalPuts = ConfigurationHelper.getBoolean(
				Environment.USE_MINIMAL_PUTS, properties, settings.getRegionFactory().isMinimalPutsEnabledByDefault()
		);
		log.info( "Optimize cache for minimal puts: " + enabledDisabled(useMinimalPuts) );
		settings.setMinimalPutsEnabled(useMinimalPuts);

		String prefix = properties.getProperty(Environment.CACHE_REGION_PREFIX);
		if ( StringHelper.isEmpty(prefix) ) prefix=null;
		if (prefix!=null) log.info("Cache region prefix: "+ prefix);
		settings.setCacheRegionPrefix(prefix);

		boolean useStructuredCacheEntries = ConfigurationHelper.getBoolean(Environment.USE_STRUCTURED_CACHE, properties, false);
		log.info( "Structured second-level cache entries: " + enabledDisabled(useStructuredCacheEntries) );
		settings.setStructuredCacheEntriesEnabled(useStructuredCacheEntries);

		if (useQueryCache) settings.setQueryCacheFactory( createQueryCacheFactory(properties) );

		//Statistics and logging:

		boolean useStatistics = ConfigurationHelper.getBoolean(Environment.GENERATE_STATISTICS, properties);
		log.info( "Statistics: " + enabledDisabled(useStatistics) );
		settings.setStatisticsEnabled(useStatistics);

		boolean useIdentifierRollback = ConfigurationHelper.getBoolean(Environment.USE_IDENTIFIER_ROLLBACK, properties);
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
		settings.setImportFiles( properties.getProperty( Environment.HBM2DDL_IMPORT_FILES ) );

		EntityMode defaultEntityMode = EntityMode.parse( properties.getProperty( Environment.DEFAULT_ENTITY_MODE ) );
		log.info( "Default entity-mode: " + defaultEntityMode );
		settings.setDefaultEntityMode( defaultEntityMode );

		boolean namedQueryChecking = ConfigurationHelper.getBoolean( Environment.QUERY_STARTUP_CHECKING, properties, true );
		log.info( "Named query checking : " + enabledDisabled( namedQueryChecking ) );
		settings.setNamedQueryStartupCheckingEnabled( namedQueryChecking );

		boolean checkNullability = ConfigurationHelper.getBoolean(Environment.CHECK_NULLABILITY, properties, true);
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
			log.debug( "using javassist as bytecode provider by default" );
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
		log.info("Query cache factory: " + queryCacheFactoryClassName);
		try {
			return (QueryCacheFactory) ReflectHelper.classForName(queryCacheFactoryClassName).newInstance();
		}
		catch (Exception cnfe) {
			throw new HibernateException("could not instantiate QueryCacheFactory: " + queryCacheFactoryClassName, cnfe);
		}
	}

	public static RegionFactory createRegionFactory(Properties properties, boolean cachingEnabled) {
		String regionFactoryClassName = ConfigurationHelper.getString(
				Environment.CACHE_REGION_FACTORY, properties, null
		);
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
		log.info( "Cache region factory : " + regionFactoryClassName );
		try {
			try {
				return (RegionFactory) ReflectHelper.classForName( regionFactoryClassName )
						.getConstructor( Properties.class )
						.newInstance( properties );
			}
			catch ( NoSuchMethodException nsme ) {
				// no constructor accepting Properties found, try no arg constructor
				log.debug(
						regionFactoryClassName + " did not provide constructor accepting java.util.Properties; " +
								"attempting no-arg constructor."
				);
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
		log.info("Query translator: " + className);
		try {
			return (QueryTranslatorFactory) ReflectHelper.classForName(className).newInstance();
		}
		catch (Exception cnfe) {
			throw new HibernateException("could not instantiate QueryTranslatorFactory: " + className, cnfe);
		}
	}

}
