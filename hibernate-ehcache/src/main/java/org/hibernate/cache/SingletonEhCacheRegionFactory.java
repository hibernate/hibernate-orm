package org.hibernate.cache;

import java.util.Properties;

/**
 * @author Alex Snaps
 */
public class SingletonEhCacheRegionFactory extends DelegatingRegionFactory {
    public SingletonEhCacheRegionFactory(Properties properties) {
        super(new net.sf.ehcache.hibernate.SingletonEhCacheRegionFactory(properties));
    }
}