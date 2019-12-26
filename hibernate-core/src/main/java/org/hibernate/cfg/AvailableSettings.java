/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import java.util.function.Supplier;
import javax.persistence.GeneratedValue;

import org.hibernate.HibernateException;
import org.hibernate.Transaction;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.registry.classloading.internal.TcclLookupPrecedence;
import org.hibernate.cache.spi.TimestampsCacheFactory;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.jpa.spi.JpaCompliance;
import org.hibernate.query.ImmutableEntityUpdateQueryHandlingMode;
import org.hibernate.query.internal.ParameterMetadataImpl;
import org.hibernate.resource.beans.container.spi.ExtendedBeanManager;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.tool.schema.JdbcMetadaAccessStrategy;
import org.hibernate.tool.schema.SourceType;

/**
 * @author Steve Ebersole
 */
public interface AvailableSettings extends org.hibernate.jpa.AvailableSettings {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA defined settings
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * The name of the {@link javax.persistence.spi.PersistenceProvider} implementor
	 * <p/>
	 * See JPA 2 sections 9.4.3 and 8.2.1.4
	 */
	String JPA_PERSISTENCE_PROVIDER = "javax.persistence.provider";

	/**
	 * The type of transactions supported by the entity managers.
	 * <p/>
	 * See JPA 2 sections 9.4.3 and 8.2.1.2
	 */
	String JPA_TRANSACTION_TYPE = "javax.persistence.transactionType";

	/**
	 * The JNDI name of a JTA {@link javax.sql.DataSource}.
	 * <p/>
	 * See JPA 2 sections 9.4.3 and 8.2.1.5
	 */
	String JPA_JTA_DATASOURCE = "javax.persistence.jtaDataSource";

	/**
	 * The JNDI name of a non-JTA {@link javax.sql.DataSource}.
	 * <p/>
	 * See JPA 2 sections 9.4.3 and 8.2.1.5
	 */
	String JPA_NON_JTA_DATASOURCE = "javax.persistence.nonJtaDataSource";

	/**
	 * The name of a JDBC driver to use to connect to the database.
	 * <p/>
	 * Used in conjunction with {@link #JPA_JDBC_URL}, {@link #JPA_JDBC_USER} and {@link #JPA_JDBC_PASSWORD}
	 * to define how to make connections to the database in lieu of
	 * a datasource (either {@link #JPA_JTA_DATASOURCE} or {@link #JPA_NON_JTA_DATASOURCE}).
	 * <p/>
	 * See section 8.2.1.9
	 */
	String JPA_JDBC_DRIVER = "javax.persistence.jdbc.driver";

	/**
	 * The JDBC connection url to use to connect to the database.
	 * <p/>
	 * Used in conjunction with {@link #JPA_JDBC_DRIVER}, {@link #JPA_JDBC_USER} and {@link #JPA_JDBC_PASSWORD}
	 * to define how to make connections to the database in lieu of
	 * a datasource (either {@link #JPA_JTA_DATASOURCE} or {@link #JPA_NON_JTA_DATASOURCE}).
	 * <p/>
	 * See section 8.2.1.9
	 */
	String JPA_JDBC_URL = "javax.persistence.jdbc.url";

	/**
	 * The JDBC connection user name.
	 * <p/>
	 * Used in conjunction with {@link #JPA_JDBC_DRIVER}, {@link #JPA_JDBC_URL} and {@link #JPA_JDBC_PASSWORD}
	 * to define how to make connections to the database in lieu of
	 * a datasource (either {@link #JPA_JTA_DATASOURCE} or {@link #JPA_NON_JTA_DATASOURCE}).
	 * <p/>
	 * See section 8.2.1.9
	 */
	String JPA_JDBC_USER = "javax.persistence.jdbc.user";

	/**
	 * The JDBC connection password.
	 * <p/>
	 * Used in conjunction with {@link #JPA_JDBC_DRIVER}, {@link #JPA_JDBC_URL} and {@link #JPA_JDBC_USER}
	 * to define how to make connections to the database in lieu of
	 * a datasource (either {@link #JPA_JTA_DATASOURCE} or {@link #JPA_NON_JTA_DATASOURCE}).
	 * <p/>
	 * See JPA 2 section 8.2.1.9
	 */
	String JPA_JDBC_PASSWORD = "javax.persistence.jdbc.password";

	/**
	 * Used to indicate whether second-level (what JPA terms shared cache) caching is
	 * enabled as per the rules defined in JPA 2 section 3.1.7.
	 * <p/>
	 * See JPA 2 sections 9.4.3 and 8.2.1.7
	 * @see javax.persistence.SharedCacheMode
	 */
	String JPA_SHARED_CACHE_MODE = "javax.persistence.sharedCache.mode";

	/**
	 * NOTE : Not a valid EMF property...
	 * <p/>
	 * Used to indicate if the provider should attempt to retrieve requested data
	 * in the shared cache.
	 *
	 * @see javax.persistence.CacheRetrieveMode
	 */
	String JPA_SHARED_CACHE_RETRIEVE_MODE ="javax.persistence.cache.retrieveMode";

	/**
	 * NOTE : Not a valid EMF property...
	 * <p/>
	 * Used to indicate if the provider should attempt to store data loaded from the database
	 * in the shared cache.
	 *
	 * @see javax.persistence.CacheStoreMode
	 */
	String JPA_SHARED_CACHE_STORE_MODE ="javax.persistence.cache.storeMode";

	/**
	 * Used to indicate what form of automatic validation is in effect as per rules defined
	 * in JPA 2 section 3.6.1.1
	 * <p/>
	 * See JPA 2 sections 9.4.3 and 8.2.1.8
	 * @see javax.persistence.ValidationMode
	 */
	String JPA_VALIDATION_MODE = "javax.persistence.validation.mode";

	/**
	 * Used to pass along any discovered validator factory.
	 */
	String JPA_VALIDATION_FACTORY = "javax.persistence.validation.factory";

	/**
	 * Used to coordinate with bean validators
	 * <p/>
	 * See JPA 2 section 8.2.1.9
	 */
	String JPA_PERSIST_VALIDATION_GROUP = "javax.persistence.validation.group.pre-persist";

	/**
	 * Used to coordinate with bean validators
	 * <p/>
	 * See JPA 2 section 8.2.1.9
	 */
	String JPA_UPDATE_VALIDATION_GROUP = "javax.persistence.validation.group.pre-update";

	/**
	 * Used to coordinate with bean validators
	 * <p/>
	 * See JPA 2 section 8.2.1.9
	 */
	String JPA_REMOVE_VALIDATION_GROUP = "javax.persistence.validation.group.pre-remove";

	/**
	 * Used to request (hint) a pessimistic lock scope.
	 * <p/>
	 * See JPA 2 sections 8.2.1.9 and 3.4.4.3
	 */
	String JPA_LOCK_SCOPE = "javax.persistence.lock.scope";

	/**
	 * Used to request (hint) a pessimistic lock timeout (in milliseconds).
	 * <p/>
	 * See JPA 2 sections 8.2.1.9 and 3.4.4.3
	 */
	String JPA_LOCK_TIMEOUT = "javax.persistence.lock.timeout";

	/**
	 * Used to pass along the CDI BeanManager, if any, to be used.
	 *
	 * According to JPA, strictly, the BeanManager should be passed in
	 * at boot-time and be ready for use at that time.  However not all
	 * environments can do this (WildFly e.g.).  To accommodate such
	 * environments, Hibernate provides 2 options:
	 *
	 *     * a proprietary CDI extension SPI (that we have proposed to
	 *     	the CDI spec group as a standard option) that can be used
	 *     	to provide delayed BeanManager access.  To use this solution,
	 *     	the reference passed as the BeanManager during bootstrap
	 *     	should be typed as {@link ExtendedBeanManager}
	 *     * delayed access to the BeanManager reference.  Here, Hibernate
	 *      will not access the reference passed as the BeanManager during
	 *      bootstrap until it is first needed.  Note however that this has
	 *      the effect of delaying any deployment problems until after
	 *      bootstrapping.
	 *
	 * This setting is used to configure Hibernate ORM's access to
	 * the BeanManager (either directly or via {@link ExtendedBeanManager}).
	 */
	String CDI_BEAN_MANAGER = "javax.persistence.bean.manager";


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// BootstrapServiceRegistry level settings
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Used to define a {@link java.util.Collection} of the {@link ClassLoader} instances Hibernate should use for
	 * class-loading and resource-lookups.
	 *
	 * @since 5.0
	 */
	String CLASSLOADERS = "hibernate.classLoaders";

	/**
	 * Used to define how the current thread context {@link ClassLoader} must be used
	 * for class lookup.
	 *
	 * @see TcclLookupPrecedence
	 */
	String TC_CLASSLOADER = "hibernate.classLoader.tccl_lookup_precedence";

	/**
	 * Names the {@link ClassLoader} used to load user application classes.
	 * @since 4.0
	 *
	 * @deprecated Use {@link #CLASSLOADERS} instead
	 */
	@Deprecated
	String APP_CLASSLOADER = "hibernate.classLoader.application";

	/**
	 * Names the {@link ClassLoader} Hibernate should use to perform resource loading.
	 * @since 4.0
	 * @deprecated Use {@link #CLASSLOADERS} instead
	 */
	@Deprecated
	String RESOURCES_CLASSLOADER = "hibernate.classLoader.resources";

	/**
	 * Names the {@link ClassLoader} responsible for loading Hibernate classes.  By default this is
	 * the {@link ClassLoader} that loaded this class.
	 * @since 4.0
	 * @deprecated Use {@link #CLASSLOADERS} instead
	 */
	@Deprecated
	String HIBERNATE_CLASSLOADER = "hibernate.classLoader.hibernate";

	/**
	 * Names the {@link ClassLoader} used when Hibernate is unable to locate classes on the
	 * {@link #APP_CLASSLOADER} or {@link #HIBERNATE_CLASSLOADER}.
	 * @since 4.0
	 * @deprecated Use {@link #CLASSLOADERS} instead
	 */
	@Deprecated
	String ENVIRONMENT_CLASSLOADER = "hibernate.classLoader.environment";

	/**
	 * @deprecated use {@link #JPA_METAMODEL_POPULATION} instead.
	 */
	@Deprecated
	String JPA_METAMODEL_GENERATION = "hibernate.ejb.metamodel.generation";

	/**
	 * Setting that indicates whether to build the JPA types. Accepts
	 * 3 values:<ul>
	 *     <li>
	 *         <b>enabled</b> - Do the build
	 *     </li>
	 *     <li>
	 *         <b>disabled</b> - Do not do the build
	 *     </li>
	 *     <li>
	 *         <b>ignoreUnsupported</b> - Do the build, but ignore any non-JPA features that would otherwise
	 *         result in a failure.
	 *     </li>
	 * </ul>
	 *
	 *
	 */
	@Deprecated
	String JPA_METAMODEL_POPULATION = "hibernate.ejb.metamodel.population";

