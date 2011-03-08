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
package org.hibernate.ejb;

/**
 * Defines the available HEM settings, both JPA-defined as well as Hibernate-specific
 * <p/>
 * NOTE : Does *not* include {@link org.hibernate.cfg.Environment} values.
 *
 * @author Steve Ebersole
 */
public class AvailableSettings {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA defined settings
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * THe name of the {@link javax.persistence.spi.PersistenceProvider} implementor
	 * <p/>
	 * See JPA 2 sections 9.4.3 and 8.2.1.4
	 */
	public static final String PROVIDER = "javax.persistence.provider";

	/**
	 * The type of transactions supported by the entity managers.
	 * <p/>
	 * See JPA 2 sections 9.4.3 and 8.2.1.2
	 */
	public static final String TRANSACTION_TYPE = "javax.persistence.transactionType";

	/**
	 * The JNDI name of a JTA {@link javax.sql.DataSource}.
	 * <p/>
	 * See JPA 2 sections 9.4.3 and 8.2.1.5
	 */
	public static final String JTA_DATASOURCE = "javax.persistence.jtaDataSource";

	/**
	 * The JNDI name of a non-JTA {@link javax.sql.DataSource}.
	 * <p/>
	 * See JPA 2 sections 9.4.3 and 8.2.1.5
	 */
	public static final String NON_JTA_DATASOURCE = "javax.persistence.nonJtaDataSource";

	/**
	 * The name of a JDBC driver to use to connect to the database.
	 * <p/>
	 * Used in conjunction with {@link #JDBC_URL}, {@link #JDBC_USER} and {@link #JDBC_PASSWORD}
	 * to define how to make connections to the database in lieu of
	 * a datasource (either {@link #JTA_DATASOURCE} or {@link #NON_JTA_DATASOURCE}).
	 * <p/>
	 * See section 8.2.1.9
	 */
	public static final String JDBC_DRIVER = "javax.persistence.jdbc.driver";

	/**
	 * The JDBC connection url to use to connect to the database.
	 * <p/>
	 * Used in conjunction with {@link #JDBC_DRIVER}, {@link #JDBC_USER} and {@link #JDBC_PASSWORD}
	 * to define how to make connections to the database in lieu of
	 * a datasource (either {@link #JTA_DATASOURCE} or {@link #NON_JTA_DATASOURCE}).
	 * <p/>
	 * See section 8.2.1.9
	 */
	public static final String JDBC_URL = "javax.persistence.jdbc.url";

	/**
	 * The JDBC connection user name.
	 * <p/>
	 * Used in conjunction with {@link #JDBC_DRIVER}, {@link #JDBC_URL} and {@link #JDBC_PASSWORD}
	 * to define how to make connections to the database in lieu of
	 * a datasource (either {@link #JTA_DATASOURCE} or {@link #NON_JTA_DATASOURCE}).
	 * <p/>
	 * See section 8.2.1.9
	 */
	public static final String JDBC_USER = "javax.persistence.jdbc.user";

	/**
	 * The JDBC connection password.
	 * <p/>
	 * Used in conjunction with {@link #JDBC_DRIVER}, {@link #JDBC_URL} and {@link #JDBC_USER}
	 * to define how to make connections to the database in lieu of
	 * a datasource (either {@link #JTA_DATASOURCE} or {@link #NON_JTA_DATASOURCE}).
	 * <p/>
	 * See JPA 2 section 8.2.1.9
	 */
	public static final String JDBC_PASSWORD = "javax.persistence.jdbc.password";

	/**
	 * Used to indicate whether second-level (what JPA terms shared cache) caching is
	 * enabled as per the rules defined in JPA 2 section 3.1.7.
	 * <p/>
	 * See JPA 2 sections 9.4.3 and 8.2.1.7
	 * @see javax.persistence.SharedCacheMode
	 */
	public static final String SHARED_CACHE_MODE = "javax.persistence.sharedCache.mode";

	/**
	 * NOTE : Not a valid EMF property...
	 * <p/>
	 * Used to indicate if the provider should attempt to retrieve requested data
	 * in the shared cache.
	 *
	 * @see javax.persistence.CacheRetrieveMode
	 */
	public static final String SHARED_CACHE_RETRIEVE_MODE ="javax.persistence.cache.retrieveMode";

	/**
	 * NOTE : Not a valid EMF property...
	 * <p/>
	 * Used to indicate if the provider should attempt to store data loaded from the database
	 * in the shared cache.
	 *
	 * @see javax.persistence.CacheStoreMode
	 */
	public static final String SHARED_CACHE_STORE_MODE ="javax.persistence.cache.storeMode";

	/**
	 * Used to indicate what form of automatic validation is in effect as per rules defined
	 * in JPA 2 section 3.6.1.1
	 * <p/>
	 * See JPA 2 sections 9.4.3 and 8.2.1.8
	 * @see javax.persistence.ValidationMode
	 */
	public static final String VALIDATION_MODE = "javax.persistence.validation.mode";

	/**
	 * Used to pass along any discovered validator factory.
	 */
	public static final String VALIDATION_FACTORY = "javax.persistence.validation.factory";

