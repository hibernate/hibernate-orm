package org.hibernate.test.cache.infinispan.collection;

import org.hibernate.cache.spi.access.AccessType;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests read-write access
 *
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class CollectionRegionReadWriteAccessTest extends AbstractCollectionRegionAccessStrategyTest {
	@Override
	protected AccessType getAccessType() {
		return AccessType.READ_WRITE;
	}

	@Override
	public void testCacheConfiguration() {
		assertFalse(isTransactional());
		assertTrue(isSynchronous());
	}
}
