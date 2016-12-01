/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.access;

import java.util.UUID;

import org.hibernate.cache.infinispan.impl.BaseTransactionalDataRegion;
import org.hibernate.cache.infinispan.util.FutureUpdate;
import org.hibernate.cache.infinispan.util.InfinispanMessageLogger;
import org.hibernate.cache.infinispan.util.InvocationAfterCompletion;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;

import org.infinispan.AdvancedCache;

/**
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class FutureUpdateSynchronization extends InvocationAfterCompletion {
	private static final InfinispanMessageLogger log = InfinispanMessageLogger.Provider.getLog( FutureUpdateSynchronization.class );

	private final UUID uuid = UUID.randomUUID();
	private final Object key;
	private final Object value;
	private final BaseTransactionalDataRegion region;
	private final long sessionTimestamp;
	private final AdvancedCache localCache;
	private final AdvancedCache asyncCache;

	public FutureUpdateSynchronization(TransactionCoordinator tc, AdvancedCache localCache, AdvancedCache asyncCache,
			boolean requiresTransaction, Object key, Object value, BaseTransactionalDataRegion region, long sessionTimestamp) {
		super(tc, requiresTransaction);
		this.localCache = localCache;
		this.asyncCache = asyncCache;
		this.key = key;
		this.value = value;
		this.region = region;
		this.sessionTimestamp = sessionTimestamp;
	}

	public UUID getUuid() {
		return uuid;
	}

	@Override
	protected void invoke(boolean success) {
		// If the region was invalidated during this session, we can't know that the value we're inserting is valid
		// so we'll just null the tombstone
		if (sessionTimestamp < region.getLastRegionInvalidation()) {
			success = false;
		}
		// Exceptions in #afterCompletion() are silently ignored, since the transaction
		// is already committed in DB. However we must not return until we update the cache.
		FutureUpdate futureUpdate = new FutureUpdate(uuid, region.nextTimestamp(), success ? this.value : null);
		for (;;) {
			try {
				// Similar to putFromLoad, we have to update this node synchronously because after transaction
				// is committed it is expected that we'll retrieve cached instance until next invalidation,
				// but the replication this node -> primary -> this node can take a while
				// We need to first execute the async update and then local one, because if we're on the primary
				// owner the local future update, would fail the async one.
				asyncCache.put(key, futureUpdate);
				localCache.put(key, futureUpdate);
				return;
			}
			catch (Exception e) {
				log.failureInAfterCompletion(e);
			}
		}
	}
}
