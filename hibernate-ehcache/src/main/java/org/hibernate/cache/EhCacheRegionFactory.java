package org.hibernate.cache;

import java.util.Properties;

/**
 * @author Alex Snaps
 */
public class EhCacheRegionFactory extends DelegatingRegionFactory {

    public EhCacheRegionFactory(final Properties properties) {
        super(new net.sf.ehcache.hibernate.EhCacheRegionFactory(properties));
    }
}