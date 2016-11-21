/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.access;

import org.hibernate.cache.infinispan.util.CacheCommandInitializer;
import org.hibernate.cache.infinispan.util.InfinispanMessageLogger;
import org.infinispan.commands.FlagAffectedCommand;
import org.infinispan.commands.write.ClearCommand;
import org.infinispan.commands.write.InvalidateCommand;
import org.infinispan.commands.write.PutKeyValueCommand;
import org.infinispan.commands.write.PutMapCommand;
import org.infinispan.commands.write.RemoveCommand;
import org.infinispan.commands.write.ReplaceCommand;
import org.infinispan.commands.write.WriteCommand;
import org.infinispan.commons.util.InfinispanCollections;
import org.infinispan.context.Flag;
import org.infinispan.context.InvocationContext;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.interceptors.InvalidationInterceptor;
import org.infinispan.jmx.annotations.MBean;

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
		if (!isPutForExternalRead(command)) {
			return handleInvalidate(ctx, command, new Object[] { command.getKey() });
		}
		return invokeNextInterceptor(ctx, command);
	}

	@Override
	public Object visitReplaceCommand(InvocationContext ctx, ReplaceCommand command) throws Throwable {
		return handleInvalidate(ctx, command, new Object[] { command.getKey() });
	}

	@Override
	public Object visitRemoveCommand(InvocationContext ctx, RemoveCommand command) throws Throwable {
		return handleInvalidate(ctx, command, new Object[] { command.getKey() });
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
		if (!isPutForExternalRead(command)) {
			return handleInvalidate(ctx, command, command.getMap().keySet().toArray());
		}
		return invokeNextInterceptor(ctx, command);
	}

	private Object handleInvalidate(InvocationContext ctx, WriteCommand command, Object[] keys) throws Throwable {
		Object retval = invokeNextInterceptor(ctx, command);
		if (command.isSuccessful() && keys != null && keys.length != 0) {
			invalidateAcrossCluster(command, keys);
		}
		return retval;
	}

	private void invalidateAcrossCluster(FlagAffectedCommand command, Object[] keys) throws Throwable {
		// increment invalidations counter if statistics maintained
		incrementInvalidations();
		InvalidateCommand invalidateCommand;
		Object sessionTransactionId = putFromLoadValidator.registerRemoteInvalidations(keys);
		if (!isLocalModeForced(command)) {
			if (sessionTransactionId == null) {
				invalidateCommand = commandsFactory.buildInvalidateCommand(InfinispanCollections.<Flag>emptySet(), keys);
			}
			else {
				invalidateCommand = commandInitializer.buildBeginInvalidationCommand(
						InfinispanCollections.<Flag>emptySet(), keys, sessionTransactionId);
			}
			if (log.isDebugEnabled()) {
				log.debug("Cache [" + rpcManager.getAddress() + "] replicating " + invalidateCommand);
			}

			rpcManager.invokeRemotely(getMembers(), invalidateCommand, isSynchronous(command) ? syncRpcOptions : asyncRpcOptions);
		}
	}

	private boolean isPutForExternalRead(FlagAffectedCommand command) {
		if (command.hasFlag(Flag.PUT_FOR_EXTERNAL_READ)) {
			log.trace("Put for external read called.  Suppressing clustered invalidation.");
			return true;
		}
		return false;
	}

}
