/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.collection;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.test.cache.infinispan.util.TestInfinispanRegionFactory;

import static org.junit.Assert.assertTrue;

/**
 * Base class for tests of TRANSACTIONAL access.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 */
public abstract class CollectionRegionTransactionalAccessTest extends AbstractCollectionRegionAccessStrategyTest {
	@Override
	protected AccessType getAccessType() {
		  return AccessType.TRANSACTIONAL;
	 }

	@Override
	protected Class<? extends RegionFactory> getRegionFactoryClass() {
		return TestInfinispanRegionFactory.Transactional.class;
	}

	/**
	* InvalidatedTransactionalTestCase.
	*
	* @author Galder Zamarreño
	* @since 3.5
	*/
	public static class Invalidation extends CollectionRegionTransactionalAccessTest {
		@Override
		public void testCacheConfiguration() {
			assertTrue("Transactions", isTransactional());
			assertTrue("Using Invalidation", isUsingInvalidation());
			assertTrue("Synchronous mode", isSynchronous());
		}
	}

}
