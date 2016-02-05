/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa;

/**
 * Defines the available HEM settings, both JPA-defined as well as Hibernate-specific
 * <p/>
 * NOTE : Does *not* include {@link org.hibernate.cfg.Environment} values.
 *
 * @author Steve Ebersole
 */
public interface AvailableSettings {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA defined settings
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * THe name of the {@link javax.persistence.spi.PersistenceProvider} implementor
	 * <p/>
	 * See JPA 2 sections 9.4.3 and 8.2.1.4
	 */
	String PROVIDER = "javax.persistence.provider";

	/**
	 * The type of transactions supported by the entity managers.
	 * <p/>
	 * See JPA 2 sections 9.4.3 and 8.2.1.2
	 */
	String TRANSACTION_TYPE = "javax.persistence.transactionType";

	/**
	 * The JNDI name of a JTA {@link javax.sql.DataSource}.
	 * <p/>
	 * See JPA 2 sections 9.4.3 and 8.2.1.5
	 */
	String JTA_DATASOURCE = "javax.persistence.jtaDataSource";

	/**
	 * The JNDI name of a non-JTA {@link javax.sql.DataSource}.
	 * <p/>
	 * See JPA 2 sections 9.4.3 and 8.2.1.5
	 */
	String NON_JTA_DATASOURCE = "javax.persistence.nonJtaDataSource";

	/**
	 * The name of a JDBC driver to use to connect to the database.
	 * <p/>
	 * Used in conjunction with {@link #JDBC_URL}, {@link #JDBC_USER} and {@link #JDBC_PASSWORD}
	 * to define how to make connections to the database in lieu of
	 * a datasource (either {@link #JTA_DATASOURCE} or {@link #NON_JTA_DATASOURCE}).
	 * <p/>
	 * See section 8.2.1.9
	 */
	String JDBC_DRIVER = "javax.persistence.jdbc.driver";

	/**
	 * The JDBC connection url to use to connect to the database.
	 * <p/>
	 * Used in conjunction with {@link #JDBC_DRIVER}, {@link #JDBC_USER} and {@link #JDBC_PASSWORD}
	 * to define how to make connections to the database in lieu of
	 * a datasource (either {@link #JTA_DATASOURCE} or {@link #NON_JTA_DATASOURCE}).
	 * <p/>
	 * See section 8.2.1.9
	 */
	String JDBC_URL = "javax.persistence.jdbc.url";

	/**
	 * The JDBC connection user name.
	 * <p/>
	 * Used in conjunction with {@link #JDBC_DRIVER}, {@link #JDBC_URL} and {@link #JDBC_PASSWORD}
	 * to define how to make connections to the database in lieu of
	 * a datasource (either {@link #JTA_DATASOURCE} or {@link #NON_JTA_DATASOURCE}).
	 * <p/>
	 * See section 8.2.1.9
	 */
	String JDBC_USER = "javax.persistence.jdbc.user";

	/**
	 * The JDBC connection password.
	 * <p/>
	 * Used in conjunction with {@link #JDBC_DRIVER}, {@link #JDBC_URL} and {@link #JDBC_USER}
	 * to define how to make connections to the database in lieu of
	 * a datasource (either {@link #JTA_DATASOURCE} or {@link #NON_JTA_DATASOURCE}).
	 * <p/>
	 * See JPA 2 section 8.2.1.9
	 */
	String JDBC_PASSWORD = "javax.persistence.jdbc.password";

	/**
	 * Used to indicate whether second-level (what JPA terms shared cache) caching is
	 * enabled as per the rules defined in JPA 2 section 3.1.7.
	 * <p/>
	 * See JPA 2 sections 9.4.3 and 8.2.1.7
	 * @see javax.persistence.SharedCacheMode
	 */
	String SHARED_CACHE_MODE = "javax.persistence.sharedCache.mode";

