/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.cfg;

import java.util.Calendar;
import java.util.function.Supplier;

import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.Incubating;
import org.hibernate.Interceptor;
import org.hibernate.Remove;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.cache.internal.NoCachingRegionFactory;
import org.hibernate.cache.spi.TimestampsCacheFactory;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.batch.spi.BatchBuilder;
import org.hibernate.id.enhanced.ImplicitDatabaseObjectNamingStrategy;
import org.hibernate.jpa.LegacySpecHints;
import org.hibernate.jpa.SpecHints;
import org.hibernate.query.spi.QueryPlan;
import org.hibernate.query.sqm.NullPrecedence;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.PersistentTableStrategy;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;
import org.hibernate.type.WrapperArrayHandling;

import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;

/**
 * Enumerates the configuration properties supported by Hibernate, including
 * properties defined by the JPA specification.
 * <p>
 * The settings defined here may be specified at configuration time:
 * <ul>
 *     <li>in a configuration file, for example, in {@code persistence.xml} or
 *         {@code hibernate.cfg.xml},
 *     <li>via {@link Configuration#setProperty(String, String)}, or
 *     <li>via {@link org.hibernate.boot.registry.StandardServiceRegistryBuilder#applySetting(String, Object)}.
 * </ul>
 * <p>
 * Note that Hibernate does not distinguish between JPA-defined configuration
 * properties and "native" configuration properties. Any property listed here
 * may be used to configure Hibernate no matter what configuration mechanism
 * or bootstrap API is used.
 *
 * @author Steve Ebersole
 */
public interface AvailableSettings {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Jakarta Persistence defined settings
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Specifies a class implementing {@link jakarta.persistence.spi.PersistenceProvider}.
	 * Naturally, this should always be {@link org.hibernate.jpa.HibernatePersistenceProvider},
	 * which is the best damn persistence provider ever. There's no need to explicitly specify
	 * this setting when there are no inferior persistence providers floating about.
	 * <p>
	 * See JPA 2 sections 9.4.3 and 8.2.1.4
	 */
	String JAKARTA_PERSISTENCE_PROVIDER = "jakarta.persistence.provider";

	/**
	 * Specifies the {@linkplain jakarta.persistence.spi.PersistenceUnitTransactionType
	 * type of transactions} supported by the entity managers. The default depends on
	 * whether the program is considered to be executing in a Java SE or EE environment:
	 * <ul>
	 *     <li>For Java SE, the default is
	 *     {@link jakarta.persistence.spi.PersistenceUnitTransactionType#RESOURCE_LOCAL
	 *     RESOURCE_LOCAL}.
	 *     <li>For Java EE, the default is
	 *     {@link jakarta.persistence.spi.PersistenceUnitTransactionType#JTA JTA}.
	 * </ul>
	 * <p>
	 * See JPA 2 sections 9.4.3 and 8.2.1.2
	 */
	String JAKARTA_TRANSACTION_TYPE = "jakarta.persistence.transactionType";

	/**
	 * Specifies the JNDI name of a JTA {@link javax.sql.DataSource}.
	 * <p>
	 * See JPA 2 sections 9.4.3 and 8.2.1.5
	 */
	String JAKARTA_JTA_DATASOURCE = "jakarta.persistence.jtaDataSource";

	/**
	 * Specifies the JNDI name of a non-JTA {@link javax.sql.DataSource}.
	 * <p>
	 * See JPA 2 sections 9.4.3 and 8.2.1.5
	 */
	String JAKARTA_NON_JTA_DATASOURCE = "jakarta.persistence.nonJtaDataSource";

	/**
	 * Specifies the name of a JDBC driver to use to connect to the database.
	 * <p>
	 * Used in conjunction with {@link #JPA_JDBC_URL}, {@link #JPA_JDBC_USER}
	 * and {@link #JPA_JDBC_PASSWORD} to specify how to connect to the database.
	 * <p>
	 * When connections are obtained from a {@link javax.sql.DataSource}, use
	 * either {@link #JPA_JTA_DATASOURCE} or {@link #JPA_NON_JTA_DATASOURCE}
	 * instead.
	 * <p>
	 * See section 8.2.1.9
	 */
	String JAKARTA_JDBC_DRIVER = "jakarta.persistence.jdbc.driver";

	/**
	 * Specifies the JDBC connection URL to use to connect to the database.
	 * <p>
	 * Used in conjunction with {@link #JPA_JDBC_DRIVER}, {@link #JPA_JDBC_USER}
	 * and {@link #JPA_JDBC_PASSWORD} to specify how to connect to the database.
	 * <p>
	 * When connections are obtained from a {@link javax.sql.DataSource}, use
	 * either {@link #JPA_JTA_DATASOURCE} or {@link #JPA_NON_JTA_DATASOURCE}
	 * instead.
	 * <p>
	 * See section 8.2.1.9
	 */
	String JAKARTA_JDBC_URL = "jakarta.persistence.jdbc.url";

	/**
	 * Specifies the database user to use when connecting via JDBC.
	 * <p>
	 * Used in conjunction with {@link #JPA_JDBC_DRIVER}, {@link #JPA_JDBC_URL}
	 * and {@link #JPA_JDBC_PASSWORD} to specify how to connect to the database.
	 * <p>
	 * See section 8.2.1.9
	 */
	String JAKARTA_JDBC_USER = "jakarta.persistence.jdbc.user";

	/**
	 * Specifies the password to use when connecting via JDBC.
	 * <p>
	 * Used in conjunction with {@link #JPA_JDBC_DRIVER}, {@link #JPA_JDBC_URL}
	 * and {@link #JPA_JDBC_USER} to specify how to connect to the database.
	 * <p>
	 * See JPA 2 section 8.2.1.9
	 */
	String JAKARTA_JDBC_PASSWORD = "jakarta.persistence.jdbc.password";

	/**
	 * When enabled, specifies that the second-level cache (which JPA calls the
	 * "shared" cache) may be used, as per the rules defined in JPA 2 section 3.1.7.
	 * <p>
	 * See JPA 2 sections 9.4.3 and 8.2.1.7
	 *
	 * @see jakarta.persistence.SharedCacheMode
	 */
	String JAKARTA_SHARED_CACHE_MODE = "jakarta.persistence.sharedCache.mode";

	/**
	 * Set a default value for {@link SpecHints#HINT_SPEC_CACHE_RETRIEVE_MODE},
	 * used when the hint is not explicitly specified.
	 * <p>
	 * It does not usually make sense to change the default from
	 * {@link jakarta.persistence.CacheRetrieveMode#USE}.
	 *
	 * @see SpecHints#HINT_SPEC_CACHE_RETRIEVE_MODE
	 */
	String JAKARTA_SHARED_CACHE_RETRIEVE_MODE = SpecHints.HINT_SPEC_CACHE_RETRIEVE_MODE;

	/**
	 * Set a default value for {@link SpecHints#HINT_SPEC_CACHE_STORE_MODE},
	 * used when the hint is not explicitly specified.
	 * <p>
	 * It does not usually make sense to change the default from
	 * {@link jakarta.persistence.CacheStoreMode#USE}.
	 *
	 * @see SpecHints#HINT_SPEC_CACHE_RETRIEVE_MODE
	 */
	String JAKARTA_SHARED_CACHE_STORE_MODE = SpecHints.HINT_SPEC_CACHE_STORE_MODE;

	/**
	 * Indicates which {@linkplain jakarta.persistence.ValidationMode form of automatic
	 * validation} is in effect as per the rules defined in JPA 2 section 3.6.1.1.
	 * <p>
	 * See JPA 2 sections 9.4.3 and 8.2.1.8
	 *
	 * @see jakarta.persistence.ValidationMode
	 */
	String JAKARTA_VALIDATION_MODE = "jakarta.persistence.validation.mode";

	/**
	 * Used to pass along any discovered {@link jakarta.validation.ValidatorFactory}.
	 * 
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyValidatorFactory(Object) 
	 */
	String JAKARTA_VALIDATION_FACTORY = "jakarta.persistence.validation.factory";

	/**
	 * Used to coordinate with bean validators.
	 * <p>
	 * See JPA 2 section 8.2.1.9
	 */
	@SuppressWarnings("unused")
	String JAKARTA_PERSIST_VALIDATION_GROUP = "jakarta.persistence.validation.group.pre-persist";

	/**
	 * Used to coordinate with bean validators.
	 * <p>
	 * See JPA 2 section 8.2.1.9
	 */
	@SuppressWarnings("unused")
	String JAKARTA_UPDATE_VALIDATION_GROUP = "jakarta.persistence.validation.group.pre-update";

	/**
	 * Used to coordinate with bean validators.
	 * <p>
	 * See JPA 2 section 8.2.1.9
	 */
	@SuppressWarnings("unused")
	String JAKARTA_REMOVE_VALIDATION_GROUP = "jakarta.persistence.validation.group.pre-remove";

	/**
	 * Set a default value for the hint {@link SpecHints#HINT_SPEC_LOCK_SCOPE},
	 * used when the hint is not explicitly specified.
	 * <p>
	 * See JPA 2 sections 8.2.1.9 and 3.4.4.3
	 *
	 * @see SpecHints#HINT_SPEC_LOCK_SCOPE
	 */
	String JAKARTA_LOCK_SCOPE = SpecHints.HINT_SPEC_LOCK_SCOPE;

	/**
	 * Set a default value for the hint {@link SpecHints#HINT_SPEC_LOCK_TIMEOUT},
	 * used when the hint is not explicitly specified.
	 * <p>
	 * See JPA 2 sections 8.2.1.9 and 3.4.4.3
	 *
	 * @see SpecHints#HINT_SPEC_LOCK_TIMEOUT
	 */
	String JAKARTA_LOCK_TIMEOUT = SpecHints.HINT_SPEC_LOCK_TIMEOUT;

	/**
	 * Used to pass a CDI {@link jakarta.enterprise.inject.spi.BeanManager} to
	 * Hibernate.
	 * <p>
	 * According to the JPA specification, the {@code BeanManager} should be
	 * passed at boot time and be ready for immediate use at that time. But
	 * not all environments can do this (WildFly, for example). To accommodate
	 * such environments, Hibernate provides two options: <ol>
	 *     <li> A proprietary CDI extension SPI (which has been proposed to the CDI
	 *          spec group as a standard option) which can be used to provide delayed
	 *          {@code BeanManager} access: to use this solution, the reference passed
	 *          as the {@code BeanManager} during bootstrap should be typed as
	 *          {@link org.hibernate.resource.beans.container.spi.ExtendedBeanManager}.
	 *     <li> Delayed access to the {@code BeanManager} reference: here, Hibernate
	 *          will not access the reference passed as the {@code BeanManager} during
	 *          bootstrap until it is first needed. Note, however, that this has the
	 *          effect of delaying the detection of any deployment problems until after
	 *          bootstrapping.
	 * </ol>
	 *
	 * This setting is used to configure access to the {@code BeanManager},
	 * either directly, or via
	 * {@link org.hibernate.resource.beans.container.spi.ExtendedBeanManager}.
	 * 
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyBeanManager(Object) 
	 */
	String JAKARTA_CDI_BEAN_MANAGER = "jakarta.persistence.bean.manager";


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// BootstrapServiceRegistry level settings
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Specifies a {@link java.util.Collection collection} of the {@link ClassLoader}
	 * instances Hibernate should use for classloading and resource loading.
	 *
	 * @since 5.0
	 */
	String CLASSLOADERS = "hibernate.classLoaders";

	/**
	 * Specifies how the {@linkplain Thread#getContextClassLoader() thread context}
	 * {@linkplain ClassLoader class loader} must be used for class lookup.
	 *
	 * @see org.hibernate.boot.registry.classloading.internal.TcclLookupPrecedence
	 */
	String TC_CLASSLOADER = "hibernate.classLoader.tccl_lookup_precedence";

	/**
	 * Setting that indicates whether to build the JPA types, either:<ul>
	 *     <li>
	 *         <b>enabled</b> - Do the build
	 *     </li>
	 *     <li>
	 *         <b>disabled</b> - Do not do the build
	 *     </li>
	 *     <li>
	 *         <b>ignoreUnsupported</b> - Do the build, but ignore any non-JPA
	 *         features that would otherwise result in a failure.
	 *     </li>
	 * </ul>
	 */
	String JPA_METAMODEL_POPULATION = "hibernate.jpa.metamodel.population";

	/**
	 * Setting that controls whether we seek out JPA "static metamodel" classes
	 * and populate them, either:<ul>
	 *     <li>
	 *         <b>enabled</b> - Do the population
	 *     </li>
	 *     <li>
	 *         <b>disabled</b> - Do not do the population
	 *     </li>
	 *     <li>
	 *         <b>skipUnsupported</b> - Do the population, but ignore any non-JPA
	 *         features that would otherwise result in the population failing.
	 *     </li>
	 * </ul>
	 */
	String STATIC_METAMODEL_POPULATION = "hibernate.jpa.static_metamodel.population";


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// StandardServiceRegistry level settings
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Specifies a {@link org.hibernate.engine.jdbc.connections.spi.ConnectionProvider}
	 * to use for obtaining JDBC connections, either:
	 * <ul>
	 *     <li>an instance of {@code ConnectionProvider},
	 *     <li>a {@link Class} representing a class that implements
	 *         {@code ConnectionProvider}, or
	 *     <li>the name of a class that implements {@code ConnectionProvider}.
	 * </ul>
	 * <p>
	 * The term {@code "class"} appears in the setting name due to legacy reasons;
	 * however it can accept instances.
	 */
	String CONNECTION_PROVIDER = "hibernate.connection.provider_class";

	/**
	 * Specifies the {@linkplain java.sql.Driver JDBC driver} class.
	 *
	 * @see java.sql.Driver
	 * @see #JAKARTA_JDBC_DRIVER
	 */
	String DRIVER = "hibernate.connection.driver_class";

	/**
	 * Specifies the JDBC connection URL.
	 *
	 * @see #JAKARTA_JDBC_URL
	 */
	String URL = "hibernate.connection.url";

	/**
	 * Specifies the database user to use when connecting via JDBC.
	 * <p>
	 * Depending on the configured
	 * {@link org.hibernate.engine.jdbc.connections.spi.ConnectionProvider}, the
	 * specified username might be used to:
	 * <ul>
	 *     <li>create a JDBC connection using
	 *         {@link java.sql.DriverManager#getConnection(String,java.util.Properties)}
	 *         or {@link java.sql.Driver#connect(String,java.util.Properties)}, or
	 *     <li>obtain a JDBC connection from a datasource, using
	 *         {@link javax.sql.DataSource#getConnection(String, String)}.
	 * </ul>
	 *
	 * @see #PASS
	 * @see #JAKARTA_JDBC_PASSWORD
	 */
	String USER = "hibernate.connection.username";

	/**
	 * Specifies password to use when connecting via JDBC.
	 *
	 * @see #USER
	 * @see #JAKARTA_JDBC_USER
	 */
	String PASS = "hibernate.connection.password";

	/**
	 * Specified the JDBC transaction isolation level.
	 */
	String ISOLATION = "hibernate.connection.isolation";

	/**
	 * Controls the autocommit mode of JDBC connections obtained from any
	 * {@link org.hibernate.engine.jdbc.connections.spi.ConnectionProvider} implementation
	 * which respects this setting, which the built-in implementations do, except for
	 * {@link org.hibernate.engine.jdbc.connections.internal.DatasourceConnectionProviderImpl}.
	 */
	String AUTOCOMMIT = "hibernate.connection.autocommit";

	/**
	 * Specifies the maximum number of inactive connections for the built-in
	 * {@linkplain org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl
	 * connection pool}.
	 */
	String POOL_SIZE = "hibernate.connection.pool_size";

	/**
	 * Specifies a {@link javax.sql.DataSource}, either:
	 * <ul>
	 *     <li>an instance of {@link javax.sql.DataSource}, or
	 *     <li>a JNDI name under which to obtain the {@link javax.sql.DataSource}.
	 * </ul>
	 * <p>
	 * For JNDI names, see also {@link #JNDI_CLASS}, {@link #JNDI_URL}, {@link #JNDI_PREFIX}, etc.
	 *
	 * @see javax.sql.DataSource
	 * @see #JAKARTA_JTA_DATASOURCE
	 * @see #JAKARTA_NON_JTA_DATASOURCE
	 */
	String DATASOURCE = "hibernate.connection.datasource";

	/**
	 * Allows a user to tell Hibernate that the connections we obtain from the configured
	 * {@link org.hibernate.engine.jdbc.connections.spi.ConnectionProvider} will already
	 * have autocommit disabled when we acquire them from the provider. When we obtain
	 * connections with autocommit already disabled, we may circumvent some operations in
	 * the interest of performance.
	 * <p>
	 * By default, Hibernate calls {@link java.sql.Connection#setAutoCommit(boolean)} on
	 * newly-obtained connections.
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyConnectionProviderDisablesAutoCommit(boolean)
	 *
	 * @since 5.2.10
	 */
	String CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT= "hibernate.connection.provider_disables_autocommit";

	/**
	 * A prefix for properties specifying arbitrary JDBC connection properties. These
	 * properties are simply passed along to the provider when creating a connection.
	 */
	String CONNECTION_PREFIX = "hibernate.connection";

