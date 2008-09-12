/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.cache;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.util.ClassLoaderUtil;

import java.net.URL;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Singleton cache Provider plugin for Hibernate 3.2 and ehcache-1.2. New in this provider is support for
 * non Serializable keys and values. This provider works as a Singleton. No matter how many Hibernate Configurations
 * you have, only one ehcache CacheManager is used. See EhCacheProvider for a non-singleton implementation.
 * <p/>
 * Ehcache-1.2 also has many other features such as cluster support and listeners, which can be used seamlessly simply
 * by configurion in ehcache.xml.
 * <p/>
 * Use <code>hibernate.cache.provider_class=net.sf.ehcache.hibernate.SingletonEhCacheProvider</code> in the Hibernate configuration
 * to enable this provider for Hibernate's second level cache.
 * <p/>
 * Updated for ehcache-1.2. Note this provider requires ehcache-1.2.jar. Make sure ehcache-1.1.jar or earlier
 * is not in the classpath or it will not work.
 * <p/>
 * See http://ehcache.sf.net for documentation on ehcache
 * <p/>
 *
 * @author Greg Luck
 * @author Emmanuel Bernard
 * @version $Id: SingletonEhCacheProvider.java 744 2008-08-16 20:10:49Z gregluck $
 */
public final class SingletonEhCacheProvider implements CacheProvider {

	/**
	 * The Hibernate system property specifying the location of the ehcache configuration file name.
	 * <p/
	 * If not set, ehcache.xml will be looked for in the root of the classpath.
	 * <p/>
	 * If set to say ehcache-1.xml, ehcache-1.xml will be looked for in the root of the classpath.
	 */
	public static final String NET_SF_EHCACHE_CONFIGURATION_RESOURCE_NAME = "net.sf.ehcache.configurationResourceName";

	private static final Logger LOG = Logger.getLogger( SingletonEhCacheProvider.class.getName() );

	/**
	 * To be backwardly compatible with a lot of Hibernate code out there, allow multiple starts and stops on the
	 * one singleton CacheManager. Keep a count of references to only stop on when only one reference is held.
	 */
	private static int referenceCount;

	private CacheManager manager;


	/**
	 * Builds a Cache.
	 * <p/>
	 * Even though this method provides properties, they are not used.
	 * Properties for EHCache are specified in the ehcache.xml file.
	 * Configuration will be read from ehcache.xml for a cache declaration
	 * where the name attribute matches the name parameter in this builder.
	 *
	 * @param name the name of the cache. Must match a cache configured in ehcache.xml
	 * @param properties not used
	 *
	 * @return a newly built cache will be built and initialised
	 *
	 * @throws org.hibernate.cache.CacheException
	 *          inter alia, if a cache of the same name already exists
	 */
	public final Cache buildCache(String name, Properties properties) throws CacheException {
		try {
			net.sf.ehcache.Ehcache cache = manager.getEhcache( name );
			if ( cache == null ) {
				SingletonEhCacheProvider.LOG.warning(
						"Could not find a specific ehcache configuration for cache named ["
								+ name + "]; using defaults."
				);
				manager.addCache( name );
				cache = manager.getEhcache( name );
				SingletonEhCacheProvider.LOG.fine( "started EHCache region: " + name );
			}
			return new EhCache( cache );
		}
		catch ( net.sf.ehcache.CacheException e ) {
			throw new CacheException( e );
		}
	}

	/**
	 * Returns the next timestamp.
	 */
	public final long nextTimestamp() {
		return Timestamper.next();
	}

	/**
	 * Callback to perform any necessary initialization of the underlying cache implementation
	 * during SessionFactory construction.
	 * <p/>
	 *
	 * @param properties current configuration settings.
	 */
	public final void start(Properties properties) throws CacheException {
		String configurationResourceName = null;
		if ( properties != null ) {
			configurationResourceName = ( String ) properties.get( NET_SF_EHCACHE_CONFIGURATION_RESOURCE_NAME );
		}
		if ( configurationResourceName == null || configurationResourceName.length() == 0 ) {
			manager = CacheManager.create();
			referenceCount++;
		}
		else {
			if ( !configurationResourceName.startsWith( "/" ) ) {
				configurationResourceName = "/" + configurationResourceName;
				if ( LOG.isLoggable( Level.FINE ) ) {
					LOG.fine(
							"prepending / to " + configurationResourceName + ". It should be placed in the root"
									+ "of the classpath rather than in a package."
					);
				}
			}
			URL url = loadResource( configurationResourceName );
			manager = CacheManager.create( url );
			referenceCount++;
		}
	}

	private URL loadResource(String configurationResourceName) {
		ClassLoader standardClassloader = ClassLoaderUtil.getStandardClassLoader();
		URL url = null;
		if ( standardClassloader != null ) {
			url = standardClassloader.getResource( configurationResourceName );
		}
		if ( url == null ) {
			url = this.getClass().getResource( configurationResourceName );
		}
		if ( LOG.isLoggable( Level.FINE ) ) {
			LOG.fine(
					"Creating EhCacheProvider from a specified resource: "
							+ configurationResourceName + " Resolved to URL: " + url
			);
		}
		if ( url == null ) {
			if ( LOG.isLoggable( Level.WARNING ) ) {
				LOG.warning(
						"A configurationResourceName was set to " + configurationResourceName +
								" but the resource could not be loaded from the classpath." +
								"Ehcache will configure itself using defaults."
				);
			}
		}
		return url;
	}

	/**
	 * Callback to perform any necessary cleanup of the underlying cache implementation
	 * during SessionFactory.close().
	 */
	public void stop() {
		if ( manager != null ) {
			referenceCount--;
			if ( referenceCount == 0 ) {
				manager.shutdown();
			}
			manager = null;
		}
	}

	/**
	 * Not sure what this is supposed to do.
	 *
	 * @return false to be safe
	 */
	public final boolean isMinimalPutsEnabledByDefault() {
		return false;
	}

}
