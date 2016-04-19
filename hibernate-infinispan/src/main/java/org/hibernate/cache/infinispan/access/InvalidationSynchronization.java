/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.access;

import java.util.UUID;

/**
 * Synchronization that should release the locks afterQuery invalidation is complete.
 *
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class InvalidationSynchronization implements javax.transaction.Synchronization {
	public final UUID uuid = UUID.randomUUID();
	private final NonTxPutFromLoadInterceptor nonTxPutFromLoadInterceptor;
	private final Object[] keys;

	public InvalidationSynchronization(NonTxPutFromLoadInterceptor nonTxPutFromLoadInterceptor, Object[] keys) {
		this.nonTxPutFromLoadInterceptor = nonTxPutFromLoadInterceptor;
		this.keys = keys;
	}

	@Override
	public void beforeCompletion() {}

	@Override
	public void afterCompletion(int status) {
		nonTxPutFromLoadInterceptor.broadcastEndInvalidationCommand(keys, uuid);
	}
}
