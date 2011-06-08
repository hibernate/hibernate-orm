package org.hibernate.test.cache.ehcache;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import java.util.Map;

import net.sf.ehcache.hibernate.EhCacheRegionFactory;

/**
 * @author Alex Snaps
 */
public class EhCacheRegionTest extends EhCacheTest {
	@Override
	protected void configCache(final Configuration cfg) {
		cfg.setProperty( Environment.CACHE_REGION_FACTORY, EhCacheRegionFactory.class.getName() );
		cfg.setProperty( Environment.CACHE_PROVIDER_CONFIG, "ehcache.xml" );
	}

	@Override
	protected Map getMapFromCacheEntry(final Object entry) {
		final Map map;
//		if ( entry instanceof Item ) {
//			map = (Map) ( (Item) entry ).getValue();
//		}
//		else {
			map = (Map) entry;
//		}
		return map;
	}
}
