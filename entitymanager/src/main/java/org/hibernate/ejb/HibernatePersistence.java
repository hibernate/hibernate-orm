//$Id$
package org.hibernate.ejb;

import java.util.Map;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.LoadState;

import org.hibernate.Hibernate;
import org.hibernate.intercept.FieldInterceptionHelper;
import org.hibernate.intercept.FieldInterceptor;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;

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
	 * ²
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

	public LoadState isLoadedWithoutReference(Object proxy, String property) {
		Object entity;
		boolean sureFromUs = false;
		if ( proxy instanceof HibernateProxy ) {
			LazyInitializer li = ( ( HibernateProxy ) proxy ).getHibernateLazyInitializer();
			if ( li.isUninitialized() ) {
				return LoadState.NOT_LOADED;
			}
			else {
				entity = li.getImplementation();
			}
			sureFromUs = true;
		}
		else {
			entity = proxy;
		}

		//we are instrumenting but we can't assume we are the only ones
		if ( FieldInterceptionHelper.isInstrumented( entity ) ) {
			FieldInterceptor interceptor = FieldInterceptionHelper.extractFieldInterceptor( entity );
			final boolean isInitialized = interceptor == null || interceptor.isInitialized( property );
			LoadState state;
			if (isInitialized && interceptor != null) {
				state = LoadState.LOADED;
			}
			else if ( interceptor != null && (! isInitialized)) {
				state = LoadState.NOT_LOADED;
			}
			else if ( sureFromUs ) { //interceptor == null
				state = LoadState.LOADED;
			}
			else {
				state = LoadState.UNKNOWN;
			}

			return state;
		}
		else {
			//can't do sureFromUs ? LoadState.LOADED : LoadState.UNKNOWN;
			//is that an association?
			return LoadState.UNKNOWN;
		}
	}

	public LoadState isLoadedWithReference(Object proxy, String property) {
		//for sure we don't instrument and for sure it's not a lazy proxy
		Object object = get(proxy, property);
		return isLoaded( object );
	}

	private Object get(Object proxy, String property) {
		final Class<?> clazz = proxy.getClass();
		try {
			try {
				final Field field = clazz.getField( property );
				setAccessibility( field );
				return field.get( proxy );
			}
			catch ( NoSuchFieldException e ) {
				final Method method = getMethod( clazz, property );
				if (method != null) {
					setAccessibility( method );
					return method.invoke( proxy );
				}
				else {
					throw new PersistenceException( "Unable to find field or method: "
							+ clazz + "#"
							+ property);
				}
			}
		}
		catch ( IllegalAccessException e ) {
			throw new PersistenceException( "Unable to access field or method: "
							+ clazz + "#"
							+ property, e);
		}
		catch ( InvocationTargetException e ) {
			throw new PersistenceException( "Unable to access field or method: "
							+ clazz + "#"
							+ property, e);
		}
	}

	/**
	 * Returns the method with the specified name or <code>null</code> if it does not exist.
	 *
	 * @param clazz The class to check.
	 * @param methodName The method name.
	 *
	 * @return Returns the method with the specified name or <code>null</code> if it does not exist.
	 */
	public static Method getMethod(Class<?> clazz, String methodName) {
		try {
			char string[] = methodName.toCharArray();
			string[0] = Character.toUpperCase( string[0] );
			methodName = new String( string );
			try {
				return clazz.getMethod( "get" + methodName );
			}
			catch ( NoSuchMethodException e ) {
				return clazz.getMethod( "is" + methodName );
			}
		}
		catch ( NoSuchMethodException e ) {
			return null;
		}
	}

	public static void setAccessibility(Member member) {
		if ( !Modifier.isPublic( member.getModifiers() ) ) {
			//Sun's ease of use, sigh...
			( ( AccessibleObject ) member ).setAccessible( true );
		}
	}

	public LoadState isLoaded(Object o) {
		if ( o instanceof HibernateProxy ) {
			final boolean isInitialized = !( ( HibernateProxy ) o ).getHibernateLazyInitializer().isUninitialized();
			return isInitialized ? LoadState.LOADED : LoadState.NOT_LOADED;
		}
		else if ( o instanceof PersistentCollection ) {
			final boolean isInitialized = ( ( PersistentCollection ) o ).wasInitialized();
			return isInitialized ? LoadState.LOADED : LoadState.NOT_LOADED;
		}
		else {
			return LoadState.UNKNOWN;
		}
	}

	/**
	 * create a factory from a canonical version
	 * @deprecated
	 */
	// This is used directly by JBoss so don't remove until further notice.  bill@jboss.org
	public EntityManagerFactory createEntityManagerFactory(Map properties) {
		Ejb3Configuration cfg = new Ejb3Configuration();
		return cfg.createEntityManagerFactory( properties );
	}

}