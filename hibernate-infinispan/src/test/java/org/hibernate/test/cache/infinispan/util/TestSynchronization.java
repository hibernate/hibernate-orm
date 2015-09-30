package org.hibernate.test.cache.infinispan.util;

import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.RegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SessionImplementor;

import javax.transaction.Synchronization;

/**
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public abstract class TestSynchronization implements javax.transaction.Synchronization {
	protected final SessionImplementor session;
	protected final Object key;
	protected final Object value;

	public TestSynchronization(SessionImplementor session, Object key, Object value) {
		this.session = session;
		this.key = key;
		this.value = value;
	}

	@Override
	public void beforeCompletion() {
	}


	public static class AfterInsert extends TestSynchronization {
		private final EntityRegionAccessStrategy strategy;

		public AfterInsert(EntityRegionAccessStrategy strategy, SessionImplementor session, Object key, Object value) {
			super(session, key, value);
			this.strategy = strategy;
		}

		@Override
		public void afterCompletion(int status) {
			strategy.afterInsert(session, key, value, null);
		}
	}

	public static class AfterUpdate extends TestSynchronization {
		private final EntityRegionAccessStrategy strategy;
		private final SoftLock lock;

		public AfterUpdate(EntityRegionAccessStrategy strategy, SessionImplementor session, Object key, Object value, SoftLock lock) {
			super(session, key, value);
			this.strategy = strategy;
			this.lock = lock;
		}

		@Override
		public void afterCompletion(int status) {
			strategy.afterUpdate(session, key, value, null, null, lock);
		}
	}

	public static class UnlockItem extends TestSynchronization {
		private final RegionAccessStrategy strategy;
		private final SoftLock lock;

		public UnlockItem(RegionAccessStrategy strategy, SessionImplementor session, Object key, SoftLock lock) {
			super(session, key, null);
			this.strategy = strategy;
			this.lock = lock;
		}

		@Override
		public void afterCompletion(int status) {
			strategy.unlockItem(session, key, lock);
		}
	}
}
