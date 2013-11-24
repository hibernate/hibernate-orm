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
package org.hibernate.cache.ehcache;

import java.net.URL;
import java.util.Properties;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.ehcache.internal.util.HibernateEhcacheUtils;
import org.hibernate.cfg.Settings;

import org.jboss.logging.Logger;

/**
 * A non-singleton EhCacheRegionFactory implementation.
 *
 * @author Chris Dennis
 * @author Greg Luck
 * @author Emmanuel Bernard
 * @author Abhishek Sanoujam
 * @author Alex Snaps
 */
public class EhCacheRegionFactory extends AbstractEhcacheRegionFactory {
	private static final EhCacheMessageLogger LOG = Logger.getMessageLogger(
			EhCacheMessageLogger.class,
			EhCacheRegionFactory.class.getName()
	);

	/**
	 * Creates a non-singleton EhCacheRegionFactory
	 */
	@SuppressWarnings("UnusedDeclaration")
	public EhCacheRegionFactory() {
	}

	/**
	 * Creates a non-singleton EhCacheRegionFactory
	 *
	 * @param prop Not used
	 */
	@SuppressWarnings("UnusedDeclaration")
	public EhCacheRegionFactory(Properties prop) {
		super();
	}

	@Override
	public void start(Settings settings, Properties properties) throws CacheException {
		this.settings = settings;
		if ( manager != null ) {
			LOG.attemptToRestartAlreadyStartedEhCacheProvider();
			return;
		}

		try {
			String configurationResourceName = null;
			if ( properties != null ) {
				configurationResourceName = (String) properties.get( NET_SF_EHCACHE_CONFIGURATION_RESOURCE_NAME );
			}
			if ( configurationResourceName == null || configurationResourceName.length() == 0 ) {
				final Configuration configuration = ConfigurationFactory.parseConfiguration();
				manager = new CacheManager( configuration );
			}
			else {
				final URL url = loadResource( configurationResourceName );
				final Configuration configuration = HibernateEhcacheUtils.loadAndCorrectConfiguration( url );
				manager = new CacheManager( configuration );
			}
			mbeanRegistrationHelper.registerMBean( manager, properties );
		}
		catch (net.sf.ehcache.CacheException e) {
			if ( e.getMessage().startsWith(
					"Cannot parseConfiguration CacheManager. Attempt to create a new instance of " +
							"CacheManager using the diskStorePath"
			) ) {
				throw new CacheException(
						"Attempt to restart an already started EhCacheRegionFactory. " +
								"Use sessionFactory.close() between repeated calls to buildSessionFactory. " +
								"Consider using SingletonEhCacheRegionFactory. Error from ehcache was: " + e.getMessage()
				);
			}
			else {
				throw new CacheException( e );
			}
		}
	}

	@Override
	public void stop() {
		try {
			if ( manager != null ) {
				mbeanRegistrationHelper.unregisterMBean();
				manager.shutdown();
				manager = null;
			}
		}
		catch (net.sf.ehcache.CacheException e) {
			throw new CacheException( e );
		}
	}

}