	/**
	 * Specifies the JNDI {@link javax.naming.InitialContext} implementation class.
	 *
	 * @see javax.naming.Context#INITIAL_CONTEXT_FACTORY
	 */
	String JNDI_CLASS = "hibernate.jndi.class";

	/**
	 * Specifies the JNDI provider/connection URL.
	 *
	 * @see javax.naming.Context#PROVIDER_URL
	 */
	String JNDI_URL = "hibernate.jndi.url";

	/**
	 * A prefix for properties specifying arbitrary JNDI {@link javax.naming.InitialContext}
	 * properties. These properties are simply passed along to the constructor
	 * {@link javax.naming.InitialContext#InitialContext(java.util.Hashtable)}.
	 */
	String JNDI_PREFIX = "hibernate.jndi";

	/**
	 * Specifies the Hibernate {@linkplain org.hibernate.dialect.Dialect SQL dialect}, either
	 * <ul>
	 *     <li>an instance of {@link org.hibernate.dialect.Dialect},
	 *     <li>a {@link Class} representing a class that extends {@code Dialect}, or
	 *     <li>the name of a class that extends {@code Dialect}.
	 * </ul>
	 * <p>
	 * By default, Hibernate will attempt to automatically determine the dialect from the
	 * {@linkplain #URL JDBC URL} and JDBC metadata, so this setting is not usually necessary.
	 *
	 * @apiNote As of Hibernate 6, this property should not be explicitly specified,
	 *          except when using a custom user-written implementation of {@code Dialect}.
	 *          Instead, applications should allow Hibernate to select the {@code Dialect}
	 *          automatically.
	 *
	 * @see org.hibernate.dialect.Dialect
	 */
	String DIALECT = "hibernate.dialect";

	/**
	 * Specifies additional {@link org.hibernate.engine.jdbc.dialect.spi.DialectResolver}
	 * implementations to register with the standard
	 * {@link org.hibernate.engine.jdbc.dialect.spi.DialectFactory}.
	 */
	String DIALECT_RESOLVERS = "hibernate.dialect_resolvers";

	/**
	 * Specifies the name of the database provider in cases where a connection to the
	 * database is not available (usually for generating scripts). In such cases, a value
	 * for this setting <em>must</em> be specified.
	 * <p>
	 * The value of this setting is expected to match the value returned by
	 * {@link java.sql.DatabaseMetaData#getDatabaseProductName()} for the target database.
	 * <p>
	 * Additionally, specifying {@value #DIALECT_DB_MAJOR_VERSION}, and perhaps even
	 * {@value #DIALECT_DB_MINOR_VERSION}, may be required for high quality DDL generation.
	 *
	 * @see #DIALECT_DB_VERSION
	 * @see #DIALECT_DB_MAJOR_VERSION
	 * @see #DIALECT_DB_MINOR_VERSION
	 *
	 * @deprecated Use {@link #JAKARTA_HBM2DDL_DB_NAME} instead
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String DIALECT_DB_NAME = "javax.persistence.database-product-name";

	/**
	 * Specifies the name of the database provider in cases where a connection to the
	 * database is not available (usually for generating scripts). This value is used
	 * to help more precisely determine how to perform schema generation tasks for the
	 * underlying database in cases where {@value #DIALECT_DB_NAME} does not provide
	 * enough distinction.
	 * <p>
	 * The value of this setting is expected to match the value returned by
	 * {@link java.sql.DatabaseMetaData#getDatabaseProductVersion()} for the target
	 * database.
	 *
	 * @see #DIALECT_DB_NAME
	 *
	 * @deprecated Use {@link #JAKARTA_HBM2DDL_DB_VERSION} instead
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String DIALECT_DB_VERSION = "javax.persistence.database-product-version";

	/**
	 * Specifies the major version of the underlying database, as would be returned by
	 * {@link java.sql.DatabaseMetaData#getDatabaseMajorVersion} for the target database.
	 * This value is used to help more precisely determine how to perform schema generation
	 * tasks for the database in cases where {@value #DIALECT_DB_NAME} does not provide
	 * enough distinction.
	 *
	 * @see #DIALECT_DB_NAME
	 * @see #DIALECT_DB_MINOR_VERSION
	 *
	 * @deprecated Use {@link #JAKARTA_HBM2DDL_DB_MAJOR_VERSION} instead
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String DIALECT_DB_MAJOR_VERSION = "javax.persistence.database-major-version";

	/**
	 * Specifies the minor version of the underlying database, as would be returned by
	 * {@link java.sql.DatabaseMetaData#getDatabaseMinorVersion} for the target database.
	 * This setting is used in {@link org.hibernate.dialect.Dialect} resolution.
	 *
	 * @see #DIALECT_DB_NAME
	 * @see #DIALECT_DB_MAJOR_VERSION
	 * @see org.hibernate.engine.jdbc.dialect.spi.DialectResolver
	 *
	 * @deprecated Use {@link #JAKARTA_HBM2DDL_DB_MINOR_VERSION} instead
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String DIALECT_DB_MINOR_VERSION = "javax.persistence.database-minor-version";

	/**
	 * Specifies the default storage engine for a relational databases that supports
	 * multiple storage engines. This property must be set either as an {@link Environment}
	 * variable or JVM System Property, since the {@link org.hibernate.dialect.Dialect} is
	 * instantiated before Hibernate property resolution.
	 *
	 * @since 5.2.9
	 */
	String STORAGE_ENGINE = "hibernate.dialect.storage_engine";

	/**
	 * Specifies the {@link org.hibernate.tool.schema.spi.SchemaManagementTool} to use for
	 * performing schema management.
	 * <p>
	 * By default, {@link org.hibernate.tool.schema.internal.HibernateSchemaManagementTool}
	 * is used.
	 *
	 * @since 5.0
	 */
	String SCHEMA_MANAGEMENT_TOOL = "hibernate.schema_management_tool";

	/**
	 * Specify the {@link org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder}
	 * implementation to use for creating instances of
	 * {@link org.hibernate.resource.transaction.spi.TransactionCoordinator}, either:
	 * <ul>
	 *     <li>an instance of {@code TransactionCoordinatorBuilder},
	 *     <li>a {@link Class} representing a class that implements {@code TransactionCoordinatorBuilder}, or
	 *     <li>the name of a class that implements {@code TransactionCoordinatorBuilder}.
	 * </ul>
	 *
	 * @since 5.0
	 */
	String TRANSACTION_COORDINATOR_STRATEGY = "hibernate.transaction.coordinator_class";

	/**
	 * Specifies the {@link org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform}
	 * implementation to use for integrating with JTA, either:
	 * <ul>
	 *     <li>an instance of {@code JtaPlatform}, or
	 *     <li>the name of a class that implements {@code JtaPlatform}.
	 * </ul>
	 *
	 * @see #JTA_PLATFORM_RESOLVER
	 *
	 * @since 4.0
	 */
	String JTA_PLATFORM = "hibernate.transaction.jta.platform";

	/**
	 * When enabled, specifies that the {@link jakarta.transaction.UserTransaction} should
	 * be used in preference to the {@link jakarta.transaction.TransactionManager} for JTA
	 * transaction management.
	 * <p>
	 * By default, the {@code TransactionManager} is preferred.
	 *
	 * @see org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform#retrieveUserTransaction
	 * @see org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform#retrieveTransactionManager
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyPreferUserTransactions(boolean)
	 *
	 * @since 5.0
	 */
	String PREFER_USER_TRANSACTION = "hibernate.jta.prefer_user_transaction";

	/**
	 * Specifies a {@link org.hibernate.engine.transaction.jta.platform.spi.JtaPlatformResolver}
	 * implementation that should be used to obtain an instance of
	 * {@link org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform}.
	 *
	 * @see #JTA_PLATFORM
	 *
	 * @since 4.3
	 */
	String JTA_PLATFORM_RESOLVER = "hibernate.transaction.jta.platform_resolver";

	/**
	 * When enabled, indicates that it is safe to cache {@link jakarta.transaction.TransactionManager}
	 * references.
	 *
	 * @since 4.0
	 */
	String JTA_CACHE_TM = "hibernate.jta.cacheTransactionManager";

	/**
	 * When enabled, indicates that it is safe to cache {@link jakarta.transaction.UserTransaction}
	 * references.
	 *
	 * @since 4.0
	 */
	String JTA_CACHE_UT = "hibernate.jta.cacheUserTransaction";


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// MetadataBuilder level settings
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * A default database catalog name to use for unqualified table names
	 *
	 * @see org.hibernate.boot.MetadataBuilder#applyImplicitCatalogName
	 */
	String DEFAULT_CATALOG = "hibernate.default_catalog";

	/**
	 * A default database schema (owner) name to use for unqualified table names
	 *
	 * @see org.hibernate.boot.MetadataBuilder#applyImplicitSchemaName
	 */
	String DEFAULT_SCHEMA = "hibernate.default_schema";

	/**
	 * Specifies the {@link org.hibernate.annotations.CacheConcurrencyStrategy} to use by
	 * default when an entity is marked {@link jakarta.persistence.Cacheable @Cacheable},
	 * but no concurrency strategy is explicitly specified via the
	 * {@link org.hibernate.annotations.Cache} annotation.
	 * <p>
	 * An explicit strategy may be specified using
	 * {@link org.hibernate.annotations.Cache#usage @Cache(usage=...)}.
	 *
	 * @see org.hibernate.boot.MetadataBuilder#applyAccessType(org.hibernate.cache.spi.access.AccessType)
	 */
	String DEFAULT_CACHE_CONCURRENCY_STRATEGY = "hibernate.cache.default_cache_concurrency_strategy";

	/**
	 * @see org.hibernate.boot.MetadataBuilder#enableImplicitForcingOfDiscriminatorsInSelect
	 */
	String FORCE_DISCRIMINATOR_IN_SELECTS_BY_DEFAULT = "hibernate.discriminator.force_in_select";

	/**
	 * The legacy behavior of Hibernate is to not use discriminators for joined inheritance
	 * (Hibernate does not need the discriminator.). However, some JPA providers do need the
	 * discriminator for handling joined inheritance, so in the interest of portability this
	 * capability has been added to Hibernate.
	 * <p>
	 * However, we want to make sure that legacy applications continue to work as well.
	 * Which puts us in a bind in terms of how to handle "implicit" discriminator mappings.
	 * The solution is to assume that the absence of discriminator metadata means to follow
	 * the legacy behavior <em>unless</em> this setting is enabled. With this setting enabled,
	 * Hibernate will interpret the absence of discriminator metadata as an indication to use
	 * the JPA defined defaults for these absent annotations.
	 * <p>
	 * See Hibernate Jira issue HHH-6911 for additional background info.
	 * <p>
	 * This setting defaults to {@code false}, meaning that implicit discriminator columns
	 * are never inferred to exist for joined inheritance hierarchies.
	 *
	 * @see org.hibernate.boot.MetadataBuilder#enableImplicitDiscriminatorsForJoinedSubclassSupport
	 * @see #IGNORE_EXPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS
	 */
	String IMPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS = "hibernate.discriminator.implicit_for_joined";

	/**
	 * The legacy behavior of Hibernate is to not use discriminators for joined inheritance
	 * (Hibernate does not need the discriminator). However, some JPA providers do need the
	 * discriminator for handling joined inheritance, so in the interest of portability this
	 * capability has been added to Hibernate.
	 * <p>
	 * Existing applications rely (implicitly or explicitly) on Hibernate ignoring any
	 * {@link jakarta.persistence.DiscriminatorColumn} declarations on joined inheritance
	 * hierarchies. This setting allows these applications to maintain the legacy behavior
	 * of {@code @DiscriminatorColumn} annotations being ignored when paired with joined
	 * inheritance.
	 * <p>
	 * See Hibernate Jira issue HHH-6911 for additional background info.
	 * <p>
	 * This setting defaults to {@code false}, meaning that explicit discriminator columns
	 * are never ignored.
	 *
	 * @see org.hibernate.boot.MetadataBuilder#enableExplicitDiscriminatorsForJoinedSubclassSupport
	 * @see #IMPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS
	 */
	String IGNORE_EXPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS = "hibernate.discriminator.ignore_explicit_for_joined";

	/**
	 * By default, Hibernate maps character data represented by {@link String}s and
	 * {@link java.sql.Clob}s to the JDBC types {@link java.sql.Types#VARCHAR} and
	 * {@link java.sql.Types#CLOB}. This setting, when enabled, turns on the use of
	 * explicit nationalized character support for mappings involving character
	 * data, specifying that the JDBC types {@link java.sql.Types#NVARCHAR} and
	 * {@link java.sql.Types#NCLOB} should be used instead.
	 * <p>
	 * This setting is relevant for use with databases with
	 * {@linkplain org.hibernate.dialect.NationalizationSupport#EXPLICIT explicit
	 * nationalization support}, and it is not needed for databases whose native
	 * {@code varchar} and {@code clob} types support Unicode data. (If you're not
	 * sure how your database handles Unicode, check out the implementation of
	 * {@link org.hibernate.dialect.Dialect#getNationalizationSupport()} for its
	 * SQL dialect.)
	 * <p>
	 * Enabling this setting has two effects:
	 * <ol>
	 *     <li>when interacting with JDBC, Hibernate uses operations like
	 *         {@link java.sql.PreparedStatement#setNString(int, String)}
	 *         {@link java.sql.PreparedStatement#setNClob(int, java.sql.NClob)}
	 *         to pass character data, and
	 *     <li>when generating DDL, the schema export tool uses {@code nchar},
	 *         {@code nvarchar}, or {@code nclob} as the generated column
	 *         type when no column type is explicitly specified using
	 *         {@link jakarta.persistence.Column#columnDefinition()}.
	 * </ol>
	 * <p>
	 * This setting is <em>disabled</em> by default, and so Unicode character data
	 * may not be persisted correctly for databases with explicit nationalization
	 * support.
	 * <p>
	 * This is a global setting applying to all mappings associated with a given
	 * {@link org.hibernate.SessionFactory}.
	 * The {@link org.hibernate.annotations.Nationalized} annotation may be used
	 * to selectively enable nationalized character support for specific columns.
	 *
	 * @see org.hibernate.boot.MetadataBuilder#enableGlobalNationalizedCharacterDataSupport(boolean)
	 * @see org.hibernate.dialect.NationalizationSupport
	 * @see org.hibernate.annotations.Nationalized
	 */
	String USE_NATIONALIZED_CHARACTER_DATA = "hibernate.use_nationalized_character_data";

	/**
	 * Specifies an implementation of {@link org.hibernate.boot.archive.scan.spi.Scanner},
	 * either:
	 * <ul>
	 *     <li>an instance of {@code Scanner},
	 *     <li>a {@link Class} representing a class that implements {@code Scanner}
	 *     <li>the name of a class that implements {@code Scanner}.
	 * </ul>
	 *
	 * @see org.hibernate.boot.MetadataBuilder#applyScanner
	 */
	String SCANNER = "hibernate.archive.scanner";

	/**
	 * Specifies an {@link org.hibernate.boot.archive.spi.ArchiveDescriptorFactory} to use
	 * in the scanning process, either:
	 * <ul>
	 *     <li>an instance of {@code ArchiveDescriptorFactory},
	 *     <li>a {@link Class} representing a class that implements {@code ArchiveDescriptorFactory}, or
	 *     <li>the name of a class that implements {@code ArchiveDescriptorFactory}.
	 * </ul>
	 * <p>
	 * See information on {@link org.hibernate.boot.archive.scan.spi.Scanner}
	 * about expected constructor forms.
	 *
	 * @see #SCANNER
	 * @see org.hibernate.boot.archive.scan.spi.Scanner
	 * @see org.hibernate.boot.archive.scan.spi.AbstractScannerImpl
	 * @see org.hibernate.boot.MetadataBuilder#applyArchiveDescriptorFactory
	 */
	String SCANNER_ARCHIVE_INTERPRETER = "hibernate.archive.interpreter";

	/**
	 * Identifies a comma-separated list of values indicating the types of things we should
	 * auto-detect during scanning. Allowable values include:
	 * <ul>
	 *     <li>{@code "class"} specifies that {@code .class} files are discovered as managed classes
	 *     <li>{@code "hbm"} specifies that {@code hbm.xml} files are discovered as mapping files
	 * </ul>
	 *
	 * @see org.hibernate.boot.MetadataBuilder#applyScanOptions
	 */
	String SCANNER_DISCOVERY = "hibernate.archive.autodetection";

	/**
	 * Used to specify the {@link org.hibernate.boot.model.naming.ImplicitNamingStrategy}
	 * class to use. The following shortcut names are defined for this setting:
	 * <ul>
	 *     <li>{@code "default"} and {@code "jpa"} are an abbreviations for
	 *     {@link org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl}
	 *     <li>{@code "legacy-jpa"} is an abbreviation for
	 *     {@link org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl}
	 *     <li>{@code "legacy-hbm"} is an abbreviation for
	 *     {@link org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyHbmImpl}
	 *     <li>{@code "component-path"} is an abbreviation for
	 *     {@link org.hibernate.boot.model.naming.ImplicitNamingStrategyComponentPathImpl}
	 * </ul>
	 * <p>
	 * By default, the {@code ImplicitNamingStrategy} registered under the key
	 * {@code "default"} is used. If no strategy is explicitly registered under that key,
	 * {@link org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl} is used.
	 *
	 * @see org.hibernate.boot.MetadataBuilder#applyImplicitNamingStrategy
	 *
	 * @since 5.0
	 */
	String IMPLICIT_NAMING_STRATEGY = "hibernate.implicit_naming_strategy";

