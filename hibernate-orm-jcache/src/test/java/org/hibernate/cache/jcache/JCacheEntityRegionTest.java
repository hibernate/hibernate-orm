package org.hibernate.cache.jcache;

import javax.cache.Cache;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.Settings;

/**
 * @author Alex Snaps
 */
public class JCacheEntityRegionTest {

	private JCacheEntityRegion region;

	@Before
	public void createRegion() {
		final Cache<Object, Object> cache = Mockito.mock( Cache.class );
		region = new JCacheEntityRegion( cache, null, null );
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testThrowsWhenCreatingTxRegionAccess() {
		region.buildAccessStrategy( AccessType.TRANSACTIONAL );
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testThrowsWhenCreatingTxRegionAccessExplicitly() {
		region.createTransactionalEntityRegionAccessStrategy();
	}
}
