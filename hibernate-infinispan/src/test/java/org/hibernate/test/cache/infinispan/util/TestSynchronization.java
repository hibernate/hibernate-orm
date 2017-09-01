package org.hibernate.test.cache.infinispan.util;

import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.CachedDomainDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public abstract class TestSynchronization implements javax.transaction.Synchronization {
	protected final SharedSessionContractImplementor session;
	protected final Object key;
	protected final Object value;
	protected final Object version;

	public TestSynchronization(SharedSessionContractImplementor session, Object key, Object value, Object version) {
		this.session = session;
		this.key = key;
		this.value = value;
		this.version = version;
	}

	@Override
	public void beforeCompletion() {
	}


	public static class AfterInsert extends TestSynchronization {
		private final EntityDataAccess strategy;

		public AfterInsert(EntityDataAccess strategy, SharedSessionContractImplementor session, Object key, Object value, Object version) {
			super(session, key, value, version);
			this.strategy = strategy;
		}

		@Override
		public void afterCompletion(int status) {
			strategy.afterInsert(session, key, value, version);
		}
	}

	public static class AfterUpdate extends TestSynchronization {
		private final EntityDataAccess strategy;
		private final SoftLock lock;

		public AfterUpdate(EntityDataAccess strategy, SharedSessionContractImplementor session, Object key, Object value, Object version, SoftLock lock) {
			super(session, key, value, version);
			this.strategy = strategy;
			this.lock = lock;
		}

		@Override
		public void afterCompletion(int status) {
			strategy.afterUpdate(session, key, value, version, null, lock);
		}
	}

	public static class UnlockItem extends TestSynchronization {
		private final CachedDomainDataAccess strategy;
		private final SoftLock lock;

		public UnlockItem(CachedDomainDataAccess strategy, SharedSessionContractImplementor session, Object key, SoftLock lock) {
			super(session, key, null, null);
			this.strategy = strategy;
			this.lock = lock;
		}

		@Override
		public void afterCompletion(int status) {
			strategy.unlockItem(session, key, lock);
		}
	}
}