	/**
	 * Specifies the {@link org.hibernate.boot.model.naming.PhysicalNamingStrategy} to use.
	 * <p>
	 * By default, {@link org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl}
	 * is used, in which case physical names are taken to be identical to logical names.
	 *
	 * @see org.hibernate.boot.MetadataBuilder#applyPhysicalNamingStrategy
	 *
	 * @since 5.0
	 */
	String PHYSICAL_NAMING_STRATEGY = "hibernate.physical_naming_strategy";

	/**
	 * An implicit naming strategy for database structures (tables, sequences) related
	 * to identifier generators.
	 * <p>
	 * Resolution uses the {@link org.hibernate.boot.registry.selector.spi.StrategySelector}
	 * service and accepts any of the forms discussed on
	 * {@link StrategySelector#resolveDefaultableStrategy(Class, Object, java.util.concurrent.Callable)}.
	 * <p>
	 * The recognized short names being:<ul>
	 *     <li>{@value org.hibernate.id.enhanced.SingleNamingStrategy#STRATEGY_NAME}</li>
	 *     <li>{@value org.hibernate.id.enhanced.LegacyNamingStrategy#STRATEGY_NAME}</li>
	 *     <li>{@value org.hibernate.id.enhanced.StandardNamingStrategy#STRATEGY_NAME}</li>
	 * </ul>
	 *
	 * @see ImplicitDatabaseObjectNamingStrategy
	 */
	@Incubating
	String ID_DB_STRUCTURE_NAMING_STRATEGY = "hibernate.id.db_structure_naming_strategy";

	/**
	 * Used to specify the {@link org.hibernate.boot.model.relational.ColumnOrderingStrategy}
	 * class to use. The following shortcut names are defined for this setting:
	 * <ul>
	 *     <li>{@code "default"} is an abbreviations for
	 *     {@link org.hibernate.boot.model.relational.ColumnOrderingStrategyStandard}
	 *     <li>{@code "legacy"} is an abbreviation for
	 *     {@link org.hibernate.boot.model.relational.ColumnOrderingStrategyLegacy}
	 * </ul>
	 * <p>
	 * By default, the {@linkplain org.hibernate.boot.model.relational.ColumnOrderingStrategy} registered under the key
	 * {@code "default"} is used. If no strategy is explicitly registered under that key,
	 * {@link org.hibernate.boot.model.relational.ColumnOrderingStrategyStandard} is used.
	 *
	 * @see org.hibernate.boot.MetadataBuilder#applyColumnOrderingStrategy
	 *
	 * @since 6.2
	 */
	String COLUMN_ORDERING_STRATEGY = "hibernate.column_ordering_strategy";

	/**
	 * Specifies the order in which metadata sources should be processed, is a delimited list
	 * of values defined by {@link MetadataSourceType}.
	 * <p>
	 * The default is {@code "hbm,class"} which that {@code hbm.xml} files should be processed
	 * first, followed by annotations (combined with {@code orm.xml} mappings).
	 *
	 * @see MetadataSourceType
	 * @see org.hibernate.boot.MetadataBuilder#applySourceProcessOrdering(MetadataSourceType...)
	 *
	 * @deprecated {@code hbm.xml} mappings are no longer supported, making this attribute irrelevant
	 */
	@SuppressWarnings("DeprecatedIsStillUsed")
	@Deprecated(since = "6", forRemoval = true)
	String ARTIFACT_PROCESSING_ORDER = "hibernate.mapping.precedence";

	/**
	 * Specifies whether to automatically quote any names that are deemed SQL keywords.
	 * <p>
	 * Auto-quoting of SQL keywords is disabled by default.
	 *
	 * @since 5.0
	 */
	String KEYWORD_AUTO_QUOTING_ENABLED = "hibernate.auto_quote_keyword";

	/**
	 * When disabled, specifies that processing of XML-based mappings should be skipped.
	 * <p>
	 * This is a performance optimization appropriate when all O/R mappings are defined
	 * exclusively using annotations.
	 * <p>
	 * By default, the XML-based mappings are taken into account.
	 *
	 * @since 5.4.1
	 */
	String XML_MAPPING_ENABLED = "hibernate.xml_mapping_enabled";

