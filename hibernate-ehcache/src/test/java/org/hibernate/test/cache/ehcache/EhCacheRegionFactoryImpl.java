package org.hibernate.test.cache.ehcache;

import java.lang.reflect.Field;
import java.util.Map;

import org.hibernate.cache.ehcache.EhCacheRegionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

/**
 * @author Alex Snaps
 */
public class EhCacheRegionFactoryImpl extends EhCacheTest {

	@Override
	protected void configCache(final Configuration cfg) {
		cfg.setProperty( Environment.CACHE_REGION_FACTORY, EhCacheRegionFactory.class.getName() );
		cfg.setProperty( Environment.CACHE_PROVIDER_CONFIG, "ehcache.xml" );
	}

	@Override
	protected Map getMapFromCacheEntry(final Object entry) {
		final Map map;
		if ( "org.hibernate.cache.ehcache.internal.strategy.AbstractReadWriteEhcacheAccessStrategy$Item".equals(
				entry.getClass()
						.getName()
		) ) {
			try {
				Field field = entry.getClass().getDeclaredField( "value" );
				field.setAccessible( true );
				map = (Map) field.get( entry );
			}
			catch ( NoSuchFieldException e ) {
				throw new RuntimeException( e );
			}
			catch ( IllegalAccessException e ) {
				throw new RuntimeException( e );
			}
		}
		else {
			map = (Map) entry;
		}
		return map;
	}
}
