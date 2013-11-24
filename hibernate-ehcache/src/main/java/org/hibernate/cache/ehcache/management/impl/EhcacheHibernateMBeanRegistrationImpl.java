/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.cache.ehcache.management.impl;

import java.lang.management.ManagementFactory;
import java.util.Properties;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Status;
import net.sf.ehcache.event.CacheManagerEventListener;

import org.hibernate.SessionFactory;
import org.hibernate.cache.ehcache.EhCacheMessageLogger;
import org.hibernate.cfg.Environment;

import org.jboss.logging.Logger;

/**
 * Implementation of {@link EhcacheHibernateMBeanRegistration}.
 * Also implements {@link net.sf.ehcache.event.CacheManagerEventListener}. Deregisters mbeans when the associated cachemanager is shutdown.
 * <p/>
 * <p/>
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 */
public class EhcacheHibernateMBeanRegistrationImpl
		implements EhcacheHibernateMBeanRegistration, CacheManagerEventListener {

	private static final EhCacheMessageLogger LOG = Logger.getMessageLogger(
			EhCacheMessageLogger.class,
			EhcacheHibernateMBeanRegistrationImpl.class.getName()
	);
	private static final int MAX_MBEAN_REGISTRATION_RETRIES = 50;
	private Status status = Status.STATUS_UNINITIALISED;
	private volatile EhcacheHibernate ehcacheHibernate;
	private volatile ObjectName cacheManagerObjectName;

	@Override
	public synchronized void registerMBeanForCacheManager(final CacheManager manager, final Properties properties)
			throws Exception {
		final String sessionFactoryName = properties.getProperty( Environment.SESSION_FACTORY_NAME );
		final String name;
		if ( sessionFactoryName == null ) {
			name = manager.getName();
		}
		else {
			name = "".equals( sessionFactoryName.trim() ) ? manager.getName() : sessionFactoryName;
		}
		registerBean( name, manager );
	}

	private void registerBean(String name, CacheManager manager) throws Exception {
		ehcacheHibernate = new EhcacheHibernate( manager );
		int tries = 0;
		boolean success;
		Exception exception = null;
		final String cacheManagerClusterUUID = manager.getClusterUUID();
		String registeredCacheManagerName;
		do {
			registeredCacheManagerName = name;
			if ( tries != 0 ) {
				registeredCacheManagerName += "_" + tries;
			}
			try {
				// register the CacheManager MBean
				final MBeanServer mBeanServer = getMBeanServer();
				cacheManagerObjectName = EhcacheHibernateMbeanNames.getCacheManagerObjectName(
						cacheManagerClusterUUID,
						registeredCacheManagerName
				);
				mBeanServer.registerMBean( ehcacheHibernate, cacheManagerObjectName );
				success = true;
				break;
			}
			catch (InstanceAlreadyExistsException e) {
				success = false;
				exception = e;
			}
			tries++;
		} while ( tries < MAX_MBEAN_REGISTRATION_RETRIES );
		if ( !success ) {
			throw new Exception(
					"Cannot register mbean for CacheManager with name" + manager.getName() + " after "
							+ MAX_MBEAN_REGISTRATION_RETRIES + " retries. Last tried name=" + registeredCacheManagerName,
					exception
			);
		}
		status = Status.STATUS_ALIVE;
	}

	private MBeanServer getMBeanServer() {
		return ManagementFactory.getPlatformMBeanServer();
	}

	@Override
	public void enableHibernateStatisticsSupport(SessionFactory sessionFactory) {
		ehcacheHibernate.enableHibernateStatistics( sessionFactory );
	}

	@Override
	public synchronized void dispose() throws CacheException {
		if ( status == Status.STATUS_SHUTDOWN ) {
			return;
		}

		try {
			getMBeanServer().unregisterMBean( cacheManagerObjectName );
		}
		catch (Exception e) {
			LOG.warn(
					"Error unregistering object instance " + cacheManagerObjectName + " . Error was " + e.getMessage(),
					e
			);
		}
		ehcacheHibernate = null;
		cacheManagerObjectName = null;
		status = Status.STATUS_SHUTDOWN;
	}

	@Override
	public synchronized Status getStatus() {
		return status;
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * NOTE : No-op in this case
	 */
	@Override
	public void init() throws CacheException {
		// no-op
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * NOTE : No-op in this case
	 */
	@Override
	public void notifyCacheAdded(String cacheName) {
		// no-op
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * NOTE : No-op in this case
	 */
	@Override
	public void notifyCacheRemoved(String cacheName) {
		// no-op
	}

}