	/**
	 * NOTE : Not a valid EMF property...
	 * <p/>
	 * Used to indicate if the provider should attempt to retrieve requested data
	 * in the shared cache.
	 *
	 * @see javax.persistence.CacheRetrieveMode
	 */
	String SHARED_CACHE_RETRIEVE_MODE ="javax.persistence.cache.retrieveMode";

	/**
	 * NOTE : Not a valid EMF property...
	 * <p/>
	 * Used to indicate if the provider should attempt to store data loaded from the database
	 * in the shared cache.
	 *
	 * @see javax.persistence.CacheStoreMode
	 */
	String SHARED_CACHE_STORE_MODE ="javax.persistence.cache.storeMode";

	/**
	 * Used to indicate what form of automatic validation is in effect as per rules defined
	 * in JPA 2 section 3.6.1.1
	 * <p/>
	 * See JPA 2 sections 9.4.3 and 8.2.1.8
	 * @see javax.persistence.ValidationMode
	 */
	String VALIDATION_MODE = "javax.persistence.validation.mode";

	/**
	 * Used to pass along any discovered validator factory.
	 */
	String VALIDATION_FACTORY = "javax.persistence.validation.factory";

	/**
	 * Used to request (hint) a pessimistic lock scope.
	 * <p/>
	 * See JPA 2 sections 8.2.1.9 and 3.4.4.3
	 */
	String LOCK_SCOPE = "javax.persistence.lock.scope";

	/**
	 * Used to request (hint) a pessimistic lock timeout (in milliseconds).
	 * <p/>
	 * See JPA 2 sections 8.2.1.9 and 3.4.4.3
	 */
	String LOCK_TIMEOUT = "javax.persistence.lock.timeout";

	/**
	 * Used to coordinate with bean validators
	 * <p/>
	 * See JPA 2 section 8.2.1.9
	 */
	String PERSIST_VALIDATION_GROUP = "javax.persistence.validation.group.pre-persist";

	/**
	 * Used to coordinate with bean validators
	 * <p/>
	 * See JPA 2 section 8.2.1.9
	 */
	String UPDATE_VALIDATION_GROUP = "javax.persistence.validation.group.pre-update";

	/**
	 * Used to coordinate with bean validators
	 * <p/>
	 * See JPA 2 section 8.2.1.9
	 */
	String REMOVE_VALIDATION_GROUP = "javax.persistence.validation.group.pre-remove";

	/**
	 * Used to pass along the CDI BeanManager, if any, to be used.
	 */
	String CDI_BEAN_MANAGER = "javax.persistence.bean.manager";

	/**
	 * @see org.hibernate.cfg.AvailableSettings#HBM2DDL_CREATE_SOURCE
	 */
	String SCHEMA_GEN_CREATE_SOURCE = org.hibernate.cfg.AvailableSettings.HBM2DDL_CREATE_SOURCE;

	/**
	 * @see org.hibernate.cfg.AvailableSettings#HBM2DDL_DROP_SOURCE
	 */
	String SCHEMA_GEN_DROP_SOURCE = org.hibernate.cfg.AvailableSettings.HBM2DDL_DROP_SOURCE;

	/**
	 * @see org.hibernate.cfg.AvailableSettings#HBM2DDL_CREATE_SCRIPT_SOURCE
	 */
	String SCHEMA_GEN_CREATE_SCRIPT_SOURCE = org.hibernate.cfg.AvailableSettings.HBM2DDL_CREATE_SCRIPT_SOURCE;

	/**
	 * @see org.hibernate.cfg.AvailableSettings#HBM2DDL_DROP_SCRIPT_SOURCE
	 */
	String SCHEMA_GEN_DROP_SCRIPT_SOURCE = org.hibernate.cfg.AvailableSettings.HBM2DDL_DROP_SCRIPT_SOURCE;

	/**
	 * @see org.hibernate.cfg.AvailableSettings#HBM2DDL_DATABASE_ACTION
	 */
	String SCHEMA_GEN_DATABASE_ACTION = org.hibernate.cfg.AvailableSettings.HBM2DDL_DATABASE_ACTION;

