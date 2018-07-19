/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.jcache.test;

import javax.cache.Cache;

import org.hibernate.cache.jcache.internal.JCacheAccessImpl;
import org.hibernate.cache.spi.Region;
import org.hibernate.cache.spi.support.DomainDataRegionTemplate;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil2.inSession;
import static org.junit.Assert.fail;

/**
 * Tests around {@link org.hibernate.cache.jcache.internal.JCacheAccessImpl}
 *
 * @author Steve Ebersole
 */
public class StorageAccessTest extends BaseUnitTestCase {

	/**
	 * Sort of the inverse test of {@link MissingCacheStrategyTest#testMissingCacheStrategyFail()}.
	 * Here building the SF should succeed.
	 */
	@Test
	public void testPreDefinedCachesAllowed() {
		TestHelper.preBuildAllCaches();
		SessionFactoryImplementor sessionFactory = TestHelper.buildStandardSessionFactory();
		sessionFactory.close();
	}

	@Test
	public void testBasicStorageAccessUse() {
		TestHelper.preBuildAllCaches();
		try (final SessionFactoryImplementor sessionFactory = TestHelper.buildStandardSessionFactory() ) {
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
		TestHelper.preBuildAllCaches();
		try (SessionFactoryImplementor sessionFactory = TestHelper.buildStandardSessionFactory() ) {
		}

		TestHelper.visitDomainRegions(
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
