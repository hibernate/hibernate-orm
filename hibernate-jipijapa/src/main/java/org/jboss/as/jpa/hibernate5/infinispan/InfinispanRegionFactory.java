/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.jboss.as.jpa.hibernate5.infinispan;

import java.util.Properties;

import org.hibernate.cache.CacheException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.service.ServiceRegistry;
import org.infinispan.AdvancedCache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.jpa.hibernate5.HibernateSecondLevelCache;
import org.jipijapa.cache.spi.Classification;
import org.jipijapa.cache.spi.Wrapper;
import org.jipijapa.event.impl.internal.Notification;

/**
 * Infinispan-backed region factory for use with standalone (i.e. non-JPA) Hibernate applications.
 * @author Paul Ferraro
 * @author Scott Marlow
 */
public class InfinispanRegionFactory extends org.infinispan.hibernate.cache.commons.InfinispanRegionFactory {
    private static final long serialVersionUID = 6526170943015350422L;

    public static final String CACHE_CONTAINER = "hibernate.cache.infinispan.container";
    public static final String DEFAULT_CACHE_CONTAINER = "hibernate";
    public static final String CACHE_PRIVATE = "private";

    private volatile Wrapper wrapper;

    public InfinispanRegionFactory() {
        super();
    }

    public InfinispanRegionFactory(Properties props) {
        super(props);
    }

    @Override
    protected EmbeddedCacheManager createCacheManager(Properties properties, final ServiceRegistry serviceRegistry) throws CacheException {
        // Find a suitable service name to represent this session factory instance
        String name = properties.getProperty(AvailableSettings.SESSION_FACTORY_NAME);
        String container = properties.getProperty(CACHE_CONTAINER, DEFAULT_CACHE_CONTAINER);
        HibernateSecondLevelCache.addSecondLevelCacheDependencies(properties, null);

        Properties cacheSettings = new Properties();
        cacheSettings.setProperty(HibernateSecondLevelCache.CACHE_TYPE, CACHE_PRIVATE);
        cacheSettings.setProperty(HibernateSecondLevelCache.CONTAINER, container);
        if (name != null) {
            cacheSettings.setProperty(HibernateSecondLevelCache.NAME, name);
        }
        cacheSettings.setProperty(HibernateSecondLevelCache.CACHES, String.join(" ", HibernateSecondLevelCache.findCaches(properties)));

        try {
            // start a private cache for non-JPA use and return the started cache.
            wrapper = Notification.startCache(Classification.INFINISPAN, cacheSettings);
            return (EmbeddedCacheManager)wrapper.getValue();
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }

    @Override
    protected void stopCacheManager() {
        // stop the private cache
        Notification.stopCache(Classification.INFINISPAN, wrapper);
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected AdvancedCache createCacheWrapper(AdvancedCache cache) {
        return cache;
    }
}