	/**
	 * Specifies the {@link org.hibernate.metamodel.CollectionClassification} to use when
	 * Hibernate detects a plural attribute typed as {@link java.util.List} with no explicit
	 * list index configuration.
	 * <p>
	 * Accepts any of:
	 * <ul>
	 *     <li>an instance of {@code CollectionClassification}
	 *     <li>the (case insensitive) name of a {@code CollectionClassification} (list e.g.)
	 *     <li>a {@link Class} representing either {@link java.util.List} or {@link java.util.Collection}
	 * </ul>
	 * <p>
	 * By default, when this property is not set, an attribute of type {@code List}
	 * is taken to have the semantics of a
	 * {@linkplain org.hibernate.metamodel.CollectionClassification#BAG bag} unless
	 * it is annotated {@link jakarta.persistence.OrderColumn} or
	 * {@link org.hibernate.annotations.ListIndexBase}.
	 *
	 * @since 6.0
	 *
	 * @see org.hibernate.annotations.Bag
	 */
	String DEFAULT_LIST_SEMANTICS = "hibernate.mapping.default_list_semantics";


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SessionFactoryBuilder level settings
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Setting used to name the Hibernate {@link org.hibernate.SessionFactory}.
	 * <p>
	 * Naming the SessionFactory allows for it to be properly serialized across JVMs as
	 * long as the same name is used on each JVM.
	 * <p>
	 * If {@link #SESSION_FACTORY_NAME_IS_JNDI} is set to {@code true}, this is also the
	 * name under which the SessionFactory is bound into JNDI on startup and from which
	 * it can be obtained from JNDI.
	 *
	 * @see #SESSION_FACTORY_NAME_IS_JNDI
	 * @see org.hibernate.internal.SessionFactoryRegistry
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyName(String) 
	 */
	String SESSION_FACTORY_NAME = "hibernate.session_factory_name";

	/**
	 * Does the value defined by {@link #SESSION_FACTORY_NAME} represent a JNDI namespace
	 * into which the {@link org.hibernate.SessionFactory} should be bound and made accessible?
	 * <p>
	 * Defaults to {@code true} for backwards compatibility.
	 * <p>
	 * Set this to {@code false} if naming a SessionFactory is needed for serialization purposes,
	 * but no writable JNDI context exists in the runtime environment or if the user simply does
	 * not want JNDI to be used.
	 *
	 * @see #SESSION_FACTORY_NAME
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyNameAsJndiName(boolean) 
	 */
	String SESSION_FACTORY_NAME_IS_JNDI = "hibernate.session_factory_name_is_jndi";

	/**
	 * Enables logging of generated SQL to the console.
	 */
	String SHOW_SQL = "hibernate.show_sql";

	/**
	 * Enables formatting of SQL logged to the console.
	 */
	String FORMAT_SQL = "hibernate.format_sql";

	/**
	 * Enables highlighting of SQL logged to the console using ANSI escape codes.
	 */
	String HIGHLIGHT_SQL = "hibernate.highlight_sql";

	/**
	 * Specifies that comments should be added to the generated SQL.
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applySqlComments(boolean)
	 */
	String USE_SQL_COMMENTS = "hibernate.use_sql_comments";

	/**
	 * Specifies the maximum depth of nested outer join fetching.
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyMaximumFetchDepth(int)
	 */
	String MAX_FETCH_DEPTH = "hibernate.max_fetch_depth";

	/**
	 * Specifies the default batch size for batch fetching. When set to
	 * a value greater than one, Hibernate will use batch fetching, when
	 * possible, to fetch any association.
	 * <p>
	 * By default, Hibernate only uses batch fetching for entities and
	 * collections explicitly annotated {@code @BatchSize}.
	 *
	 * @see org.hibernate.annotations.BatchSize
	 * @see org.hibernate.Session#setFetchBatchSize(int)
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyDefaultBatchFetchSize(int)
	 */
	String DEFAULT_BATCH_FETCH_SIZE = "hibernate.default_batch_fetch_size";

	/**
	 * When enabled, Hibernate will use subselect fetching, when possible, to
	 * fetch any collection.
	 * <p>
	 * By default, Hibernate only uses subselect fetching for collections
	 * explicitly annotated {@code @Fetch(SUBSELECT)}.
	 *
	 * @since 6.3
	 *
	 * @see org.hibernate.annotations.FetchMode#SUBSELECT
	 * @see org.hibernate.Session#setSubselectFetchingEnabled(boolean)
	 * @see org.hibernate.boot.SessionFactoryBuilder#applySubselectFetchEnabled(boolean)
	 */
	String USE_SUBSELECT_FETCH = "hibernate.use_subselect_fetch";

	/**
	 * When enabled, specifies that JDBC scrollable {@code ResultSet}s may be used.
	 * This property is only necessary when there is no {@code ConnectionProvider},
	 * that is, when the client is supplying JDBC connections.
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyScrollableResultsSupport(boolean)
	 */
	String USE_SCROLLABLE_RESULTSET = "hibernate.jdbc.use_scrollable_resultset";

	/**
	 * Specifies that generated primary keys may be retrieved using the JDBC 3
	 * {@link java.sql.PreparedStatement#getGeneratedKeys()} operation.
	 * <p>
	 * Usually, performance will be improved if this behavior is enabled, assuming
	 * the JDBC driver supports {@code getGeneratedKeys()}.
	 *
	 * @see java.sql.PreparedStatement#getGeneratedKeys
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyGetGeneratedKeysSupport(boolean)
	 */
	String USE_GET_GENERATED_KEYS = "hibernate.jdbc.use_get_generated_keys";

	/**
	 * Gives the JDBC driver a hint as to the number of rows that should be fetched
	 * from the database when more rows are needed. If {@code 0}, the JDBC driver's
	 * default settings will be used.
	 *
	 * @see java.sql.PreparedStatement#setFetchSize(int)
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyJdbcFetchSize(int)
	 * @see org.hibernate.ScrollableResults#setFetchSize(int)
	 */
	String STATEMENT_FETCH_SIZE = "hibernate.jdbc.fetch_size";

	/**
	 * Specifies the maximum JDBC batch size. A nonzero value enables batch updates.
	 *
	 * @see java.sql.PreparedStatement#executeBatch()
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyJdbcBatchSize(int)
	 */
	String STATEMENT_BATCH_SIZE = "hibernate.jdbc.batch_size";

	/**
	 * Specifies a custom {@link BatchBuilder}.
	 */
	String BATCH_STRATEGY = "hibernate.jdbc.factory_class";

	/**
	 * When enabled, specifies that {@linkplain jakarta.persistence.Version versioned}
	 * data should be included in batching.
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyJdbcBatchingForVersionedEntities(boolean)
	 */
	String BATCH_VERSIONED_DATA = "hibernate.jdbc.batch_versioned_data";

	/**
	 * Specifies the {@linkplain java.util.TimeZone time zone} to use in the JDBC driver,
	 * which is supposed to match the database timezone.
	 * <p>
	 * This is the timezone what will be passed to
	 * {@link java.sql.PreparedStatement#setTimestamp(int, java.sql.Timestamp, java.util.Calendar)}
	 * {@link java.sql.PreparedStatement#setTime(int, java.sql.Time, java.util.Calendar)},
	 * {@link java.sql.ResultSet#getTimestamp(int, Calendar)}, and
	 * {@link java.sql.ResultSet#getTime(int, Calendar)} when binding parameters.
	 * <p>
	 * The time zone may be given as:
	 * <ul>
	 *     <li>an instance of {@link java.util.TimeZone},
	 *     <li>an instance of {@link java.time.ZoneId}, or
	 *     <li>a time zone ID string to be passed to {@link java.time.ZoneId#of(String)}.
	 * </ul>
	 * <p>
	 * By default, the {@linkplain java.util.TimeZone#getDefault() JVM default time zone}
	 * is assumed by the JDBC driver.
	 *
	 * @since 5.2.3
	 */
	String JDBC_TIME_ZONE = "hibernate.jdbc.time_zone";

	/**
	 * When enabled, specifies that the {@link org.hibernate.Session} should be
	 * closed automatically at the end of each transaction.
	 * 
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyAutoClosing(boolean) 
	 */
	String AUTO_CLOSE_SESSION = "hibernate.transaction.auto_close_session";

	/**
	 * When enabled, specifies that automatic flushing should occur during the JTA
	 * {@link jakarta.transaction.Synchronization#beforeCompletion()} callback.
	 * 
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyAutoFlushing(boolean) 
	 */
	String FLUSH_BEFORE_COMPLETION = "hibernate.transaction.flush_before_completion";

	/**
	 * Specifies how Hibernate should manage JDBC connections in terms of acquisition
	 * and release, either:
	 * <ul>
	 *     <li>an instance of the enumeration
	 *         {@link org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode},
	 *         or
	 *     <li>the name of one of its instances.
	 * </ul>
	 * <p>
	 * The default is {@code DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION}.
	 *
	 * @see org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyConnectionHandlingMode(PhysicalConnectionHandlingMode)
	 *
	 * @since 5.2
	 */
	String CONNECTION_HANDLING = "hibernate.connection.handling_mode";

	/**
	 * Specifies a {@link org.hibernate.context.spi.CurrentSessionContext} for
	 * scoping the {@linkplain org.hibernate.SessionFactory#getCurrentSession()
	 * current session}, either:
	 * <ul>
	 *     <li>{@code jta}, {@code thread}, or {@code managed}, or
	 *     <li>the name of a class implementing
	 *     {@code org.hibernate.context.spi.CurrentSessionContext}.
	 * </ul>
	 * If this property is not set, but JTA support is enabled, then
	 * {@link org.hibernate.context.internal.JTASessionContext} is used
	 * by default.
	 *
	 * @see org.hibernate.SessionFactory#getCurrentSession()
	 * @see org.hibernate.context.spi.CurrentSessionContext
	 */
	String CURRENT_SESSION_CONTEXT_CLASS = "hibernate.current_session_context_class";

	/**
	 * When enabled, specifies that the generated identifier of an entity is unset
	 * when the entity is {@linkplain org.hibernate.Session#remove(Object) deleted}.
	 * <p>
	 * By default, generated identifiers are never unset.
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyIdentifierRollbackSupport(boolean)
	 */
	String USE_IDENTIFIER_ROLLBACK = "hibernate.use_identifier_rollback";

	/**
	 * When enabled, specifies that property access should be optimized via the use
	 * of generated bytecode.
	 *
	 * @deprecated Will be removed without replacement. See HHH-15631
	 */
	@Deprecated(forRemoval = true)
	@SuppressWarnings("DeprecatedIsStillUsed")
	String USE_REFLECTION_OPTIMIZER = "hibernate.bytecode.use_reflection_optimizer";

	/**
	 * When enabled, specifies that Hibernate should attempt to map parameter names
	 * given in a {@link org.hibernate.procedure.ProcedureCall} or
	 * {@link jakarta.persistence.StoredProcedureQuery} to named parameters of the
	 * JDBC {@link java.sql.CallableStatement}.
	 *
	 * @see org.hibernate.boot.spi.SessionFactoryOptions#isUseOfJdbcNamedParametersEnabled()
	 *
	 * @since 6.0
	 */
	String CALLABLE_NAMED_PARAMS_ENABLED = "hibernate.query.proc.callable_named_params_enabled";

	/**
	 * Specifies a {@link org.hibernate.query.hql.HqlTranslator} to use for HQL query
	 * translation.
	 */
	String SEMANTIC_QUERY_PRODUCER = "hibernate.query.hql.translator";

	/**
	 * Specifies a {@link org.hibernate.query.sqm.sql.SqmTranslatorFactory} to use for
	 * HQL query translation.
	 */
	String SEMANTIC_QUERY_TRANSLATOR = "hibernate.query.sqm.translator";

	/**
	 * Defines the "global" strategy to use for handling HQL and Criteria mutation queries.
	 * Specifies a {@link org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy}..
	 */
	String QUERY_MULTI_TABLE_MUTATION_STRATEGY = "hibernate.query.mutation_strategy";

	/**
	 * Defines the "global" strategy to use for handling HQL and Criteria insert queries.
	 * Specifies a {@link org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy}.
	 */
	String QUERY_MULTI_TABLE_INSERT_STRATEGY = "hibernate.query.insert_strategy";

	/**
	 * When enabled, specifies that named queries be checked during startup.
	 * <p>
	 * By default, named queries are checked at startup.
	 * <p>
	 * Mainly intended for use in test environments.
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyNamedQueryCheckingOnStartup(boolean)
	 */
	String QUERY_STARTUP_CHECKING = "hibernate.query.startup_check";

	/**
	 * Enable ordering of update statements by primary key value, for the purpose of more
	 * efficient JDBC batching
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyOrderingOfUpdates(boolean)
	 */
	String ORDER_UPDATES = "hibernate.order_updates";

	/**
	 * Enable ordering of insert statements by primary key value, for the purpose of more
	 * efficient JDBC batching.
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyOrderingOfInserts(boolean)
	 */
	String ORDER_INSERTS = "hibernate.order_inserts";

	/**
	 * Allows JPA callbacks (via {@link jakarta.persistence.PreUpdate} and friends) to be
	 * completely disabled. Mostly useful to save some memory when they are not used.
	 * <p>
	 * JPA callbacks are enabled by default. Set this property to {@code false} to disable
	 * them.
	 * <p>
	 * Experimental and will likely be removed as soon as the memory overhead is resolved.
	 *
	 * @see org.hibernate.jpa.event.spi.CallbackType
	 *
	 * @since 5.4
	 */
	@Incubating
	String JPA_CALLBACKS_ENABLED = "hibernate.jpa_callbacks.enabled";

	/**
	 * Specifies the default {@linkplain NullPrecedence precedence of null values} in the HQL
	 * {@code ORDER BY} clause, either {@code none}, {@code first}, or {@code last}.
	 * <p>
	 * The default is {@code none}.
	 *
	 * @see NullPrecedence
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyDefaultNullPrecedence(NullPrecedence)
	 */
	String DEFAULT_NULL_ORDERING = "hibernate.order_by.default_null_ordering";

	/**
	 * When enabled, specifies that JDBC statement warnings should be logged.
	 * <p>
	 * The default is determined by
	 * {@link org.hibernate.dialect.Dialect#isJdbcLogWarningsEnabledByDefault()}.
	 *
	 * @see java.sql.Statement#getWarnings()
	 *
	 * @since 5.1
	 */
	String LOG_JDBC_WARNINGS = "hibernate.jdbc.log.warnings";

	/**
	 * Identifies a {@link org.hibernate.resource.beans.container.spi.BeanContainer}
	 * to be used.
	 * <p>
	 * Note that for CDI-based containers setting this is not necessary - simply
	 * pass the {@link jakarta.enterprise.inject.spi.BeanManager} to use via
	 * {@link #CDI_BEAN_MANAGER} and optionally specify {@link #DELAY_CDI_ACCESS}.
	 * This setting useful to integrate non-CDI bean containers such as Spring.
	 *
	 * @since 5.3
	 */
	String BEAN_CONTAINER = "hibernate.resource.beans.container";

	/**
	 * Used in conjunction with {@value #BEAN_CONTAINER} when CDI is used.
	 * <p>
	 * By default, to be JPA spec compliant, Hibernate should access the CDI
	 * {@link jakarta.enterprise.inject.spi.BeanManager} while bootstrapping the
	 * {@link org.hibernate.SessionFactory}.  In some cases however this can lead
	 * to a chicken/egg situation where the JPA provider immediately accesses the
	 * {@code BeanManager} when managed beans are awaiting JPA PU injection.
	 * <p>
	 * This setting tells Hibernate to delay accessing until first use.
	 * <p>
	 * This setting has the decided downside that bean config problems will not
	 * be done at deployment time, but will instead manifest at runtime. For this
	 * reason, the preferred means for supplying a CDI BeanManager is to provide
	 * an implementation of
	 * {@link org.hibernate.resource.beans.container.spi.ExtendedBeanManager} which
	 * gives Hibernate a callback when the {@code BeanManager} is ready for use.
	 *
	 * @since 5.0.8
	 */
	String DELAY_CDI_ACCESS = "hibernate.delay_cdi_access";

	/**
	 * Controls whether Hibernate can try to create beans other than converters
	 * and listeners using CDI.  Only meaningful when a CDI {@link #BEAN_CONTAINER container}
	 * is used.
	 * <p>
	 * By default, Hibernate will only attempt to create converter and listener beans using CDI.
	 *
	 * @since 6.2
	 */
	String ALLOW_EXTENSIONS_IN_CDI = "hibernate.cdi.extensions";


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
	 * Proxool property to configure the Proxool provider using an XML ({@code /path/to/file.xml})
	 */
	String PROXOOL_XML = "hibernate.proxool.xml";

	/**
	 * Proxool property to configure the Proxool provider using a properties file
	 * ({@code /path/to/proxool.properties})
	 */
	String PROXOOL_PROPERTIES = "hibernate.proxool.properties";

	/**
	 * Proxool property to configure the Proxool Provider from an already existing pool
	 * ({@code true} / {@code false})
	 */
	String PROXOOL_EXISTING_POOL = "hibernate.proxool.existing_pool";

	/**
	 * Proxool property with the Proxool pool alias to use
	 * (Required for {@link #PROXOOL_EXISTING_POOL}, {@link #PROXOOL_PROPERTIES}, or
	 * {@link #PROXOOL_XML})
	 */
	String PROXOOL_POOL_ALIAS = "hibernate.proxool.pool_alias";


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Second-level cache settings
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * The {@link org.hibernate.cache.spi.RegionFactory} implementation, either:
	 * <ul>
	 *     <li>an instance of {@link org.hibernate.cache.spi.RegionFactory},
	 *     <li>a {@link Class} implementing {@link org.hibernate.cache.spi.RegionFactory}, or
	 *     <li>he name of a class implementing {@link org.hibernate.cache.spi.RegionFactory}.
	 * </ul>
	 * <p>
	 * Defaults to {@link NoCachingRegionFactory}, so that caching is disabled.
	 *
	 * @see #USE_SECOND_LEVEL_CACHE
	 */
	String CACHE_REGION_FACTORY = "hibernate.cache.region.factory_class";

	/**
	 * Specifies the {@link org.hibernate.cache.spi.CacheKeysFactory} to use, either:
	 * <ul>
	 *     <li>an instance of {@link org.hibernate.cache.spi.CacheKeysFactory},
	 *     <li>a {@link Class} implementing {@link org.hibernate.cache.spi.CacheKeysFactory},
	 *     <li>the name of a class implementing {@link org.hibernate.cache.spi.CacheKeysFactory},
	 *     <li>{@code "default"} as a short name for {@link org.hibernate.cache.internal.DefaultCacheKeysFactory}, or
	 *     <li>{@code "simple"} as a short name for {@link org.hibernate.cache.internal.SimpleCacheKeysFactory}.
	 * </ul>
	 *
	 * @since 5.2
	 *
	 * @deprecated this is only honored for {@code hibernate-infinispan}
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String CACHE_KEYS_FACTORY = "hibernate.cache.keys_factory";

	/**
	 * When enabled, specifies that the second-level cache may be used.
	 * <p>
	 * By default, if the configured {@link org.hibernate.cache.spi.RegionFactory}
	 * is not the {@link org.hibernate.cache.internal.NoCachingRegionFactory}, then
	 * the second-level cache is enabled. Otherwise, the second-level cache is disabled.
	 *
	 * @see #CACHE_REGION_FACTORY
	 * @see org.hibernate.boot.SessionFactoryBuilder#applySecondLevelCacheSupport(boolean)
	 */
	String USE_SECOND_LEVEL_CACHE = "hibernate.cache.use_second_level_cache";

	/**
	 * Enable the query cache (disabled by default).
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyQueryCacheSupport(boolean)
	 */
	String USE_QUERY_CACHE = "hibernate.cache.use_query_cache";

	/**
	 * Specifies the {@link org.hibernate.cache.spi.TimestampsCacheFactory} to use.
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyTimestampsCacheFactory(TimestampsCacheFactory)
	 */
	String QUERY_CACHE_FACTORY = "hibernate.cache.query_cache_factory";

	/**
	 * The {@code CacheProvider} region name prefix
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyCacheRegionPrefix(String)
	 */
	String CACHE_REGION_PREFIX = "hibernate.cache.region_prefix";

	/**
	 * Optimize interaction with the second-level cache to minimize writes, at the cost
	 * of an additional read before each write. This setting is useful if writes to the
	 * cache are much more expensive than reads from the cache, for example, if the cache
	 * is a distributed cache.
	 * <p>
	 * It's not usually necessary to set this explicitly because, by default, it's set
	 * to a {@linkplain org.hibernate.boot.SessionFactoryBuilder#applyMinimalPutsForCaching(boolean)
	 * sensible value} by the second-level cache implementation.
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyMinimalPutsForCaching(boolean)
	 */
	String USE_MINIMAL_PUTS = "hibernate.cache.use_minimal_puts";

	/**
	 * Enables the use of structured second-level cache entries. This makes the cache
	 * entries human-readable, but carries a performance cost.
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyStructuredCacheEntries(boolean)
	 */
	String USE_STRUCTURED_CACHE = "hibernate.cache.use_structured_entries";

	/**
	 * Enables the automatic eviction of a bidirectional association's collection
	 * cache when an element in the {@link jakarta.persistence.ManyToOne} collection
	 * is added, updated, or removed without properly managing the change on the
	 * {@link jakarta.persistence.OneToMany} side.
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyAutomaticEvictionOfCollectionCaches(boolean)
	 */
	String AUTO_EVICT_COLLECTION_CACHE = "hibernate.cache.auto_evict_collection_cache";

	/**
	 * Enable direct storage of entity references into the second level cache when
	 * applicable. This is appropriate only for immutable entities.
	 * <p>
	 * By default, entities are always stored in a "disassembled" form, that is, as
	 * a tuple of attribute values.
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyDirectReferenceCaching(boolean)
	 */
	String USE_DIRECT_REFERENCE_CACHE_ENTRIES = "hibernate.cache.use_reference_entries";






	// Still to categorize

	/**
	 * When enabled, all database identifiers are quoted.
	 */
	String GLOBALLY_QUOTED_IDENTIFIERS = "hibernate.globally_quoted_identifiers";

	/**
	 * Assuming {@link #GLOBALLY_QUOTED_IDENTIFIERS}, this allows global quoting
	 * to skip column definitions defined by {@link jakarta.persistence.Column},
	 * {@link jakarta.persistence.JoinColumn}, etc.
	 * <p>
	 * JPA states that column definitions are subject to global quoting, so by default
	 * this setting is {@code false} for JPA compliance. Set to {@code true} to avoid
	 * explicit column names being quoted due to global quoting (they will still be
	 * quoted if explicitly quoted in the annotation or XML).
	 */
	String GLOBALLY_QUOTED_IDENTIFIERS_SKIP_COLUMN_DEFINITIONS = "hibernate.globally_quoted_identifiers_skip_column_definitions";

	/**
	 * Enable nullability checking, raises an exception if an attribute marked as
	 * {@linkplain jakarta.persistence.Basic#optional() not null} is null at runtime.
	 * <p>
	 * Defaults to disabled if Bean Validation is present in the classpath and
	 * annotations are used, or enabled otherwise.
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyNullabilityChecking(boolean)
	 */
	String CHECK_NULLABILITY = "hibernate.check_nullability";

	/**
	 * Selects a bytecode enhancement library.
	 * <p>
	 * At present only bytebuddy is supported, bytebuddy being the default since version 5.3.
	 */
	String BYTECODE_PROVIDER = "hibernate.bytecode.provider";

	String JPAQL_STRICT_COMPLIANCE= "hibernate.query.jpaql_strict_compliance";

	/**
	 * When a generator specifies an increment-size and an optimizer was not explicitly
	 * specified, which of the "pooled" optimizers should be preferred? Can specify an
	 * optimizer short name or the name of a class which implements
	 * {@link org.hibernate.id.enhanced.Optimizer}.
	 */
	String PREFERRED_POOLED_OPTIMIZER = "hibernate.id.optimizer.pooled.preferred";

	/**
	 * When enabled, specifies that {@linkplain QueryPlan query plans} should be
	 * {@linkplain org.hibernate.query.spi.QueryInterpretationCache cached}.
	 * <p>
	 * By default, the query plan cache is disabled, unless one of the configuration
	 * properties {@value #QUERY_PLAN_CACHE_MAX_SIZE} or
	 * {@value #QUERY_PLAN_CACHE_PARAMETER_METADATA_MAX_SIZE} is set.
	 */
	String QUERY_PLAN_CACHE_ENABLED = "hibernate.query.plan_cache_enabled";

	/**
	 * The maximum number of entries in the
	 * {@linkplain org.hibernate.query.spi.QueryInterpretationCache
	 * query interpretation cache}.
	 * <p>
	 * The default maximum is
	 * {@value org.hibernate.query.spi.QueryEngine#DEFAULT_QUERY_PLAN_MAX_COUNT}.
	 *
	 * @see org.hibernate.query.spi.QueryInterpretationCache
	 */
	String QUERY_PLAN_CACHE_MAX_SIZE = "hibernate.query.plan_cache_max_size";

	/**
	 * The maximum number of {@link org.hibernate.query.ParameterMetadata} instances
	 * maintained by the {@link org.hibernate.query.spi.QueryInterpretationCache}.
	 * <p>
	 *
	 * @deprecated this setting is not currently used
	 */
	@Deprecated(since="6.0")
	String QUERY_PLAN_CACHE_PARAMETER_METADATA_MAX_SIZE = "hibernate.query.plan_parameter_metadata_max_size";

	/**
	 * When enabled, specifies that Hibernate should not use contextual LOB creation.
	 *
	 * @see org.hibernate.engine.jdbc.LobCreator
	 * @see org.hibernate.engine.jdbc.LobCreationContext
	 */
	String NON_CONTEXTUAL_LOB_CREATION = "hibernate.jdbc.lob.non_contextual_creation";


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SchemaManagementTool settings
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Setting to perform {@link org.hibernate.tool.schema.spi.SchemaManagementTool}
	 * actions automatically as part of the {@link org.hibernate.SessionFactory}
	 * lifecycle. Valid options are enumerated by {@link org.hibernate.tool.schema.Action}.
	 * <p>
	 * Interpreted in combination with {@link #JAKARTA_HBM2DDL_DATABASE_ACTION} and
	 * {@link #JAKARTA_HBM2DDL_SCRIPTS_ACTION}. If no value is specified, the default
	 * is {@link org.hibernate.tool.schema.Action#NONE "none"}.
	 *
	 * @see org.hibernate.tool.schema.Action
	 */
	String HBM2DDL_AUTO = "hibernate.hbm2ddl.auto";

	/**
	 * @deprecated Use {@link #JAKARTA_HBM2DDL_DATABASE_ACTION} instead
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String HBM2DDL_DATABASE_ACTION = "javax.persistence.schema-generation.database.action";

	/**
	 * @deprecated Use {@link #JAKARTA_HBM2DDL_SCRIPTS_ACTION} instead
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String HBM2DDL_SCRIPTS_ACTION = "javax.persistence.schema-generation.scripts.action";

	/**
	 * @deprecated Use {@link #JAKARTA_HBM2DDL_CONNECTION} instead
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String HBM2DDL_CONNECTION = "javax.persistence.schema-generation-connection";

	/**
	 * @deprecated  Migrate to {@link #JAKARTA_HBM2DDL_CREATE_SOURCE} instead
	 * @see org.hibernate.tool.schema.SourceType
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String HBM2DDL_CREATE_SOURCE = "javax.persistence.schema-generation.create-source";

	/**
	 * @deprecated Migrate to {@link #JAKARTA_HBM2DDL_DROP_SOURCE}.
	 * @see org.hibernate.tool.schema.SourceType
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String HBM2DDL_DROP_SOURCE = "javax.persistence.schema-generation.drop-source";

	/**
	 * @deprecated Migrate to {@link #JAKARTA_HBM2DDL_CREATE_SCRIPT_SOURCE}
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String HBM2DDL_CREATE_SCRIPT_SOURCE = "javax.persistence.schema-generation.create-script-source";

	/**
	 * @deprecated Migrate to {@link #JAKARTA_HBM2DDL_DROP_SCRIPT_SOURCE}
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String HBM2DDL_DROP_SCRIPT_SOURCE = "javax.persistence.schema-generation.drop-script-source";

	/**
	 * @deprecated Migrate to {@link #JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET}
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String HBM2DDL_SCRIPTS_CREATE_TARGET = "javax.persistence.schema-generation.scripts.create-target";

	/**
	 * For cases where the {@value #HBM2DDL_SCRIPTS_ACTION} value indicates that schema commands
	 * should be written to DDL script file, specifies if schema commands should be appended to
	 * the end of the file rather than written at the beginning of the file.
	 * <p>
	 * Values are: {@code true} for appending schema commands to the end of the file, {@code false}
	 * for writing schema commands at the beginning.
	 * <p>
	 * The default value is {@code true}
	 */
	String HBM2DDL_SCRIPTS_CREATE_APPEND = "hibernate.hbm2ddl.schema-generation.script.append";

	/**
	 * @deprecated Migrate to {@link #JAKARTA_HBM2DDL_SCRIPTS_DROP_TARGET}
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String HBM2DDL_SCRIPTS_DROP_TARGET = "javax.persistence.schema-generation.scripts.drop-target";

	/**
	 * Specifies a comma-separated list of file names of scripts containing SQL DML statements that
	 * should be executed after schema export completes. The order of the scripts is significant,
	 * with the first script in the list being executed first.
	 * <p>
	 * The scripts are only executed if the schema is created by Hibernate, that is, if
	 * {@value #HBM2DDL_AUTO} is set to {@code create} or {@code create-drop}.
	 * <p>
	 * The default value is {@code /import.sql}.
	 * <p>
	 * The JPA-standard setting {@link #JAKARTA_HBM2DDL_CREATE_SCRIPT_SOURCE} is now preferred.
	 */
	String HBM2DDL_IMPORT_FILES = "hibernate.hbm2ddl.import_files";

	/**
	 * @deprecated Use {@link #JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE} instead
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String HBM2DDL_LOAD_SCRIPT_SOURCE = "javax.persistence.sql-load-script-source";

	/**
	 * The {@link org.hibernate.tool.schema.spi.SqlScriptCommandExtractor} implementation
	 * to use for parsing source/import files specified by {@link #JAKARTA_HBM2DDL_CREATE_SCRIPT_SOURCE},
	 * {@link #JAKARTA_HBM2DDL_DROP_SCRIPT_SOURCE} or {@link #HBM2DDL_IMPORT_FILES}. Either:
	 * <ul>
	 * <li>an instance of {@link org.hibernate.tool.schema.spi.SqlScriptCommandExtractor},
	 * <li>a {@link Class} object representing a class that implements {@code SqlScriptCommandExtractor},
	 *     or
	 * <li>the name of a class that implements {@code SqlScriptCommandExtractor}.
	 * </ul>
	 * <p>
	 * The correct extractor to use depends on the format of the SQL script:
	 * <ul>
	 * <li>if the script has one complete SQL statement per line, use
	 *     {@link org.hibernate.tool.schema.internal.script.SingleLineSqlScriptExtractor}, or
	 * <li>if a script contains statements spread over multiple lines, use
	 *     {@link org.hibernate.tool.schema.internal.script.MultiLineSqlScriptExtractor}.
	 * </ul>
	 * <p>
	 * The default value is {@code org.hibernate.tool.schema.internal.script.SingleLineSqlScriptExtractor}.
	 *
	 * @see org.hibernate.tool.schema.internal.script.SingleLineSqlScriptExtractor
	 * @see org.hibernate.tool.schema.internal.script.MultiLineSqlScriptExtractor
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
	 * @deprecated Use {@link #JAKARTA_HBM2DDL_CREATE_SCHEMAS} instead
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String HBM2DDL_CREATE_SCHEMAS = "javax.persistence.create-database-schemas";

	/**
	 * Specifies what type of schema tooling action should be performed against the
	 * database specified using either {@value #JAKARTA_HBM2DDL_CONNECTION} or the
	 * configured {@link org.hibernate.engine.jdbc.connections.spi.ConnectionProvider}
	 * for the {@link org.hibernate.SessionFactory}.
	 * <p>
	 * Valid options are enumerated by {@link org.hibernate.tool.schema.Action}.
	 * <p>
	 * This setting takes precedence over {@value #HBM2DDL_AUTO}.
	 * <p>
	 * If no value is specified, the default is
	 * {@link org.hibernate.tool.schema.Action#NONE "none"}.
	 *
	 * @see org.hibernate.tool.schema.Action
	 * @see #JAKARTA_HBM2DDL_CONNECTION
	 * @see #JAKARTA_JDBC_URL
	 */
	String JAKARTA_HBM2DDL_DATABASE_ACTION = "jakarta.persistence.schema-generation.database.action";

	/**
	 * Specifies what type of schema tooling action should be written to script files.
	 * <p>
	 * Valid options are enumerated by {@link org.hibernate.tool.schema.Action}.
	 * <p>
	 * The script file is identified using {@value #JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET}.
	 * <p>
	 * If no value is specified, the default is
	 * {@link org.hibernate.tool.schema.Action#NONE "none"}.
	 *
	 * @see org.hibernate.tool.schema.Action
	 * @see #JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET
	 * @see #JAKARTA_HBM2DDL_SCRIPTS_DROP_TARGET
	 */
	String JAKARTA_HBM2DDL_SCRIPTS_ACTION = "jakarta.persistence.schema-generation.scripts.action";

	/**
	 * Allows passing a specific {@link java.sql.Connection} instance to be used by
	 * {@link org.hibernate.tool.schema.spi.SchemaManagementTool} for the purpose of
	 * determining the {@link org.hibernate.dialect.Dialect}, and for performing
	 * {@link #JAKARTA_HBM2DDL_DATABASE_ACTION database actions} if requested.
	 * <p>
	 * For {@code Dialect} resolution, {@value #JAKARTA_HBM2DDL_DB_NAME} and, optionally,
	 * {@value JAKARTA_HBM2DDL_DB_VERSION}, {@value #JAKARTA_HBM2DDL_DB_MAJOR_VERSION},
	 * and {@value #JAKARTA_HBM2DDL_DB_MINOR_VERSION} can be used instead
	 *
	 * @see #JAKARTA_HBM2DDL_DB_NAME
	 * @see #JAKARTA_HBM2DDL_DB_VERSION
	 * @see #JAKARTA_HBM2DDL_DB_MAJOR_VERSION
	 * @see #JAKARTA_HBM2DDL_DB_MINOR_VERSION
	 */
	String JAKARTA_HBM2DDL_CONNECTION = "jakarta.persistence.schema-generation-connection";

	/**
	 * Specifies the name of the database vendor (as would be reported by
	 * {@link java.sql.DatabaseMetaData#getDatabaseProductName}) for the purpose of
	 * determining the {@link org.hibernate.dialect.Dialect} to use.
	 * <p>
	 * For cases when the name of the database vendor is not enough alone, a combination
	 * of {@value JAKARTA_HBM2DDL_DB_VERSION}, {@value #JAKARTA_HBM2DDL_DB_MAJOR_VERSION}
	 * {@value #JAKARTA_HBM2DDL_DB_MINOR_VERSION} can be used instead
	 *
	 * @see #JAKARTA_HBM2DDL_DB_VERSION
	 * @see #JAKARTA_HBM2DDL_DB_MAJOR_VERSION
	 * @see #JAKARTA_HBM2DDL_DB_MINOR_VERSION
	 *
	 * @implSpec {@link #JAKARTA_HBM2DDL_DATABASE_ACTION database actions} are not
	 * available when supplying just the name and versions
	 */
	String JAKARTA_HBM2DDL_DB_NAME = "jakarta.persistence.database-product-name";

	/**
	 * Used in conjunction with {@value #JAKARTA_HBM2DDL_DB_NAME} for the purpose of
	 * determining the {@link org.hibernate.dialect.Dialect} to use when the name does
	 * not provide enough detail.
	 * <p>
	 * The value is expected to match what would be returned from
	 * {@link java.sql.DatabaseMetaData#getDatabaseProductVersion()}) for the
	 * underlying database.
	 *
	 * @see #JAKARTA_HBM2DDL_DB_NAME
	 */
	String JAKARTA_HBM2DDL_DB_VERSION = "jakarta.persistence.database-product-version";

	/**
	 * Used in conjunction with {@value #JAKARTA_HBM2DDL_DB_NAME} for the purpose of
	 * determining the {@link org.hibernate.dialect.Dialect} to use when the name does
	 * not provide enough detail.
	 * <p>
	 * The value is expected to match what would be returned from
	 * {@link java.sql.DatabaseMetaData#getDatabaseMajorVersion()}) for the underlying
	 * database.
	 *
	 * @see #JAKARTA_HBM2DDL_DB_NAME
	 */
	String JAKARTA_HBM2DDL_DB_MAJOR_VERSION = "jakarta.persistence.database-major-version";

	/**
	 * Used in conjunction with {@value #JAKARTA_HBM2DDL_DB_NAME} for the purpose of
	 * determining the {@link org.hibernate.dialect.Dialect} to use when the name does
	 * not provide enough detail.
	 * <p>
	 * The value is expected to match what would be returned from
	 * {@link java.sql.DatabaseMetaData#getDatabaseMinorVersion()}) for the underlying
	 * database.
	 *
	 * @see #JAKARTA_HBM2DDL_DB_NAME
	 */
	String JAKARTA_HBM2DDL_DB_MINOR_VERSION = "jakarta.persistence.database-minor-version";

	/**
	 * Specifies whether schema generation commands for schema creation are to be determined
	 * based on object/relational mapping metadata, DDL scripts, or a combination of the two.
	 * See {@link org.hibernate.tool.schema.SourceType} for the list of legal values.
	 * <p>
	 * If no value is specified, a default is inferred as follows:
	 * <ul>
	 *     <li>if source scripts are specified via {@value #JAKARTA_HBM2DDL_CREATE_SCRIPT_SOURCE},
	 *     then {@link org.hibernate.tool.schema.SourceType#SCRIPT "script"} is assumed, or
	 *     <li>otherwise, {@link org.hibernate.tool.schema.SourceType#SCRIPT "metadata"} is
	 *     assumed.
	 * </ul>
	 *
	 * @see org.hibernate.tool.schema.SourceType
	 */
	String JAKARTA_HBM2DDL_CREATE_SOURCE = "jakarta.persistence.schema-generation.create-source";

	/**
	 * Specifies whether schema generation commands for schema dropping are to be determined
	 * based on object/relational mapping metadata, DDL scripts, or a combination of the two.
	 * See {@link org.hibernate.tool.schema.SourceType} for the list of legal values.
	 * <p>
	 * If no value is specified, a default is inferred as follows:
	 * <ul>
	 *     <li>if source scripts are specified via {@value #JAKARTA_HBM2DDL_DROP_SCRIPT_SOURCE},
	 *     then {@linkplain org.hibernate.tool.schema.SourceType#SCRIPT "script"} is assumed, or
	 *     <li>otherwise, {@linkplain org.hibernate.tool.schema.SourceType#SCRIPT "metadata"}
	 *     is assumed.
	 * </ul>
	 *
	 * @see org.hibernate.tool.schema.SourceType
	 */
	String JAKARTA_HBM2DDL_DROP_SOURCE = "jakarta.persistence.schema-generation.drop-source";

	/**
	 * Specifies the CREATE script file as either a {@link java.io.Reader} configured for reading
	 * the DDL script file or a string designating a file {@link java.net.URL} for the DDL script.
	 * <p>
	 * Hibernate historically also accepted {@link #HBM2DDL_IMPORT_FILES} for a similar purpose.
	 * This setting is now preferred.
	 *
	 * @see #JAKARTA_HBM2DDL_CREATE_SOURCE
	 * @see #HBM2DDL_IMPORT_FILES
	 */
	String JAKARTA_HBM2DDL_CREATE_SCRIPT_SOURCE = "jakarta.persistence.schema-generation.create-script-source";

	/**
	 * Specifies the DROP script file as either a {@link java.io.Reader} configured for reading
	 * the DDL script file or a string designating a file {@link java.net.URL} for the DDL script.
	 *
	 * @see #JAKARTA_HBM2DDL_DROP_SOURCE
	 */
	String JAKARTA_HBM2DDL_DROP_SCRIPT_SOURCE = "jakarta.persistence.schema-generation.drop-script-source";

	/**
	 * For cases where {@value #JAKARTA_HBM2DDL_SCRIPTS_ACTION} indicates that schema creation
	 * commands should be written to a script file, this setting specifies either a
	 * {@link java.io.Writer} configured for output of the DDL script or a string specifying
	 * the file URL for the DDL script.
	 *
	 * @see #JAKARTA_HBM2DDL_SCRIPTS_ACTION
	 */
	String JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET = "jakarta.persistence.schema-generation.scripts.create-target";

	/**
	 * For cases where {@value #JAKARTA_HBM2DDL_SCRIPTS_ACTION} indicates that schema
	 * drop commands should be written to a script file, this setting specifies either a
	 * {@link java.io.Writer} configured for output of the DDL script or a string
	 * specifying the file URL for the DDL script.
	 *
	 * @see #JAKARTA_HBM2DDL_SCRIPTS_ACTION
	 */
	String JAKARTA_HBM2DDL_SCRIPTS_DROP_TARGET = "jakarta.persistence.schema-generation.scripts.drop-target";

	/**
	 * JPA-standard variant of {@link #HBM2DDL_IMPORT_FILES} for specifying a database
	 * initialization script to be run as part of schema-export
	 * <p>
	 * Specifies a {@link java.io.Reader} configured for reading of the SQL load script
	 * or a string designating the {@link java.net.URL} for the SQL load script.
	 */
	String JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE = "jakarta.persistence.sql-load-script-source";

	/**
	 * The JPA variant of {@link #HBM2DDL_CREATE_NAMESPACES} used to specify whether database
	 * schemas used in the mapping model should be created on export in addition to creating
	 * the tables, sequences, etc.
	 * <p>
	 * The default is {@code false}, meaning to not create schemas
	 */
	String JAKARTA_HBM2DDL_CREATE_SCHEMAS = "jakarta.persistence.create-database-schemas";

	/**
	 * Used to specify the {@link org.hibernate.tool.schema.spi.SchemaFilterProvider} to be
	 * used by create, drop, migrate and validate operations on the database schema. A
	 * {@code SchemaFilterProvider} provides filters that can be used to limit the scope of
	 * these operations to specific namespaces, tables and sequences. All objects are
	 * included by default.
	 *
	 * @since 5.1
	 */
	String HBM2DDL_FILTER_PROVIDER = "hibernate.hbm2ddl.schema_filter_provider";

	/**
	 * Setting to choose the strategy used to access the JDBC Metadata.
	 * <p>
	 * Valid options are defined by {@link org.hibernate.tool.schema.JdbcMetadaAccessStrategy}.
	 * {@link org.hibernate.tool.schema.JdbcMetadaAccessStrategy#GROUPED} is the default.
	 *
	 * @see org.hibernate.tool.schema.JdbcMetadaAccessStrategy
	 */
	String HBM2DDL_JDBC_METADATA_EXTRACTOR_STRATEGY = "hibernate.hbm2ddl.jdbc_metadata_extraction_strategy";

	/**
	 * Identifies the delimiter to use to separate schema management statements in script
	 * outputs.
	 * <p>
	 * The default value is {@code ;}.
	 */
	String HBM2DDL_DELIMITER = "hibernate.hbm2ddl.delimiter";

	/**
	 * The name of the charset used by the schema generation resource.
	 * <p>
	 * By default, the JVM default charset is used.
	 *
	 * @since 5.2.3
	 */
	String HBM2DDL_CHARSET_NAME = "hibernate.hbm2ddl.charset_name";

	/**
	 * When enabled, specifies that the schema migration tool should halt on any error,
	 * terminating the bootstrap process.
	 *
	 * @since 5.2.4
	 */
	String HBM2DDL_HALT_ON_ERROR = "hibernate.hbm2ddl.halt_on_error";

	/**
	 * Used with the {@link jakarta.persistence.ConstraintMode#PROVIDER_DEFAULT}
	 * strategy for foreign key mapping.
	 * <p>
	 * Valid values are {@link jakarta.persistence.ConstraintMode#CONSTRAINT} and
	 * {@link jakarta.persistence.ConstraintMode#NO_CONSTRAINT}.
	 * <p>
	 * The default value is {@link jakarta.persistence.ConstraintMode#CONSTRAINT}.
	 *
	 * @since 5.4
	 */
	String HBM2DDL_DEFAULT_CONSTRAINT_MODE = "hibernate.hbm2ddl.default_constraint_mode";

	/**
	 * Setting to identify a {@link org.hibernate.CustomEntityDirtinessStrategy} to use.
	 * May specify either a class name or an instance.
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyCustomEntityDirtinessStrategy(CustomEntityDirtinessStrategy)
	 */
	String CUSTOM_ENTITY_DIRTINESS_STRATEGY = "hibernate.entity_dirtiness_strategy";

	/**
	 * The {@link org.hibernate.annotations.Where @Where} annotation specifies a
	 * restriction on the table rows which are visible as entity class instances or
	 * collection elements.
	 * <p>
	 * This setting controls whether the restriction applied to an entity should be
	 * applied to association fetches (for one-to-one, many-to-one, one-to-many, and
	 * many-to-many associations) which target the entity.
	 *
	 * @apiNote The setting is very misnamed - it applies across all entity associations,
	 *          not only to collections.
	 *
	 * @implSpec Enabled ({@code true}) by default, meaning the restriction is applied.
	 *           When this setting is explicitly disabled ({@code false}), the restriction
	 *           is not applied.
	 *
	 * @deprecated Originally added as a backwards compatibility flag
	 */
	@Remove @Deprecated( forRemoval = true, since = "6.2" )
	String USE_ENTITY_WHERE_CLAUSE_FOR_COLLECTIONS = "hibernate.use_entity_where_clause_for_collections";

	/**
	 * Specifies a {@link org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider}
	 * to use. Since {@code MultiTenantConnectionProvider} is also a service, it may be configured
	 * directly via the {@link org.hibernate.boot.registry.StandardServiceRegistryBuilder}.
	 *
	 * @since 4.1
	 */
	String MULTI_TENANT_CONNECTION_PROVIDER = "hibernate.multi_tenant_connection_provider";

	/**
	 * Specifies a {@link org.hibernate.context.spi.CurrentTenantIdentifierResolver} to use,
	 * either:
	 * <ul>
	 *     <li>an instance of {@code CurrentTenantIdentifierResolver},
	 *     <li>a {@link Class} representing an class that implements {@code CurrentTenantIdentifierResolver}, or
	 *     <li>the name of a class that implements {@code CurrentTenantIdentifierResolver}.
	 * </ul>
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyCurrentTenantIdentifierResolver(CurrentTenantIdentifierResolver)
	 *
	 * @since 4.1
	 */
	String MULTI_TENANT_IDENTIFIER_RESOLVER = "hibernate.tenant_identifier_resolver";

	/**
	 * Specifies an {@link org.hibernate.Interceptor} implementation associated with
	 * the {@link org.hibernate.SessionFactory} and propagated to each {@code Session}
	 * created from the {@code SessionFactory}. Either:
	 * <ul>
	 *     <li>an instance of {@code Interceptor},
	 *     <li>a {@link Class} representing a class that implements {@code Interceptor}, or
	 *     <li>the name of a class that implements {@code Interceptor}.
	 * </ul>
	 * <p>
	 * This setting identifies an {@code Interceptor} which is effectively a singleton
	 * across all the sessions opened from the {@code SessionFactory} to which it is
	 * applied; the same instance will be passed to each {@code Session}. If there
	 * should be a separate instance of {@code Interceptor} for each {@code Session},
	 * use {@link #SESSION_SCOPED_INTERCEPTOR} instead.
	 * 
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyInterceptor(Interceptor) 
	 *
	 * @since 5.0
	 */
	String INTERCEPTOR = "hibernate.session_factory.interceptor";

	/**
	 * Specifies an {@link org.hibernate.Interceptor} implementation associated with
	 * the {@link org.hibernate.SessionFactory} and propagated to each {@code Session}
	 * created from the {@code SessionFactory}. Either:
	 * <ul>
	 *     <li>a {@link Class} representing a class that implements {@code Interceptor},
	 *     <li>the name of a class that implements {@code Interceptor}, or
	 *     <li>an instance of {@link Supplier} used to obtain the interceptor.
	 * </ul>
	 * <p>
	 * Note that this setting cannot specify an {@code Interceptor} instance.
	 * <p>
	 * This setting identifies an {@code Interceptor} implementation that is to be
	 * applied to every {@code Session} opened from the {@code SessionFactory}, but
	 * unlike {@link #INTERCEPTOR}, a separate instance created for each {@code Session}.
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyStatelessInterceptor(Class)
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyStatelessInterceptor(Supplier)
	 * 
	 * @since 5.2
	 */
	String SESSION_SCOPED_INTERCEPTOR = "hibernate.session_factory.session_scoped_interceptor";

	/**
	 * Specifies a {@link org.hibernate.resource.jdbc.spi.StatementInspector}
	 * implementation associated with the {@link org.hibernate.SessionFactory},
	 * either:
	 * <ul>
	 *     <li>an instance of {@code StatementInspector},
	 *     <li>a {@link Class} representing an class that implements {@code StatementInspector}, or
	 *     <li>the name of a class that implements {@code StatementInspector}.
	 * </ul>
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyStatementInspector(StatementInspector)
	 *
	 * @since 5.0
	 */
	String STATEMENT_INSPECTOR = "hibernate.session_factory.statement_inspector";

	/**
	 * Allows a detached proxy or lazy collection to be fetched even when not
	 * associated with an open persistence context, by creating a temporary
	 * persistence context when the proxy or collection is accessed. This
	 * behavior is not recommended, since it can easily break transaction
	 * isolation or lead to data aliasing. It is therefore disabled by default.
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyLazyInitializationOutsideTransaction(boolean)
	 */
	String ENABLE_LAZY_LOAD_NO_TRANS = "hibernate.enable_lazy_load_no_trans";

	/**
	 * Specifies the {@link org.hibernate.loader.BatchFetchStyle} to use,
	 * either the name of a {code BatchFetchStyle} instance, or an instance
	 * of {@code BatchFetchStyle}.
	 *
	 * @deprecated An appropriate batch-fetch style is selected automatically
	 */
	@Deprecated(since = "6.0")
	@SuppressWarnings("DeprecatedIsStillUsed")
	String BATCH_FETCH_STYLE = "hibernate.batch_fetch_style";

	/**
	 * Controls how {@linkplain org.hibernate.loader.ast.spi.Loader entity loaders}
	 * are created.
	 * <p>
	 * When {@code true}, the default, the loaders are only created on first
	 * access; this ensures that all access patterns which are not useful
	 * to the application are never instantiated, possibly saving a
	 * substantial amount of memory for applications having many entities.
	 * The only exception is the loader for {@link org.hibernate.LockMode#NONE},
	 * which will always be eagerly initialized; this is necessary to
	 * detect mapping errors.
	 * <p>
	 * {@code false} indicates that all loaders should be created up front;
	 * this will consume more memory but ensures all necessary memory is
	 * allocated right away.
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyDelayedEntityLoaderCreations(boolean)
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
	 * <p>
	 * Default is {@code true} (enabled).
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyJtaTrackingByThread(boolean)
	 */
	String JTA_TRACK_BY_THREAD = "hibernate.jta.track_by_thread";

	/**
	 * If enabled, allows schema update and validation to support synonyms. Due
	 * to the possibility that this would return duplicate tables (especially in
	 * Oracle), this is disabled by default.
	 */
	String ENABLE_SYNONYMS = "hibernate.synonyms";

	/**
	 * Specifies a comma-separated list of extra table types, in addition to the
	 * default types {@code "TABLE"} and {@code "VIEW"}, to recognize as physical
	 * tables when performing schema update, creation and validation.
	 *
	 * @since 5.0
	 */
	String EXTRA_PHYSICAL_TABLE_TYPES = "hibernate.hbm2ddl.extra_physical_table_types";

	/**
	 * Unique columns and unique keys both use unique constraints in most dialects.
	 * The schema exporter must create these constraints, but database support for
	 * finding existing constraints is extremely inconsistent. Worse, unique constraints
	 * without explicit names are assigned names with randomly generated characters.
	 * <p>
	 * Therefore, select from these strategies:
	 * <ul>
	 *     <li>{@link org.hibernate.tool.schema.UniqueConstraintSchemaUpdateStrategy#DROP_RECREATE_QUIETLY
	 *         DROP_RECREATE_QUIETLY}:
	 *         Attempt to drop, then (re-)create each unique constraint,
	 *         ignoring any exceptions thrown.
	 *         This is the default.
	 *     <li>{@link org.hibernate.tool.schema.UniqueConstraintSchemaUpdateStrategy#RECREATE_QUIETLY
	 *         RECREATE_QUIETLY}:
	 *         attempt to (re-)create unique constraints,
	 *         ignoring exceptions thrown if the constraint already existed.
	 *     <li>{@link org.hibernate.tool.schema.UniqueConstraintSchemaUpdateStrategy#SKIP
	 *         SKIP}:
	 *         do not attempt to create unique constraints on a schema update.
	 * </ul>
	 */
	String UNIQUE_CONSTRAINT_SCHEMA_UPDATE_STRATEGY = "hibernate.schema_update.unique_constraint_strategy";

	/**
	 * When enabled, specifies that {@linkplain org.hibernate.stat.Statistics statistics}
	 * should be collected.
	 * 
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyStatisticsSupport(boolean) 
	 */
	String GENERATE_STATISTICS = "hibernate.generate_statistics";

	/**
	 * Controls whether {@linkplain org.hibernate.stat.SessionStatistics session metrics}
	 * should be {@linkplain org.hibernate.engine.internal.StatisticalLoggingSessionEventListener
	 * logged} for any session in which statistics are being collected.
	 * <p>
	 * By default, logging of session metrics is disabled unless {@link #GENERATE_STATISTICS}
	 * is enabled.
	 */
	String LOG_SESSION_METRICS = "hibernate.session.events.log";

	/**
	 * Specifies a duration in milliseconds defining the minimum query execution time that
	 * characterizes a "slow" query. Any SQL query which takes longer than this amount of
	 * time to execute will be logged.
	 * <p>
	 * A value of {@code 0}, the default, disables logging of "slow" queries.
	 */
	String LOG_SLOW_QUERY = "hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS";

	/**
	 * Defines a default {@link org.hibernate.SessionEventListener} to be applied to
	 * newly-opened {@link org.hibernate.Session}s.
	 */
	String AUTO_SESSION_EVENTS_LISTENER = "hibernate.session.events.auto";

	/**
	 * Enable instantiation of composite/embedded objects when all attribute values
	 * are {@code null}. The default (and historical) behavior is that a {@code null}
	 * reference will be used to represent the composite value when all of its
	 * attributes are {@code null}.
	 *
	 * @apiNote This is an experimental feature that has known issues. It should not
	 *          be used in production until it is stabilized. See Hibernate JIRA issue
	 *          HHH-11936 for details.
	 *
	 * @deprecated It makes no sense at all to enable this at the global level for a
	 *             persistence unit. If anything, it could be a setting specific to
	 *             a given embeddable class. But, four years after the introduction of
	 *             this feature, it's still marked experimental and has multiple known
	 *             unresolved bugs. It's therefore time for those who advocated for
	 *             this feature to accept defeat.
	 *
	 * @since 5.1
	 */
	@Incubating
	@Deprecated(since = "6")
	String CREATE_EMPTY_COMPOSITES_ENABLED = "hibernate.create_empty_composites.enabled";

	/**
	 * When enabled, allows access to the {@link org.hibernate.Transaction} even when
	 * using a JTA for transaction management.
	 * <p>
	 * Values are {@code true}, which grants access, and {@code false}, which does not.
	 * <p>
	 * The default behavior is to allow access unless Hibernate is bootstrapped via JPA.
	 */
	String ALLOW_JTA_TRANSACTION_ACCESS = "hibernate.jta.allowTransactionAccess";

	/**
	 * When enabled, allows update operations outside a transaction.
	 * <p>
	 * Since version 5.2 Hibernate conforms with the JPA specification and disallows
	 * flushing any update outside a transaction.
	 * <p>
	 * Values are {@code true}, which allows flushing outside a transaction, and
	 * {@code false}, which does not.
	 * <p>
	 * The default behavior is to disallow update operations outside a transaction.
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#allowOutOfTransactionUpdateOperations(boolean)
	 *
	 * @since 5.2
	 */
	String ALLOW_UPDATE_OUTSIDE_TRANSACTION = "hibernate.allow_update_outside_transaction";

	/**
	 * When enabled, allows calls to {@link jakarta.persistence.EntityManager#refresh(Object)}
	 * and {@link org.hibernate.Session#refresh(Object)} on a detached entity instance.
	 * <p>
	 * Values are {@code true}, which allows refreshing a detached instance and {@code false},
	 * which does not. When refreshing is disallowed, an {@link IllegalArgumentException}
	 * is thrown.
	 * <p>
	 * The default behavior is to allow refreshing a detached instance unless Hibernate
	 * is bootstrapped via JPA.
	 *
	 * @since 5.2
	 */
	String ALLOW_REFRESH_DETACHED_ENTITY = "hibernate.allow_refresh_detached_entity";

	/**
	 * Setting that specifies how Hibernate will respond when multiple representations of
	 * the same persistent entity ("entity copy") are detected while merging.
	 * <p>
	 * The possible values are:
	 * <ul>
	 *     <li>disallow (the default): throws {@link IllegalStateException} if an entity
	 *         copy is detected
	 *     <li>allow: performs the merge operation on each entity copy that is detected
	 *     <li>log: (provided for testing only) performs the merge operation on each entity
	 *         copy that is detected and logs information about the entity copies. This
	 *         setting requires DEBUG logging be enabled for
	 *         {@link org.hibernate.event.internal.EntityCopyAllowedLoggedObserver}.
	 * </ul>
	 * <p>
	 * Alternatively, the application may customize the behavior by providing an
	 * implementation of {@link org.hibernate.event.spi.EntityCopyObserver} and setting
	 * the property {@value #MERGE_ENTITY_COPY_OBSERVER} to the class name.
	 * <p>
	 * When this property is set to {@code allow} or {@code log}, Hibernate will merge
	 * each entity copy detected while cascading the merge operation. In the process of
	 * merging each entity copy, Hibernate will cascade the merge operation from each
	 * entity copy to its associations with {@link jakarta.persistence.CascadeType#MERGE}
	 * or {@link jakarta.persistence.CascadeType#ALL}. The entity state resulting from
	 * merging an entity copy will be overwritten when another entity copy is merged.
	 *
	 * @since 4.3
	 */
	@SuppressWarnings("JavaDoc")
	String MERGE_ENTITY_COPY_OBSERVER = "hibernate.event.merge.entity_copy_observer";

	/**
	 * By default, {@linkplain jakarta.persistence.criteria.CriteriaBuilder criteria}
	 * queries use bind parameters for any value passed via the JPA Criteria API.
	 * <ul>
	 *     <li>The {@link org.hibernate.query.criteria.ValueHandlingMode#BIND "bind"}
	 *     mode uses bind variables for any literal value.
	 *     <li>The {@link org.hibernate.query.criteria.ValueHandlingMode#INLINE "inline"}
	 *     mode inlines values as SQL literals.
	 * </ul>
	 * <p>
	 * The default value is {@link org.hibernate.query.criteria.ValueHandlingMode#BIND}.
	 *
	 * @since 6.0.0
	 *
	 * @see org.hibernate.query.criteria.ValueHandlingMode
	 */
	String CRITERIA_VALUE_HANDLING_MODE = "hibernate.criteria.value_handling_mode";

	/**
	 * When enabled, specifies that {@linkplain org.hibernate.query.Query queries}
	 * created through {@link jakarta.persistence.EntityManager#createQuery(CriteriaQuery)},
	 * {@link jakarta.persistence.EntityManager#createQuery(CriteriaUpdate)} or
	 * {@link jakarta.persistence.EntityManager#createQuery(CriteriaDelete)}
	 * must create a copy of the passed object such that the resulting {@link jakarta.persistence.Query}
	 * is not affected by any mutations to the original criteria query.
	 * <p>
	 * If disabled, it is assumed that users do not mutate the criteria query afterwards
	 * and due to that, no copy will be created, which will improve performance.
	 * <p>
	 * When bootstrapping Hibernate through the native bootstrap APIs this setting is disabled
	 * i.e. no copies are created to not hurt performance.
	 * When bootstrapping Hibernate through the JPA SPI this setting is enabled.
	 * When enabled, criteria query objects are copied, as required by the Jakarta Persistence specification.
	 *
	 * @since 6.0
	 */
	String CRITERIA_COPY_TREE = "hibernate.criteria.copy_tree";

	/**
	 * Specifies a default value for all {@link org.hibernate.jpa.spi.JpaCompliance}
	 * flags. Each individual flag may still be overridden by explicitly specifying
	 * its specific configuration property.
	 *
	 * @see #JPA_TRANSACTION_COMPLIANCE
	 * @see #JPA_QUERY_COMPLIANCE
	 * @see #JPA_LIST_COMPLIANCE
	 * @see #JPA_ORDER_BY_MAPPING_COMPLIANCE
	 * @see #JPA_CLOSED_COMPLIANCE
	 * @see #JPA_PROXY_COMPLIANCE
	 * @see #JPA_CACHING_COMPLIANCE
	 * @see #JPA_ID_GENERATOR_GLOBAL_SCOPE_COMPLIANCE
	 * @see #JPA_LOAD_BY_ID_COMPLIANCE
	 *
	 * @since 6.0
	 */
	String JPA_COMPLIANCE = "hibernate.jpa.compliance";

	/**
	 * When enabled, specifies that the Hibernate {@link org.hibernate.Transaction}
	 * should behave according to the semantics defined by the JPA specification for
	 * an {@link jakarta.persistence.EntityTransaction}.
	 *
	 * @see org.hibernate.jpa.spi.JpaCompliance#isJpaTransactionComplianceEnabled()
	 * @see org.hibernate.boot.SessionFactoryBuilder#enableJpaTransactionCompliance(boolean)
	 *
	 * @since 5.3
	 */
	String JPA_TRANSACTION_COMPLIANCE = "hibernate.jpa.compliance.transaction";

	/**
	 * When enabled, specifies that every {@linkplain org.hibernate.query.Query query}
	 * must strictly follow the specified behavior of {@link jakarta.persistence.Query}.
	 * The affects JPQL queries, criteria queries, and native SQL queries.
	 * <p>
	 * This setting modifies the behavior of the JPQL query translator, and of the
	 * {@code Query} interface itself. In particular, it forces all methods of
	 * {@code Query} to throw the exception types defined by the JPA specification.
	 * <p>
	 * If enabled, any deviations from the JPQL specification results in an exception.
	 * Therefore, this setting is not recommended, since it prohibits the use of many
	 * useful features of HQL.
	 *
	 * @see org.hibernate.jpa.spi.JpaCompliance#isJpaQueryComplianceEnabled()
	 * @see org.hibernate.boot.SessionFactoryBuilder#enableJpaQueryCompliance(boolean)
	 *
	 * @since 5.3
	 */
	String JPA_QUERY_COMPLIANCE = "hibernate.jpa.compliance.query";

	/**
	 * Controls whether Hibernate should treat what it would usually consider a
	 * {@linkplain org.hibernate.collection.spi.PersistentBag "bag"}, that is, a
	 * list with no index column, whose element order is not persistent, as a true
	 * {@link org.hibernate.collection.spi.PersistentList list} with an index column
	 * and a persistent element order.
	 * <p>
	 * If enabled, Hibernate will recognize it as a list where the
	 * {@link jakarta.persistence.OrderColumn} annotation is simply missing
	 * (and its defaults will apply).
	 *
	 * @see org.hibernate.jpa.spi.JpaCompliance#isJpaListComplianceEnabled()
	 * @see org.hibernate.boot.SessionFactoryBuilder#enableJpaListCompliance(boolean)
	 *
	 * @since 5.3
	 *
	 * @deprecated Use {@link #DEFAULT_LIST_SEMANTICS} instead. The specification
	 * actually leaves this behavior undefined, saying that portable applications
	 * should not rely on any specific behavior for a {@link java.util.List} with
	 * no {@code @OrderColumn}.
	 */
	@Deprecated( since = "6.0" )
	@SuppressWarnings("DeprecatedIsStillUsed")
	String JPA_LIST_COMPLIANCE	= "hibernate.jpa.compliance.list";

	/**
	 * JPA specifies that items occurring in {@link jakarta.persistence.OrderBy}
	 * lists must be references to entity attributes, whereas Hibernate, by default,
	 * allows more complex expressions.
	 * <p>
	 * If enabled, an exception is thrown for items which are not entity attribute
	 * references.
	 *
	 * @see org.hibernate.jpa.spi.JpaCompliance#isJpaOrderByMappingComplianceEnabled()
	 * @see org.hibernate.boot.SessionFactoryBuilder#enableJpaOrderByMappingCompliance(boolean)
	 *
	 * @since 6.0
	 */
	String JPA_ORDER_BY_MAPPING_COMPLIANCE	= "hibernate.jpa.compliance.orderby";

	/**
	 * JPA specifies that an {@link IllegalStateException} must be thrown by
	 * {@link jakarta.persistence.EntityManager#close()} and
	 * {@link jakarta.persistence.EntityManagerFactory#close()} if the object has
	 * already been closed. By default, Hibernate treats any additional call to
	 * {@code close()} as a noop.
	 * <p>
	 * When enabled, this setting forces Hibernate to throw an exception if
	 * {@code close()} is called on an instance that was already closed.
	 *
	 * @see org.hibernate.jpa.spi.JpaCompliance#isJpaClosedComplianceEnabled()
	 * @see org.hibernate.boot.SessionFactoryBuilder#enableJpaClosedCompliance(boolean)
	 *
	 * @since 5.3
	 */
	String JPA_CLOSED_COMPLIANCE = "hibernate.jpa.compliance.closed";

	/**
	 * The JPA specification insists that an
	 * {@link jakarta.persistence.EntityNotFoundException} must be thrown whenever
	 * an uninitialized entity proxy with no corresponding row in the database is
	 * accessed. For most programs, this results in many completely unnecessary
	 * round trips to the database.
	 * <p>
	 * Traditionally, Hibernate does not initialize an entity proxy when its
	 * identifier attribute is accessed, since the identifier value is already
	 * known and held in the proxy instance. This behavior saves the round trip
	 * to the database.
	 * <p>
	 * When enabled, this setting forces Hibernate to initialize the entity proxy
	 * when its identifier is accessed. Clearly, this setting is not recommended.
	 *
	 * @see org.hibernate.jpa.spi.JpaCompliance#isJpaProxyComplianceEnabled()
	 *
	 * @since 5.2.13
	 */
	String JPA_PROXY_COMPLIANCE = "hibernate.jpa.compliance.proxy";

	/**
	 * By default, Hibernate uses second-level cache invalidation for entities
	 * with {@linkplain jakarta.persistence.SecondaryTable secondary tables}
	 * in order to avoid the possibility of inconsistent cached data in the
	 * case where different transactions simultaneously update different table
	 * rows corresponding to the same entity instance.
	 * <p>
	 * The JPA TCK, for no good reason, requires that entities with secondary
	 * tables be immediately cached in the second-level cache rather than
	 * invalidated and re-cached on a subsequent read.
	 * <p>
	 * Note that Hibernate's default behavior here is safer and more careful
	 * than the behavior mandated by the TCK but YOLO.
	 * <p>
	 * When enabled, this setting makes Hibernate pass the TCK.
	 *
	 * @see org.hibernate.jpa.spi.JpaCompliance#isJpaCacheComplianceEnabled()
	 * @see org.hibernate.persister.entity.AbstractEntityPersister#isCacheInvalidationRequired()
	 *
	 * @since 5.3
	 */
	String JPA_CACHING_COMPLIANCE = "hibernate.jpa.compliance.caching";

	/**
	 * Determines whether the scope of any identifier generator name specified
	 * via {@link jakarta.persistence.TableGenerator#name()} or
	 * {@link jakarta.persistence.SequenceGenerator#name()} is considered global
	 * to the persistence unit, or local to the entity in which identifier generator
	 * is defined.
	 * <p>
	 * If enabled, the name will be considered globally scoped, and so the existence
	 * of two different generators with the same name will be considered a collision,
	 * and will result in an exception during bootstrap.
	 *
	 * @see org.hibernate.jpa.spi.JpaCompliance#isGlobalGeneratorScopeEnabled()
	 *
	 * @since 5.2.17
	 */
	String JPA_ID_GENERATOR_GLOBAL_SCOPE_COMPLIANCE = "hibernate.jpa.compliance.global_id_generators";

	/**
	 * Determines if an identifier value passed to
	 * {@link jakarta.persistence.EntityManager#find} or
	 * {@link jakarta.persistence.EntityManager#getReference} may be
	 * {@linkplain  org.hibernate.type.descriptor.java.JavaType#coerce coerced} to
	 * the identifier type declared by the entity. For example, an {@link Integer}
	 * argument might be widened to {@link Long}.
	 * <p>
	 * By default, coercion is allowed. When enabled, coercion is disallowed, as
	 * required by the JPA specification.
	 *
	 * @see org.hibernate.jpa.spi.JpaCompliance#isLoadByIdComplianceEnabled()
	 *
	 * @since 6.0
	 */
	String JPA_LOAD_BY_ID_COMPLIANCE = "hibernate.jpa.compliance.load_by_id";

	/**
	 * Determines if the identifier value stored in the database table backing a
	 * {@linkplain jakarta.persistence.TableGenerator table generator} is the last
	 * value returned by the identifier generator, or the next value to be returned.
	 * <p>
	 * By default, the value stored in the database table is the last generated value.
	 *
	 * @since 5.3
	 */
	String TABLE_GENERATOR_STORE_LAST_USED = "hibernate.id.generator.stored_last_used";

	/**
	 * When {@linkplain org.hibernate.query.Query#setMaxResults(int) pagination} is used
	 * in combination with a {@code fetch join} applied to a collection or many-valued
	 * association, the limit must be applied in-memory instead of on the database. This
	 * typically has terrible performance characteristics, and should be avoided.
	 * <p>
	 * When enabled, this setting specifies that an exception should be thrown for any
	 * query which would result in the limit being applied in-memory.
	 * <p>
	 * By default, the exception is <em>disabled</em>, and the possibility of terrible
	 * performance is left as a problem for the client to avoid.
	 *
	 * @since 5.2.13
	 */
	String FAIL_ON_PAGINATION_OVER_COLLECTION_FETCH = "hibernate.query.fail_on_pagination_over_collection_fetch";

	/**
	 * This setting defines how {@link org.hibernate.annotations.Immutable} entities
	 * are handled when executing a bulk update query. Valid options are enumerated
	 * by {@link org.hibernate.query.ImmutableEntityUpdateQueryHandlingMode}:
	 * <ul>
	 *     <li>{@link org.hibernate.query.ImmutableEntityUpdateQueryHandlingMode#WARNING "warning"}
	 *     specifies that a warning log message is issued when an
	 *     {@linkplain org.hibernate.annotations.Immutable immutable} entity is to be
	 *     updated via a bulk update statement, and
	 *     <li>{@link org.hibernate.query.ImmutableEntityUpdateQueryHandlingMode#EXCEPTION "exception"}
	 *     specifies that a {@link org.hibernate.HibernateException} should be thrown.
	 * </ul>
	 * <p>
	 * By default, a warning is logged.
	 *
	 * @since 5.2.17
	 *
	 * @see org.hibernate.query.ImmutableEntityUpdateQueryHandlingMode
	 */
	String IMMUTABLE_ENTITY_UPDATE_QUERY_HANDLING_MODE = "hibernate.query.immutable_entity_update_query_handling_mode";

	/**
	 * Determines how parameters occurring in a SQL {@code IN} predicate are expanded.
	 * By default, the {@code IN} predicate expands to include sufficient bind parameters
	 * to accommodate the specified arguments.
	 * <p>
	 * However, for database systems supporting execution plan caching, there's a
	 * better chance of hitting the cache if the number of possible {@code IN} clause
	 * parameter list lengths is smaller.
	 * <p>
	 * When this setting is enabled, we expand the number of bind parameters to an
	 * integer power of two: 4, 8, 16, 32, 64. Thus, if 5, 6, or 7 arguments are bound
	 * to a parameter, a SQL statement with 8 bind parameters in the {@code IN} clause
	 * will be used, and null will be bound to the left-over parameters.
	 *
	 * @since 5.2.17
	 */
	String IN_CLAUSE_PARAMETER_PADDING = "hibernate.query.in_clause_parameter_padding";

	/**
	 * This setting controls the number of {@link org.hibernate.stat.QueryStatistics}
	 * entries that will be stored by the Hibernate {@link org.hibernate.stat.Statistics}
	 * object.
	 * <p>
	 * The default value is {@value org.hibernate.stat.Statistics#DEFAULT_QUERY_STATISTICS_MAX_SIZE}.
	 *
	 * @since 5.4
	 */
	String QUERY_STATISTICS_MAX_SIZE = "hibernate.statistics.query_max_size";

	/**
	 * This setting defines the {@link org.hibernate.id.SequenceMismatchStrategy} used
	 * when Hibernate detects a mismatch between a sequence configuration in an entity
	 * mapping and its database sequence object counterpart.
	 * <p>
	 * Possible values are {@link org.hibernate.id.SequenceMismatchStrategy#EXCEPTION},
	 * {@link org.hibernate.id.SequenceMismatchStrategy#LOG},
	 * {@link org.hibernate.id.SequenceMismatchStrategy#FIX}
	 * and {@link org.hibernate.id.SequenceMismatchStrategy#NONE}.
	 * <p>
	 * The default value is {@link org.hibernate.id.SequenceMismatchStrategy#EXCEPTION},
	 * meaning that an exception is thrown when such a conflict is detected.
	 *
	 * @since 5.4
	 */
	String SEQUENCE_INCREMENT_SIZE_MISMATCH_STRATEGY = "hibernate.id.sequence.increment_size_mismatch_strategy";

	/**
	 * Specifies the preferred JDBC type for storing boolean values. When no
	 * type is explicitly specified, a sensible
	 * {@link org.hibernate.dialect.Dialect#getPreferredSqlTypeCodeForBoolean()
	 * dialect-specific default type code} is used.
	 * <p>
	 * Can be overridden locally using {@link org.hibernate.annotations.JdbcType},
	 * {@link org.hibernate.annotations.JdbcTypeCode}, and friends.
	 * <p>
	 * Can also specify the name of the {@link org.hibernate.type.SqlTypes} constant
	 * field, for example, {@code hibernate.type.preferred_boolean_jdbc_type=BIT}.
	 *
	 * @since 6.0
	 */
	@Incubating
	String PREFERRED_BOOLEAN_JDBC_TYPE = "hibernate.type.preferred_boolean_jdbc_type";

	/**
	 * The preferred JDBC type to use for storing {@link java.util.UUID} values.
	 * Defaults to {@link org.hibernate.type.SqlTypes#UUID}.
	 * <p>
	 * Can be overridden locally using {@link org.hibernate.annotations.JdbcType},
	 * {@link org.hibernate.annotations.JdbcTypeCode}, and friends.
	 * <p>
	 * Can also specify the name of the {@link org.hibernate.type.SqlTypes} constant
	 * field, for example, {@code hibernate.type.preferred_uuid_jdbc_type=CHAR}.
	 *
	 * @since 6.0
	 */
	@Incubating
	String PREFERRED_UUID_JDBC_TYPE = "hibernate.type.preferred_uuid_jdbc_type";

	/**
	 * The preferred JDBC type to use for storing {@link java.time.Duration} values.
	 * Defaults to {@link org.hibernate.type.SqlTypes#NUMERIC}.
	 * <p>
	 * Can be overridden locally using {@link org.hibernate.annotations.JdbcType},
	 * {@link org.hibernate.annotations.JdbcTypeCode}, and friends.
	 * <p>
	 * Can also specify the name of the {@link org.hibernate.type.SqlTypes} constant
	 * field, for example, {@code hibernate.type.preferred_duration_jdbc_type=INTERVAL_SECOND}.
	 *
	 * @since 6.0
	 */
	@Incubating
	String PREFERRED_DURATION_JDBC_TYPE = "hibernate.type.preferred_duration_jdbc_type";

	/**
	 * Specifies the preferred JDBC type for storing {@link java.time.Instant} values.
	 * Defaults to {@link org.hibernate.type.SqlTypes#TIMESTAMP_UTC}.
	 * is used.
	 * <p>
	 * Can be overridden locally using {@link org.hibernate.annotations.JdbcType},
	 * {@link org.hibernate.annotations.JdbcTypeCode}, and friends.
	 * <p>
	 * Can also specify the name of the {@link org.hibernate.type.SqlTypes} constant
	 * field, for example, {@code hibernate.type.preferred_instant_jdbc_type=TIMESTAMP}.
	 *
	 * @since 6.0
	 */
	@Incubating
	String PREFERRED_INSTANT_JDBC_TYPE = "hibernate.type.preferred_instant_jdbc_type";

	/**
	 * Specifies a {@link org.hibernate.type.format.FormatMapper} used for JSON
	 * serialization and deserialization, either:
	 * <ul>
	 *     <li>an instance of {@code FormatMapper},
	 *     <li>a {@link Class} representing a class that implements {@code FormatMapper},
	 *     <li>the name of a class that implements {@code FormatMapper}, or
	 *     <li>one of the shorthand constants {@code jackson} or {@code jsonb}.
	 * </ul>
	 * <p>
	 * By default, the first of the possible providers that is available at runtime is
	 * used, according to the listing order.
	 *
	 * @since 6.0
	 */
	@Incubating
	String JSON_FORMAT_MAPPER = "hibernate.type.json_format_mapper";

	/**
	 * Specifies a {@link org.hibernate.type.format.FormatMapper} used for XML
	 * serialization and deserialization, either:
	 * <ul>
	 *     <li>an instance of {@code FormatMapper},
	 *     <li>a {@link Class} representing a class that implements {@code FormatMapper},
	 *     <li>the name of a class that implements {@code FormatMapper}, or
	 *     <li>one of the shorthand constants {@code jackson} or {@code jaxb}.
	 * </ul>
	 * <p>
	 * By default, the first of the possible providers that is available at runtime is
	 * used, according to the listing order.
	 *
	 * @since 6.0.1
	 */
	@Incubating
	String XML_FORMAT_MAPPER = "hibernate.type.xml_format_mapper";

	/**
	 * Configurable control over how to handle {@code Byte[]} and {@code Character[]} types
	 * encountered in the application domain model.  Allowable semantics are defined by
	 * {@link WrapperArrayHandling}.  Accepted values include:<ol>
	 *     <li>{@link WrapperArrayHandling} instance</li>
	 *     <li>case-insensitive name of a {@link WrapperArrayHandling} instance (e.g. {@code allow})</li>
	 * </ol>
	 *
	 * @since 6.2
	 */
	@Incubating
	String WRAPPER_ARRAY_HANDLING = "hibernate.type.wrapper_array_handling";

	/**
	 * Specifies the default strategy for storage of the timezone information for the zoned
	 * datetime types {@link java.time.OffsetDateTime} and {@link java.time.ZonedDateTime}.
	 * The possible options for this setting are enumerated by
	 * {@link org.hibernate.annotations.TimeZoneStorageType}.
	 * <p>
	 * The default is {@link org.hibernate.annotations.TimeZoneStorageType#DEFAULT DEFAULT},
	 * which guarantees that the {@linkplain java.time.OffsetDateTime#toInstant() instant}
	 * represented by a zoned datetime type is preserved by a round trip to the database.
	 * It does <em>not</em> guarantee that the time zone or offset is preserved.
	 * <p>
	 * For backward compatibility with older versions of Hibernate, set this property to
	 * {@link org.hibernate.annotations.TimeZoneStorageType#NORMALIZE NORMALIZE}.
	 * <p>
	 * The default strategy specified using this setting may be overridden using the
	 * annotation {@link org.hibernate.annotations.TimeZoneStorage}.
	 *
	 * @see org.hibernate.annotations.TimeZoneStorageType
	 * @see org.hibernate.annotations.TimeZoneStorage
	 *
	 * @since 6.0
	 */
	String TIMEZONE_DEFAULT_STORAGE = "hibernate.timezone.default_storage";

	/**
	 * Controls whether to use JDBC markers (`?`) or dialect native markers for parameters
	 * within {@linkplain java.sql.PreparedStatement preparable} SQL statements.
	 *
	 * @implNote {@code False} by default, indicating standard JDBC parameter markers (`?`)
	 * are used.  Set to {@code true} to use the Dialect's native markers, if any.  For
	 * Dialects without native markers, the standard JDBC strategy is used.
	 *
	 * @see ParameterMarkerStrategy
	 * @see org.hibernate.dialect.Dialect#getNativeParameterMarkerStrategy()
	 *
	 * @since 6.2
	 */
	@Incubating
	String DIALECT_NATIVE_PARAM_MARKERS = "hibernate.dialect.native_param_markers";


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Java (javax) Persistence defined settings
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Specifies a class implementing {@link jakarta.persistence.spi.PersistenceProvider}.
	 * <p>
	 * See JPA 2 sections 9.4.3 and 8.2.1.4
	 *
	 * @deprecated Use {@link #JAKARTA_PERSISTENCE_PROVIDER} instead
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String JPA_PERSISTENCE_PROVIDER = "javax.persistence.provider";

	/**
	 * The type of transactions supported by the entity managers.
	 * <p>
	 * See JPA 2 sections 9.4.3 and 8.2.1.2
	 *
	 * @deprecated Use {@link #JAKARTA_TRANSACTION_TYPE} instead
	 */
	@Deprecated
	String JPA_TRANSACTION_TYPE = "javax.persistence.transactionType";

	/**
	 * The JNDI name of a JTA {@link javax.sql.DataSource}.
	 * <p>
	 * See JPA 2 sections 9.4.3 and 8.2.1.5
	 *
	 * @deprecated Use {@link #JAKARTA_JTA_DATASOURCE} instead
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String JPA_JTA_DATASOURCE = "javax.persistence.jtaDataSource";

	/**
	 * The JNDI name of a non-JTA {@link javax.sql.DataSource}.
	 * <p>
	 * See JPA 2 sections 9.4.3 and 8.2.1.5
	 *
	 * @deprecated Use {@link #JAKARTA_NON_JTA_DATASOURCE} instead
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String JPA_NON_JTA_DATASOURCE = "javax.persistence.nonJtaDataSource";

	/**
	 * The name of a JDBC driver to use to connect to the database.
	 * <p>
	 * Used in conjunction with {@link #JPA_JDBC_URL}, {@link #JPA_JDBC_USER} and
	 * {@link #JPA_JDBC_PASSWORD} to specify how to connect to the database.
	 * <p>
	 * When connections are obtained from a {@link javax.sql.DataSource}, use either
	 * {@link #JPA_JTA_DATASOURCE} or {@link #JPA_NON_JTA_DATASOURCE} instead.
	 * <p>
	 * See section 8.2.1.9
	 *
	 * @deprecated Use {@link #JAKARTA_JDBC_DRIVER} instead
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String JPA_JDBC_DRIVER = "javax.persistence.jdbc.driver";

	/**
	 * The JDBC connection URL to use to connect to the database.
	 * <p>
	 * Used in conjunction with {@link #JPA_JDBC_DRIVER}, {@link #JPA_JDBC_USER} and
	 * {@link #JPA_JDBC_PASSWORD} to specify how to connect to the database.
	 * <p>
	 * When connections are obtained from a {@link javax.sql.DataSource}, use either
	 * {@link #JPA_JTA_DATASOURCE} or {@link #JPA_NON_JTA_DATASOURCE} instead.
	 * <p>
	 * See section 8.2.1.9
	 *
	 * @deprecated Use {@link #JAKARTA_JDBC_URL} instead
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String JPA_JDBC_URL = "javax.persistence.jdbc.url";

	/**
	 * The database user to use when connecting via JDBC.
	 * <p>
	 * Used in conjunction with {@link #JPA_JDBC_DRIVER}, {@link #JPA_JDBC_URL} and
	 * {@link #JPA_JDBC_PASSWORD} to specify how to connect to the database.
	 * <p>
	 * See section 8.2.1.9
	 *
	 * @deprecated Use {@link #JAKARTA_JDBC_USER} instead
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String JPA_JDBC_USER = "javax.persistence.jdbc.user";

	/**
	 * The password to use when connecting via JDBC.
	 * <p>
	 * Used in conjunction with {@link #JPA_JDBC_DRIVER}, {@link #JPA_JDBC_URL} and
	 * {@link #JPA_JDBC_USER} to specify how to connect to the database.
	 * <p>
	 * See JPA 2 section 8.2.1.9
	 *
	 * @deprecated Use {@link #JAKARTA_JDBC_PASSWORD} instead
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String JPA_JDBC_PASSWORD = "javax.persistence.jdbc.password";

	/**
	 * Used to indicate whether second-level (what JPA terms shared cache)
	 * caching is enabled as per the rules defined in JPA 2 section 3.1.7.
	 * <p>
	 * See JPA 2 sections 9.4.3 and 8.2.1.7
	 * @see jakarta.persistence.SharedCacheMode
	 *
	 * @deprecated Use {@link #JAKARTA_SHARED_CACHE_MODE} instead
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String JPA_SHARED_CACHE_MODE = "javax.persistence.sharedCache.mode";

	/**
	 * Used to indicate if the provider should attempt to retrieve requested
	 * data in the shared cache.
	 *
	 * @see jakarta.persistence.CacheRetrieveMode
	 *
	 * @deprecated Use {@link #JAKARTA_SHARED_CACHE_RETRIEVE_MODE} instead
	 *
	 * @apiNote This is not a legal property for an {@code EntityManagerFactory}.
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String JPA_SHARED_CACHE_RETRIEVE_MODE = "javax.persistence.cache.retrieveMode";

	/**
	 * Used to indicate if the provider should attempt to store data loaded from the database
	 * in the shared cache.
	 *
	 * @see jakarta.persistence.CacheStoreMode
	 *
	 * @deprecated Use {@link #JAKARTA_SHARED_CACHE_STORE_MODE} instead
	 *
	 * @apiNote This is not a legal property for an {@code EntityManagerFactory}.
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String JPA_SHARED_CACHE_STORE_MODE = "javax.persistence.cache.storeMode";

	/**
	 * Used to indicate what form of automatic validation is in effect as
	 * per rules defined in JPA 2 section 3.6.1.1.
	 * <p>
	 * See JPA 2 sections 9.4.3 and 8.2.1.8
	 *
	 * @see jakarta.persistence.ValidationMode
	 *
	 * @deprecated Use {@link #JAKARTA_VALIDATION_MODE} instead
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String JPA_VALIDATION_MODE = "javax.persistence.validation.mode";

	/**
	 * Used to pass along any discovered validator factory.
	 *
	 * @deprecated Use {@link #JAKARTA_VALIDATION_FACTORY} instead
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String JPA_VALIDATION_FACTORY = "javax.persistence.validation.factory";

	/**
	 * Used to coordinate with bean validators.
	 * <p>
	 * See JPA 2 section 8.2.1.9
	 *
	 * @deprecated Use {@link #JAKARTA_PERSIST_VALIDATION_GROUP} instead
	 */
	@Deprecated
	String JPA_PERSIST_VALIDATION_GROUP = "javax.persistence.validation.group.pre-persist";

	/**
	 * Used to coordinate with bean validators.
	 * <p>
	 * See JPA 2 section 8.2.1.9
	 *
	 * @deprecated Use {@link #JAKARTA_UPDATE_VALIDATION_GROUP} instead
	 */
	@Deprecated
	String JPA_UPDATE_VALIDATION_GROUP = "javax.persistence.validation.group.pre-update";

	/**
	 * Used to coordinate with bean validators.
	 * <p>
	 * See JPA 2 section 8.2.1.9
	 *
	 * @deprecated Use {@link #JAKARTA_REMOVE_VALIDATION_GROUP} instead
	 */
	@Deprecated
	String JPA_REMOVE_VALIDATION_GROUP = "javax.persistence.validation.group.pre-remove";

	/**
	 * Used to request (hint) a pessimistic lock scope.
	 * <p>
	 * See JPA 2 sections 8.2.1.9 and 3.4.4.3
	 *
	 * @deprecated Use {@link #JAKARTA_LOCK_SCOPE} instead
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String JPA_LOCK_SCOPE = LegacySpecHints.HINT_JAVAEE_LOCK_SCOPE;

	/**
	 * Used to request (hint) a pessimistic lock timeout (in milliseconds).
	 * <p>
	 * See JPA 2 sections 8.2.1.9 and 3.4.4.3
	 *
	 * @deprecated Use {@link #JAKARTA_LOCK_TIMEOUT} instead
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String JPA_LOCK_TIMEOUT = LegacySpecHints.HINT_JAVAEE_LOCK_TIMEOUT;

	/**
	 * Used to pass a CDI {@link jakarta.enterprise.inject.spi.BeanManager} to
	 * Hibernate.
	 * <p>
	 * According to the JPA specification, the {@code BeanManager} should be
	 * passed at boot time and be ready for immediate use at that time. But
	 * not all environments can do this (WildFly, for example). To accommodate
	 * such environments, Hibernate provides two options:
	 * <ol>
	 * <li>A proprietary CDI extension SPI (which has been proposed to the CDI
	 * spec group as a standard option) which can be used to provide delayed
	 * {@code BeanManager} access: to use this solution, the reference passed
	 * as the {@code BeanManager} during bootstrap should be typed as
	 * {@link org.hibernate.resource.beans.container.spi.ExtendedBeanManager}.
	 * <li>Delayed access to the {@code BeanManager} reference: here, Hibernate
	 * will not access the reference passed as the {@code BeanManager} during
	 * bootstrap until it is first needed. Note, however, that this has the
	 * effect of delaying the detection of any deployment problems until after
	 * bootstrapping.
	 * </ol>
	 * This setting is used to configure access to the {@code BeanManager},
	 * either directly, or via
	 * {@link org.hibernate.resource.beans.container.spi.ExtendedBeanManager}.
	 *
	 * @deprecated Use {@link #JAKARTA_CDI_BEAN_MANAGER} instead
	 */
	@Deprecated
	String CDI_BEAN_MANAGER = "javax.persistence.bean.manager";


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// pulled from jpa.AvailableSettings

	/**
	 * Used to determine flush mode.
	 *
	 * @deprecated There are much better ways to control the flush mode of a session,
	 *             for example, {@link org.hibernate.SessionBuilder#flushMode} or
	 *             {@link org.hibernate.Session#setHibernateFlushMode}.
	 *
	 * @see org.hibernate.jpa.HibernateHints#HINT_FLUSH_MODE
	 */
	@Deprecated(since = "6.2", forRemoval = true)
	@SuppressWarnings("DeprecatedIsStillUsed")
	String FLUSH_MODE = "org.hibernate.flushMode";

	String CFG_XML_FILE = "hibernate.cfg_xml_file";
	String ORM_XML_FILES = "hibernate.orm_xml_files";
	String HBM_XML_FILES = "hibernate.hbm_xml_files";
	String LOADED_CLASSES = "hibernate.loaded_classes";

	/**
	 * Event listener configuration properties follow the pattern
	 * {@code hibernate.event.listener.eventType packageName.ClassName1, packageName.ClassName2}
	 */
	String EVENT_LISTENER_PREFIX = "hibernate.event.listener";

	/**
	 * Entity cache configuration properties follow the pattern
	 * {@code hibernate.classcache.packagename.ClassName usage[, region]}
	 * where {@code usage} is the cache strategy used and {@code region} the cache region name
	 */
	String CLASS_CACHE_PREFIX = "hibernate.classcache";

	/**
	 * Collection cache configuration properties follow the pattern
	 * {@code hibernate.collectioncache.packagename.ClassName.role usage[, region]}
	 * where {@code usage} is the cache strategy used and {@code region} the cache region name
	 */
	String COLLECTION_CACHE_PREFIX = "hibernate.collectioncache";

	/**
	 * Enable dirty tracking feature in runtime bytecode enhancement
	 *
	 * @deprecated Will be removed without replacement. See HHH-15641
	 */
	@Deprecated(forRemoval = true)
	@SuppressWarnings("DeprecatedIsStillUsed")
	String ENHANCER_ENABLE_DIRTY_TRACKING = "hibernate.enhancer.enableDirtyTracking";

	/**
	 * Enable lazy loading feature in runtime bytecode enhancement
	 *
	 * @deprecated Will be removed without replacement. See HHH-15641
	 */
	@SuppressWarnings("DeprecatedIsStillUsed")
	@Deprecated(forRemoval = true)
	String ENHANCER_ENABLE_LAZY_INITIALIZATION = "hibernate.enhancer.enableLazyInitialization";

	/**
	 * Enable association management feature in runtime bytecode enhancement
	 */
	String ENHANCER_ENABLE_ASSOCIATION_MANAGEMENT = "hibernate.enhancer.enableAssociationManagement";

	/**
	 * Specifies the name of the persistence unit.
	 */
	String PERSISTENCE_UNIT_NAME = "hibernate.persistenceUnitName";

	/**
	 * Specifies a class which implements {@link org.hibernate.SessionFactoryObserver} and has
	 * a constructor with no parameters.
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#addSessionFactoryObservers(SessionFactoryObserver...)
	 */
	String SESSION_FACTORY_OBSERVER = "hibernate.session_factory_observer";

	/**
	 * Specifies a class which implements {@link org.hibernate.jpa.spi.IdentifierGeneratorStrategyProvider},
	 * and has a constructor with no parameters.
	 *
	 * @deprecated use {@link org.hibernate.id.factory.spi.GenerationTypeStrategyRegistration} instead
	 */
	@Deprecated(since = "6.0")
	@SuppressWarnings("DeprecatedIsStillUsed")
	String IDENTIFIER_GENERATOR_STRATEGY_PROVIDER = "hibernate.identifier_generator_strategy_provider";

	/**
	 * When enabled, specifies that the persistent context should be discarded when either
	 * {@link org.hibernate.Session#close()} or {@link jakarta.persistence.EntityManager#close()}
	 * is called.
	 * <p>
	 * By default, the persistent context is not discarded, as per the JPA specification.
	 */
	String DISCARD_PC_ON_CLOSE = "hibernate.discard_pc_on_close";

	/**
	 * Whether XML should be validated against their schema as Hibernate reads them.
	 * <p>
	 * Default is {@code true}
	 *
	 * @since 6.1
	 */
	String VALIDATE_XML = "hibernate.validate_xml";

	/**
	 * Enables processing {@code hbm.xml} mappings by transforming them to {@code mapping.xml} and using
	 * that processor.  Default is false, must be opted-into.
	 *
	 * @since 6.1
	 */
	String TRANSFORM_HBM_XML = "hibernate.transform_hbm_xml.enabled";

	/**
	 * How features in a {@code hbm.xml} file which are not supported for transformation should be handled.
	 * <p>
	 * Default is {@link org.hibernate.boot.jaxb.hbm.transform.UnsupportedFeatureHandling#ERROR}
	 *
	 * @see org.hibernate.boot.jaxb.hbm.transform.UnsupportedFeatureHandling
	 * @since 6.1
	 */
	String TRANSFORM_HBM_XML_FEATURE_HANDLING = "hibernate.transform_hbm_xml.unsupported_feature_handling";

	/**
	 * Allows creation of {@linkplain org.hibernate.dialect.temptable.TemporaryTableKind#PERSISTENT persistent}
	 * temporary tables at application startup to be disabled. By default, table creation is enabled.
	 */
	String BULK_ID_STRATEGY_PERSISTENT_TEMPORARY_CREATE_TABLES = PersistentTableStrategy.CREATE_ID_TABLES;
	/**
	 * Allows dropping of {@linkplain org.hibernate.dialect.temptable.TemporaryTableKind#PERSISTENT persistent}
	 * temporary tables at application shutdown to be disabled. By default, table dropping is enabled.
	 */
	String BULK_ID_STRATEGY_PERSISTENT_TEMPORARY_DROP_TABLES = PersistentTableStrategy.DROP_ID_TABLES;
	/**
	 * Allows creation of {@linkplain org.hibernate.dialect.temptable.TemporaryTableKind#GLOBAL global}
	 * temporary tables at application startup to be disabled. By default, table creation is enabled.
	 */
	String BULK_ID_STRATEGY_GLOBAL_TEMPORARY_CREATE_TABLES = GlobalTemporaryTableStrategy.CREATE_ID_TABLES;
	/**
	 * Allows dropping of {@linkplain org.hibernate.dialect.temptable.TemporaryTableKind#GLOBAL global}
	 * temporary tables at application shutdown to be disabled. By default, table dropping is enabled.
	 */
	String BULK_ID_STRATEGY_GLOBAL_TEMPORARY_DROP_TABLES = GlobalTemporaryTableStrategy.DROP_ID_TABLES;
	/**
	 * Allows dropping of {@linkplain org.hibernate.dialect.temptable.TemporaryTableKind#LOCAL local}
	 * temporary tables at transaction commit to be enabled. By default, table dropping is disabled,
	 * and the database will drop the temporary tables automatically.
	 */
	String BULK_ID_STRATEGY_LOCAL_TEMPORARY_DROP_TABLES = LocalTemporaryTableStrategy.DROP_ID_TABLES;
}