	/**
	 * Setting that controls whether we seek out JPA "static metamodel" classes and populate them.  Accepts
	 * 3 values:<ul>
	 *     <li>
	 *         <b>enabled</b> -Do the population
	 *     </li>
	 *     <li>
	 *         <b>disabled</b> - Do not do the population
	 *     </li>
	 *     <li>
	 *         <b>skipUnsupported</b> - Do the population, but ignore any non-JPA features that would otherwise
	 *         result in the population failing.
	 *     </li>
	 * </ul>
	 */
	String STATIC_METAMODEL_POPULATION = "hibernate.jpa.static_metamodel.population";


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// StandardServiceRegistry level settings
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Names the {@link org.hibernate.engine.jdbc.connections.spi.ConnectionProvider} to use for obtaining
	 * JDBC connections.  Can reference:<ul>
	 *     <li>an instance of ConnectionProvider</li>
	 *     <li>a {@code Class<? extends ConnectionProvider>} reference</li>
	 *     <li>a {@code Class<? extends ConnectionProvider>} FQN</li>
	 * </ul>
	 * <p/>
	 * The term {@code "class"} appears in the setting name due to legacy reasons; however it can accept instances.
	 */
	String CONNECTION_PROVIDER ="hibernate.connection.provider_class";

	/**
	 * Names the {@literal JDBC} driver class
	 */
	String DRIVER ="hibernate.connection.driver_class";

	/**
	 * Names the {@literal JDBC} connection url.
	 */
	String URL ="hibernate.connection.url";

	/**
	 * Names the connection user.  This might mean one of 2 things in out-of-the-box Hibernate
	 * {@link org.hibernate.engine.jdbc.connections.spi.ConnectionProvider}: <ul>
	 *     <li>The username used to pass along to create the JDBC connection</li>
	 *     <li>The username used to obtain a JDBC connection from a data source</li>
	 * </ul>
	 */
	String USER ="hibernate.connection.username";

	/**
	 * Names the connection password.  See usage discussion on {@link #USER}
	 */
	String PASS ="hibernate.connection.password";

	/**
	 * Names the {@literal JDBC} transaction isolation level
	 */
	String ISOLATION ="hibernate.connection.isolation";

	/**
	 * Controls the autocommit mode of {@literal JDBC} Connections obtained
	 * from a non-DataSource ConnectionProvider - assuming the ConnectionProvider
	 * impl properly leverages this setting (the provided Hibernate impls all
	 * do).
	 */
	String AUTOCOMMIT = "hibernate.connection.autocommit";

	/**
	 * Maximum number of inactive connections for the built-in Hibernate connection pool.
	 */
	String POOL_SIZE ="hibernate.connection.pool_size";

	/**
	 * Names a {@link javax.sql.DataSource}.  Can reference:<ul>
	 *     <li>a {@link javax.sql.DataSource} instance</li>
	 *     <li>a {@literal JNDI} name under which to locate the {@link javax.sql.DataSource}</li>
	 * </ul>
	 * For JNDI names, ses also {@link #JNDI_CLASS}, {@link #JNDI_URL}, {@link #JNDI_PREFIX}, etc.
	 */
	String DATASOURCE ="hibernate.connection.datasource";

	/**
	 * Allows a user to tell Hibernate that the Connections we obtain from the configured
	 * ConnectionProvider will already have auto-commit disabled when we acquire them from
	 * the provider.  When we get connections already in auto-commit, this allows us to circumvent
	 * some operations in the interest of performance.
	 * <p/>
	 * Default value is {@code false} - do not skip, aka call setAutocommit
	 *
	 * @since 5.2.10
	 */
	String CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT= "hibernate.connection.provider_disables_autocommit";

	/**
	 * Names a prefix used to define arbitrary JDBC connection properties.  These properties are passed along to
	 * the {@literal JDBC} provider when creating a connection.
	 */
	String CONNECTION_PREFIX = "hibernate.connection";

	/**
	 * Names the {@literal JNDI} {@link javax.naming.InitialContext} class.
	 *
	 * @see javax.naming.Context#INITIAL_CONTEXT_FACTORY
	 */
	String JNDI_CLASS ="hibernate.jndi.class";

	/**
	 * Names the {@literal JNDI} provider/connection url
	 *
	 * @see javax.naming.Context#PROVIDER_URL
	 */
	String JNDI_URL ="hibernate.jndi.url";

	/**
	 * Names a prefix used to define arbitrary {@literal JNDI} {@link javax.naming.InitialContext} properties.  These
	 * properties are passed along to {@link javax.naming.InitialContext#InitialContext(java.util.Hashtable)}
	 */
	String JNDI_PREFIX = "hibernate.jndi";

	/**
	 * Names the Hibernate {@literal SQL} {@link org.hibernate.dialect.Dialect} class
	 */
	String DIALECT ="hibernate.dialect";

	/**
	 * Names any additional {@link org.hibernate.engine.jdbc.dialect.spi.DialectResolver} implementations to
	 * register with the standard {@link org.hibernate.engine.jdbc.dialect.spi.DialectFactory}.
	 */
	String DIALECT_RESOLVERS = "hibernate.dialect_resolvers";

	/**
	 * Defines the default storage engine for the relational databases that support multiple storage engines.
	 * This property must be set either as an Environment variable or JVM System Property.
	 * That is because the Dialect is bootstrapped prior to Hibernate property resolution.
	 *
	 * @since 5.2.9
	 */
	String STORAGE_ENGINE = "hibernate.dialect.storage_engine";

	/**
	 * Used to specify the {@link org.hibernate.tool.schema.spi.SchemaManagementTool} to use for performing
	 * schema management.  The default is to use {@link org.hibernate.tool.schema.internal.HibernateSchemaManagementTool}
	 *
	 * @since 5.0
	 */
	String SCHEMA_MANAGEMENT_TOOL = "hibernate.schema_management_tool";

	/**
	 * Names the implementation of {@link TransactionCoordinatorBuilder} to use for
	 * creating {@link TransactionCoordinator} instances.
	 * <p/>
	 * Can be<ul>
	 *     <li>TransactionCoordinatorBuilder instance</li>
	 *     <li>TransactionCoordinatorBuilder implementation {@link Class} reference</li>
	 *     <li>TransactionCoordinatorBuilder implementation class name (FQN) or short-name</li>
	 * </ul>
	 *
	 * @since 5.0
	 */
	String TRANSACTION_COORDINATOR_STRATEGY = "hibernate.transaction.coordinator_class";

	/**
	 * Names the {@link org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform} implementation to use for integrating
	 * with {@literal JTA} systems.  Can reference either a {@link org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform}
	 * instance or the name of the {@link org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform} implementation class
	 *
	 * @since 4.0
	 */
	String JTA_PLATFORM = "hibernate.transaction.jta.platform";

	/**
	 * Should we prefer using the {@link org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform#retrieveUserTransaction}
	 * over using {@link org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform#retrieveTransactionManager}?
	 *
	 * Default is <code>false</code>
	 *
	 * @since 5.0
	 */
	String PREFER_USER_TRANSACTION = "hibernate.jta.prefer_user_transaction";

	/**
	 * Names the {@link org.hibernate.engine.transaction.jta.platform.spi.JtaPlatformResolver} implementation to use.
	 * @since 4.3
	 */
	String JTA_PLATFORM_RESOLVER = "hibernate.transaction.jta.platform_resolver";

	/**
	 * A configuration value key used to indicate that it is safe to cache
	 * {@link javax.transaction.TransactionManager} references.
	 *
	 * @since 4.0
	 */
	String JTA_CACHE_TM = "hibernate.jta.cacheTransactionManager";

	/**
	 * A configuration value key used to indicate that it is safe to cache
	 * {@link javax.transaction.UserTransaction} references.
	 *
	 * @since 4.0
	 */
	String JTA_CACHE_UT = "hibernate.jta.cacheUserTransaction";

	/**
	 * `true` / `false - should zero be used as the base for JDBC-style parameters
	 * found in native-queries?
	 *
	 * @since 5.3
	 *
	 * @see DeprecationLogger#logUseOfDeprecatedZeroBasedJdbcStyleParams
	 *
	 * @deprecated This is a temporary backwards-compatibility setting to help applications
	 * using versions prior to 5.3 in upgrading.  Deprecation warnings are issued when this
	 * is set to `true`.
	 */
	@Deprecated
	String JDBC_TYLE_PARAMS_ZERO_BASE = "hibernate.query.sql.jdbc_style_params_base";


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// MetadataBuilder level settings
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * A default database catalog name to use for unqualified tablenames
	 *
	 * @see MetadataBuilder#applyImplicitCatalogName
	 */
	String DEFAULT_CATALOG = "hibernate.default_catalog";

	/**
	 * A default database schema (owner) name to use for unqualified tablenames
	 *
	 * @see MetadataBuilder#applyImplicitSchemaName
	 */
	String DEFAULT_SCHEMA = "hibernate.default_schema";

	/**
	 * Setting used to give the name of the default {@link org.hibernate.annotations.CacheConcurrencyStrategy}
	 * to use when either {@link javax.persistence.Cacheable @Cacheable} or
	 * {@link org.hibernate.annotations.Cache @Cache} is used.  {@link org.hibernate.annotations.Cache @Cache(strategy="..")} is used to override.
	 *
	 * @see MetadataBuilder#applyAccessType(org.hibernate.cache.spi.access.AccessType)
	 */
	String DEFAULT_CACHE_CONCURRENCY_STRATEGY = "hibernate.cache.default_cache_concurrency_strategy";

	/**
	 * Setting which indicates whether or not the new {@link org.hibernate.id.IdentifierGenerator} are used
	 * for AUTO, TABLE and SEQUENCE.
	 * <p/>
	 * Default is {@code true}.  Existing applications may want to disable this (set it {@code false}) for
	 * upgrade compatibility.
	 *
	 * @see MetadataBuilder#enableNewIdentifierGeneratorSupport
	 */
	String USE_NEW_ID_GENERATOR_MAPPINGS = "hibernate.id.new_generator_mappings";

	/**
	 * @see org.hibernate.boot.MetadataBuilder#enableImplicitForcingOfDiscriminatorsInSelect(boolean)
	 */
	String FORCE_DISCRIMINATOR_IN_SELECTS_BY_DEFAULT = "hibernate.discriminator.force_in_select";

	/**
	 * The legacy behavior of Hibernate is to not use discriminators for joined inheritance (Hibernate does not need
	 * the discriminator...).  However, some JPA providers do need the discriminator for handling joined inheritance.
	 * In the interest of portability this capability has been added to Hibernate too.
	 * <p/>
	 * However, we want to make sure that legacy applications continue to work as well.  Which puts us in a bind in
	 * terms of how to handle "implicit" discriminator mappings.  The solution is to assume that the absence of
	 * discriminator metadata means to follow the legacy behavior *unless* this setting is enabled.  With this setting
	 * enabled, Hibernate will interpret the absence of discriminator metadata as an indication to use the JPA
	 * defined defaults for these absent annotations.
	 * <p/>
	 * See Hibernate Jira issue HHH-6911 for additional background info.
	 *
	 * @see MetadataBuilder#enableImplicitDiscriminatorsForJoinedSubclassSupport
	 * @see #IGNORE_EXPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS
	 */
	String IMPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS = "hibernate.discriminator.implicit_for_joined";

