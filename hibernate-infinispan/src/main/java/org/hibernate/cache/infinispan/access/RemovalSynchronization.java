/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.access;

import org.hibernate.cache.infinispan.impl.BaseTransactionalDataRegion;
import org.hibernate.cache.infinispan.util.InvocationAfterCompletion;
import org.hibernate.cache.infinispan.util.VersionedEntry;
import org.hibernate.resource.transaction.TransactionCoordinator;
import org.infinispan.AdvancedCache;

import java.util.concurrent.TimeUnit;

/**
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class RemovalSynchronization extends InvocationAfterCompletion {
	private final BaseTransactionalDataRegion region;
	private final Object key;
	private final Object version;

	public RemovalSynchronization(TransactionCoordinator tc, AdvancedCache cache, boolean requiresTransaction, BaseTransactionalDataRegion region, Object key, Object version) {
		super(tc, cache, requiresTransaction);
		this.region = region;
		this.key = key;
		this.version = version;
	}

	@Override
	protected void invoke(boolean success, AdvancedCache cache) {
		if (success) {
			if (version == null) {
				cache.put(key, new VersionedEntry(null, null, region.nextTimestamp()), region.getTombstoneExpiration(), TimeUnit.MILLISECONDS);
			}
			else {
				cache.put(key, new VersionedEntry(null, version, Long.MIN_VALUE), region.getTombstoneExpiration(), TimeUnit.MILLISECONDS);
			}
		}
	}
}
