package org.hibernate.cache.jcache;

import javax.cache.Cache;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.hibernate.cache.spi.access.AccessType;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

/**
 * @author Alex Snaps
 */
public class JCacheTransactionalDataRegionTest {

	private JCacheTransactionalDataRegion region;

	@Before
	public void createRegion() {
		final Cache<Object, Object> cache = Mockito.mock( Cache.class );
		region = new JCacheTransactionalDataRegion( cache, null, null );
	}

	@Test(expected = NullPointerException.class)
	public void testThrowsOnNullCache() {
		new JCacheTransactionalDataRegion( null, null, null );
	}

	@Test
	public void testIsNotTransactionAware() {
		assertThat( region.isTransactionAware(), is( false ) );
	}

	@Test
	public void testDelegatesClearToCache() {
		final Cache<Object, Object> cache = region.getCache();
		region.clear();
		verify( cache ).removeAll();
	}

	@Test
	public void testDelegatesGetToCache() {
		final Cache<Object, Object> cache = region.getCache();
		region.get( "foo" );
		verify( cache ).get( "foo" );
	}

	@Test
	public void testSupportsAllAccessTypesButTx() {
		for ( AccessType type : AccessType.values() ) {
			if ( type != AccessType.TRANSACTIONAL ) {
				assertThat(
						"JCacheTransactionalDataRegion should support " + type,
						region.supportedAccessTypes().contains( type ),
						is( true )
				);
			}
			else {
				assertThat(
						"JCacheTransactionalDataRegion NOT should support " + type,
						region.supportedAccessTypes().contains( type ),
						is( false )
				);
			}
		}
	}
}
