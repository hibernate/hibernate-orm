/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.access;

import org.infinispan.commands.write.DataWriteCommand;
import org.infinispan.context.Flag;
import org.infinispan.context.InvocationContext;
import org.infinispan.interceptors.locking.NonTransactionalLockingInterceptor;
import org.infinispan.util.concurrent.locks.LockUtil;

/**
 * With regular {@link org.infinispan.interceptors.locking.NonTransactionalLockingInterceptor},
 * async replication does not work in combination with synchronous replication: sync replication
 * relies on locking to order writes on backup while async replication relies on FIFO-ordering
 * from primary to backup. If these two combine, there's a possibility that on backup two modifications
 * modifications will proceed concurrently.
 * Similar issue threatens consistency when the command has {@link org.infinispan.context.Flag#CACHE_MODE_LOCAL}
 * - these commands don't acquire locks either.
 *
 * Therefore, this interceptor locks the entry in all situations but when it is sending message to primary owner
 * (locking then could lead to deadlocks).
 */
public class LockingInterceptor extends NonTransactionalLockingInterceptor {
	@Override
	protected Object visitDataWriteCommand(InvocationContext ctx, DataWriteCommand command) throws Throwable {
		try {
			// Clear any metadata; we'll set them as appropriate in TombstoneCallInterceptor
			command.setMetadata(null);

			boolean shouldLock;
			if (hasSkipLocking(command)) {
				shouldLock = false;
			}
			else if (command.hasFlag(Flag.CACHE_MODE_LOCAL)) {
				shouldLock = true;
			}
			else if (!ctx.isOriginLocal()) {
				shouldLock = true;
			}
			else if (LockUtil.getLockOwnership(command.getKey(), cdl) == LockUtil.LockOwnership.PRIMARY) {
				shouldLock = true;
			}
			else {
				shouldLock = false;
			}
			if (shouldLock) {
				lockAndRecord(ctx, command.getKey(), getLockTimeoutMillis(command));
			}
			return invokeNextInterceptor(ctx, command);
		}
		finally {
			lockManager.unlockAll(ctx);
		}
	}
}
