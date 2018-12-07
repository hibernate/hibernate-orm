/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.ehcache.test;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cache.ehcache.ConfigSettings;
import org.hibernate.cache.ehcache.internal.EhcacheRegionFactory;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.cache.spi.support.SimpleTimestamper;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EhcacheLockTimeoutStringConfigurationTest
		extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected void addSettings(Map settings) {
		settings.put( Environment.CACHE_REGION_FACTORY, "ehcache" );
		settings.put( ConfigSettings.EHCACHE_CONFIGURATION_CACHE_LOCK_TIMEOUT, "1000" );
	}

	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Event.class
		};
	}

	@Entity(name = "Event")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class Event {

		@Id
		@GeneratedValue
		private Long id;
	}

	@Test
	public void test() {
		CacheImplementor cacheImplementor = sessionFactory().getCache();
		EhcacheRegionFactory regionFactory = (EhcacheRegionFactory) cacheImplementor.getRegion( Event.class.getName() ).getRegionFactory();
		assertEquals( TimeUnit.SECONDS.toMillis( 1 ) * SimpleTimestamper.ONE_MS, regionFactory.getTimeout() );
	}

}
