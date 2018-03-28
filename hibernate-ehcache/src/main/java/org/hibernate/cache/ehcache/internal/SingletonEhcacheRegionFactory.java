/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.ehcache.internal;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.ehcache.CacheManager;

import net.sf.ehcache.config.Configuration;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.engine.config.spi.ConfigurationService;

import org.jboss.logging.Logger;

import static org.hibernate.cache.ehcache.ConfigSettings.EHCACHE_CONFIGURATION_RESOURCE_NAME;
import static org.hibernate.cache.ehcache.internal.HibernateEhcacheUtils.overwriteCacheManagerIfConfigured;
import static org.hibernate.cache.ehcache.internal.HibernateEhcacheUtils.setCacheManagerNameIfNeeded;

/**
 * @author Steve Ebersole
 */
public class SingletonEhcacheRegionFactory extends EhcacheRegionFactory {
	private static final Logger LOG = Logger.getLogger( SingletonEhcacheRegionFactory.class );

	private static final AtomicInteger REFERENCE_COUNT = new AtomicInteger();

	@Override
	protected CacheManager resolveCacheManager(SessionFactoryOptions settings, Map properties) {
		try {
			String configurationResourceName = getOptions().getServiceRegistry()
					.getService( ConfigurationService.class )
					.getSetting( EHCACHE_CONFIGURATION_RESOURCE_NAME, value -> value == null ? null : value.toString() );

			if ( configurationResourceName == null || configurationResourceName.length() == 0 ) {
				try {
					REFERENCE_COUNT.incrementAndGet();
					return CacheManager.create();
				}
				catch (RuntimeException e) {
					REFERENCE_COUNT.decrementAndGet();
					throw e;
				}
			}

			URL url;
			try {
				url = new URL( configurationResourceName );
			}
			catch (MalformedURLException e) {
				if ( !configurationResourceName.startsWith( "/" ) ) {
					configurationResourceName = "/" + configurationResourceName;
					LOG.debugf(
							"prepending / to %s. It should be placed in the root of the classpath rather than in a package.",
							configurationResourceName
					);
				}
				url = loadResource( configurationResourceName );
			}

			try {
				REFERENCE_COUNT.incrementAndGet();
				Configuration config = HibernateEhcacheUtils.loadAndCorrectConfiguration( url );
				setCacheManagerNameIfNeeded( settings, config, properties );
				return CacheManager.create( config );
			}
			catch (RuntimeException e) {
				REFERENCE_COUNT.decrementAndGet();
				throw e;
			}
		}
		catch (net.sf.ehcache.CacheException e) {
			throw new CacheException( e );
		}
	}

	@Override
	protected void releaseFromUse() {
		if ( REFERENCE_COUNT.decrementAndGet() == 0 ) {
			super.releaseFromUse();
		}
	}
}
