/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.jboss.as.jpa.hibernate5;

import static org.infinispan.hibernate.cache.commons.InfinispanRegionFactory.COLLECTION_CACHE_RESOURCE_PROP;
import static org.infinispan.hibernate.cache.commons.InfinispanRegionFactory.DEF_ENTITY_RESOURCE;
import static org.infinispan.hibernate.cache.commons.InfinispanRegionFactory.DEF_QUERY_RESOURCE;
import static org.infinispan.hibernate.cache.commons.InfinispanRegionFactory.ENTITY_CACHE_RESOURCE_PROP;
import static org.infinispan.hibernate.cache.commons.InfinispanRegionFactory.IMMUTABLE_ENTITY_CACHE_RESOURCE_PROP;
import static org.infinispan.hibernate.cache.commons.InfinispanRegionFactory.INFINISPAN_CONFIG_RESOURCE_PROP;
import static org.infinispan.hibernate.cache.commons.InfinispanRegionFactory.NATURAL_ID_CACHE_RESOURCE_PROP;
import static org.infinispan.hibernate.cache.commons.InfinispanRegionFactory.PENDING_PUTS_CACHE_RESOURCE_PROP;
import static org.infinispan.hibernate.cache.commons.InfinispanRegionFactory.QUERY_CACHE_RESOURCE_PROP;
import static org.infinispan.hibernate.cache.commons.InfinispanRegionFactory.TIMESTAMPS_CACHE_RESOURCE_PROP;
import static org.jboss.as.jpa.hibernate5.infinispan.InfinispanRegionFactory.CACHE_CONTAINER;
import static org.jboss.as.jpa.hibernate5.infinispan.InfinispanRegionFactory.DEFAULT_CACHE_CONTAINER;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.hibernate.cfg.AvailableSettings;
import org.jboss.as.jpa.hibernate5.infinispan.SharedInfinispanRegionFactory;
import org.jipijapa.cache.spi.Classification;
import org.jipijapa.event.impl.internal.Notification;

/**
 * Second level cache setup.
 *
 * @author Scott Marlow
 */
public class HibernateSecondLevelCache {

    private static final String DEFAULT_REGION_FACTORY = SharedInfinispanRegionFactory.class.getName();

    public static final String CACHE_TYPE = "cachetype";    // shared (jpa) or private (for native applications)
    public static final String CONTAINER = "container";
    public static final String NAME = "name";
    public static final String CACHES = "caches";

    public static void addSecondLevelCacheDependencies(Properties mutableProperties, String scopedPersistenceUnitName) {

        if (mutableProperties.getProperty(AvailableSettings.CACHE_REGION_PREFIX) == null) {
            // cache entries for this PU will be identified by scoped pu name + Entity class name

            if (scopedPersistenceUnitName != null) {
                mutableProperties.setProperty(AvailableSettings.CACHE_REGION_PREFIX, scopedPersistenceUnitName);
            }
        }
        String regionFactory = mutableProperties.getProperty(AvailableSettings.CACHE_REGION_FACTORY);
        if (regionFactory == null) {
            regionFactory = DEFAULT_REGION_FACTORY;
            mutableProperties.setProperty(AvailableSettings.CACHE_REGION_FACTORY, regionFactory);
        }
        if (regionFactory.equals(DEFAULT_REGION_FACTORY)) {
            // Set infinispan defaults
            String container = mutableProperties.getProperty(CACHE_CONTAINER);
            if (container == null) {
                container = DEFAULT_CACHE_CONTAINER;
                mutableProperties.setProperty(CACHE_CONTAINER, container);
            }

            /**
             * AS will need the ServiceBuilder<?> builder that used to be passed to PersistenceProviderAdaptor.addProviderDependencies
             */
            Properties cacheSettings = new Properties();
            cacheSettings.setProperty(CONTAINER, container);
            cacheSettings.setProperty(CACHES, String.join(" ", findCaches(mutableProperties)));

            Notification.addCacheDependencies(Classification.INFINISPAN, cacheSettings);
        }
    }

    public static Set<String> findCaches(Properties properties) {
        Set<String> caches = new HashSet<>();

        caches.add(properties.getProperty(ENTITY_CACHE_RESOURCE_PROP, DEF_ENTITY_RESOURCE));
        caches.add(properties.getProperty(IMMUTABLE_ENTITY_CACHE_RESOURCE_PROP, DEF_ENTITY_RESOURCE));
        caches.add(properties.getProperty(COLLECTION_CACHE_RESOURCE_PROP, DEF_ENTITY_RESOURCE));
        caches.add(properties.getProperty(NATURAL_ID_CACHE_RESOURCE_PROP, DEF_ENTITY_RESOURCE));
        if (properties.containsKey(PENDING_PUTS_CACHE_RESOURCE_PROP)) {
            caches.add(properties.getProperty(PENDING_PUTS_CACHE_RESOURCE_PROP));
        }
        if (Boolean.parseBoolean(properties.getProperty(AvailableSettings.USE_QUERY_CACHE))) {
            caches.add(properties.getProperty(QUERY_CACHE_RESOURCE_PROP, DEF_QUERY_RESOURCE));
            caches.add(properties.getProperty(TIMESTAMPS_CACHE_RESOURCE_PROP, DEF_QUERY_RESOURCE));
        }

        int length = INFINISPAN_CONFIG_RESOURCE_PROP.length();
        String customRegionPrefix = INFINISPAN_CONFIG_RESOURCE_PROP.substring(0, length - 3) + properties.getProperty(AvailableSettings.CACHE_REGION_PREFIX, "");
        String customRegionSuffix = INFINISPAN_CONFIG_RESOURCE_PROP.substring(length - 4, length);

        for (String propertyName : properties.stringPropertyNames()) {
            if (propertyName.startsWith(customRegionPrefix) && propertyName.endsWith(customRegionSuffix)) {
                caches.add(properties.getProperty(propertyName));
            }
        }

        return caches;
    }
}