	/**
	 * @see org.hibernate.cfg.AvailableSettings#HBM2DDL_SCRIPTS_ACTION
	 */
	String SCHEMA_GEN_SCRIPTS_ACTION = org.hibernate.cfg.AvailableSettings.HBM2DDL_SCRIPTS_ACTION;

	/**
	 * @see org.hibernate.cfg.AvailableSettings#HBM2DDL_SCRIPTS_CREATE_TARGET
	 */
	String SCHEMA_GEN_SCRIPTS_CREATE_TARGET = org.hibernate.cfg.AvailableSettings.HBM2DDL_SCRIPTS_CREATE_TARGET;

	/**
	 * @see org.hibernate.cfg.AvailableSettings#HBM2DDL_SCRIPTS_DROP_TARGET
	 */
	String SCHEMA_GEN_SCRIPTS_DROP_TARGET = org.hibernate.cfg.AvailableSettings.HBM2DDL_SCRIPTS_DROP_TARGET;

	/**
	 * @see org.hibernate.cfg.AvailableSettings#HBM2DLL_CREATE_NAMESPACES
	 * @see org.hibernate.cfg.AvailableSettings#HBM2DLL_CREATE_SCHEMAS
	 */
	String SCHEMA_GEN_CREATE_SCHEMAS = org.hibernate.cfg.AvailableSettings.HBM2DLL_CREATE_SCHEMAS;

	/**
	 * @see org.hibernate.cfg.AvailableSettings#HBM2DDL_CONNECTION
	 */
	String SCHEMA_GEN_CONNECTION = org.hibernate.cfg.AvailableSettings.HBM2DDL_CONNECTION;

	/**
	 * @see org.hibernate.cfg.AvailableSettings#HBM2DDL_DB_NAME
	 */
	String SCHEMA_GEN_DB_NAME = org.hibernate.cfg.AvailableSettings.HBM2DDL_DB_NAME;

	/**
	 * @see org.hibernate.cfg.AvailableSettings#HBM2DDL_DB_MAJOR_VERSION
	 */
	String SCHEMA_GEN_DB_MAJOR_VERSION = org.hibernate.cfg.AvailableSettings.HBM2DDL_DB_MAJOR_VERSION;

	/**
	 * @see org.hibernate.cfg.AvailableSettings#HBM2DDL_DB_MINOR_VERSION
	 */
	String SCHEMA_GEN_DB_MINOR_VERSION = org.hibernate.cfg.AvailableSettings.HBM2DDL_DB_MINOR_VERSION;

