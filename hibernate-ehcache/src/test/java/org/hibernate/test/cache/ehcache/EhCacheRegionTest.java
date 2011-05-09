package org.hibernate.test.cache.ehcache;

import org.hibernate.cache.spi.ReadWriteCache.Item;
import org.hibernate.cache.internal.EhCacheProvider;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import java.util.Map;

/**
 * @author Alex Snaps
 */
public class EhCacheRegionTest extends EhCacheTest {
	@Override
	protected void configCache(final Configuration cfg) {
		cfg.setProperty( Environment.CACHE_PROVIDER, EhCacheProvider.class.getName() );
		cfg.setProperty( Environment.CACHE_PROVIDER_CONFIG, "ehcache.xml" );
	}

	@Override
	protected Map getMapFromCacheEntry(final Object entry) {
		final Map map;
		if ( entry instanceof Item ) {
			map = (Map) ( (Item) entry ).getValue();
		}
		else {
			map = (Map) entry;
		}
		return map;
	}
}
