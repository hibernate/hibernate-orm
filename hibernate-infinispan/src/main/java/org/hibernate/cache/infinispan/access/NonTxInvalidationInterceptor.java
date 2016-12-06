/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.access;

import org.hibernate.cache.infinispan.util.CacheCommandInitializer;
import org.hibernate.cache.infinispan.util.InfinispanMessageLogger;
import org.infinispan.commands.write.ClearCommand;
import org.infinispan.commands.write.InvalidateCommand;
import org.infinispan.commands.write.PutKeyValueCommand;
import org.infinispan.commands.write.PutMapCommand;
import org.infinispan.commands.write.RemoveCommand;
import org.infinispan.commands.write.ReplaceCommand;
import org.infinispan.commands.write.WriteCommand;
import org.infinispan.context.Flag;
import org.infinispan.context.InvocationContext;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.interceptors.InvalidationInterceptor;
import org.infinispan.jmx.annotations.MBean;
import org.infinispan.util.concurrent.locks.RemoteLockCommand;

import java.util.Collections;

/**
 * This interceptor should completely replace default InvalidationInterceptor.
 * We need to send custom invalidation commands with transaction identifier (as the invalidation)
 * since we have to do a two-phase invalidation (releasing the locks as JTA synchronization),
 * although the cache itself is non-transactional.
 *
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 * @author Mircea.Markus@jboss.com
 * @author Galder Zamarreño
 */
@MBean(objectName = "Invalidation", description = "Component responsible for invalidating entries on remote caches when entries are written to locally.")
public class NonTxInvalidationInterceptor extends BaseInvalidationInterceptor {
	private final PutFromLoadValidator putFromLoadValidator;
	private CacheCommandInitializer commandInitializer;

	private static final InfinispanMessageLogger log = InfinispanMessageLogger.Provider.getLog(InvalidationInterceptor.class);

	public NonTxInvalidationInterceptor(PutFromLoadValidator putFromLoadValidator) {
		this.putFromLoadValidator = putFromLoadValidator;
	}

	@Inject
	public void injectDependencies(CacheCommandInitializer commandInitializer) {
		this.commandInitializer = commandInitializer;
	}

	@Override
	public Object visitPutKeyValueCommand(InvocationContext ctx, PutKeyValueCommand command) throws Throwable {
		if (command.hasFlag(Flag.PUT_FOR_EXTERNAL_READ)) {
			return invokeNextInterceptor(ctx, command);
		}
		else {
			boolean isTransactional = putFromLoadValidator.registerRemoteInvalidation(command.getKey(), command.getKeyLockOwner());
			if (!isTransactional) {
				throw new IllegalStateException("Put executed without transaction!");
			}
			if (!putFromLoadValidator.beginInvalidatingWithPFER(command.getKeyLockOwner(), command.getKey(), command.getValue())) {
				log.failedInvalidatePendingPut(command.getKey(), cacheName);
			}
			RemoveCommand removeCommand = commandsFactory.buildRemoveCommand(command.getKey(), null, command.getFlags());
			Object retval = invokeNextInterceptor(ctx, removeCommand);
			if (command.isSuccessful()) {
				invalidateAcrossCluster(command, isTransactional, command.getKey());
			}
			return retval;
		}
	}

	@Override
	public Object visitReplaceCommand(InvocationContext ctx, ReplaceCommand command) throws Throwable {
		throw new UnsupportedOperationException("Unexpected replace");
	}

	@Override
	public Object visitRemoveCommand(InvocationContext ctx, RemoveCommand command) throws Throwable {
		boolean isTransactional = putFromLoadValidator.registerRemoteInvalidation(command.getKey(), command.getKeyLockOwner());
		if (isTransactional) {
			if (!putFromLoadValidator.beginInvalidatingKey(command.getKeyLockOwner(), command.getKey())) {
				log.failedInvalidatePendingPut(command.getKey(), cacheName);
			}
		}
		else {
			log.trace("This is an eviction, not invalidating anything");
		}
		Object retval = invokeNextInterceptor(ctx, command);
		if (command.isSuccessful()) {
			invalidateAcrossCluster(command, isTransactional, command.getKey());
		}
		return retval;
	}

	@Override
	public Object visitClearCommand(InvocationContext ctx, ClearCommand command) throws Throwable {
		Object retval = invokeNextInterceptor(ctx, command);
		if (!isLocalModeForced(command)) {
			// just broadcast the clear command - this is simplest!
			if (ctx.isOriginLocal()) {
				rpcManager.invokeRemotely(getMembers(), command, isSynchronous(command) ? syncRpcOptions : asyncRpcOptions);
			}
		}
		return retval;
	}

	@Override
	public Object visitPutMapCommand(InvocationContext ctx, PutMapCommand command) throws Throwable {
		throw new UnsupportedOperationException("Unexpected putAll");
	}

	private <T extends WriteCommand & RemoteLockCommand> void invalidateAcrossCluster(T command, boolean isTransactional, Object key) throws Throwable {
		// increment invalidations counter if statistics maintained
		incrementInvalidations();
		InvalidateCommand invalidateCommand;
		if (!isLocalModeForced(command)) {
			if (isTransactional) {
				invalidateCommand = commandInitializer.buildBeginInvalidationCommand(
						Collections.emptySet(), new Object[] { key }, command.getKeyLockOwner());
			}
			else {
				invalidateCommand = commandsFactory.buildInvalidateCommand(Collections.emptySet(), new Object[] { key });
			}
			if (log.isDebugEnabled()) {
				log.debug("Cache [" + rpcManager.getAddress() + "] replicating " + invalidateCommand);
			}

			rpcManager.invokeRemotely(getMembers(), invalidateCommand, isSynchronous(command) ? syncRpcOptions : asyncRpcOptions);
		}
	}

}