	/**
	 * Specifies a {@link java.io.Reader} configured for reading of the SQL load script or a string designating the
	 * file {@link java.net.URL} for the SQL load script.
	 * <p/>
	 * A "SQL load script" is a script that performs some database initialization (INSERT, etc).
	 */
	String SCHEMA_GEN_LOAD_SCRIPT_SOURCE = "javax.persistence.sql-load-script-source";


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Hibernate specific settings
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Query hint (aka {@link javax.persistence.Query#setHint}) for applying
	 * an alias specific lock mode (aka {@link org.hibernate.Query#setLockMode}).
	 * <p/>
	 * Either {@link org.hibernate.LockMode} or {@link javax.persistence.LockModeType}
	 * are accepted.  Also the String names of either are accepted as well.  <tt>null</tt>
	 * is additionally accepted as meaning {@link org.hibernate.LockMode#NONE}.
	 * <p/>
	 * Usage is to concatenate this setting name and the alias name together, separated
	 * by a dot.  For example<code>Query.setHint( "org.hibernate.lockMode.a", someLockMode )</code>
	 * would apply <code>someLockMode</code> to the alias <code>"a"</code>.
	 */
	//Use the org.hibernate prefix. instead of hibernate. as it is a query hint se QueryHints
	String ALIAS_SPECIFIC_LOCK_MODE = "org.hibernate.lockMode";

	/**
	 * JAR autodetection artifacts class, hbm
	 *
	 * @deprecated Use {@link org.hibernate.cfg.AvailableSettings#SCANNER_DISCOVERY} instead
	 */
	@Deprecated
	String AUTODETECTION = org.hibernate.cfg.AvailableSettings.SCANNER_DISCOVERY;

	/**
	 * cfg.xml configuration file used
	 */
	String CFG_FILE = "hibernate.ejb.cfgfile";

	/**
	 * Caching configuration should follow the following pattern
	 * hibernate.ejb.classcache.<fully.qualified.Classname> usage[, region]
	 * where usage is the cache strategy used and region the cache region name
	 */
	String CLASS_CACHE_PREFIX = "hibernate.ejb.classcache";

	/**
	 * Caching configuration should follow the following pattern
	 * hibernate.ejb.collectioncache.<fully.qualified.Classname>.<role> usage[, region]
	 * where usage is the cache strategy used and region the cache region name
	 */
	String COLLECTION_CACHE_PREFIX = "hibernate.ejb.collectioncache";

	/**
	 * Interceptor class name, the class has to have a no-arg constructor
	 * the interceptor instance is shared amongst all EntityManager of a given EntityManagerFactory
	 */
	String INTERCEPTOR = "hibernate.ejb.interceptor";

	/**
	 * Interceptor class name, the class has to have a no-arg constructor
	 */
	String SESSION_INTERCEPTOR = "hibernate.ejb.interceptor.session_scoped";

	/**
	 * SessionFactoryObserver class name, the class must have a no-arg constructor
	 */
	String SESSION_FACTORY_OBSERVER = "hibernate.ejb.session_factory_observer";

	/**
	 * Naming strategy class name, the class has to have a no-arg constructor
	 *
	 * @deprecated Use {@link org.hibernate.cfg.AvailableSettings#IMPLICIT_NAMING_STRATEGY}
	 * or {@link org.hibernate.cfg.AvailableSettings#PHYSICAL_NAMING_STRATEGY} instead
	 */
	@Deprecated
	String NAMING_STRATEGY = "hibernate.ejb.naming_strategy";

	/**
	 * IdentifierGeneratorStrategyProvider class name, the class must have a no-arg constructor
	 * @deprecated if possible wait of Hibernate 4.1 and theService registry (MutableIdentifierGeneratorStrategy service)
	 */
	@Deprecated
	String IDENTIFIER_GENERATOR_STRATEGY_PROVIDER = "hibernate.ejb.identifier_generator_strategy_provider";

	/**
	 * Event configuration should follow the following pattern
	 * hibernate.ejb.event.[eventType] f.q.c.n.EventListener1, f.q.c.n.EventListener12 ...
	 */
	String EVENT_LISTENER_PREFIX = "hibernate.ejb.event";

	/**
	 * Enable the class file enhancement
	 *
	 * @deprecated Split into multiple 'hibernate.enhancer.enable[]' properties
	 */
	@Deprecated
	String USE_CLASS_ENHANCER = "hibernate.ejb.use_class_enhancer";

	/**
	 * Enable dirty tracking feature in runtime bytecode enhancement
	 */
	String ENHANCER_ENABLE_DIRTY_TRACKING = "hibernate.enhancer.enableDirtyTracking";

	/**
	 * Enable lazy loading feature in runtime bytecode enhancement
	 */
	String ENHANCER_ENABLE_LAZY_INITIALIZATION = "hibernate.enhancer.enableLazyInitialization";

	/**
	 * Enable association management feature in runtime bytecode enhancement
	 */
	String ENHANCER_ENABLE_ASSOCIATION_MANAGEMENT = "hibernate.enhancer.enableAssociationManagement";

	/**
	 * Whether or not discard persistent context on entityManager.close()
	 * The EJB3 compliant and default choice is false
	 */
	String DISCARD_PC_ON_CLOSE = "hibernate.ejb.discard_pc_on_close";

	/**
	 * Consider this as experimental
	 * It is not recommended to set up this property, the configuration is stored
	 * in the JNDI in a serialized form
	 *
	 * @deprecated Configuration going away.
	 */
	@Deprecated
	String CONFIGURATION_JNDI_NAME = "hibernate.ejb.configuration_jndi_name";

	/**
	 * Used to determine flush mode.
	 */
	//Use the org.hibernate prefix. instead of hibernate. as it is a query hint se QueryHints
	String FLUSH_MODE = "org.hibernate.flushMode";

	/**
	 * @deprecated Prefer {@link org.hibernate.cfg.AvailableSettings#SCANNER} instead
	 */
	@Deprecated
	@SuppressWarnings("UnusedDeclaration")
	String SCANNER = org.hibernate.cfg.AvailableSettings.SCANNER_DEPRECATED;

	/**
	 * List of classes names
	 * Internal use only
	 *
	 * @deprecated Was never intended for external use
	 */
	@Deprecated
	@SuppressWarnings("UnusedDeclaration")
	String CLASS_NAMES = "hibernate.ejb.classes";

	/**
	 * List of annotated packages
	 * Internal use only
	 *
	 * @deprecated Was never intended for external use
	 */
	@Deprecated
	@SuppressWarnings("UnusedDeclaration")
	String PACKAGE_NAMES = "hibernate.ejb.packages";

	/**
	 * EntityManagerFactory name
	 */
	String ENTITY_MANAGER_FACTORY_NAME = "hibernate.ejb.entitymanager_factory_name";

	/**
	 * @deprecated use {@link #JPA_METAMODEL_POPULATION} instead.
	 */
	@Deprecated
	String JPA_METAMODEL_GENERATION = "hibernate.ejb.metamodel.generation";

	/**
	 * Setting that controls whether we seek out JPA "static metamodel" classes and populate them.  Accepts
	 * 3 values:<ul>
	 *     <li>
	 *         <b>enabled</b> - Do the population
	 *     </li>
	 *     <li>
	 *         <b>disabled</b> - Do not do the population
	 *     </li>
	 *     <li>
	 *         <b>ignoreUnsupported</b> - Do the population, but ignore any non-JPA features that would otherwise
	 *         result in the population failing.
	 *     </li>
	 * </ul>
	 *
	 */
	String JPA_METAMODEL_POPULATION = "hibernate.ejb.metamodel.population";


	/**
	 * List of classes names
	 * Internal use only
	 */
	String XML_FILE_NAMES = "hibernate.ejb.xml_files";
	String HBXML_FILES = "hibernate.hbmxml.files";
	String LOADED_CLASSES = "hibernate.ejb.loaded.classes";

	/**
	 * Deprecated
	 *
	 * @deprecated Use {@link org.hibernate.cfg.AvailableSettings#JACC_CONTEXT_ID} instead
	 */
	@Deprecated
	String JACC_CONTEXT_ID = org.hibernate.cfg.AvailableSettings.JACC_CONTEXT_ID;

	/**
	 * Deprecated
	 *
	 * @deprecated Use {@link org.hibernate.cfg.AvailableSettings#JACC_PREFIX} instead
	 */
	@Deprecated
	String JACC_PREFIX = org.hibernate.cfg.AvailableSettings.JACC_PREFIX;

	/**
	 * Deprecated
	 *
	 * @deprecated Use {@link org.hibernate.cfg.AvailableSettings#JACC_ENABLED} instead
	 */
	@Deprecated
	String JACC_ENABLED = org.hibernate.cfg.AvailableSettings.JACC_ENABLED;

	/**
	 * Used to pass along the name of the persistence unit.
	 */
	String PERSISTENCE_UNIT_NAME = "hibernate.ejb.persistenceUnitName";

	/**
	 * Defines delayed access to CDI BeanManager.  Starting in 5.1 the preferred means for CDI
	 * bootstrapping is through org.hibernate.jpa.event.spi.jpa.ExtendedBeanManager
	 *
	 * @since 5.0.8
	 */
	String DELAY_CDI_ACCESS = "hibernate.delay_cdi_access";
}
