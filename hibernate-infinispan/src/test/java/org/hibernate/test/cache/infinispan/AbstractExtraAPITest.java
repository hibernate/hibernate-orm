package org.hibernate.test.cache.infinispan;

import org.hibernate.cache.internal.CacheDataDescriptionImpl;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.access.RegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.compare.ComparableComparator;

import org.hibernate.test.cache.infinispan.util.InfinispanTestingSetup;
import org.hibernate.test.cache.infinispan.util.TestingKeyFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

/**
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public abstract class AbstractExtraAPITest<S extends RegionAccessStrategy> extends AbstractNonFunctionalTest {
	@Rule
	public InfinispanTestingSetup infinispanTestIdentifier = new InfinispanTestingSetup();

	public static final String REGION_NAME = "test/com.foo.test";
	public static final Object KEY = TestingKeyFactory.generateCollectionCacheKey( "KEY" );
	public static final CacheDataDescription CACHE_DATA_DESCRIPTION
			= new CacheDataDescriptionImpl(true, true, ComparableComparator.INSTANCE, null);
	protected static final SharedSessionContractImplementor SESSION = mock(SharedSessionContractImplementor.class);

	protected S accessStrategy;
	protected NodeEnvironment environment;

	@Before
	public final void prepareLocalAccessStrategy() throws Exception {
		environment = new NodeEnvironment( createStandardServiceRegistryBuilder() );
		environment.prepare();

		// Sleep a bit to avoid concurrent FLUSH problem
		avoidConcurrentFlush();

		accessStrategy = getAccessStrategy();
	}

	protected abstract S getAccessStrategy();

	@After
	public final void releaseLocalAccessStrategy() throws Exception {
		if ( environment != null ) {
			environment.release();
		}
	}

	@Test
	public void testLockItem() {
		assertNull( accessStrategy.lockItem(SESSION, KEY, Integer.valueOf( 1 ) ) );
	}

	@Test
	public void testLockRegion() {
		assertNull( accessStrategy.lockRegion() );
	}

	@Test
	public void testUnlockItem() {
		accessStrategy.unlockItem(SESSION, KEY, new MockSoftLock() );
	}

	@Test
	public void testUnlockRegion() {
		accessStrategy.unlockItem(SESSION, KEY, new MockSoftLock() );
	}

	public static class MockSoftLock implements SoftLock {
	}
}
