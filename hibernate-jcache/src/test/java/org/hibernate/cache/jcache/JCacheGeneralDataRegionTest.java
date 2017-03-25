/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.jcache;

import javax.cache.Cache;
import org.hibernate.engine.spi.SessionImplementor;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.verify;

/**
 * @author Alex Snaps
 */
public class JCacheGeneralDataRegionTest {

	JCacheGeneralDataRegion region;

	@Before
	public void createRegion() {
		final Cache<Object, Object> mock = Mockito.mock( Cache.class );
		region = new JCacheGeneralDataRegion( mock );
	}

	@Test
	public void testDelegatesGetToCache() {
		region.get( Mockito.mock(SessionImplementor.class), "foo" );
		verify( region.getCache() ).get( "foo" );
	}

	@Test
	public void testDelegatesPutToCache() {
		region.put( Mockito.mock(SessionImplementor.class), "foo", "bar" );
		verify( region.getCache() ).put( "foo", "bar" );
	}

	@Test
	public void testDelegatesEvictKeyToCache() {
		region.evict( "foo" );
		verify( region.getCache() ).remove( "foo" );
	}

	@Test
	public void testDelegatesEvictAllToCache() {
		region.evictAll();
		verify( region.getCache() ).removeAll();
	}
}
