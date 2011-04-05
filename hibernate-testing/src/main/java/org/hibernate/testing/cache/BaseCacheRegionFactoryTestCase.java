package org.hibernate.testing.cache;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * @author Alex Snaps
 */
public abstract class BaseCacheRegionFactoryTestCase extends BaseCacheTestCase {
	public BaseCacheRegionFactoryTestCase(String x) {
		super(x);
	}

	@Override
	protected void configCache(final Configuration cfg) {
		cfg.setProperty(Environment.CACHE_REGION_FACTORY, getCacheRegionFactory().getName());

		if (getConfigResourceKey() != null) {
			cfg.setProperty(getConfigResourceKey(), getConfigResourceLocation());
		}
	}

	protected abstract Class getCacheRegionFactory();

	protected Map getMapFromCachedEntry(final Object entry) {
		final Map map;
		if ("net.sf.ehcache.hibernate.strategy.AbstractReadWriteEhcacheAccessStrategy$Item".equals(entry.getClass().getName())) {
			try {
				Field field = entry.getClass().getDeclaredField("value");
				field.setAccessible(true);
				map = (Map)field.get(entry);
			} catch (NoSuchFieldException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		} else {
			map = (Map)entry;
		}
		return map;
	}
}
