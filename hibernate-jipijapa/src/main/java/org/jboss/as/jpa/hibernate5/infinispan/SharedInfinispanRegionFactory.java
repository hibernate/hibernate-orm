/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.jboss.as.jpa.hibernate5.infinispan;

import java.util.Properties;

import org.hibernate.cache.CacheException;
import org.hibernate.service.ServiceRegistry;
import org.infinispan.AdvancedCache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.jpa.hibernate5.HibernateSecondLevelCache;
import org.jipijapa.cache.spi.Classification;
import org.jipijapa.cache.spi.Wrapper;
import org.jipijapa.event.impl.internal.Notification;

/**
 * Infinispan-backed region factory that retrieves its cache manager from the Infinispan subsystem.
 * This is used for (JPA) container managed persistence contexts.
 * Each deployment application will use a unique Hibernate cache region name in the shared cache.
 *
 * @author Paul Ferraro
 * @author Scott Marlow
 */
public class SharedInfinispanRegionFactory extends InfinispanRegionFactory {
    private static final long serialVersionUID = -3277051412715973863L;
    private volatile Wrapper wrapper;
    public SharedInfinispanRegionFactory() {
        super();
    }

    public SharedInfinispanRegionFactory(Properties props) {
        super(props);
    }

    @Override
    protected EmbeddedCacheManager createCacheManager(Properties properties, final ServiceRegistry serviceRegistry) {
        String container = properties.getProperty(CACHE_CONTAINER, DEFAULT_CACHE_CONTAINER);
        Properties cacheSettings = new Properties();
        cacheSettings.put(HibernateSecondLevelCache.CONTAINER, container);
        try {
            // Get the (shared) cache manager for JPA application use
            wrapper = Notification.startCache(Classification.INFINISPAN, cacheSettings);
            return (EmbeddedCacheManager)wrapper.getValue();
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }

    /**
     * Do not attempt to stop our cache manager because it wasn't created by this region factory.
     * Base class stop() will call the base stopCacheRegions()
     */
    @Override
    protected void stopCacheManager() {
        // notify that the cache is not used but skip the stop since its shared for all jpa applications.
        Notification.stopCache(Classification.INFINISPAN, wrapper);
    }


    @SuppressWarnings("rawtypes")
    @Override
    protected AdvancedCache createCacheWrapper(AdvancedCache cache) {
        cache.start();
        return cache;
    }
}