	/**
	 * The legacy behavior of Hibernate is to not use discriminators for joined inheritance (Hibernate does not need
	 * the discriminator...).  However, some JPA providers do need the discriminator for handling joined inheritance.
	 * In the interest of portability this capability has been added to Hibernate too.
	 * <p/>
	 * Existing applications rely (implicitly or explicitly) on Hibernate ignoring any DiscriminatorColumn declarations
	 * on joined inheritance hierarchies.  This setting allows these applications to maintain the legacy behavior
	 * of DiscriminatorColumn annotations being ignored when paired with joined inheritance.
	 * <p/>
	 * See Hibernate Jira issue HHH-6911 for additional background info.
	 *
	 * @see MetadataBuilder#enableExplicitDiscriminatorsForJoinedSubclassSupport
	 * @see #IMPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS
	 */
	String IGNORE_EXPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS = "hibernate.discriminator.ignore_explicit_for_joined";

	/**
	 * Enable nationalized character support on all string / clob based attribute ( string, char, clob, text etc ).
	 *
	 * Default is {@code false}.
	 *
	 * @see MetadataBuilder#enableGlobalNationalizedCharacterDataSupport(boolean)
	 */
	String USE_NATIONALIZED_CHARACTER_DATA = "hibernate.use_nationalized_character_data";
	/**
	 * The deprecated name.  Use {@link #SCANNER} or {@link #SCANNER_ARCHIVE_INTERPRETER} instead.
	 */
	String SCANNER_DEPRECATED = "hibernate.ejb.resource_scanner";

	/**
	 * Pass an implementation of {@link org.hibernate.boot.archive.scan.spi.Scanner}.
	 * Accepts either:<ul>
	 *     <li>an actual instance</li>
	 *     <li>a reference to a Class that implements Scanner</li>
	 *     <li>a fully qualified name (String) of a Class that implements Scanner</li>
	 * </ul>
	 *
	 * @see org.hibernate.boot.MetadataBuilder#applyScanner
	 */
	String SCANNER = "hibernate.archive.scanner";

	/**
	 * Pass {@link org.hibernate.boot.archive.spi.ArchiveDescriptorFactory} to use
	 * in the scanning process.  Accepts either:<ul>
	 *     <li>an ArchiveDescriptorFactory instance</li>
	 *     <li>a reference to a Class that implements ArchiveDescriptorFactory</li>
	 *     <li>a fully qualified name (String) of a Class that implements ArchiveDescriptorFactory</li>
	 * </ul>
	 * <p/>
	 * See information on {@link org.hibernate.boot.archive.scan.spi.Scanner}
	 * about expected constructor forms.
	 *
	 * @see #SCANNER
	 * @see org.hibernate.boot.archive.scan.spi.Scanner
	 * @see org.hibernate.boot.archive.scan.spi.AbstractScannerImpl
	 * @see MetadataBuilder#applyArchiveDescriptorFactory
	 */
	String SCANNER_ARCHIVE_INTERPRETER = "hibernate.archive.interpreter";

	/**
	 * Identifies a comma-separate list of values indicating the types of
	 * things we should auto-detect during scanning.  Allowable values include:<ul>
	 *     <li>"class" - discover classes - .class files are discovered as managed classes</li>
	 *     <li>"hbm" - discover hbm mapping files - hbm.xml files are discovered as mapping files</li>
	 * </ul>
	 *
	 * @see org.hibernate.boot.MetadataBuilder#applyScanOptions
	 */
	String SCANNER_DISCOVERY = "hibernate.archive.autodetection";

	/**
	 * Used to specify the {@link org.hibernate.boot.model.naming.ImplicitNamingStrategy} class to use.  The following
	 * short-names are defined for this setting:<ul>
	 *     <li>"default" -> {@link org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl}</li>
	 *     <li>"jpa" -> {@link org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl}</li>
	 *     <li>"legacy-jpa" -> {@link org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl}</li>
	 *     <li>"legacy-hbm" -> {@link org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyHbmImpl}</li>
	 *     <li>"component-path" -> {@link org.hibernate.boot.model.naming.ImplicitNamingStrategyComponentPathImpl}</li>
	 * </ul>
	 *
	 * The default is defined by the ImplicitNamingStrategy registered under the "default" key.  If that happens to
	 * be empty, the fallback is to use {@link org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl}.
	 *
	 * @see MetadataBuilder#applyImplicitNamingStrategy
	 *
	 * @since 5.0
	 */
	String IMPLICIT_NAMING_STRATEGY = "hibernate.implicit_naming_strategy";

	/**
	 * Used to specify the {@link org.hibernate.boot.model.naming.PhysicalNamingStrategy} class to use.
	 *
	 * @see MetadataBuilder#applyPhysicalNamingStrategy
	 *
	 * @since 5.0
	 */
	String PHYSICAL_NAMING_STRATEGY = "hibernate.physical_naming_strategy";

	/**
	 * Used to specify the order in which metadata sources should be processed.  Value
	 * is a delimited-list whose elements are defined by {@link org.hibernate.cfg.MetadataSourceType}.
	 * <p/>
	 * Default is {@code "hbm,class"} which indicates to process {@code hbm.xml} files followed by
	 * annotations (combined with {@code orm.xml} mappings).
	 *
	 * @see MetadataBuilder#applySourceProcessOrdering(org.hibernate.cfg.MetadataSourceType...)
	 */
	String ARTIFACT_PROCESSING_ORDER = "hibernate.mapping.precedence";

	/**
	 * Specifies whether to automatically quote any names that are deemed keywords.  Auto-quoting
	 * is disabled by default. Set to true to enable it.
	 *
	 * @since 5.0
	 */
	String KEYWORD_AUTO_QUOTING_ENABLED = "hibernate.auto_quote_keyword";

