//$Id: EhCacheProvider.java 9964 2006-05-30 15:40:54Z epbernard $
/**
 *  Copyright 2003-2006 Greg Luck, Jboss Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hibernate.cache;

import java.util.Properties;
import java.net.URL;

import net.sf.ehcache.CacheManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.cfg.Environment;
import org.hibernate.util.StringHelper;
import org.hibernate.util.ConfigHelper;

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

    private static final Log log = LogFactory.getLog(EhCacheProvider.class);

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
                log.warn("Could not find configuration [" + name + "]; using defaults.");
                manager.addCache(name);
                cache = manager.getCache(name);
                log.debug("started EHCache region: " + name);
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
            log.warn("Attempt to restart an already started EhCacheProvider. Use sessionFactory.close() " +
                    " between repeated calls to buildSessionFactory. Using previously created EhCacheProvider." +
                    " If this behaviour is required, consider using net.sf.ehcache.hibernate.SingletonEhCacheProvider.");
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
            } else {
                throw e;
            }
        }
	}

	private URL loadResource(String configurationResourceName) {
		URL url = ConfigHelper.locateConfig( configurationResourceName );
        if (log.isDebugEnabled()) {
            log.debug("Creating EhCacheProvider from a specified resource: "
                    + configurationResourceName + " Resolved to URL: " + url);
        }
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
