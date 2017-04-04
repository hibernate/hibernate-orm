/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.jcache;

import java.util.Iterator;
import java.util.Map;
import javax.cache.Cache;
import javax.cache.CacheManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Alex Snaps
 */
public class JCacheRegionTest {

	public static final String CACHE_NAME = "foo";
	private JCacheRegion region;

	@Before
	public void createRegion() {
		final Cache mock = Mockito.mock( Cache.class );
		when( mock.getName() ).thenReturn( CACHE_NAME );
		this.region = new JCacheRegion( mock );
	}

	@Test
	public void testDestroyCallsDestroyCacheOnCacheManager() {
		final Cache<Object, Object> cache = region.getCache();
		CacheManager cacheManager = Mockito.mock( CacheManager.class );
		when( cache.getCacheManager() ).thenReturn( cacheManager );
		when( cacheManager.getCache( CACHE_NAME ) ).thenReturn( cache );
		region.destroy();
		verify( cacheManager ).destroyCache( CACHE_NAME );
	}

	@Test
	public void testDelegatesGetNameToCache() {
		assertThat( region.getName(), is( CACHE_NAME ) );
	}

	@Test
	public void testDelegatesContainsToCache() {
		final Cache<Object, Object> cache = region.getCache();
		region.contains( "bar" );
		verify( cache ).containsKey( "bar" );
	}

	@Test
	public void testSupportsToMap() {
		final Cache<Object, Object> cache = region.getCache();
		final Iterator mock = Mockito.mock( Iterator.class );
		when( mock.hasNext() ).thenReturn( true ).thenReturn( false );
		when( mock.next() ).thenReturn( new Cache.Entry<Object, Object>() {
											@Override
											public Object getKey() {
												return "foo";
											}

											@Override
											public Object getValue() {
												return "bar";
											}

											@Override
											public <T> T unwrap(Class<T> clazz) {
												throw new UnsupportedOperationException( "Implement me!" );
											}
										} );
		when( cache.iterator() ).thenReturn( mock );
		final Map<String, String> map = region.toMap();
		assertThat( map.size(), is(1) );
		assertThat( map.get( "foo" ), equalTo( "bar" ));
	}

}
