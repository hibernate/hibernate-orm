package org.hibernate.test.cache.ehcache;

import java.util.Map;

import org.hibernate.cache.ehcache.EhCacheRegionFactory;
import org.hibernate.cache.ehcache.internal.strategy.ItemValueExtractor;
import org.hibernate.cfg.Environment;

/**
 * @author Alex Snaps
 */
public class EhCacheRegionTest extends EhCacheTest {
	@Override
	@SuppressWarnings("unchecked")
	protected void addSettings(Map settings) {
		super.addSettings( settings );

		settings.put( Environment.CACHE_REGION_FACTORY, EhCacheRegionFactory.class.getName() );
		settings.put( Environment.CACHE_PROVIDER_CONFIG, "ehcache.xml" );
	}

	@Override
	protected Map getMapFromCacheEntry(final Object entry) {
		final Map map;
		if ( entry.getClass()
				.getName()
				.equals( "org.hibernate.cache.ehcache.internal.strategy.AbstractReadWriteEhcacheAccessStrategy$Item" ) ) {
			map = ItemValueExtractor.getValue( entry );
		}
		else {
			map = (Map) entry;
		}
		return map;
	}
}