	/**
	 * Used to request (hint) a pessimistic lock scope.
	 * <p/>
	 * See JPA 2 sections 8.2.1.9 and 3.4.4.3
	 */
	public static final String LOCK_SCOPE = "javax.persistence.lock.scope";

	/**
	 * Used to request (hint) a pessimistic lock timeout (in milliseconds).
	 * <p/>
	 * See JPA 2 sections 8.2.1.9 and 3.4.4.3
	 */
	public static final String LOCK_TIMEOUT = "javax.persistence.lock.timeout";

	/**
	 * Used to coordinate with bean validators
	 * <p/>
	 * See JPA 2 section 8.2.1.9
	 */
	public static final String PERSIST_VALIDATION_GROUP = "javax.persistence.validation.group.pre-persist";

	/**
	 * Used to coordinate with bean validators
	 * <p/>
	 * See JPA 2 section 8.2.1.9
	 */
	public static final String UPDATE_VALIDATION_GROUP = "javax.persistence.validation.group.pre-update";

	/**
	 * Used to coordinate with bean validators
	 * <p/>
	 * See JPA 2 section 8.2.1.9
	 */
	public static final String REMOVE_VALIDATION_GROUP = "javax.persistence.validation.group.pre-remove";


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
	public static final String ALIAS_SPECIFIC_LOCK_MODE = "org.hibernate.lockMode";

	/**
	 * JAR autodetection artifacts class, hbm
	 */
	public static final String AUTODETECTION = "hibernate.archive.autodetection";

	/**
	 * cfg.xml configuration file used
	 */
	public static final String CFG_FILE = "hibernate.ejb.cfgfile";

	/**
	 * Caching configuration should follow the following pattern
	 * hibernate.ejb.classcache.<fully.qualified.Classname> usage[, region]
	 * where usage is the cache strategy used and region the cache region name
	 */
	public static final String CLASS_CACHE_PREFIX = "hibernate.ejb.classcache";

	/**
	 * Caching configuration should follow the following pattern
	 * hibernate.ejb.collectioncache.<fully.qualified.Classname>.<role> usage[, region]
	 * where usage is the cache strategy used and region the cache region name
	 */
	public static final String COLLECTION_CACHE_PREFIX = "hibernate.ejb.collectioncache";

	/**
	 * Interceptor class name, the class has to have a no-arg constructor
	 * the interceptor instance is shared amongst all EntityManager of a given EntityManagerFactory
	 */
	public static final String INTERCEPTOR = "hibernate.ejb.interceptor";

	/**
	 * Interceptor class name, the class has to have a no-arg constructor
	 */
	public static final String SESSION_INTERCEPTOR = "hibernate.ejb.interceptor.session_scoped";

	/**
	 * Naming strategy class name, the class has to have a no-arg constructor
	 */
	public static final String NAMING_STRATEGY = "hibernate.ejb.naming_strategy";

	/**
	 * Event configuration should follow the following pattern
	 * hibernate.ejb.event.[eventType] f.q.c.n.EventListener1, f.q.c.n.EventListener12 ...
	 */
	public static final String EVENT_LISTENER_PREFIX = "hibernate.ejb.event";

	/**
	 * Enable the class file enhancement
	 */
	public static final String USE_CLASS_ENHANCER = "hibernate.ejb.use_class_enhancer";

	/**
	 * Whether or not discard persistent context on entityManager.close()
	 * The EJB3 compliant and default choice is false
	 */
	public static final String DISCARD_PC_ON_CLOSE = "hibernate.ejb.discard_pc_on_close";

	/**
	 * Consider this as experimental
	 * It is not recommended to set up this property, the configuration is stored
	 * in the JNDI in a serialized form
	 */
	public static final String CONFIGURATION_JNDI_NAME = "hibernate.ejb.configuration_jndi_name";

	/**
	 * Used to determine flush mode.
	 */
	//Use the org.hibernate prefix. instead of hibernate. as it is a query hint se QueryHints
	public static final String FLUSH_MODE = "org.hibernate.flushMode";

	/**
	 * Pass an implementation of {@link org.hibernate.ejb.packaging.Scanner}:
	 *  - preferably an actual instance
	 *  - or a class name with a no-arg constructor 
	 */
	public static final String SCANNER = "hibernate.ejb.resource_scanner";

	/**
	 * List of classes names
	 * Internal use only
	 */
	public static final String CLASS_NAMES = "hibernate.ejb.classes";

	/**
	 * List of annotated packages
	 * Internal use only
	 */
	public static final String PACKAGE_NAMES = "hibernate.ejb.packages";

	/**
	 * List of classes names
	 * Internal use only
	 */
	public static final String XML_FILE_NAMES = "hibernate.ejb.xml_files";
	public static final String HBXML_FILES = "hibernate.hbmxml.files";
	public static final String LOADED_CLASSES = "hibernate.ejb.loaded.classes";
	public static final String JACC_CONTEXT_ID = "hibernate.jacc.ctx.id";
	public static final String JACC_PREFIX = "hibernate.jacc";
	public static final String JACC_ENABLED = "hibernate.jacc.enabled";
	public static final String PERSISTENCE_UNIT_NAME = "hibernate.ejb.persistenceUnitName";
}
