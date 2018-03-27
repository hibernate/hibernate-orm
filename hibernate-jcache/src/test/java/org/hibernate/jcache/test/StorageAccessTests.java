/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.jcache.test;

import javax.cache.Cache;
import javax.cache.CacheManager;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.jcache.JCacheHelper;
import org.hibernate.cache.jcache.internal.JCacheAccessImpl;
import org.hibernate.cache.spi.Region;
import org.hibernate.cache.spi.support.DomainDataRegionTemplate;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.spi.ServiceException;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import org.hamcrest.CoreMatchers;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.hibernate.testing.transaction.TransactionUtil2.inSession;
import static org.junit.Assert.fail;

/**
 * Tests around {@link org.hibernate.cache.jcache.internal.JCacheAccessImpl}
 *
 * @author Steve Ebersole
 */
public class StorageAccessTests extends BaseUnitTestCase {

	public static final String NON_CACHE_NAME = "not-a-cache";

	@Test
	public void testOnTheFlyCreationDisallowed() {
		// first, lets make sure that the region name we think is non-existent really does not exist
		final CacheManager cacheManager = JCacheHelper.locateStandardCacheManager();
		assertThat( cacheManager.getCache( NON_CACHE_NAME ), nullValue() );

		// and now let's try to build the standard testing SessionFactory, without pre-defining caches
		try (SessionFactoryImplementor sessionFactory = TestHelper.buildStandardSessionFactory( false ) ) {
			fail();
		}
		catch (ServiceException expected) {
			assertTyping( CacheException.class, expected.getCause() );
			assertThat( expected.getMessage(), CoreMatchers.equalTo( "Unable to create requested service [" + org.hibernate.cache.spi.CacheImplementor.class.getName() + "]" ) );
			assertThat( expected.getCause().getMessage(), CoreMatchers.startsWith( "On-the-fly creation of JCache Cache objects is not supported" ) );
		}
		catch (CacheException expected) {
			assertThat( expected.getMessage(), CoreMatchers.equalTo( "On-the-fly creation of JCache Cache objects is not supported" ) );
		}
	}

	@Test
	public void testPreDefinedCachesAllowed() {
		// sort of the inverse test of #testOnTheFlyCreationDisallowed.  Here building the SF
		// should succeed

		SessionFactoryImplementor sessionFactory = TestHelper.buildStandardSessionFactory( true );
		sessionFactory.close();
	}

	@Test
	public void testBasicStorageAccessUse() {
		try (final SessionFactoryImplementor sessionFactory = TestHelper.buildStandardSessionFactory( true ) ) {
			final Region region = sessionFactory.getCache().getRegion( TestHelper.entityRegionNames[0] );

			final JCacheAccessImpl access = (JCacheAccessImpl) ( (DomainDataRegionTemplate) region ).getCacheStorageAccess();
			final Cache jcache = access.getUnderlyingCache();

			inSession(
					sessionFactory,
					s -> {
						access.putIntoCache( "key", "value", s );
						assertThat( jcache.get( "key" ), equalTo( "value" ) );
						assertThat( access.getFromCache( "key", s ), equalTo( "value" ) );

						access.removeFromCache( "key", s );
						assertThat( jcache.get( "key" ), nullValue() );
						assertThat( access.getFromCache( "key", s ), nullValue() );
					}
			);
		}
	}

	@Test
	@SuppressWarnings({"EmptyTryBlock", "unused"})
	public void testCachesReleasedOnSessionFactoryClose() {
		try (SessionFactoryImplementor sessionFactory = TestHelper.buildStandardSessionFactory( true ) ) {
		}

		TestHelper.visitAllRegions(
				cache -> {
					if ( cache == null ) {
						return;
					}

					if ( cache.isClosed() ) {
						return;
					}

					fail( "Cache was not closed " );
				}
		);
	}
}
