/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.ehcache.management.impl;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Properties;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import net.sf.ehcache.CacheManager;

import org.hibernate.SessionFactory;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.ehcache.EhCacheMessageLogger;
import org.hibernate.cfg.Environment;
import org.hibernate.internal.SessionFactoryRegistry;

import org.jboss.logging.Logger;

/**
 * Helper class for registering mbeans for ehcache backed hibernate second level cache
 * <p/>
 * <p/>
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @author <a href="mailto:alexsnaps@terracottatech.com">Alex Snaps</a>
 */
public class ProviderMBeanRegistrationHelper {
	private static final EhCacheMessageLogger LOG = Logger.getMessageLogger(
			EhCacheMessageLogger.class,
			ProviderMBeanRegistrationHelper.class.getName()
	);
	private static final int MILLIS_PER_SECOND = 1000;
	private static final int SLEEP_MILLIS = 500;

	private volatile EhcacheHibernateMBeanRegistrationImpl ehcacheHibernateMBeanRegistration;

	/**
	 * Registers mbean for the input cache manager and the session factory name
	 *
	 * @param manager the backing cachemanager
	 * @param properties session factory config properties
	 */
	public void registerMBean(final CacheManager manager, final Properties properties) {
		if ( Boolean.getBoolean( "tc.active" ) ) {
			ehcacheHibernateMBeanRegistration = new EhcacheHibernateMBeanRegistrationImpl();
			manager.getTimer().scheduleAtFixedRate(
					new RegisterMBeansTask( ehcacheHibernateMBeanRegistration, manager, properties ), SLEEP_MILLIS,
					SLEEP_MILLIS
			);
		}
	}

	/**
	 * Unregisters previously registered mbean.
	 */
	public void unregisterMBean() {
		if ( ehcacheHibernateMBeanRegistration != null ) {
			ehcacheHibernateMBeanRegistration.dispose();
			ehcacheHibernateMBeanRegistration = null;
		}
	}

	/**
	 * Task for running mbean registration that can be scheduled in a timer
	 */
	private static class RegisterMBeansTask extends TimerTask {
		private static final int NUM_SECONDS = 30;
		private long startTime;
		private final AtomicBoolean mbeanRegistered = new AtomicBoolean( false );
		private final EhcacheHibernateMBeanRegistrationImpl ehcacheHibernateMBeanRegistration;
		private final CacheManager manager;
		private final Properties properties;

		public RegisterMBeansTask(
				EhcacheHibernateMBeanRegistrationImpl ehcacheHibernateMBeanRegistration,
				CacheManager manager, Properties properties) {
			this.ehcacheHibernateMBeanRegistration = ehcacheHibernateMBeanRegistration;
			this.manager = manager;
			this.properties = properties;
		}

		@Override
		public void run() {
			LOG.debug( "Running mbean initializer task for ehcache hibernate..." );
			startTime = System.currentTimeMillis();
			if ( mbeanRegistered.compareAndSet( false, true ) ) {
				try {
					ehcacheHibernateMBeanRegistration.registerMBeanForCacheManager( manager, properties );
					LOG.debug( "Successfully registered bean" );
				}
				catch (Exception e) {
					throw new CacheException( e );
				}
			}
			final SessionFactory sessionFactory = locateSessionFactory();
			if ( sessionFactory == null ) {
				LOG.debug(
						"SessionFactory is probably still being initialized..."
								+ " waiting for it to complete before enabling hibernate statistics monitoring via JMX"
				);
				if ( System.currentTimeMillis() > startTime + (NUM_SECONDS * MILLIS_PER_SECOND) ) {
					LOG.info( "Hibernate statistics monitoring through JMX is DISABLED." );
					LOG.info(
							"Failed to look up SessionFactory after " + NUM_SECONDS + " seconds using session-factory properties '"
									+ properties + "'"
					);
					this.cancel();
				}
			}
			else {
				ehcacheHibernateMBeanRegistration.enableHibernateStatisticsSupport( sessionFactory );
				LOG.info( "Hibernate statistics monitoring through JMX is ENABLED. " );
				this.cancel();
			}
		}

		private SessionFactory locateSessionFactory() {
			final String jndiName = properties.getProperty( Environment.SESSION_FACTORY_NAME );
			if ( jndiName != null ) {
				return SessionFactoryRegistry.INSTANCE.getNamedSessionFactory( jndiName );
			}
			try {
				final Class factoryType = SessionFactoryRegistry.class;
				final Field instancesField = getField( factoryType, "sessionFactoryMap" );
				instancesField.setAccessible( true );
				final Map map = (Map) instancesField.get( SessionFactoryRegistry.INSTANCE );
				if ( map == null ) {
					return null;
				}
				for ( Object o : map.values() ) {
					final SessionFactory sessionFactory = (SessionFactory) o;
					final Class sessionFactoryType = sessionFactory.getClass();
					final Field propertiesField = getField( sessionFactoryType, "properties" );
					if ( propertiesField != null ) {
						propertiesField.setAccessible( true );
						final Properties props = (Properties) propertiesField.get( sessionFactory );
						if ( props != null && props.equals( properties ) ) {
							return sessionFactory;
						}
					}
				}
			}
			catch (RuntimeException re) {
				LOG.error( "Error locating Hibernate Session Factory", re );
			}
			catch (IllegalAccessException iae) {
				LOG.error( "Error locating Hibernate Session Factory", iae );
			}
			return null;
		}
	}

	private static Field getField(Class c, String fieldName) {
		for ( Field field : c.getDeclaredFields() ) {
			if ( field.getName().equals( fieldName ) ) {
				return field;
			}
		}
		throw new NoSuchFieldError( "Type '" + c + "' has no field '" + fieldName + "'" );
	}
}
