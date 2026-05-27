/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.FetchType;
import jakarta.persistence.PersistenceConfiguration;
import jakarta.persistence.PersistenceUnitTransactionType;
import jakarta.persistence.SchemaManagementAction;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import org.hibernate.SessionFactory;
import org.hibernate.StatementObserver;
import org.hibernate.boot.scan.spi.ScanningProvider;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.CacheSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.cfg.JpaComplianceSettings;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.cfg.PersistenceSettings;
import org.hibernate.cfg.SchemaToolingSettings;
import org.hibernate.cfg.StatisticsSettings;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.tool.schema.Action;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * Extends the Jakarta Persistence-defined {@link PersistenceConfiguration}
 * with operations specific to Hibernate.
 * <p>
 * An instance of {@code Configuration} may be obtained simply by
 * {@linkplain #HibernatePersistenceConfiguration(String) instantiation},
 * and may be used to aggregate:
 * <ul>
 * <li>{@linkplain #property(String, Object) configuration properties}
 *     from various sources, and
 * <li>entity O/R mappings, defined in either
 *     {@linkplain #managedClasses(Class...) annotated classes}, or
 *     {@linkplain #mappingFiles(Collection) XML mapping documents}.
 * </ul>
 * <p>
 * Standard JPA configuration properties are enumerated by the supertype
 * {@link PersistenceConfiguration}. All configuration properties understood
 * by Hibernate are enumerated by {@link AvailableSettings}.
 * <pre>{@code
 * SessionFactory factory = new HibernatePersistenceConfiguration()
 *     // scan classes for mapping annotations
 *     .managedClasses(Item.class, Bid.class, User.class)
 *     // set a configuration property
 *     .setProperty(PersistenceConfiguration.JDBC_DATASOURCE,
 *                  "java:comp/env/jdbc/test")
 *     .buildSessionFactory();
 * }</pre>
 * <p>
 * When instantiated, an instance of
 * {@code HibernatePersistenceConfiguration} has its properties initially
 * populated from the {@linkplain Environment#getProperties() environment},
 * including:
 * <ul>
 * <li>JVM {@linkplain System#getProperties() system properties}, and
 * <li>properties specified in {@code hibernate.properties}.
 * </ul>
 * <p>
 * When a {@linkplain #rootUrl() root URL} is supplied, or when at least
 * one {@linkplain #jarFileUrls() JAR file URL} is supplied, and when
 * {@code hibernate-scan-jandex} or some other service implementing
 * {@link ScanningProvider} is available,
 * the given URLs are scanned for entity classes, embeddable classes,
 * mapped superclasses, converters, and XML mappings, alleviating the
 * need to call {@link #managedClass} or {@link #mappingFile}.
 *
 * @apiNote The specification explicitly encourages implementors to extend
 *          {@link PersistenceConfiguration} to accommodate vendor-specific
 *          extensions in a more typesafe way. Of course, programs which
 *          desire configuration logic to be portable between JPA providers
 *          should use {@code PersistenceConfiguration} directly.
 *
 * @author Steve Ebersole
 *
 * @since 7.0
 */
public class HibernatePersistenceConfiguration extends PersistenceConfiguration {

	private final URL rootUrl;
	private final List<URL> jarFileUrls = new ArrayList<>();

	/**
	 * Create a new empty configuration. An empty configuration does not
	 * typically hold enough information for successful invocation of
	 * {@link #createEntityManagerFactory()}.
	 *
	 * @param name the name of the persistence unit, which may be used by
	 * the persistence provider for logging and error reporting
	 */
	public HibernatePersistenceConfiguration(@Nonnull String name) {
		super( name );
		this.rootUrl = null;
	}

	/**
	 * Create a new empty configuration with a given {@linkplain #rootUrl root URL}
	 * used for {@linkplain PersistenceSettings#SCANNER entity discovery}
	 * via scanning.
	 * <p>
	 * The module {@code hibernate-scan-jandex} must be added as a dependency,
	 * or some other implementation of the service
	 * {@link ScanningProvider} must be made
	 * available.
	 *
	 * @param name the name of the persistence unit, which may be used by
	 * the persistence provider for logging and error reporting
	 * @param rootURL the root URL of the persistence unit
	 *
	 * @since 7.1
	 */
	public HibernatePersistenceConfiguration(@Nonnull String name, @Nullable URL rootURL) {
		super( name );
		this.rootUrl = rootURL;
	}

	/**
	 * Create a new empty configuration with the {@linkplain #rootUrl root URL}
	 * inferred from the given class file and used for
	 * {@linkplain PersistenceSettings#SCANNER entity discovery} via scanning.
	 * <p>
	 * The module {@code hibernate-scan-jandex} must be added as a dependency,
	 * or some other implementation of the service
	 * {@link ScanningProvider} must be made
	 * available.
	 *
	 * @param name the name of the persistence unit, which may be used by
	 * the persistence provider for logging and error reporting
	 * @param classFromRootUrl a class loaded from the root URL of the
	 * persistence unit
	 *
	 * @since 7.1
	 */
	public HibernatePersistenceConfiguration(@Nonnull String name, @Nonnull Class<?> classFromRootUrl) {
		this( name, classFromRootUrl.getProtectionDomain().getCodeSource().getLocation() );
	}

	/**
	 * Create a new {@link SessionFactory} based on this configuration.
	 */
	@Override
	@Nonnull
	public SessionFactory createEntityManagerFactory() {
		new EntityManagerFactoryBuilderImpl( this ).build();

		return (SessionFactory) super.createEntityManagerFactory();
	}

	/**
	 * JDBC driver class name. This setting is ignored when Hibernate is configured
	 * to obtain connections from a {@link javax.sql.DataSource}.
	 *
	 * @see #JDBC_DRIVER
	 */
	@Nonnull
	public HibernatePersistenceConfiguration jdbcDriver(String driverName) {
		property( JDBC_DRIVER, driverName );
		return this;
	}

	/**
	 * JDBC URL. This setting is ignored when Hibernate is configured to obtain
	 * connections from a {@link javax.sql.DataSource}.
	 *
	 * @see #JDBC_URL
	 */
	@Nonnull
	public HibernatePersistenceConfiguration jdbcUrl(String url) {
		property( JDBC_URL, url );
		return this;
	}

	/**
	 * Username for JDBC authentication.
	 *
	 * @see #JDBC_USER
	 * @see #jdbcPassword
	 * @see java.sql.DriverManager#getConnection(String, String, String)
	 * @see javax.sql.DataSource#getConnection(String, String)
	 */
	@Nonnull
	public HibernatePersistenceConfiguration jdbcUsername(String username) {
		property( JDBC_USER, username );
		return this;
	}

	/**
	 * Password for JDBC authentication.
	 *
	 * @see #JDBC_PASSWORD
	 * @see #jdbcUsername
	 * @see java.sql.DriverManager#getConnection(String, String, String)
	 * @see javax.sql.DataSource#getConnection(String, String)
	 */
	@Nonnull
	public HibernatePersistenceConfiguration jdbcPassword(String password) {
		property( JDBC_PASSWORD, password );
		return this;
	}

	/**
	 * Username and password for JDBC authentication.
	 *
	 * @see #JDBC_USER
	 * @see #JDBC_PASSWORD
	 * @see #jdbcUsername
	 * @see #jdbcPassword
	 * @see java.sql.DriverManager#getConnection(String, String, String)
	 * @see javax.sql.DataSource#getConnection(String, String)
	 */
	@Nonnull
	public HibernatePersistenceConfiguration jdbcCredentials(String username, String password) {
		jdbcUsername( username );
		jdbcPassword( password );
		return this;
	}

	/**
	 * The JDBC connection pool size. This setting is ignored when Hibernate is
	 * configured to obtain connections from a {@link javax.sql.DataSource}.
	 *
	 * @see JdbcSettings#POOL_SIZE
	 */
	@Nonnull
	public HibernatePersistenceConfiguration jdbcPoolSize(int poolSize) {
		property( JdbcSettings.POOL_SIZE, poolSize );
		return this;
	}

	/**
	 * The JDBC {@linkplain java.sql.Connection#setAutoCommit autocommit mode}
	 * for pooled connections. This setting is ignored when Hibernate is
	 * configured to obtain connections from a {@link javax.sql.DataSource}.
	 *
	 * @see JdbcSettings#AUTOCOMMIT
	 */
	@Nonnull
	public HibernatePersistenceConfiguration jdbcAutocommit(boolean autocommit) {
		property( JdbcSettings.AUTOCOMMIT, autocommit );
		return this;
	}

	/**
	 * The JDBC {@linkplain java.sql.Connection#setTransactionIsolation transaction
	 * isolation level}. This setting is ignored when Hibernate is configured to
	 * obtain connections from a {@link javax.sql.DataSource}.
	 * <p>
	 * Possible values are enumerated by {@link java.sql.Connection}:
	 * {@link java.sql.Connection#TRANSACTION_READ_UNCOMMITTED},
	 * {@link java.sql.Connection#TRANSACTION_READ_COMMITTED},
	 * {@link java.sql.Connection#TRANSACTION_REPEATABLE_READ}, and
	 * {@link java.sql.Connection#TRANSACTION_SERIALIZABLE}.
	 *
	 * @see JdbcSettings#ISOLATION
	 */
	@Nonnull
	public HibernatePersistenceConfiguration jdbcTransactionIsolation(int isolationLevel) {
		property( JdbcSettings.ISOLATION, isolationLevel );
		return this;
	}

	/**
	 * Enables SQL logging to the console.
	 * <p>
	 * Sets {@value AvailableSettings#SHOW_SQL}, {@value AvailableSettings#FORMAT_SQL},
	 * and {@value AvailableSettings#HIGHLIGHT_SQL}.
	 *
	 * @param showSql should SQL be logged to console?
	 * @param formatSql should logged SQL be formatted
	 * @param highlightSql should logged SQL be highlighted with pretty colors
	 */
	@Nonnull
	public HibernatePersistenceConfiguration showSql(boolean showSql, boolean formatSql, boolean highlightSql) {
		property( JdbcSettings.SHOW_SQL, showSql );
		property( JdbcSettings.FORMAT_SQL, formatSql );
		property( JdbcSettings.HIGHLIGHT_SQL, highlightSql );
		return this;
	}

	/**
	 * Specifies whether Hibernate will strictly adhere to compliance with Jakarta Persistence for
	 * all aspects of {@linkplain jakarta.persistence.Query} handling.
	 *
	 * @see JpaComplianceSettings#JPA_QUERY_COMPLIANCE
	 */
	@Nonnull
	public HibernatePersistenceConfiguration queryCompliance(boolean enabled) {
		property( JpaComplianceSettings.JPA_QUERY_COMPLIANCE, enabled );
		return this;
	}

	/**
	 * Specifies whether Hibernate will strictly adhere to compliance with Jakarta Persistence for
	 * all aspects of transaction handling.
	 *
	 * @see JpaComplianceSettings#JPA_TRANSACTION_COMPLIANCE
	 */
	@Nonnull
	public HibernatePersistenceConfiguration transactionCompliance(boolean enabled) {
		property( JpaComplianceSettings.JPA_TRANSACTION_COMPLIANCE, enabled );
		return this;
	}

	/**
	 * Specifies whether Hibernate will strictly adhere to compliance with Jakarta Persistence for
	 * handling around calls to {@linkplain EntityManager#close()},
	 * {@linkplain EntityManager#isOpen()},
	 * {@linkplain EntityManagerFactory#close()} and
	 * {@linkplain EntityManagerFactory#isOpen()}
	 *
	 * @see JpaComplianceSettings#JPA_CLOSED_COMPLIANCE
	 */
	@Nonnull
	public HibernatePersistenceConfiguration closedCompliance(boolean enabled) {
		property( JpaComplianceSettings.JPA_CLOSED_COMPLIANCE, enabled );
		return this;
	}

	/**
	 * Specifies whether Hibernate will strictly adhere to compliance with Jakarta Persistence for
	 * handling of proxies.
	 *
	 * @see JpaComplianceSettings#JPA_PROXY_COMPLIANCE
	 */
	@Nonnull
	public HibernatePersistenceConfiguration proxyCompliance(boolean enabled) {
		property( JpaComplianceSettings.JPA_PROXY_COMPLIANCE, enabled );
		return this;
	}

	/**
	 * Specifies whether Hibernate will strictly adhere to compliance with Jakarta Persistence for
	 * handling of proxies.
	 *
	 * @see JpaComplianceSettings#JPA_PROXY_COMPLIANCE
	 */
	@Nonnull
	public HibernatePersistenceConfiguration cachingCompliance(boolean enabled) {
		property( JpaComplianceSettings.JPA_PROXY_COMPLIANCE, enabled );
		return this;
	}

	/**
	 * Specifies whether Hibernate will strictly adhere to compliance with Jakarta Persistence for
	 * in terms of collecting all named value generators globally, regardless of location.
	 *
	 * @see JpaComplianceSettings#JPA_ID_GENERATOR_GLOBAL_SCOPE_COMPLIANCE
	 */
	@Nonnull
	public HibernatePersistenceConfiguration globalGeneratorCompliance(boolean enabled) {
		property( JpaComplianceSettings.JPA_ID_GENERATOR_GLOBAL_SCOPE_COMPLIANCE, enabled );
		return this;
	}

	/**
	 * Specifies whether Hibernate will strictly adhere to compliance with Jakarta Persistence for
	 * the interpretation of {@link jakarta.persistence.OrderBy}.
	 *
	 * @see JpaComplianceSettings#JPA_ORDER_BY_MAPPING_COMPLIANCE
	 */
	@Nonnull
	public HibernatePersistenceConfiguration orderByMappingCompliance(boolean enabled) {
		property( JpaComplianceSettings.JPA_ORDER_BY_MAPPING_COMPLIANCE, enabled );
		return this;
	}

	/**
	 * Specifies whether Hibernate will strictly adhere to compliance with Jakarta Persistence for
	 * the allowed type of identifier value passed to
	 * {@link jakarta.persistence.EntityManager#getReference} and
	 * {@link jakarta.persistence.EntityManager#find}
	 *
	 * @see JpaComplianceSettings#JPA_LOAD_BY_ID_COMPLIANCE
	 */
	@Nonnull
	public HibernatePersistenceConfiguration loadByIdCompliance(boolean enabled) {
		property( JpaComplianceSettings.JPA_LOAD_BY_ID_COMPLIANCE, enabled );
		return this;
	}

	/**
	 * Enable or disable the second-level and query caches.
	 */
	@Nonnull
	public HibernatePersistenceConfiguration caching(@Nonnull CachingType type) {
		assert Objects.nonNull( type );
		if ( type == CachingType.NONE || type == CachingType.AUTO ) {
			property( CacheSettings.USE_SECOND_LEVEL_CACHE, false );
			property( CacheSettings.USE_QUERY_CACHE, false );
		}
		else if ( type == CachingType.BOTH ) {
			property( CacheSettings.USE_SECOND_LEVEL_CACHE, true );
			property( CacheSettings.USE_QUERY_CACHE, true );
		}
		else if ( type == CachingType.DATA ) {
			property( CacheSettings.USE_SECOND_LEVEL_CACHE, true );
			property( CacheSettings.USE_QUERY_CACHE, false );
		}
		else if ( type == CachingType.QUERY ) {
			property( CacheSettings.USE_SECOND_LEVEL_CACHE, false );
			property( CacheSettings.USE_QUERY_CACHE, true );
		}

		return this;
	}

	/**
	 * If {@linkplain CachingType#DATA data caching} is enabled, configure the
	 * type of concurrency access that should be applied if not explicitly specified
	 * on a cache region.
	 *
	 * @see org.hibernate.annotations.Cache#usage
	 * @see CacheSettings#DEFAULT_CACHE_CONCURRENCY_STRATEGY
	 */
	@Nonnull
	public HibernatePersistenceConfiguration cachingAccessType(@Nonnull AccessType type) {
		// todo (7.0) : should this enable second-level cache if not?
		property( CacheSettings.DEFAULT_CACHE_CONCURRENCY_STRATEGY, type );
		return this;
	}

	/**
	 * Specify a {@linkplain StatementInspector} to be applied to all Sessions/EntityManagers
	 *
	 * @see JdbcSettings#STATEMENT_INSPECTOR
	 */
	@Nonnull
	public HibernatePersistenceConfiguration statementInspector(Class<? extends StatementInspector> inspectorImpl) {
		property( JdbcSettings.STATEMENT_INSPECTOR, inspectorImpl );
		return this;
	}

	/**
	 * Specify a {@linkplain StatementInspector} to be applied to all Sessions/EntityManagers
	 *
	 * @see JdbcSettings#STATEMENT_INSPECTOR
	 */
	@Nonnull
	public HibernatePersistenceConfiguration statementInspector(StatementInspector inspector) {
		property( JdbcSettings.STATEMENT_INSPECTOR, inspector );
		return this;
	}

	/**
	 * Configure a default catalog name to be used for database objects (tables, sequences, etc) which do not
	 * explicitly specify one.
	 *
	 * @see MappingSettings#DEFAULT_CATALOG
	 */
	@Nonnull
	public HibernatePersistenceConfiguration defaultCatalog(String catalogName) {
		property( MappingSettings.DEFAULT_CATALOG, catalogName );
		return this;
	}

	/**
	 * Configure a default schema name to be used for database objects (tables, sequences, etc) which do not
	 * explicitly specify one.
	 *
	 * @see MappingSettings#DEFAULT_SCHEMA
	 */
	@Nonnull
	public HibernatePersistenceConfiguration defaultSchema(String schemaName) {
		property( MappingSettings.DEFAULT_SCHEMA, schemaName );
		return this;
	}

	/**
	 * Configure a default schema name to be used for database objects (tables, sequences, etc) which do not
	 * explicitly specify one.
	 *
	 * @see MappingSettings#USE_NATIONALIZED_CHARACTER_DATA
	 */
	public HibernatePersistenceConfiguration nationalizedCharacterData(boolean enabled) {
		property( MappingSettings.USE_NATIONALIZED_CHARACTER_DATA, enabled );
		return this;
	}

	/**
	 * Configures whether Hibernate should process XML mappings ({@code orm.xml} files).
	 *
	 * @see MappingSettings#XML_MAPPING_ENABLED
	 */
	public HibernatePersistenceConfiguration xmlMappings(boolean enabled) {
		property( MappingSettings.XML_MAPPING_ENABLED, enabled );
		return this;
	}

	/**
	 * Configures whether Hibernate should validate (via schema descriptor) XML files.
	 *
	 * @see MappingSettings#VALIDATE_XML
	 */
	public HibernatePersistenceConfiguration xmlValidation(boolean enabled) {
		property( MappingSettings.VALIDATE_XML, enabled );
		return this;
	}

	/**
	 * Configures whether Hibernate should collect {@linkplain org.hibernate.stat.Statistics}.
	 *
	 * @see StatisticsSettings#GENERATE_STATISTICS
	 */
	public HibernatePersistenceConfiguration collectStatistics(boolean enabled) {
		property( StatisticsSettings.GENERATE_STATISTICS, enabled );
		return this;
	}

	/**
	 * Add the specified classes as {@linkplain #managedClasses() managed classes}.
	 *
	 * @see #managedClass
	 */
	@Nonnull
	public HibernatePersistenceConfiguration managedClasses(@Nonnull Class<?>... managedClasses) {
		Collections.addAll( managedClasses(), managedClasses );
		return this;
	}

	/**
	 * Add the specified classes as {@linkplain #managedClasses() managed classes}.
	 *
	 * @see #managedClass
	 */
	@Nonnull
	public HibernatePersistenceConfiguration managedClasses(@Nonnull Collection<Class<?>> managedClasses) {
		managedClasses().addAll( managedClasses );
		return this;
	}

	/**
	 * Add the specified resource names as {@linkplain #mappingFiles() mapping files}.
	 *
	 * @see #mappingFiles()
	 */
	@Nonnull
	public HibernatePersistenceConfiguration mappingFiles(@Nonnull String... names) {
		Collections.addAll( mappingFiles(), names );
		return this;
	}

	/**
	 * Add the specified resource names as {@linkplain #mappingFiles() mapping files}.
	 *
	 * @see #mappingFiles()
	 */
	@Nonnull
	public HibernatePersistenceConfiguration mappingFiles(@Nonnull Collection<String> names) {
		mappingFiles().addAll( names );
		return this;
	}

	/**
	 * Root URL of the persistence unit.
	 * When {@linkplain org.hibernate.cfg.PersistenceSettings#SCANNER
	 * entity discovery} is enabled, this root URL will be scanned for entities.
	 *
	 * @see org.hibernate.cfg.PersistenceSettings#SCANNER
	 * @see jakarta.persistence.spi.PersistenceUnitInfo#getPersistenceUnitRootUrl
	 *
	 * @since 7.1
	 */
	@Nullable
	public URL rootUrl() {
		return rootUrl;
	}

	/**
	 * URLs of JAR files.
	 * When {@linkplain org.hibernate.cfg.PersistenceSettings#SCANNER
	 * entity discovery} is enabled, the JAR files will be scanned for entities.
	 *
	 * @see org.hibernate.cfg.PersistenceSettings#SCANNER
	 * @see jakarta.persistence.spi.PersistenceUnitInfo#getJarFileUrls
	 *
	 * @since 7.1
	 */
	@Nonnull
	public List<URL> jarFileUrls() {
		return jarFileUrls;
	}

	/**
	 * Add the specified URL as a {@linkplain #jarFileUrls() JAR file}.
	 *
	 * @see #jarFileUrls()
	 *
	 * @since 7.1
	 */
	@Nonnull
	public HibernatePersistenceConfiguration jarFileUrl(@Nonnull URL url) {
		jarFileUrls.add( url );
		return this;
	}

	/**
	 * Add the specified URLs as {@linkplain #jarFileUrls() JAR files}.
	 *
	 * @see #jarFileUrls()
	 *
	 * @since 7.1
	 */
	@Nonnull
	public HibernatePersistenceConfiguration jarFileUrls(@Nonnull URL... urls) {
		Collections.addAll( jarFileUrls, urls );
		return this;
	}

	/**
	 * Add the specified URLs as {@linkplain #jarFileUrls() JAR files}.
	 *
	 * @see #jarFileUrls()
	 *
	 * @since 7.1
	 */
	@Nonnull
	public HibernatePersistenceConfiguration jarFileUrls(@Nonnull Collection<URL> urls) {
		jarFileUrls.addAll( urls );
		return this;
	}

	/**
	 * Locates the jar file in which the given class is located and adds its URL
	 * as a {@linkplain #jarFileUrls() JAR file}.
	 *
	 * @see #jarFileUrls()
	 *
	 * @since 8.0
	 */
	@Nonnull
	public HibernatePersistenceConfiguration jarFileUrl(@Nonnull Class<?> classFromJarFile) {
		jarFileUrls.add( classFromJarFile.getProtectionDomain().getCodeSource().getLocation() );
		return this;
	}

	/**
	 * Locates the jar file in which the given class is located and adds its URL
	 * as a {@linkplain #jarFileUrls() JAR file}.
	 *
	 * @see #jarFileUrls()
	 *
	 * @since 8.0
	 */
	@Nonnull
	public HibernatePersistenceConfiguration jarFileUrls(@Nonnull Class<?>... classesFromJarFiles) {
		for ( int i = 0; i < classesFromJarFiles.length; i++ ) {
			jarFileUrls.add( classesFromJarFiles[i].getProtectionDomain().getCodeSource().getLocation() );
		}
		return this;
	}

	/**
	 * Specify the {@linkplain Action action} to take in terms of automatic
	 * database schema tooling.
	 *
	 * @apiNote This only controls tooling as exported directly to the database.  To
	 * output tooling commands to scripts, use {@linkplain #properties(Map) config properties}
	 * instead with appropriate {@linkplain SchemaToolingSettings settings}.
	 *
	 * @see SchemaToolingSettings#HBM2DDL_AUTO
	 */
	@Nonnull
	public HibernatePersistenceConfiguration schemaToolingAction(Action action) {
		property( SchemaToolingSettings.HBM2DDL_AUTO, action );
		return this;
	}

	/**
	 * Applies a {@linkplain StatementObserver} to the {@linkplain EntityManagerFactory} being built.
	 *
	 * @see JdbcSettings#STATEMENT_OBSERVER
	 */
	@Nonnull
	public HibernatePersistenceConfiguration statementObserver(StatementObserver statementObserver) {
		property( JdbcSettings.STATEMENT_OBSERVER, statementObserver );
		return this;
	}

	/**
	 * Applies a {@linkplain StatementObserver} to the {@linkplain EntityManagerFactory} being built.
	 *
	 * @see JdbcSettings#STATEMENT_OBSERVER
	 */
	@Nonnull
	public HibernatePersistenceConfiguration statementObserver(Class<? extends StatementObserver> statementObserverImpl) {
		property( JdbcSettings.STATEMENT_OBSERVER, statementObserverImpl );
		return this;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// covariant overrides

	@Override
	@Nonnull
	public HibernatePersistenceConfiguration provider(@Nullable String providerClassName) {
		super.provider( providerClassName );
		return this;
	}

	@Override
	@Nonnull
	public HibernatePersistenceConfiguration jtaDataSource(@Nullable String dataSourceJndiName) {
		super.jtaDataSource( dataSourceJndiName );
		return this;
	}

	@Override
	@Nonnull
	public HibernatePersistenceConfiguration nonJtaDataSource(@Nullable String dataSourceJndiName) {
		super.nonJtaDataSource( dataSourceJndiName );
		return this;
	}

	@Override
	@Nonnull
	public HibernatePersistenceConfiguration managedClass(@Nonnull Class<?> managedClass) {
		super.managedClass( managedClass );
		return this;
	}

	@Override
	@Nonnull
	public HibernatePersistenceConfiguration mappingFile(@Nonnull String name) {
		super.mappingFile( name );
		return this;
	}

	@Override
	@Nonnull
	public HibernatePersistenceConfiguration transactionType(@Nonnull PersistenceUnitTransactionType transactionType) {
		super.transactionType( transactionType );
		return this;
	}

	@Override
	@Nonnull
	public HibernatePersistenceConfiguration sharedCacheMode(@Nonnull SharedCacheMode sharedCacheMode) {
		super.sharedCacheMode( sharedCacheMode );
		return this;
	}

	@Override
	@Nonnull
	public HibernatePersistenceConfiguration validationMode(@Nonnull ValidationMode validationMode) {
		super.validationMode( validationMode );
		return this;
	}

	@Override
	@Nonnull
	public HibernatePersistenceConfiguration defaultToOneFetchType(@Nonnull FetchType defaultToOneFetchType) {
		super.defaultToOneFetchType( defaultToOneFetchType );
		return this;
	}

	@Override
	@Nonnull
	public HibernatePersistenceConfiguration schemaManagementDatabaseAction(@Nonnull SchemaManagementAction action) {
		super.schemaManagementDatabaseAction( action );
		return this;
	}

	@Override
	@Nonnull
	public HibernatePersistenceConfiguration schemaManagementScriptsAction(@Nonnull SchemaManagementAction action) {
		super.schemaManagementScriptsAction( action );
		return this;
	}

	@Override
	@Nonnull
	public HibernatePersistenceConfiguration property(@Nonnull String name, @Nullable Object value) {
		super.property( name, value );
		return this;
	}

	@Override
	@Nonnull
	public HibernatePersistenceConfiguration properties(@Nonnull Map<String, ?> properties) {
		super.properties( properties );
		return this;
	}
}
