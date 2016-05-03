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
	// JPA-defined settings - general
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * @deprecated (since 5.2) use {@link org.hibernate.cfg.AvailableSettings#JPA_PERSISTENCE_PROVIDER} instead
	 */
	@Deprecated
	String PROVIDER = org.hibernate.cfg.AvailableSettings.JPA_PERSISTENCE_PROVIDER;

	/**
	 * @deprecated (since 5.2) use {@link org.hibernate.cfg.AvailableSettings#JPA_TRANSACTION_TYPE} instead
	 */
	@Deprecated
	String TRANSACTION_TYPE = org.hibernate.cfg.AvailableSettings.JPA_TRANSACTION_TYPE;

	/**
	 * @deprecated (since 5.2) use {@link org.hibernate.cfg.AvailableSettings#JPA_JTA_DATASOURCE} instead
	 */
	@Deprecated
	String JTA_DATASOURCE = org.hibernate.cfg.AvailableSettings.JPA_JTA_DATASOURCE;

	/**
	 * @deprecated (since 5.2) use {@link org.hibernate.cfg.AvailableSettings#JPA_NON_JTA_DATASOURCE} instead
	 */
	@Deprecated
	String NON_JTA_DATASOURCE = org.hibernate.cfg.AvailableSettings.JPA_NON_JTA_DATASOURCE;

	/**
	 * @deprecated (since 5.2) use {@link org.hibernate.cfg.AvailableSettings#JPA_JDBC_DRIVER} instead
	 */
	@Deprecated
	String JDBC_DRIVER = org.hibernate.cfg.AvailableSettings.JPA_JDBC_DRIVER;

	/**
	 * @deprecated (since 5.2) use {@link org.hibernate.cfg.AvailableSettings#JPA_JDBC_URL} instead
	 */
	@Deprecated
	String JDBC_URL = org.hibernate.cfg.AvailableSettings.JPA_JDBC_URL;

	/**
	 * @deprecated (since 5.2) use {@link org.hibernate.cfg.AvailableSettings#JPA_JDBC_USER} instead
	 */
	@Deprecated
	String JDBC_USER = org.hibernate.cfg.AvailableSettings.JPA_JDBC_USER;

	/**
	 * @deprecated (since 5.2) use {@link org.hibernate.cfg.AvailableSettings#JPA_JDBC_USER} instead
	 */
	@Deprecated
	String JDBC_PASSWORD = org.hibernate.cfg.AvailableSettings.JPA_JDBC_USER;

	/**
	 * @deprecated (since 5.2) use {@link org.hibernate.cfg.AvailableSettings#JPA_SHARED_CACHE_MODE} instead
	 */
	@Deprecated
	String SHARED_CACHE_MODE = org.hibernate.cfg.AvailableSettings.JPA_SHARED_CACHE_MODE;

	/**
	 * @deprecated (since 5.2) use {@link org.hibernate.cfg.AvailableSettings#JPA_SHARED_CACHE_RETRIEVE_MODE} instead
	 */
	@Deprecated
	String SHARED_CACHE_RETRIEVE_MODE = org.hibernate.cfg.AvailableSettings.JPA_SHARED_CACHE_RETRIEVE_MODE;

	/**
	 * @deprecated (since 5.2) use {@link org.hibernate.cfg.AvailableSettings#JPA_SHARED_CACHE_STORE_MODE} instead
	 */
	@Deprecated
	String SHARED_CACHE_STORE_MODE = org.hibernate.cfg.AvailableSettings.JPA_SHARED_CACHE_STORE_MODE;

	/**
	 * @deprecated (since 5.2) use {@link org.hibernate.cfg.AvailableSettings#JPA_VALIDATION_MODE} instead
	 */
	@Deprecated
	String VALIDATION_MODE = org.hibernate.cfg.AvailableSettings.JPA_VALIDATION_MODE;

	/**
	 * @deprecated (since 5.2) use {@link org.hibernate.cfg.AvailableSettings#JPA_VALIDATION_FACTORY} instead
	 */
	@Deprecated
	String VALIDATION_FACTORY = org.hibernate.cfg.AvailableSettings.JPA_VALIDATION_FACTORY;

	/**
	 * @deprecated (since 5.2) use {@link org.hibernate.cfg.AvailableSettings#JPA_PERSIST_VALIDATION_GROUP} instead
	 */
	@Deprecated
	String PERSIST_VALIDATION_GROUP = org.hibernate.cfg.AvailableSettings.JPA_PERSIST_VALIDATION_GROUP;

	/**
	 * @deprecated (since 5.2) use {@link org.hibernate.cfg.AvailableSettings#JPA_UPDATE_VALIDATION_GROUP} instead
	 */
	@Deprecated
	String UPDATE_VALIDATION_GROUP = org.hibernate.cfg.AvailableSettings.JPA_UPDATE_VALIDATION_GROUP;

	/**
	 * @deprecated (since 5.2) use {@link org.hibernate.cfg.AvailableSettings#JPA_REMOVE_VALIDATION_GROUP} instead
	 */
	@Deprecated
	String REMOVE_VALIDATION_GROUP = org.hibernate.cfg.AvailableSettings.JPA_REMOVE_VALIDATION_GROUP;

	/**
	 * @deprecated (since 5.2) use {@link org.hibernate.cfg.AvailableSettings#JPA_LOCK_SCOPE} instead
	 */
	@Deprecated
	String LOCK_SCOPE = org.hibernate.cfg.AvailableSettings.JPA_LOCK_SCOPE;

	/**
	 * @deprecated (since 5.2) use {@link org.hibernate.cfg.AvailableSettings#JPA_LOCK_TIMEOUT} instead
	 */
	@Deprecated
	String LOCK_TIMEOUT = org.hibernate.cfg.AvailableSettings.JPA_LOCK_TIMEOUT;

	/**
	 * @deprecated (since 5.2) use {@link org.hibernate.cfg.AvailableSettings#CDI_BEAN_MANAGER} instead
	 */
	@Deprecated
	String CDI_BEAN_MANAGER = org.hibernate.cfg.AvailableSettings.CDI_BEAN_MANAGER;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA-defined settings - schema export
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * @deprecated (since 5.2) use {@link org.hibernate.cfg.AvailableSettings#HBM2DDL_CREATE_SOURCE} instead
	 */
	@Deprecated
	String SCHEMA_GEN_CREATE_SOURCE = org.hibernate.cfg.AvailableSettings.HBM2DDL_CREATE_SOURCE;

	/**
	 * @deprecated (since 5.2) use {@link org.hibernate.cfg.AvailableSettings#HBM2DDL_DROP_SOURCE} instead
	 */
	@Deprecated
	String SCHEMA_GEN_DROP_SOURCE = org.hibernate.cfg.AvailableSettings.HBM2DDL_DROP_SOURCE;

	/**
	 * @deprecated (since 5.2) use {@link org.hibernate.cfg.AvailableSettings#HBM2DDL_CREATE_SCRIPT_SOURCE} instead
	 */
	@Deprecated
	String SCHEMA_GEN_CREATE_SCRIPT_SOURCE = org.hibernate.cfg.AvailableSettings.HBM2DDL_CREATE_SCRIPT_SOURCE;

	/**
	 * @deprecated (since 5.2) use {@link org.hibernate.cfg.AvailableSettings#HBM2DDL_DROP_SCRIPT_SOURCE} instead
	 */
	@Deprecated
	String SCHEMA_GEN_DROP_SCRIPT_SOURCE = org.hibernate.cfg.AvailableSettings.HBM2DDL_DROP_SCRIPT_SOURCE;

	/**
	 * @deprecated (since 5.2) use {@link org.hibernate.cfg.AvailableSettings#HBM2DDL_DATABASE_ACTION} instead
	 */
	@Deprecated
	String SCHEMA_GEN_DATABASE_ACTION = org.hibernate.cfg.AvailableSettings.HBM2DDL_DATABASE_ACTION;

	/**
	 * @deprecated (since 5.2) use {@link org.hibernate.cfg.AvailableSettings#HBM2DDL_SCRIPTS_ACTION} instead
	 */
	@Deprecated
	String SCHEMA_GEN_SCRIPTS_ACTION = org.hibernate.cfg.AvailableSettings.HBM2DDL_SCRIPTS_ACTION;

	/**
	 * @deprecated (since 5.2) use {@link org.hibernate.cfg.AvailableSettings#HBM2DDL_SCRIPTS_CREATE_TARGET} instead
	 */
	@Deprecated
	String SCHEMA_GEN_SCRIPTS_CREATE_TARGET = org.hibernate.cfg.AvailableSettings.HBM2DDL_SCRIPTS_CREATE_TARGET;

	/**
	 * @deprecated (since 5.2) use {@link org.hibernate.cfg.AvailableSettings#HBM2DDL_SCRIPTS_DROP_TARGET} instead
	 */
	@Deprecated
	String SCHEMA_GEN_SCRIPTS_DROP_TARGET = org.hibernate.cfg.AvailableSettings.HBM2DDL_SCRIPTS_DROP_TARGET;

	/**
	 * @deprecated (since 5.2) use {@link org.hibernate.cfg.AvailableSettings#HBM2DLL_CREATE_NAMESPACES}
	 * or {@link org.hibernate.cfg.AvailableSettings#HBM2DLL_CREATE_SCHEMAS} instead
	 */
	@Deprecated
	String SCHEMA_GEN_CREATE_SCHEMAS = org.hibernate.cfg.AvailableSettings.HBM2DLL_CREATE_NAMESPACES;

	/**
	 * @deprecated (since 5.2) use {@link org.hibernate.cfg.AvailableSettings#HBM2DDL_CONNECTION} instead
	 */
	@Deprecated
	String SCHEMA_GEN_CONNECTION = org.hibernate.cfg.AvailableSettings.HBM2DDL_CONNECTION;

	/**
	 * @deprecated (since 5.2) use {@link org.hibernate.cfg.AvailableSettings#HBM2DDL_DB_NAME} instead
	 */
	@Deprecated
	String SCHEMA_GEN_DB_NAME = org.hibernate.cfg.AvailableSettings.HBM2DDL_DB_NAME;

	/**
	 * @deprecated (since 5.2) use {@link org.hibernate.cfg.AvailableSettings#HBM2DDL_DB_MAJOR_VERSION} instead
	 */
	@Deprecated
	String SCHEMA_GEN_DB_MAJOR_VERSION = org.hibernate.cfg.AvailableSettings.HBM2DDL_DB_MAJOR_VERSION;

	/**
	 * @deprecated (since 5.2) use {@link org.hibernate.cfg.AvailableSettings#HBM2DDL_DB_MINOR_VERSION} instead
	 */
	@Deprecated
	String SCHEMA_GEN_DB_MINOR_VERSION = org.hibernate.cfg.AvailableSettings.HBM2DDL_DB_MINOR_VERSION;

	/**
	 * @deprecated (since 5.2) use {@link org.hibernate.cfg.AvailableSettings#HBM2DDL_LOAD_SCRIPT_SOURCE} instead
	 */
	@Deprecated
	String SCHEMA_GEN_LOAD_SCRIPT_SOURCE = org.hibernate.cfg.AvailableSettings.HBM2DDL_LOAD_SCRIPT_SOURCE;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Hibernate specific settings
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * @deprecated (since 5.2) use {@link org.hibernate.cfg.AvailableSettings#INTERCEPTOR} instead
	 */
	@Deprecated
	String INTERCEPTOR = "hibernate.ejb.interceptor";

	/**
	 * @deprecated (since 5.2) use {@link org.hibernate.cfg.AvailableSettings#SESSION_SCOPED_INTERCEPTOR} instead
	 */
	@Deprecated
	String SESSION_INTERCEPTOR = "hibernate.ejb.interceptor.session_scoped";

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
	 * SessionFactoryObserver class name, the class must have a no-arg constructor
	 */
	String SESSION_FACTORY_OBSERVER = "hibernate.ejb.session_factory_observer";

	/**
	 * IdentifierGeneratorStrategyProvider class name, the class must have a no-arg constructor
	 */
	String IDENTIFIER_GENERATOR_STRATEGY_PROVIDER = "hibernate.ejb.identifier_generator_strategy_provider";

	/**
	 * Event configuration should follow the following pattern
	 * hibernate.ejb.event.[eventType] f.q.c.n.EventListener1, f.q.c.n.EventListener12 ...
	 */
	String EVENT_LISTENER_PREFIX = "hibernate.ejb.event";

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
	 * Used to determine flush mode.
	 */
	//Use the org.hibernate prefix. instead of hibernate. as it is a query hint se QueryHints
	String FLUSH_MODE = "org.hibernate.flushMode";


	/**
	 * EntityManagerFactory name
	 */
	String ENTITY_MANAGER_FACTORY_NAME = "hibernate.ejb.entitymanager_factory_name";


	/**
	 * List of classes names
	 * Internal use only
	 */
	String XML_FILE_NAMES = "hibernate.ejb.xml_files";
	String HBXML_FILES = "hibernate.hbmxml.files";
	String LOADED_CLASSES = "hibernate.ejb.loaded.classes";


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

	/**
	 * Setting that allows access to the underlying {@link org.hibernate.Transaction}, even
	 * when using a JTA since normal JPA operations prohibit this behavior.
	 * <p/>
	 * Values are {@code true} grants access, {@code false} does not.
	 * <p/>
	 * The default behavior is to allow access unless the session is bootstrapped via JPA.
	 */
	String ALLOW_JTA_TRANSACTION_ACCESS = "hibernate.jta.allowTransactionAccess";
}
