/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
 * Boston, MA  02110-1301  USA\
 */
package org.hibernate.ejb;

import java.util.Map;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.LoadState;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.ProviderUtil;

import org.hibernate.ejb.util.PersistenceUtilHelper;

/**
 * Hibernate EJB3 persistence provider implementation
 *
 * @author Gavin King
 */
public class HibernatePersistence implements javax.persistence.spi.PersistenceProvider {

	/**
	 * Provider
	 */
	public static final String PROVIDER = "javax.persistence.provider";
	/**
	 * �
	 * transaction type
	 */
	public static final String TRANSACTION_TYPE = "javax.persistence.transactionType";
	/**
	 * JTA datasource name
	 */
	public static final String JTA_DATASOURCE = "javax.persistence.jtaDataSource";
	/**
	 * Non JTA datasource name
	 */
	public static final String NON_JTA_DATASOURCE = "javax.persistence.nonJtaDataSource";
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

	//The following properties are for Internal use only
	/**
	 * link to the alternative Hibernate configuration file
	 * Internal use only
	 */
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


	/**
	 * Get an entity manager factory by its entity manager name and given the
	 * appropriate extra properties. Those proeprties override the one get through
	 * the peristence.xml file.
	 *
	 * @param persistenceUnitName entity manager name
	 * @param overridenProperties properties passed to the persistence provider
	 * @return initialized EntityManagerFactory
	 */
	public EntityManagerFactory createEntityManagerFactory(String persistenceUnitName, Map overridenProperties) {
		Ejb3Configuration cfg = new Ejb3Configuration();
		Ejb3Configuration configured = cfg.configure( persistenceUnitName, overridenProperties );
		return configured != null ? configured.buildEntityManagerFactory() : null;
	}

	public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map map) {
		Ejb3Configuration cfg = new Ejb3Configuration();
		Ejb3Configuration configured = cfg.configure( info, map );
		return configured != null ? configured.buildEntityManagerFactory() : null;
	}

	private final ProviderUtil providerUtil = new ProviderUtil() {
		public LoadState isLoadedWithoutReference(Object proxy, String property) {
			return PersistenceUtilHelper.isLoadedWithoutReference( proxy, property );
		}

		public LoadState isLoadedWithReference(Object proxy, String property) {
			return PersistenceUtilHelper.isLoadedWithReference( proxy, property );
		}

		public LoadState isLoaded(Object o) {
			return PersistenceUtilHelper.isLoaded(o);
		}
	};

	public ProviderUtil getProviderUtil() {
		return providerUtil;
	}

	/**
	 * create a factory from a canonical version
	 * @deprecated
	 */
	public EntityManagerFactory createEntityManagerFactory(Map properties) {
		// This is used directly by JBoss so don't remove until further notice.  bill@jboss.org
		Ejb3Configuration cfg = new Ejb3Configuration();
		return cfg.createEntityManagerFactory( properties );
	}

}