/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.access;

import java.util.UUID;

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

	public FutureUpdateSynchronization(TransactionCoordinator tc, AdvancedCache cache, boolean requiresTransaction, Object key, Object value) {
		super(tc, cache, requiresTransaction);
		this.key = key;
		this.value = value;
	}

	public UUID getUuid() {
		return uuid;
	}

	@Override
	protected void invoke(boolean success, AdvancedCache cache) {
		// Exceptions in #afterCompletion() are silently ignored, since the transaction
		// is already committed in DB. However we must not return until we update the cache.
		for (;;) {
			try {
				cache.put(key, new FutureUpdate(uuid, success ? value : null));
				return;
			}
			catch (Exception e) {
				log.failureInAfterCompletion(e);
			}
		}
	}
}
