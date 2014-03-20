/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.jcache;

import java.util.Properties;

import javax.cache.Cache;
import javax.cache.CacheManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.AccessType;

import static java.util.Collections.EMPTY_MAP;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Alex Snaps
 */
public class JCacheRegionFactoryTest {

	private JCacheRegionFactory factory;

	@Before
	public void createFactory() {
		factory = new JCacheRegionFactory();
	}

	@Test
	public void testIsInitiallyNotStarted() {
		assertThat( factory.isStarted(), is( false ) );
		assertThat( factory.getCacheManager(), nullValue() );
	}

	@Test
	public void testMaintainsStartedFlag() {
		factory.start( null, EMPTY_MAP );
		assertThat( factory.isStarted(), is( true ) );
		assertThat( factory.getCacheManager(), notNullValue() );
		factory.stop();
		assertThat( factory.isStarted(), is( false ) );
		assertThat( factory.getCacheManager(), nullValue() );
	}

	@Test(expected = javax.cache.CacheException.class)
	public void testFailsOnNotFindingProvider() {
		final Properties properties = new Properties();
		properties.setProperty( JCacheRegionFactory.PROVIDER, "no.such.thing" );
		factory.start( null, properties );
	}

	@Test(expected = CacheException.class)
	public void testFailsOnInvalidURI() {
		final Properties properties = new Properties();
		properties.setProperty( JCacheRegionFactory.CONFIG_URI, "_fil:" );
		factory.start( null, properties );
	}

	@Test
	public void testDefaultAccessIsReadWrite() {
		assertThat( factory.getDefaultAccessType(), is( AccessType.READ_WRITE ) );
	}

	@Test
	public void testUsesMinimalPutsAsDefault() {
		assertThat( factory.isMinimalPutsEnabledByDefault(), is( true ) );
	}

	@Test
	public void testRemainsStoppedOnFailure() {
		final Properties properties = new Properties();
		properties.setProperty( JCacheRegionFactory.CONFIG_URI, "_fil:" );
		try {
			factory.start( null, properties );
			fail();
		}
		catch ( CacheException e ) {
			assertThat( factory.isStarted(), is( false ) );
		}

		properties.setProperty( JCacheRegionFactory.PROVIDER, "no.such.thing" );
		try {
			factory.start( null, properties );
			fail();
		}
		catch ( javax.cache.CacheException e ) {
			assertThat( factory.isStarted(), is( false ) );
		}
	}

	@Test
	public void testStopsCacheManagerOnShutdown() {
		factory.start( null, EMPTY_MAP );
		final CacheManager cacheManager = factory.getCacheManager();
		assertThat( cacheManager.isClosed(), is( false ) );
		factory.stop();
		assertThat(cacheManager.isClosed(), is(true));
	}

	@Test(expected = IllegalStateException.class)
	public void testThrowsIllegalStateExceptionWhenNotStarted() {
		factory.getOrCreateCache( "foo", null, null );
	}

	@Test
	public void testCreatesNonExistingCacheNamedLikeRegion() {
		factory.start( null, EMPTY_MAP );
		final Cache<Object, Object> foo = factory.getOrCreateCache( "foo", null, null );
		assertThat( foo, notNullValue());
		assertThat( factory.getCacheManager().getCache( "foo" ), sameInstance( foo ));
		assertThat( factory.getOrCreateCache( "foo", null, null ), sameInstance( foo ));
	}

	@After
	public void stopFactory() {
		if ( factory.isStarted() ) {
			factory.stop();
		}
	}
}