	/**
	 * Allows to skip processing of XML Mapping.
	 * This is for people using exclusively annotations to define their model, and might
	 * be able to improve efficiency of booting Hibernate ORM.
	 * By default, the XML mapping is taken into account.
	 * @since 5.4.1
	 */
	String XML_MAPPING_ENABLED = "hibernate.xml_mapping_enabled";


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SessionFactoryBuilder level settings
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Setting used to name the Hibernate {@link org.hibernate.SessionFactory}.
	 *
	 * Naming the SessionFactory allows for it to be properly serialized across JVMs as
	 * long as the same name is used on each JVM.
	 *
	 * If {@link #SESSION_FACTORY_NAME_IS_JNDI} is set to {@code true}, this is also the
	 * name under which the SessionFactory is bound into JNDI on startup and from which
	 * it can be obtained from JNDI.
	 *
	 * @see #SESSION_FACTORY_NAME_IS_JNDI
	 * @see org.hibernate.internal.SessionFactoryRegistry
	 */
	String SESSION_FACTORY_NAME = "hibernate.session_factory_name";

	/**
	 * Does the value defined by {@link #SESSION_FACTORY_NAME} represent a JNDI namespace into which
	 * the {@link org.hibernate.SessionFactory} should be bound and made accessible?
	 *
	 * Defaults to {@code true} for backwards compatibility.
	 *
	 * Set this to {@code false} if naming a SessionFactory is needed for serialization purposes, but
	 * no writable JNDI context exists in the runtime environment or if the user simply does not want
	 * JNDI to be used.
	 *
	 * @see #SESSION_FACTORY_NAME
	 */
	String SESSION_FACTORY_NAME_IS_JNDI = "hibernate.session_factory_name_is_jndi";

	/**
	 * Enable logging of generated SQL to the console
	 */
	String SHOW_SQL ="hibernate.show_sql";

	/**
	 * Enable formatting of SQL logged to the console
	 */
	String FORMAT_SQL ="hibernate.format_sql";

	/**
	 * Add comments to the generated SQL
	 */
	String USE_SQL_COMMENTS ="hibernate.use_sql_comments";

	/**
	 * Maximum depth of outer join fetching
	 */
	String MAX_FETCH_DEPTH = "hibernate.max_fetch_depth";

	/**
	 * The default batch size for batch fetching
	 */
	String DEFAULT_BATCH_FETCH_SIZE = "hibernate.default_batch_fetch_size";

	/**
	 * Use <tt>java.io</tt> streams to read / write binary data from / to JDBC
	 */
	String USE_STREAMS_FOR_BINARY = "hibernate.jdbc.use_streams_for_binary";

	/**
	 * Use JDBC scrollable <tt>ResultSet</tt>s. This property is only necessary when there is
	 * no <tt>ConnectionProvider</tt>, ie. the user is supplying JDBC connections.
	 */
	String USE_SCROLLABLE_RESULTSET = "hibernate.jdbc.use_scrollable_resultset";

	/**
	 * Tells the JDBC driver to attempt to retrieve row Id with the JDBC 3.0 PreparedStatement.getGeneratedKeys()
	 * method. In general, performance will be better if this property is set to true and the underlying
	 * JDBC driver supports getGeneratedKeys().
	 */
	String USE_GET_GENERATED_KEYS = "hibernate.jdbc.use_get_generated_keys";

	/**
	 * Gives the JDBC driver a hint as to the number of rows that should be fetched from the database
	 * when more rows are needed. If <tt>0</tt>, JDBC driver default settings will be used.
	 */
	String STATEMENT_FETCH_SIZE = "hibernate.jdbc.fetch_size";

	/**
	 * Maximum JDBC batch size. A nonzero value enables batch updates.
	 */
	String STATEMENT_BATCH_SIZE = "hibernate.jdbc.batch_size";
	/**
	 * Select a custom batcher.
	 */
	String BATCH_STRATEGY = "hibernate.jdbc.factory_class";

	/**
	 * Should versioned data be included in batching?
	 */
	String BATCH_VERSIONED_DATA = "hibernate.jdbc.batch_versioned_data";

	/**
	 * Default JDBC TimeZone. Unless specified, the JVM default TimeZone is going to be used by the underlying JDBC Driver.
	 *
	 * @since 5.2.3
	 */
	String JDBC_TIME_ZONE = "hibernate.jdbc.time_zone";

	/**
	 * Enable automatic session close at end of transaction
	 */
	String AUTO_CLOSE_SESSION = "hibernate.transaction.auto_close_session";

	/**
	 * Enable automatic flush during the JTA <tt>beforeCompletion()</tt> callback
	 */
	String FLUSH_BEFORE_COMPLETION = "hibernate.transaction.flush_before_completion";

	/**
	 * Specifies how Hibernate should acquire JDBC connections.  Should generally only configure
	 * this or {@link #RELEASE_CONNECTIONS}, not both
	 *
	 * @see org.hibernate.ConnectionAcquisitionMode
	 *
	 * @since 5.1
	 *
	 * @deprecated (since 5.2) use {@link #CONNECTION_HANDLING} instead
	 */
	@Deprecated
	String ACQUIRE_CONNECTIONS = "hibernate.connection.acquisition_mode";

	/**
	 * Specifies how Hibernate should release JDBC connections.  Should generally only configure
	 * this or {@link #ACQUIRE_CONNECTIONS}, not both
	 *
	 * @see org.hibernate.ConnectionReleaseMode
	 *
	 * @deprecated (since 5.2) use {@link #CONNECTION_HANDLING} instead
	 */
	@Deprecated
	String RELEASE_CONNECTIONS = "hibernate.connection.release_mode";

	/**
	 * Specifies how Hibernate should manage JDBC connections in terms of acquiring and releasing.
	 * Supersedes {@link #ACQUIRE_CONNECTIONS} and {@link #RELEASE_CONNECTIONS}
	 *
	 * @see org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode
	 *
	 * @since 5.2
	 */
	String CONNECTION_HANDLING = "hibernate.connection.handling_mode";

	/**
	 * Context scoping impl for {@link org.hibernate.SessionFactory#getCurrentSession()} processing.
	 */
	String CURRENT_SESSION_CONTEXT_CLASS = "hibernate.current_session_context_class";

	String USE_IDENTIFIER_ROLLBACK = "hibernate.use_identifier_rollback";

	/**
	 * Use bytecode libraries optimized property access
	 */
	String USE_REFLECTION_OPTIMIZER = "hibernate.bytecode.use_reflection_optimizer";

	/**
	 * Configure the global BytecodeProvider implementation to generate class names matching the
	 * existing naming patterns.
	 * It is not a good idea to rely on a classname to check if a class is an Hibernate proxy,
	 * yet some frameworks are currently relying on this.
	 * This option is disabled by default and will log a deprecation warning when enabled.
	 */
	String ENFORCE_LEGACY_PROXY_CLASSNAMES = "hibernate.bytecode.enforce_legacy_proxy_classnames";

	/**
	 * Should Hibernate use enhanced entities "as a proxy"?
	 *
	 * E.g., when an application uses {@link org.hibernate.Session#load} against an enhanced
	 * class, enabling this will allow Hibernate to create an "empty" instance of the enhanced
	 * class to act as the proxy - it contains just the identifier which is later used to
	 * trigger the base initialization but no other data is loaded
	 *
	 * Not enabling this (the legacy default behavior) would cause the "base" attributes to
	 * be loaded.  Any lazy-group attributes would not be initialized.
	 *
	 * Applications using bytecode enhancement and switching to allowing this should be careful
	 * in use of the various {@link org.hibernate.Hibernate} methods such as
	 * {@link org.hibernate.Hibernate#isInitialized},
	 * {@link org.hibernate.Hibernate#isPropertyInitialized}, etc - enabling this setting changes
	 * the results of those methods
	 *
	 * @implSpec See {@link org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor}
	 */
	String ALLOW_ENHANCEMENT_AS_PROXY = "hibernate.bytecode.allow_enhancement_as_proxy";

	/**
	 * The classname of the HQL query parser factory
	 */
	String QUERY_TRANSLATOR = "hibernate.query.factory_class";

	/**
	 * A comma-separated list of token substitutions to use when translating a Hibernate
	 * query to SQL
	 */
	String QUERY_SUBSTITUTIONS = "hibernate.query.substitutions";

	/**
	 * Should named queries be checked during startup (the default is enabled).
	 * <p/>
	 * Mainly intended for test environments.
	 */
	String QUERY_STARTUP_CHECKING = "hibernate.query.startup_check";

	/**
	 * Setting which indicates whether or not Java constant follow the Java Naming conventions.
	 * <p/>
	 * Default is {@code true}. Existing applications may want to disable this (set it {@code false}) if non-conventional Java constants are used.
	 * However, there is a significant performance overhead for using non-conventional Java constants since Hibernate cannot determine if aliases
	 * should be treated as Java constants or not.
	 *
	 * @since 5.2
	 */
	String CONVENTIONAL_JAVA_CONSTANTS = "hibernate.query.conventional_java_constants";

	/**
	 * The {@link org.hibernate.exception.spi.SQLExceptionConverter} to use for converting SQLExceptions
	 * to Hibernate's JDBCException hierarchy.  The default is to use the configured
	 * {@link org.hibernate.dialect.Dialect}'s preferred SQLExceptionConverter.
	 */
	String SQL_EXCEPTION_CONVERTER = "hibernate.jdbc.sql_exception_converter";

	/**
	 * Enable wrapping of JDBC result sets in order to speed up column name lookups for
	 * broken JDBC drivers
	 */
	String WRAP_RESULT_SETS = "hibernate.jdbc.wrap_result_sets";

	/**
	 * Indicates if exception handling for a SessionFactory built via Hibernate's native bootstrapping
	 * should behave the same as native exception handling in Hibernate ORM 5.1, When set to {@code true},
	 * {@link HibernateException} will not be wrapped or converted according to the JPA specification.
	 * <p/>
	 * This setting will be ignored for a SessionFactory built via JPA bootstrapping.
	 * <p/>
	 * Values are {@code true}  or {@code false}.
	 * Default value is {@code false}
	 */
	String NATIVE_EXCEPTION_HANDLING_51_COMPLIANCE = "hibernate.native_exception_handling_51_compliance";

	/**
	 * Enable ordering of update statements by primary key value
	 */
	String ORDER_UPDATES = "hibernate.order_updates";

	/**
	 * Enable ordering of insert statements for the purpose of more efficient JDBC batching.
	 */
	String ORDER_INSERTS = "hibernate.order_inserts";

	/**
	 * JPA Callbacks are enabled by default. Set this to {@code false} to disable them.
	 * Mostly useful to save a bit of memory when they are not used.
	 * Experimental and will likely be removed as soon as the memory overhead is resolved.
	 * @since 5.4
	 */
	String JPA_CALLBACKS_ENABLED = "hibernate.jpa_callbacks.enabled";

	/**
	 * Default precedence of null values in {@code ORDER BY} clause.  Supported options: {@code none} (default),
	 * {@code first}, {@code last}.
	 */
	String DEFAULT_NULL_ORDERING = "hibernate.order_by.default_null_ordering";

	/**
	 * Enable fetching JDBC statement warning for logging.
	 *
	 * Values are {@code true}  or {@code false} .
	 * Default value is {@link org.hibernate.dialect.Dialect#isJdbcLogWarningsEnabledByDefault()}
	 *
	 * @since 5.1
	 */
	String LOG_JDBC_WARNINGS =  "hibernate.jdbc.log.warnings";

	/**
	 * Identifies an explicit {@link org.hibernate.resource.beans.container.spi.BeanContainer}
	 * to be used.
	 *
	 * Note that for CDI-based containers setting this is not necessary - simply
	 * pass the BeanManager to use via {@link #CDI_BEAN_MANAGER} and
	 * optionally specify {@link #DELAY_CDI_ACCESS}.  This setting is more meant to
	 * integrate non-CDI bean containers such as Spring.
	 *
	 * @since 5.3
	 */
	String BEAN_CONTAINER = "hibernate.resource.beans.container";



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// c3p0 connection pooling specific settings
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * A setting prefix used to indicate settings that target the hibernate-c3p0 integration
	 */
	String C3P0_CONFIG_PREFIX = "hibernate.c3p0";

	/**
	 * Maximum size of C3P0 connection pool
	 */
	String C3P0_MAX_SIZE = "hibernate.c3p0.max_size";

	/**
	 * Minimum size of C3P0 connection pool
	 */
	String C3P0_MIN_SIZE = "hibernate.c3p0.min_size";

	/**
	 * Maximum idle time for C3P0 connection pool
	 */
	String C3P0_TIMEOUT = "hibernate.c3p0.timeout";

	/**
	 * Maximum size of C3P0 statement cache
	 */
	String C3P0_MAX_STATEMENTS = "hibernate.c3p0.max_statements";

	/**
	 * Number of connections acquired when pool is exhausted
	 */
	String C3P0_ACQUIRE_INCREMENT = "hibernate.c3p0.acquire_increment";

	/**
	 * Idle time before a C3P0 pooled connection is validated
	 */
	String C3P0_IDLE_TEST_PERIOD = "hibernate.c3p0.idle_test_period";



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// proxool connection pooling specific settings
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * A setting prefix used to indicate settings that target the hibernate-proxool integration
	 */
	String PROXOOL_CONFIG_PREFIX = "hibernate.proxool";

	/**
	 * Proxool/Hibernate property prefix
	 * @deprecated Use {@link #PROXOOL_CONFIG_PREFIX} instead
	 */
	@Deprecated
	String PROXOOL_PREFIX = PROXOOL_CONFIG_PREFIX;

	/**
	 * Proxool property to configure the Proxool Provider using an XML (<tt>/path/to/file.xml</tt>)
	 */
	String PROXOOL_XML = "hibernate.proxool.xml";

	/**
	 * Proxool property to configure the Proxool Provider  using a properties file (<tt>/path/to/proxool.properties</tt>)
	 */
	String PROXOOL_PROPERTIES = "hibernate.proxool.properties";

	/**
	 * Proxool property to configure the Proxool Provider from an already existing pool (<tt>true</tt> / <tt>false</tt>)
	 */
	String PROXOOL_EXISTING_POOL = "hibernate.proxool.existing_pool";

	/**
	 * Proxool property with the Proxool pool alias to use
	 * (Required for <tt>PROXOOL_EXISTING_POOL</tt>, <tt>PROXOOL_PROPERTIES</tt>, or
	 * <tt>PROXOOL_XML</tt>)
	 */
	String PROXOOL_POOL_ALIAS = "hibernate.proxool.pool_alias";


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Second-level cache settings
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * The {@link org.hibernate.cache.spi.RegionFactory} implementation.  Can refer to:<ul>
	 *     <li>an Object implementing {@link org.hibernate.cache.spi.RegionFactory}</li>
	 *     <li>a Class implementing {@link org.hibernate.cache.spi.RegionFactory}</li>
	 *     <li>FQN of a Class implementing {@link org.hibernate.cache.spi.RegionFactory}</li>
	 * </ul>
	 */
	String CACHE_REGION_FACTORY = "hibernate.cache.region.factory_class";

	/**
	 * Allow control to specify the {@link org.hibernate.cache.spi.CacheKeysFactory} impl to use.
	 * Can refer to:<ul>
	 *     <li>an Object implementing {@link org.hibernate.cache.spi.CacheKeysFactory}</li>
	 *     <li>a Class implementing {@link org.hibernate.cache.spi.CacheKeysFactory}</li>
	 *     <li>FQN of a Class implementing {@link org.hibernate.cache.spi.CacheKeysFactory}</li>
	 *     <li>'default' as a short name for {@link org.hibernate.cache.internal.DefaultCacheKeysFactory}</li>
	 *     <li>'simple' as a short name for {@link org.hibernate.cache.internal.SimpleCacheKeysFactory}</li>
	 * </ul>
	 *
	 * @since 5.2 - note that currently this is only honored for hibernate-infinispan
	 */
	String CACHE_KEYS_FACTORY = "hibernate.cache.keys_factory";

	/**
	 * The <tt>CacheProvider</tt> implementation class
	 */
	String CACHE_PROVIDER_CONFIG = "hibernate.cache.provider_configuration_file_resource_path";

	/**
	 * Enable the second-level cache.
	 * <p>
	 * By default, if the currently configured {@link org.hibernate.cache.spi.RegionFactory} is not the {@link org.hibernate.cache.internal.NoCachingRegionFactory},
	 * then the second-level cache is going to be enabled. Otherwise, the second-level cache is disabled.
	 */
	String USE_SECOND_LEVEL_CACHE = "hibernate.cache.use_second_level_cache";

	/**
	 * Enable the query cache (disabled by default)
	 */
	String USE_QUERY_CACHE = "hibernate.cache.use_query_cache";

	/**
	 * The {@link TimestampsCacheFactory} implementation class.
	 */
	String QUERY_CACHE_FACTORY = "hibernate.cache.query_cache_factory";

	/**
	 * The <tt>CacheProvider</tt> region name prefix
	 */
	String CACHE_REGION_PREFIX = "hibernate.cache.region_prefix";

	/**
	 * Optimize the cache for minimal puts instead of minimal gets
	 */
	String USE_MINIMAL_PUTS = "hibernate.cache.use_minimal_puts";

	/**
	 * Enable use of structured second-level cache entries
	 */
	String USE_STRUCTURED_CACHE = "hibernate.cache.use_structured_entries";

	/**
	 * Enables the automatic eviction of a bi-directional association's collection cache when an element in the
	 * ManyToOne collection is added/updated/removed without properly managing the change on the OneToMany side.
	 */
	String AUTO_EVICT_COLLECTION_CACHE = "hibernate.cache.auto_evict_collection_cache";

	/**
	 * Enable direct storage of entity references into the second level cache when applicable (immutable data, etc).
	 * Default is to not store direct references.
	 */
	String USE_DIRECT_REFERENCE_CACHE_ENTRIES = "hibernate.cache.use_reference_entries";






	// Still to categorize

	/**
	 * The EntityMode in which set the Session opened from the SessionFactory.
	 */
	String DEFAULT_ENTITY_MODE = "hibernate.default_entity_mode";

	/**
	 * Should all database identifiers be quoted.  A {@code true}/{@code false} option.
	 */
	String GLOBALLY_QUOTED_IDENTIFIERS = "hibernate.globally_quoted_identifiers";

	/**
	 * Assuming {@link #GLOBALLY_QUOTED_IDENTIFIERS}, this allows such global quoting
	 * to skip column-definitions as defined by {@link javax.persistence.Column},
	 * {@link javax.persistence.JoinColumn}, etc.
	 * <p/>
	 * JPA states that column-definitions are subject to global quoting, so by default this setting
	 * is {@code false} for JPA compliance.  Set to {@code true} to avoid column-definitions
	 * being quoted due to global quoting (they will still be quoted if explicitly quoted in the
	 * annotation/xml).
	 */
	String GLOBALLY_QUOTED_IDENTIFIERS_SKIP_COLUMN_DEFINITIONS = "hibernate.globally_quoted_identifiers_skip_column_definitions";

	/**
	 * Enable nullability checking.
	 * Raises an exception if a property marked as not-null is null.
	 * Default to false if Bean Validation is present in the classpath and Hibernate Annotations is used,
	 * true otherwise.
	 */
	String CHECK_NULLABILITY = "hibernate.check_nullability";


	/**
	 * Pick which bytecode enhancing library to use. Currently supports javassist and bytebuddy, bytebuddy being the default since version 5.3.
	 */
	String BYTECODE_PROVIDER = "hibernate.bytecode.provider";

	String JPAQL_STRICT_COMPLIANCE= "hibernate.query.jpaql_strict_compliance";

	/**
	 * When using pooled {@link org.hibernate.id.enhanced.Optimizer optimizers}, prefer interpreting the
	 * database value as the lower (lo) boundary.  The default is to interpret it as the high boundary.
	 *
	 * @deprecated Use {@link #PREFERRED_POOLED_OPTIMIZER} instead
	 */
	@Deprecated
	String PREFER_POOLED_VALUES_LO = "hibernate.id.optimizer.pooled.prefer_lo";

	/**
	 * When a generator specified an increment-size and an optimizer was not explicitly specified, which of
	 * the "pooled" optimizers should be preferred?  Can specify an optimizer short name or an Optimizer
	 * impl FQN.
	 */
	String PREFERRED_POOLED_OPTIMIZER = "hibernate.id.optimizer.pooled.preferred";

	/**
	 * The maximum number of strong references maintained by {@link org.hibernate.engine.query.spi.QueryPlanCache}. Default is 128.
	 * @deprecated in favor of {@link #QUERY_PLAN_CACHE_PARAMETER_METADATA_MAX_SIZE}
	 */
	@Deprecated
	String QUERY_PLAN_CACHE_MAX_STRONG_REFERENCES = "hibernate.query.plan_cache_max_strong_references";

	/**
	 * The maximum number of soft references maintained by {@link org.hibernate.engine.query.spi.QueryPlanCache}. Default is 2048.
	 * @deprecated in favor of {@link #QUERY_PLAN_CACHE_MAX_SIZE}
	 */
	@Deprecated
	String QUERY_PLAN_CACHE_MAX_SOFT_REFERENCES = "hibernate.query.plan_cache_max_soft_references";

	/**
	 * The maximum number of entries including:
	 * <ul>
	 *     <li>{@link org.hibernate.engine.query.spi.HQLQueryPlan}</li>
	 *     <li>{@link org.hibernate.engine.query.spi.FilterQueryPlan}</li>
	 *     <li>{@link org.hibernate.engine.query.spi.NativeSQLQueryPlan}</li>
	 * </ul>
	 *
	 * maintained by {@link org.hibernate.engine.query.spi.QueryPlanCache}. Default is 2048.
	 */
	String QUERY_PLAN_CACHE_MAX_SIZE = "hibernate.query.plan_cache_max_size";

	/**
	 * The maximum number of {@link ParameterMetadataImpl} maintained
	 * by {@link org.hibernate.engine.query.spi.QueryPlanCache}. Default is 128.
	 */
	String QUERY_PLAN_CACHE_PARAMETER_METADATA_MAX_SIZE = "hibernate.query.plan_parameter_metadata_max_size";

	/**
	 * Should we not use contextual LOB creation (aka based on {@link java.sql.Connection#createBlob()} et al).
	 */
	String NON_CONTEXTUAL_LOB_CREATION = "hibernate.jdbc.lob.non_contextual_creation";


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SchemaManagementTool settings
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Setting to perform SchemaManagementTool actions automatically as part of
	 * the SessionFactory lifecycle.  Valid options are defined by the
	 * {@link org.hibernate.tool.schema.Action} enum.
	 * <p/>
	 * Interpreted in combination with {@link #HBM2DDL_DATABASE_ACTION} and
	 * {@link #HBM2DDL_SCRIPTS_ACTION}.  If no value is specified, the default
	 * is "none" ({@link org.hibernate.tool.schema.Action#NONE}).
	 *
	 * @see org.hibernate.tool.schema.Action
	 */
	String HBM2DDL_AUTO = "hibernate.hbm2ddl.auto";

	/**
	 * Setting to perform SchemaManagementTool actions against the database directly via JDBC
	 * automatically as part of the SessionFactory lifecycle.  Valid options are defined by the
	 * {@link org.hibernate.tool.schema.Action} enum.
	 * <p/>
	 * Interpreted in combination with {@link #HBM2DDL_AUTO}.  If no value is specified, the default
	 * is "none" ({@link org.hibernate.tool.schema.Action#NONE}).
	 *
	 * @see org.hibernate.tool.schema.Action
	 */
	String HBM2DDL_DATABASE_ACTION = "javax.persistence.schema-generation.database.action";

	/**
	 * Setting to perform SchemaManagementTool actions writing the commands into a DDL script file.
	 * Valid options are defined by the {@link org.hibernate.tool.schema.Action} enum.
	 * <p/>
	 * Interpreted in combination with {@link #HBM2DDL_AUTO}.  If no value is specified, the default
	 * is "none" ({@link org.hibernate.tool.schema.Action#NONE}).
	 *
	 * @see org.hibernate.tool.schema.Action
	 */
	String HBM2DDL_SCRIPTS_ACTION = "javax.persistence.schema-generation.scripts.action";

	/**
	 * Allows passing a specific {@link java.sql.Connection} instance to be used by SchemaManagementTool.
	 * <p/>
	 * May also be used to determine the values for {@value #HBM2DDL_DB_NAME},
	 * {@value #HBM2DDL_DB_MAJOR_VERSION} and {@value #HBM2DDL_DB_MINOR_VERSION}.
	 */
	String HBM2DDL_CONNECTION = "javax.persistence.schema-generation-connection";

	/**
	 * Specifies the name of the database provider in cases where a Connection to the underlying database is
	 * not available (aka, mainly in generating scripts).  In such cases, a value for this setting
	 * *must* be specified.
	 * <p/>
	 * The value of this setting is expected to match the value returned by
	 * {@link java.sql.DatabaseMetaData#getDatabaseProductName()} for the target database.
	 * <p/>
	 * Additionally specifying {@value #HBM2DDL_DB_MAJOR_VERSION} and/or {@value #HBM2DDL_DB_MINOR_VERSION}
	 * may be required to understand exactly how to generate the required schema commands.
	 *
	 * @see #HBM2DDL_DB_MAJOR_VERSION
	 * @see #HBM2DDL_DB_MINOR_VERSION
	 */
	@SuppressWarnings("JavaDoc")
	String HBM2DDL_DB_NAME = "javax.persistence.database-product-name";

	/**
	 * Specifies the major version of the underlying database, as would be returned by
	 * {@link java.sql.DatabaseMetaData#getDatabaseMajorVersion} for the target database.  This value is used to
	 * help more precisely determine how to perform schema generation tasks for the underlying database in cases
	 * where {@value #HBM2DDL_DB_NAME} does not provide enough distinction.

	 * @see #HBM2DDL_DB_NAME
	 * @see #HBM2DDL_DB_MINOR_VERSION
	 */
	String HBM2DDL_DB_MAJOR_VERSION = "javax.persistence.database-major-version";

	/**
	 * Specifies the minor version of the underlying database, as would be returned by
	 * {@link java.sql.DatabaseMetaData#getDatabaseMinorVersion} for the target database.  This value is used to
	 * help more precisely determine how to perform schema generation tasks for the underlying database in cases
	 * where the combination of {@value #HBM2DDL_DB_NAME} and {@value #HBM2DDL_DB_MAJOR_VERSION} does not provide
	 * enough distinction.
	 *
	 * @see #HBM2DDL_DB_NAME
	 * @see #HBM2DDL_DB_MAJOR_VERSION
	 */
	String HBM2DDL_DB_MINOR_VERSION = "javax.persistence.database-minor-version";

	/**
	 * Specifies whether schema generation commands for schema creation are to be determined based on object/relational
	 * mapping metadata, DDL scripts, or a combination of the two.  See {@link SourceType} for valid set of values.
	 * If no value is specified, a default is assumed as follows:<ul>
	 *     <li>
	 *         if source scripts are specified (per {@value #HBM2DDL_CREATE_SCRIPT_SOURCE}),then "scripts" is assumed
	 *     </li>
	 *     <li>
	 *         otherwise, "metadata" is assumed
	 *     </li>
	 * </ul>
	 *
	 * @see SourceType
	 */
	String HBM2DDL_CREATE_SOURCE = "javax.persistence.schema-generation.create-source";

	/**
	 * Specifies whether schema generation commands for schema dropping are to be determined based on object/relational
	 * mapping metadata, DDL scripts, or a combination of the two.  See {@link SourceType} for valid set of values.
	 * If no value is specified, a default is assumed as follows:<ul>
	 *     <li>
	 *         if source scripts are specified (per {@value #HBM2DDL_DROP_SCRIPT_SOURCE}),then "scripts" is assumed
	 *     </li>
	 *     <li>
	 *         otherwise, "metadata" is assumed
	 *     </li>
	 * </ul>
	 *
	 * @see SourceType
	 */
	String HBM2DDL_DROP_SOURCE = "javax.persistence.schema-generation.drop-source";

	/**
	 * Specifies the CREATE script file as either a {@link java.io.Reader} configured for reading of the DDL script
	 * file or a string designating a file {@link java.net.URL} for the DDL script.
	 * <p/>
	 * Hibernate historically also accepted {@link #HBM2DDL_IMPORT_FILES} for a similar purpose.  This setting
	 * should be preferred over {@link #HBM2DDL_IMPORT_FILES} moving forward
	 *
	 * @see #HBM2DDL_CREATE_SOURCE
	 * @see #HBM2DDL_IMPORT_FILES
	 */
	String HBM2DDL_CREATE_SCRIPT_SOURCE = "javax.persistence.schema-generation.create-script-source";

	/**
	 * Specifies the DROP script file as either a {@link java.io.Reader} configured for reading of the DDL script
	 * file or a string designating a file {@link java.net.URL} for the DDL script.
	 *
	 * @see #HBM2DDL_DROP_SOURCE
	 */
	String HBM2DDL_DROP_SCRIPT_SOURCE = "javax.persistence.schema-generation.drop-script-source";

	/**
	 * For cases where the {@value #HBM2DDL_SCRIPTS_ACTION} value indicates that schema creation commands should
	 * be written to DDL script file, {@value #HBM2DDL_SCRIPTS_CREATE_TARGET} specifies either a
	 * {@link java.io.Writer} configured for output of the DDL script or a string specifying the file URL for the DDL
	 * script.
	 *
	 * @see #HBM2DDL_SCRIPTS_ACTION
	 */
	@SuppressWarnings("JavaDoc")
	String HBM2DDL_SCRIPTS_CREATE_TARGET = "javax.persistence.schema-generation.scripts.create-target";

	/**
	 * For cases where the {@value #HBM2DDL_SCRIPTS_ACTION} value indicates that schema drop commands should
	 * be written to DDL script file, {@value #HBM2DDL_SCRIPTS_DROP_TARGET} specifies either a
	 * {@link java.io.Writer} configured for output of the DDL script or a string specifying the file URL for the DDL
	 * script.
	 *
	 * @see #HBM2DDL_SCRIPTS_ACTION
	 */
	@SuppressWarnings("JavaDoc")
	String HBM2DDL_SCRIPTS_DROP_TARGET = "javax.persistence.schema-generation.scripts.drop-target";

	/**
	 * Comma-separated names of the optional files containing SQL DML statements executed
	 * during the SessionFactory creation.
	 * File order matters, the statements of a give file are executed before the statements of the
	 * following files.
	 * <p/>
	 * These statements are only executed if the schema is created ie if <tt>hibernate.hbm2ddl.auto</tt>
	 * is set to <tt>create</tt> or <tt>create-drop</tt>.
	 * <p/>
	 * The default value is <tt>/import.sql</tt>
	 * <p/>
	 * {@link #HBM2DDL_CREATE_SCRIPT_SOURCE} / {@link #HBM2DDL_DROP_SCRIPT_SOURCE} should be preferred
	 * moving forward
	 */
	String HBM2DDL_IMPORT_FILES = "hibernate.hbm2ddl.import_files";

	/**
	 * JPA variant of {@link #HBM2DDL_IMPORT_FILES}
	 * <p/>
	 * Specifies a {@link java.io.Reader} configured for reading of the SQL load script or a string designating the
	 * file {@link java.net.URL} for the SQL load script.
	 * <p/>
	 * A "SQL load script" is a script that performs some database initialization (INSERT, etc).
	 */
	String HBM2DDL_LOAD_SCRIPT_SOURCE = "javax.persistence.sql-load-script-source";

	/**
	 * Reference to the {@link org.hibernate.tool.hbm2ddl.ImportSqlCommandExtractor} implementation class
	 * to use for parsing source/import files as defined by {@link #HBM2DDL_CREATE_SCRIPT_SOURCE},
	 * {@link #HBM2DDL_DROP_SCRIPT_SOURCE} or {@link #HBM2DDL_IMPORT_FILES}.
	 * <p/>
	 * Reference may refer to an instance, a Class implementing ImportSqlCommandExtractor of the FQN
	 * of the ImportSqlCommandExtractor implementation.  If the FQN is given, the implementation
	 * must provide a no-arg constructor.
	 * <p/>
	 * The default value is {@link org.hibernate.tool.hbm2ddl.SingleLineSqlCommandExtractor}.
	 */
	String HBM2DDL_IMPORT_FILES_SQL_EXTRACTOR = "hibernate.hbm2ddl.import_files_sql_extractor";

	/**
	 * Specifies whether to automatically create also the database schema/catalog.
	 * The default is false.
	 *
	 * @since 5.0
	 */
	String HBM2DDL_CREATE_NAMESPACES = "hibernate.hbm2ddl.create_namespaces";


	/**
	 * Specifies whether to automatically create also the database schema/catalog.
	 * The default is false.
	 *
	 * @since 5.0
	 * @deprecated
	 */
	@Deprecated
	String HBM2DLL_CREATE_NAMESPACES = "hibernate.hbm2dll.create_namespaces";

	/**
	 * The JPA variant of {@link #HBM2DDL_CREATE_NAMESPACES}
	 * <p/>
	 * Specifies whether the persistence provider is to create the database schema(s) in addition to creating
	 * database objects (tables, sequences, constraints, etc).  The value of this boolean property should be set
	 * to {@code true} if the persistence provider is to create schemas in the database or to generate DDL that
	 * contains "CREATE SCHEMA" commands.  If this property is not supplied (or is explicitly {@code false}), the
	 * provider should not attempt to create database schemas.
	 */
	String HBM2DDL_CREATE_SCHEMAS = "javax.persistence.create-database-schemas";

	/**
	 * @deprecated Use {@link #HBM2DDL_CREATE_SCHEMAS} instead: this variable name had a typo.
	 */
	@Deprecated
	String HBM2DLL_CREATE_SCHEMAS = HBM2DDL_CREATE_SCHEMAS;

	/**
	 * Used to specify the {@link org.hibernate.tool.schema.spi.SchemaFilterProvider} to be used by
	 * create, drop, migrate and validate operations on the database schema.  SchemaFilterProvider
	 * provides filters that can be used to limit the scope of these operations to specific namespaces,
	 * tables and sequences. All objects are included by default.
	 *
	 * @since 5.1
	 */
	String HBM2DDL_FILTER_PROVIDER = "hibernate.hbm2ddl.schema_filter_provider";

	/**
	 * Setting to choose the strategy used to access the JDBC Metadata.
	 *
	 * Valid options are defined by the {@link JdbcMetadaAccessStrategy} enum.
	 *
	 * {@link JdbcMetadaAccessStrategy#GROUPED} is the default value.
	 *
	 * @see JdbcMetadaAccessStrategy
	 */
	String HBM2DDL_JDBC_METADATA_EXTRACTOR_STRATEGY = "hibernate.hbm2ddl.jdbc_metadata_extraction_strategy";

	/**
	 * Identifies the delimiter to use to separate schema management statements in script outputs
	 */
	String HBM2DDL_DELIMITER = "hibernate.hbm2ddl.delimiter";

	/**
	 * The name of the charset used by the schema generation resource. Without specifying this configuration property, the JVM default charset is used.
	 *
	 * @since 5.2.3
	 */
	String HBM2DDL_CHARSET_NAME = "hibernate.hbm2ddl.charset_name";

	/**
	 * Whether the schema migration tool should halt on error, therefore terminating the bootstrap process.
	 *
	 * @since 5.2.4
	 */
	String HBM2DDL_HALT_ON_ERROR = "hibernate.hbm2ddl.halt_on_error";

	String JMX_ENABLED = "hibernate.jmx.enabled";
	String JMX_PLATFORM_SERVER = "hibernate.jmx.usePlatformServer";
	String JMX_AGENT_ID = "hibernate.jmx.agentId";
	String JMX_DOMAIN_NAME = "hibernate.jmx.defaultDomain";
	String JMX_SF_NAME = "hibernate.jmx.sessionFactoryName";
	String JMX_DEFAULT_OBJ_NAME_DOMAIN = "org.hibernate.core";

	/**
	 * Setting to identify a {@link org.hibernate.CustomEntityDirtinessStrategy} to use.  May point to
	 * either a class name or instance.
	 */
	String CUSTOM_ENTITY_DIRTINESS_STRATEGY = "hibernate.entity_dirtiness_strategy";

	/**
	 * Controls whether an entity's "where" clause, mapped using <code>@Where(clause="....")</code>
	 * or <code>&lt;entity ... where="..."&gt;</code>, is taken into account when loading one-to-many
	 * or many-to-many collections of that type of entity.
	 * <p/>
	 * This setting has no affect on collections of embeddable values containing an association to
	 * that type of entity.
	 * <p/>
	 * When `true` (the default), the entity's "where" clause will be taken into account when loading
	 * one-to-many or many-to-many collections of that type of entity.
	 * <p/>
	 * `false` indicates that the entity's "where" clause will be ignored when loading one-to-many or
	 * many-to-many collections of that type of entity.
	 */
	String USE_ENTITY_WHERE_CLAUSE_FOR_COLLECTIONS = "hibernate.use_entity_where_clause_for_collections";

	/**
	 * Strategy for multi-tenancy.

	 * @see org.hibernate.MultiTenancyStrategy
	 * @since 4.0
	 */
	String MULTI_TENANT = "hibernate.multiTenancy";

	/**
	 * Names a {@link org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider} implementation to
	 * use.  As MultiTenantConnectionProvider is also a service, can be configured directly through the
	 * {@link org.hibernate.boot.registry.StandardServiceRegistryBuilder}
	 *
	 * @since 4.1
	 */
	String MULTI_TENANT_CONNECTION_PROVIDER = "hibernate.multi_tenant_connection_provider";

	/**
	 * Names a {@link org.hibernate.context.spi.CurrentTenantIdentifierResolver} implementation to use.
	 * <p/>
	 * Can be<ul>
	 *     <li>CurrentTenantIdentifierResolver instance</li>
	 *     <li>CurrentTenantIdentifierResolver implementation {@link Class} reference</li>
	 *     <li>CurrentTenantIdentifierResolver implementation class name</li>
	 * </ul>
	 *
	 * @since 4.1
	 */
	String MULTI_TENANT_IDENTIFIER_RESOLVER = "hibernate.tenant_identifier_resolver";

	/**
	 * Names a {@link org.hibernate.Interceptor} implementation to be applied to the
	 * {@link org.hibernate.SessionFactory} and propagated to each Session created from the SessionFactory.
	 * This setting identifies an Interceptor which is effectively a singleton across all the Sessions
	 * opened from the SessionFactory to which it is applied; the same instance will be passed to each Session.
	 * <p/>
	 * See {@link #SESSION_SCOPED_INTERCEPTOR} for an approach to create unique Interceptor instances for each Session
	 * <p/>
	 * Can reference<ul>
	 *     <li>Interceptor instance</li>
	 *     <li>Interceptor implementation {@link Class} reference</li>
	 *     <li>Interceptor implementation class name</li>
	 * </ul>
	 *
	 * @since 5.0
	 */
	String INTERCEPTOR = "hibernate.session_factory.interceptor";

	/**
	 * Names a {@link org.hibernate.Interceptor} implementation to be applied to the
	 * {@link org.hibernate.SessionFactory} and propagated to each Session created from the SessionFactory.
	 * This setting identifies an Interceptor implementation that is to be applied to every Session opened
	 * from the SessionFactory, but unlike {@link #INTERCEPTOR} a unique instance of the Interceptor is
	 * used for each Session.
	 * <p/>
	 * Can reference<ul>
	 *     <li>Interceptor implementation {@link Class} reference</li>
	 *     <li>Interceptor implementation class name</li>
	 *     <li>{@link Supplier} instance which is used to retrieve the interceptor</li>
	 * </ul>
	 * Note specifically that this setting cannot name an Interceptor instance.
	 *
	 * @since 5.2
	 */
	String SESSION_SCOPED_INTERCEPTOR = "hibernate.session_factory.session_scoped_interceptor";

	/**
	 * Names a {@link org.hibernate.resource.jdbc.spi.StatementInspector} implementation to be applied to
	 * the {@link org.hibernate.SessionFactory}.  Can reference<ul>
	 *     <li>StatementInspector instance</li>
	 *     <li>StatementInspector implementation {@link Class} reference</li>
	 *     <li>StatementInspector implementation class name (FQN)</li>
	 * </ul>
	 *
	 * @since 5.0
	 */
	String STATEMENT_INSPECTOR = "hibernate.session_factory.statement_inspector";

	String ENABLE_LAZY_LOAD_NO_TRANS = "hibernate.enable_lazy_load_no_trans";

	String HQL_BULK_ID_STRATEGY = "hibernate.hql.bulk_id_strategy";

	/**
	 * Names the {@link org.hibernate.loader.BatchFetchStyle} to use.  Can specify either the
	 * {@link org.hibernate.loader.BatchFetchStyle} name (insensitively), or a
	 * {@link org.hibernate.loader.BatchFetchStyle} instance.
	 *
	 * {@code LEGACY} is the default value.
	 */
	String BATCH_FETCH_STYLE = "hibernate.batch_fetch_style";

	/**
	 * Controls how the individual Loaders for an entity are created.
	 *
	 * When `true` (the default), only the minimal set of Loaders are
	 * created.  These include the handling for {@link org.hibernate.LockMode#READ}
	 * and {@link org.hibernate.LockMode#NONE} as well as specialized Loaders for
	 * merge and refresh handling.
	 *
	 * `false` indicates that all loaders should be created up front
	 *
	 * @since 5.3
	 */
	String DELAY_ENTITY_LOADER_CREATIONS = "hibernate.loader.delay_entity_loader_creations";

	/**
	 * A transaction can be rolled back by another thread ("tracking by thread")
	 * -- not the original application. Examples of this include a JTA
	 * transaction timeout handled by a background reaper thread.  The ability
	 * to handle this situation requires checking the Thread ID every time
	 * Session is called.  This can certainly have performance considerations.
	 *
	 * Default is <code>true</code> (enabled).
	 */
	String JTA_TRACK_BY_THREAD = "hibernate.jta.track_by_thread";

	String JACC_CONTEXT_ID = "hibernate.jacc_context_id";
	String JACC_PREFIX = "hibernate.jacc";
	String JACC_ENABLED = "hibernate.jacc.enabled";

	/**
	 * If enabled, allows schema update and validation to support synonyms.  Due
	 * to the possibility that this would return duplicate tables (especially in
	 * Oracle), this is disabled by default.
	 */
	String ENABLE_SYNONYMS = "hibernate.synonyms";

	/**
	 * Identifies a comma-separate list of values to specify extra table types,
	 * other than the default "TABLE" value, to recognize as defining a physical table
	 * by schema update, creation and validation.
	 *
	 * @since 5.0
	 */
	String EXTRA_PHYSICAL_TABLE_TYPES = "hibernate.hbm2ddl.extra_physical_table_types";

	/**
	 * @deprecated use {@link #EXTRA_PHYSICAL_TABLE_TYPES} instead.
	 */
	@Deprecated
	String DEPRECATED_EXTRA_PHYSICAL_TABLE_TYPES = "hibernate.hbm2dll.extra_physical_table_types";

	/**
	 * Unique columns and unique keys both use unique constraints in most dialects.
	 * SchemaUpdate needs to create these constraints, but DB's
	 * support for finding existing constraints is extremely inconsistent. Further,
	 * non-explicitly-named unique constraints use randomly generated characters.
	 *
	 * Therefore, select from these strategies.
	 * {@link org.hibernate.tool.hbm2ddl.UniqueConstraintSchemaUpdateStrategy#DROP_RECREATE_QUIETLY} (DEFAULT):
	 * 			Attempt to drop, then (re-)create each unique constraint.
	 * 			Ignore any exceptions thrown.
	 * {@link org.hibernate.tool.hbm2ddl.UniqueConstraintSchemaUpdateStrategy#RECREATE_QUIETLY}:
	 * 			attempt to (re-)create unique constraints,
	 * 			ignoring exceptions thrown if the constraint already existed
	 * {@link org.hibernate.tool.hbm2ddl.UniqueConstraintSchemaUpdateStrategy#SKIP}:
	 * 			do not attempt to create unique constraints on a schema update
	 */
	String UNIQUE_CONSTRAINT_SCHEMA_UPDATE_STRATEGY = "hibernate.schema_update.unique_constraint_strategy";

	/**
	 * Enable statistics collection
	 */
	String GENERATE_STATISTICS = "hibernate.generate_statistics";

	/**
	 * A setting to control whether to {@link org.hibernate.engine.internal.StatisticalLoggingSessionEventListener} is
	 * enabled on all Sessions (unless explicitly disabled for a given Session).  The default value of this
	 * setting is determined by the value for {@link #GENERATE_STATISTICS}, meaning that if collection of statistics
	 * is enabled logging of Session metrics is enabled by default too.
	 */
	String LOG_SESSION_METRICS = "hibernate.session.events.log";

	/**
	 * Setting that logs query which executed slower than specified milliseconds. Default is 0 (disabled).
	 */
	String LOG_SLOW_QUERY = "hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS";

	/**
	 * Defines a default {@link org.hibernate.SessionEventListener} to be applied to opened Sessions.
	 */
	String AUTO_SESSION_EVENTS_LISTENER = "hibernate.session.events.auto";

	/**
	 * Global setting for whether NULL parameter bindings should be passed to database
	 * procedure/function calls as part of {@link org.hibernate.procedure.ProcedureCall}
	 * handling.  Implicitly Hibernate will not pass the NULL, the intention being to allow
	 * any default argument values to be applied.
	 * <p/>
	 * This defines a global setting, which can then be controlled per parameter via
	 * {@link org.hibernate.procedure.ParameterRegistration#enablePassingNulls(boolean)}
	 * <p/>
	 * Values are {@code true} (pass the NULLs) or {@code false} (do not pass the NULLs).
	 *
	 * @deprecated (5.3) Hibernate determines it implicitly
	 */
	@Deprecated
	String PROCEDURE_NULL_PARAM_PASSING = "hibernate.proc.param_null_passing";

	/**
	 * [EXPERIMENTAL] Enable instantiation of composite/embedded objects when all of its attribute values are {@code null}.
	 * The default (and historical) behavior is that a {@code null} reference will be used to represent the
	 * composite when all of its attributes are {@code null}
	 * <p/>
	 * This is an experimental feature that has known issues. It should not be used in production
	 * until it is stabilized. See Hibernate Jira issue HHH-11936 for details.
	 *
	 * @since 5.1
	 */
	String CREATE_EMPTY_COMPOSITES_ENABLED = "hibernate.create_empty_composites.enabled";

	/**
	 * Setting that allows access to the underlying {@link org.hibernate.Transaction}, even
	 * when using a JTA since normal JPA operations prohibit this behavior.
	 * <p/>
	 * Values are {@code true} grants access, {@code false} does not.
	 * <p/>
	 * The default behavior is to allow access unless the session is bootstrapped via JPA.
	 */
	String ALLOW_JTA_TRANSACTION_ACCESS = "hibernate.jta.allowTransactionAccess";

	/**
	 * Setting that allows to perform update operations outside of a transaction boundary.
	 *
	 * Since version 5.2 Hibernate conforms with the JPA specification and does not allow anymore
	 * to flush any update out of a transaction boundary.
	 * <p/>
	 * Values are: {@code true} to allow flush operations out of a transaction, {@code false} to disallow.
	 * <p/>
	 * The default behavior is {@code false}
	 *
	 * @since 5.2
	 */
	String ALLOW_UPDATE_OUTSIDE_TRANSACTION = "hibernate.allow_update_outside_transaction";

	/**
	 * Setting which indicates whether or not the new JOINS over collection tables should be rewritten to subqueries.
	 * <p/>
	 * Default is {@code true}.  Existing applications may want to disable this (set it {@code false}) for
	 * upgrade compatibility.
	 *
	 * @since 5.2
	 */
	String COLLECTION_JOIN_SUBQUERY = "hibernate.collection_join_subquery";

	/**
	 * Setting that allows to call {@link javax.persistence.EntityManager#refresh(Object)}
	 * or {@link org.hibernate.Session#refresh(Object)} on a detached entity instance when the {@link org.hibernate.Session} is obtained from
	 * a JPA {@link javax.persistence.EntityManager}).
	 * <p>
	 * <p/>
	 * Values are: {@code true} permits the refresh, {@code false} does not permit the detached instance refresh and an {@link IllegalArgumentException} is thrown.
	 * <p/>
	 * The default value is {@code false} when the Session is bootstrapped via JPA {@link javax.persistence.EntityManagerFactory}, otherwise is {@code true}
	 *
	 * @since 5.2
	 */
	String ALLOW_REFRESH_DETACHED_ENTITY = "hibernate.allow_refresh_detached_entity";

	/**
	 * Setting that specifies how Hibernate will respond when multiple representations of the same persistent entity ("entity copy") is detected while merging.
	 * <p/>
	 * The possible values are:
	 *
	 * <ul>
	 *     <li>disallow (the default): throws {@link java.lang.IllegalStateException} if an entity copy is detected</li>
	 *     <li>allow: performs the merge operation on each entity copy that is detected</li>
	 *     <li>log: (provided for testing only) performs the merge operation on each entity copy that is detected and logs information about the entity copies.
	 *     This setting requires DEBUG logging be enabled for {@link org.hibernate.event.internal.EntityCopyAllowedLoggedObserver}.
	 *     </li>
	 * </ul>
	 *
	 * <p/>
	 * In addition, the application may customize the behavior by providing an implementation of {@link org.hibernate.event.spi.EntityCopyObserver} and setting {@code hibernate.event.merge.entity_copy_observer} to the class name.
	 * When this property is set to {@code allow} or {@code log}, Hibernate will merge each entity copy detected while cascading the merge operation.
	 * In the process of merging each entity copy, Hibernate will cascade the merge operation from each entity copy to its associations with {@code CascadeType.MERGE} or {@code CascadeType.ALL}.
	 * The entity state resulting from merging an entity copy will be overwritten when another entity copy is merged.
	 *
	 * @since 4.3
	 */
	String MERGE_ENTITY_COPY_OBSERVER = "hibernate.event.merge.entity_copy_observer";

	/**
	 * Setting which indicates whether or not to use {@link org.hibernate.dialect.pagination.LimitHandler}
	 * implementations that sacrifices performance optimizations to allow legacy 4.x limit behavior.
	 * </p>
	 * Legacy 4.x behavior favored performing pagination in-memory by avoiding the use of the offset
	 * value, which is overall poor performance.  In 5.x, the limit handler behavior favors performance
	 * thus if the dialect doesn't support offsets, an exception is thrown instead.
	 * </p>
	 * Default is {@code false}.
	 *
	 * @since 5.2.5
	 */
	String USE_LEGACY_LIMIT_HANDLERS = "hibernate.legacy_limit_handler";


	/**
	 * Setting which indicates if {@link org.hibernate.query.Query#setParameter} should not perform parameters validation
	 *
	 * This setting is applied only when the Session is bootstrapped via JPA {@link javax.persistence.EntityManagerFactory}
	 *
	 * </p>
	 * Values are: {@code true} indicates the validation should be performed, {@code false} otherwise
	 * <p>
	 * The default value is {@code true} when the Session is bootstrapped via JPA {@link javax.persistence.EntityManagerFactory},
	 * otherwise is {@code false}
	 *
	 */
	String VALIDATE_QUERY_PARAMETERS = "hibernate.query.validate_parameters";

	/**
	 * By default, Criteria queries uses bind parameters for any literal that is not a numeric value.
	 *
	 * However, to increase the likelihood of JDBC statement caching,
	 * you might want to use bind parameters for numeric values too.
	 * The {@link org.hibernate.query.criteria.LiteralHandlingMode#BIND} mode will use bind variables for any literal value.
	 *
	 * The {@link org.hibernate.query.criteria.LiteralHandlingMode#INLINE} mode will inline literal values as-is.
	 * To prevent SQL injection, never use {@link org.hibernate.query.criteria.LiteralHandlingMode#INLINE} with String variables.
	 * Always use constants with the {@link org.hibernate.query.criteria.LiteralHandlingMode#INLINE} mode.
	 * </p>
	 * Valid options are defined by the {@link org.hibernate.query.criteria.LiteralHandlingMode} enum.
	 * </p>
	 * The default value is {@link org.hibernate.query.criteria.LiteralHandlingMode#AUTO}
	 *
	 * @since 5.2.12
	 * @see org.hibernate.query.criteria.LiteralHandlingMode
	 */
	String CRITERIA_LITERAL_HANDLING_MODE = "hibernate.criteria.literal_handling_mode";

	/**
	 * True/false setting indicating whether the value specified for {@link GeneratedValue#generator()}
	 * should be used as the sequence/table name when no matching {@link javax.persistence.SequenceGenerator}
	 * or {@link javax.persistence.TableGenerator} is found.
	 *
	 * The default value is `true` meaning that {@link GeneratedValue#generator()} will be used as the
	 * sequence/table name by default.  Users migrating from earlier versions using the legacy
	 * `hibernate_sequence` name should disable this setting.
	 */
	String PREFER_GENERATOR_NAME_AS_DEFAULT_SEQUENCE_NAME = "hibernate.model.generator_name_as_sequence_name";

	/**
	 * Should Hibernate's {@link Transaction} behave as
	 * defined by the spec for JPA's {@link javax.persistence.EntityTransaction}
	 * since it extends the JPA one.
	 *
	 * @see JpaCompliance#isJpaTransactionComplianceEnabled()
	 * @since 5.3
	 */
	String JPA_TRANSACTION_COMPLIANCE = "hibernate.jpa.compliance.transaction";

	/**
	 * Controls whether Hibernate's handling of {@link javax.persistence.Query}
	 * (JPQL, Criteria and native-query) should strictly follow the JPA spec.
	 * This includes both in terms of parsing or translating a query as well
	 * as calls to the {@link javax.persistence.Query} methods throwing spec
	 * defined exceptions where as Hibernate might not.
	 *
	 * Deviations result in an exception if enabled
	 *
	 * @see JpaCompliance#isJpaQueryComplianceEnabled()
	 * @since 5.3
	 */
	String JPA_QUERY_COMPLIANCE = "hibernate.jpa.compliance.query";

	/**
	 * Controls whether Hibernate should recognize what it considers a "bag"
	 * ({@link org.hibernate.collection.internal.PersistentBag}) as a List
	 * ({@link org.hibernate.collection.internal.PersistentList}) or as a bag.
	 *
	 * If enabled, we will recognize it as a List where {@link javax.persistence.OrderColumn}
	 * is just missing (and its defaults will apply).
	 *
	 * @see JpaCompliance#isJpaListComplianceEnabled()
	 * @since 5.3
	 */
	String JPA_LIST_COMPLIANCE	= "hibernate.jpa.compliance.list";

	/**
	 * JPA defines specific exceptions on specific methods when called on
	 * {@link javax.persistence.EntityManager} and {@link javax.persistence.EntityManagerFactory}
	 * when those objects have been closed.  This setting controls
	 * whether the spec defined behavior or Hibernate's behavior will be used.
	 *
	 * If enabled Hibernate will operate in the JPA specified way throwing
	 * exceptions when the spec says it should.
	 *
	 * @see JpaCompliance#isJpaClosedComplianceEnabled()
	 * @since 5.3
	 */
	String JPA_CLOSED_COMPLIANCE = "hibernate.jpa.compliance.closed";

	/**
	 * The JPA spec says that a {@link javax.persistence.EntityNotFoundException}
	 * should be thrown when accessing an entity Proxy which does not have an associated
	 * table row in the database.
	 *
	 * Traditionally, Hibernate does not initialize an entity Proxy when accessing its
	 * identifier since we already know the identifier value, hence we can save a database roundtrip.
	 *
	 * If enabled Hibernate will initialize the entity Proxy even when accessing its identifier.
	 *
	 * @see JpaCompliance#isJpaProxyComplianceEnabled()
	 * @since 5.2.13
	 */
	String JPA_PROXY_COMPLIANCE = "hibernate.jpa.compliance.proxy";

	/**
	 * @see JpaCompliance#isJpaCacheComplianceEnabled()
	 * @since 5.3
	 */
	String JPA_CACHING_COMPLIANCE = "hibernate.jpa.compliance.caching";

	/**
	 * Determine if the scope of {@link javax.persistence.TableGenerator#name()} and {@link javax.persistence.SequenceGenerator#name()} should be
	 * considered globally or locally defined.
	 *
	 * If enabled, the names will be considered globally scoped so defining two different generators with the same name
	 * will cause a name collision and an exception will be thrown during the bootstrap phase.
	 *
	 * @see JpaCompliance#isGlobalGeneratorScopeEnabled()
	 * @since 5.2.17
	 */
	String JPA_ID_GENERATOR_GLOBAL_SCOPE_COMPLIANCE = "hibernate.jpa.compliance.global_id_generators";

	/**
	 * True/False setting indicating if the value stored in the table used by the {@link javax.persistence.TableGenerator}
	 * is the last value generated or the next value to be used.
	 *
	 * The default value is true.
	 *
	 * @since 5.3
	 */
	String TABLE_GENERATOR_STORE_LAST_USED = "hibernate.id.generator.stored_last_used";

	/**
	 * Raises an exception when in-memory pagination over collection fetch is about to be performed.
	 * Disabled by default. Set to true to enable.
	 *
	 * @since 5.2.13
	 */
	String FAIL_ON_PAGINATION_OVER_COLLECTION_FETCH = "hibernate.query.fail_on_pagination_over_collection_fetch";

	/**
	 * This setting defines how {@link org.hibernate.annotations.Immutable} entities are handled when executing a
	 * bulk update {@link javax.persistence.Query}.
	 *
	 * By default, the ({@link ImmutableEntityUpdateQueryHandlingMode#WARNING}) mode is used, meaning that
	 * a warning log message is issued when an {@link org.hibernate.annotations.Immutable} entity
	 * is to be updated via a bulk update statement.
	 *
	 * If the ({@link ImmutableEntityUpdateQueryHandlingMode#EXCEPTION}) mode is used, then a
	 * {@link HibernateException} is thrown instead.
	 * </p>
	 * Valid options are defined by the {@link ImmutableEntityUpdateQueryHandlingMode} enum.
	 * </p>
	 * The default value is {@link ImmutableEntityUpdateQueryHandlingMode#WARNING}
	 *
	 * @since 5.2.17
	 * @see org.hibernate.query.ImmutableEntityUpdateQueryHandlingMode
	 */
	String IMMUTABLE_ENTITY_UPDATE_QUERY_HANDLING_MODE = "hibernate.query.immutable_entity_update_query_handling_mode";

	/**
	 * By default, the IN clause expands to include all bind parameter values.
	 * </p>
	 * However, for database systems supporting execution plan caching,
	 * there's a better chance of hitting the cache if the number of possible IN clause parameters lowers.
	 * </p>
	 * For this reason, we can expand the bind parameters to power-of-two: 4, 8, 16, 32, 64.
	 * This way, an IN clause with 5, 6, or 7 bind parameters will use the 8 IN clause,
	 * therefore reusing its execution plan.
	 * </p>
	 * If you want to activate this feature, you need to set this property to {@code true}.
	 * </p>
	 * The default value is {@code false}.
	 *
	 * @since 5.2.17
	 */
	String IN_CLAUSE_PARAMETER_PADDING = "hibernate.query.in_clause_parameter_padding";

	/**
	 * This setting controls the number of {@link org.hibernate.stat.QueryStatistics} entries
	 * that will be stored by the Hibernate {@link org.hibernate.stat.Statistics} object.
	 * </p>
	 * The default value is given by the {@link org.hibernate.stat.Statistics#DEFAULT_QUERY_STATISTICS_MAX_SIZE} constant value.
	 *
	 * @since 5.4
	 */
	String QUERY_STATISTICS_MAX_SIZE = "hibernate.statistics.query_max_size";

	/**
	 * This setting defines the {@link org.hibernate.id.SequenceMismatchStrategy} used when
	 * Hibernate detects a mismatch between a sequence configuration in an entity mapping
	 * and its database sequence object counterpart.
	 * </p>
	 * Possible values are {@link org.hibernate.id.SequenceMismatchStrategy#EXCEPTION},
	 * {@link org.hibernate.id.SequenceMismatchStrategy#LOG}, and
	 * {@link org.hibernate.id.SequenceMismatchStrategy#FIX}.
	 * </p>
	 * The default value is given by the {@link org.hibernate.id.SequenceMismatchStrategy#EXCEPTION},
	 * meaning that an Exception is thrown when detecting such a conflict.
	 *
	 * @since 5.4
	 */
	String SEQUENCE_INCREMENT_SIZE_MISMATCH_STRATEGY = "hibernate.id.sequence.increment_size_mismatch_strategy";

	/**
	 * <p>
	 * When you use {@link javax.persistence.InheritanceType#JOINED} strategy for inheritance mapping and query
	 * a value from an entity, all superclass tables are joined in the query regardless you need them. With
	 * this setting set to true only superclass tables which are really needed are joined.
	 * </p>
	 * <p>
	 * The default value is true.
	 * </p>
	 *
	 * @since 5.4
	 */
	String OMIT_JOIN_OF_SUPERCLASS_TABLES = "hibernate.query.omit_join_of_superclass_tables";

}
