/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.access;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.hibernate.cache.infinispan.impl.BaseTransactionalDataRegion;
import org.hibernate.cache.infinispan.util.InvocationAfterCompletion;
import org.hibernate.cache.infinispan.util.Tombstone;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;

import org.infinispan.AdvancedCache;

/**
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class TombstoneSynchronization<K> extends InvocationAfterCompletion {
	private final UUID uuid = UUID.randomUUID();
	private final BaseTransactionalDataRegion region;
	private final K key;

	public TombstoneSynchronization(TransactionCoordinator tc, AdvancedCache cache, boolean requiresTransaction, BaseTransactionalDataRegion region, K key) {
		super(tc, cache, requiresTransaction);
		this.key = key;
		this.region = region;
	}

	public UUID getUuid() {
		return uuid;
	}

	public K getKey() {
		return key;
	}

	@Override
	public void beforeCompletion() {
	}

	@Override
	public void invoke(boolean success, AdvancedCache cache) {
		Tombstone tombstone = new Tombstone(uuid, region.nextTimestamp(), true);
		cache.put(key, tombstone, region.getTombstoneExpiration(), TimeUnit.MILLISECONDS);
	}
}
