/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
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
 */
package org.hibernate.cache;

import java.net.URL;
import java.util.Properties;
import net.sf.ehcache.CacheManager;
import org.hibernate.cfg.Environment;
import org.hibernate.internal.util.ConfigHelper;
import org.hibernate.internal.util.StringHelper;
import org.jboss.logging.Logger;

/**
 * Cache Provider plugin for Hibernate
 *
 * Use <code>hibernate.cache.provider_class=org.hibernate.cache.EhCacheProvider</code>
 * in Hibernate 3.x or later
 *
 * Taken from EhCache 0.9 distribution
 * @author Greg Luck
 * @author Emmanuel Bernard
 */
/**
 * Cache Provider plugin for ehcache-1.2. New in this provider are ehcache support for multiple
 * Hibernate session factories, each with its own ehcache configuration, and non Serializable keys and values.
 * Ehcache-1.2 also has many other features such as cluster support and listeners, which can be used seamlessly simply
 * by configurion in ehcache.xml.
 * <p/>
 * Use <code>hibernate.cache.provider_class=org.hibernate.cache.EhCacheProvider</code> in the Hibernate configuration
 * to enable this provider for Hibernate's second level cache.
 * <p/>
 * When configuring multiple ehcache CacheManagers, as you would where you have multiple Hibernate Configurations and
 * multiple SessionFactories, specify in each Hibernate configuration the ehcache configuration using
 * the property <code>hibernate.cache.provider_configuration_file_resource_path</code> An example to set an ehcache configuration
 * called ehcache-2.xml would be <code>hibernate.cache.provider_configuration_file_resource_path=/ehcache-2.xml</code>. If the leading
 * slash is not there one will be added. The configuration file will be looked for in the root of the classpath.
 * <p/>
 * Updated for ehcache-1.2. Note this provider requires ehcache-1.2.jar. Make sure ehcache-1.1.jar or earlier
 * is not in the classpath or it will not work.
 * <p/>
 * See http://ehcache.sf.net for documentation on ehcache
 * <p/>
 *
 * @author Greg Luck
 * @author Emmanuel Bernard
 */
public class EhCacheProvider implements CacheProvider {

    private static final EhCacheLogger LOG = Logger.getMessageLogger(EhCacheLogger.class, EhCacheProvider.class.getName());

	private CacheManager manager;

    /**
     * Builds a Cache.
     * <p>
     * Even though this method provides properties, they are not used.
     * Properties for EHCache are specified in the ehcache.xml file.
     * Configuration will be read from ehcache.xml for a cache declaration
     * where the name attribute matches the name parameter in this builder.
     *
     * @param name the name of the cache. Must match a cache configured in ehcache.xml
     * @param properties not used
     * @return a newly built cache will be built and initialised
     * @throws CacheException inter alia, if a cache of the same name already exists
     */
    public Cache buildCache(String name, Properties properties) throws CacheException {
	    try {
            net.sf.ehcache.Cache cache = manager.getCache(name);
            if (cache == null) {
                LOG.unableToFindConfiguration(name);
                manager.addCache(name);
                cache = manager.getCache(name);
                LOG.debugf("Started EHCache region: %s", name);
            }
            return new EhCache(cache);
	    }
        catch (net.sf.ehcache.CacheException e) {
            throw new CacheException(e);
        }
    }

    /**
     * Returns the next timestamp.
     */
    public long nextTimestamp() {
        return Timestamper.next();
    }

	/**
	 * Callback to perform any necessary initialization of the underlying cache implementation
	 * during SessionFactory construction.
	 *
	 * @param properties current configuration settings.
	 */
	public void start(Properties properties) throws CacheException {
		if (manager != null) {
            LOG.attemptToRestartAlreadyStartedEhCacheProvider();
            return;
        }
        try {
            String configurationResourceName = null;
            if (properties != null) {
                configurationResourceName = (String) properties.get( Environment.CACHE_PROVIDER_CONFIG );
            }
            if ( StringHelper.isEmpty( configurationResourceName ) ) {
                manager = new CacheManager();
            } else {
                URL url = loadResource(configurationResourceName);
                manager = new CacheManager(url);
            }
        } catch (net.sf.ehcache.CacheException e) {
			//yukky! Don't you have subclasses for that!
			//TODO race conditions can happen here
			if (e.getMessage().startsWith("Cannot parseConfiguration CacheManager. Attempt to create a new instance of " +
                    "CacheManager using the diskStorePath")) {
                throw new CacheException("Attempt to restart an already started EhCacheProvider. Use sessionFactory.close() " +
                    " between repeated calls to buildSessionFactory. Consider using net.sf.ehcache.hibernate.SingletonEhCacheProvider."
						, e );
            }
            throw e;
        }
	}

	private URL loadResource(String configurationResourceName) {
		URL url = ConfigHelper.locateConfig( configurationResourceName );
        LOG.debugf("Creating EhCacheProvider from a specified resource: %s Resolved to URL: %s", configurationResourceName, url);
        return url;
    }

	/**
	 * Callback to perform any necessary cleanup of the underlying cache implementation
	 * during SessionFactory.close().
	 */
	public void stop() {
		if (manager != null) {
            manager.shutdown();
            manager = null;
        }
	}

	public boolean isMinimalPutsEnabledByDefault() {
		return false;
	}

}
