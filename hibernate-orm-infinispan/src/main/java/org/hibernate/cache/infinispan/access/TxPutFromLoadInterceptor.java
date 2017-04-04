/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.access;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.cache.infinispan.util.CacheCommandInitializer;
import org.hibernate.cache.infinispan.util.EndInvalidationCommand;
import org.hibernate.cache.infinispan.util.InfinispanMessageLogger;

import org.infinispan.commands.VisitableCommand;
import org.infinispan.commands.tx.CommitCommand;
import org.infinispan.commands.tx.PrepareCommand;
import org.infinispan.commands.tx.RollbackCommand;
import org.infinispan.commands.write.PutKeyValueCommand;
import org.infinispan.commands.write.RemoveCommand;
import org.infinispan.commands.write.WriteCommand;
import org.infinispan.container.DataContainer;
import org.infinispan.context.Flag;
import org.infinispan.context.InvocationContext;
import org.infinispan.context.impl.TxInvocationContext;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.annotations.Start;
import org.infinispan.interceptors.base.BaseRpcInterceptor;
import org.infinispan.remoting.inboundhandler.DeliverOrder;
import org.infinispan.remoting.rpc.ResponseMode;
import org.infinispan.remoting.rpc.RpcManager;
import org.infinispan.remoting.rpc.RpcOptions;
import org.infinispan.remoting.transport.Address;
import org.infinispan.statetransfer.StateTransferManager;
import org.infinispan.transaction.xa.GlobalTransaction;

/**
 * Intercepts transactions in Infinispan, calling {@link PutFromLoadValidator#beginInvalidatingKey(Object, Object)}
 * before locks are acquired (and the entry is invalidated) and sends {@link EndInvalidationCommand} to release
 * invalidation throught {@link PutFromLoadValidator#endInvalidatingKey(Object, Object)} after the transaction
 * is committed.
 *
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
class TxPutFromLoadInterceptor extends BaseRpcInterceptor {
	private final static InfinispanMessageLogger log = InfinispanMessageLogger.Provider.getLog(TxPutFromLoadInterceptor.class);
	private PutFromLoadValidator putFromLoadValidator;
	private final String cacheName;
	private RpcManager rpcManager;
	private CacheCommandInitializer cacheCommandInitializer;
	private DataContainer dataContainer;
	private StateTransferManager stateTransferManager;
	private RpcOptions asyncUnordered;

	public TxPutFromLoadInterceptor(PutFromLoadValidator putFromLoadValidator, String cacheName) {
		this.putFromLoadValidator = putFromLoadValidator;
		this.cacheName = cacheName;
	}

	@Inject
	public void injectDependencies(RpcManager rpcManager, CacheCommandInitializer cacheCommandInitializer, DataContainer dataContainer, StateTransferManager stateTransferManager) {
		this.rpcManager = rpcManager;
		this.cacheCommandInitializer = cacheCommandInitializer;
		this.dataContainer = dataContainer;
		this.stateTransferManager = stateTransferManager;
	}

	@Start
	public void start() {
		asyncUnordered = rpcManager.getRpcOptionsBuilder(ResponseMode.ASYNCHRONOUS, DeliverOrder.NONE).build();
	}

	private void beginInvalidating(InvocationContext ctx, Object key) {
		TxInvocationContext txCtx = (TxInvocationContext) ctx;
		// make sure that the command is registered in the transaction
		txCtx.addAffectedKey(key);

		GlobalTransaction globalTransaction = txCtx.getGlobalTransaction();
		if (!putFromLoadValidator.beginInvalidatingKey(globalTransaction, key)) {
			log.failedInvalidatePendingPut(key, cacheName);
		}
	}

	@Override
	public Object visitPutKeyValueCommand(InvocationContext ctx, PutKeyValueCommand command) throws Throwable {
		if (!command.hasFlag(Flag.PUT_FOR_EXTERNAL_READ)) {
			beginInvalidating(ctx, command.getKey());
		}
		return invokeNextInterceptor(ctx, command);
	}

	@Override
	public Object visitRemoveCommand(InvocationContext ctx, RemoveCommand command) throws Throwable {
		beginInvalidating(ctx, command.getKey());
		return invokeNextInterceptor(ctx, command);
	}

	// We need to intercept PrepareCommand, not InvalidateCommand since the interception takes
	// place before EntryWrappingInterceptor and the PrepareCommand is multiplexed into InvalidateCommands
	// as part of EntryWrappingInterceptor
	@Override
	public Object visitPrepareCommand(TxInvocationContext ctx, PrepareCommand command) throws Throwable {
		if (ctx.isOriginLocal()) {
			// We can't wait to commit phase to remove the entry locally (invalidations are processed in 1pc
			// on remote nodes, so only local case matters here). The problem is that while the entry is locked
			// reads still can take place and we can read outdated collection after reading updated entity
			// owning this collection from DB; when this happens, the version lock on entity cannot protect
			// us against concurrent modification of the collection. Therefore, we need to remove the entry
			// here (even without lock!) and let possible update happen in commit phase.
			for (WriteCommand wc : command.getModifications()) {
				for (Object key : wc.getAffectedKeys()) {
					dataContainer.remove(key);
				}
			}
		}
		else {
			for (WriteCommand wc : command.getModifications()) {
				Set<Object> keys = wc.getAffectedKeys();
				if (log.isTraceEnabled()) {
					log.tracef("Invalidating keys %s with lock owner %s", keys, ctx.getLockOwner());
				}
				for (Object key : keys ) {
					putFromLoadValidator.beginInvalidatingKey(ctx.getLockOwner(), key);
				}
			}
		}
		return invokeNextInterceptor(ctx, command);
	}

	@Override
	public Object visitCommitCommand(TxInvocationContext ctx, CommitCommand command) throws Throwable {
		if (log.isTraceEnabled()) {
			log.tracef( "Commit command received, end invalidation" );
		}

		return endInvalidationAndInvokeNextInterceptor(ctx, command);
	}

	@Override
	public Object visitRollbackCommand(TxInvocationContext ctx, RollbackCommand command) throws Throwable {
		if (log.isTraceEnabled()) {
			log.tracef( "Rollback command received, end invalidation" );
		}

		return endInvalidationAndInvokeNextInterceptor(ctx, command);
	}

	protected Object endInvalidationAndInvokeNextInterceptor(TxInvocationContext<?> ctx, VisitableCommand command) throws Throwable {
		try {
			if (ctx.isOriginLocal()) {
				// We cannot use directly ctx.getAffectedKeys() and that includes keys from local-only operations.
				// During evictAll inside transaction this would cause unnecessary invalidate command
				if (!ctx.getModifications().isEmpty()) {
					Object[] keys = ctx.getModifications().stream()
						.flatMap(mod -> mod.getAffectedKeys().stream()).distinct().toArray();

					if (log.isTraceEnabled()) {
						log.tracef( "Sending end invalidation for keys %s asynchronously, modifications are %s",
							Arrays.toString(keys), ctx.getCacheTransaction().getModifications());
					}

					GlobalTransaction globalTransaction = ctx.getGlobalTransaction();
					EndInvalidationCommand commitCommand = cacheCommandInitializer.buildEndInvalidationCommand(
							cacheName, keys, globalTransaction);
					List<Address> members = stateTransferManager.getCacheTopology().getMembers();
					rpcManager.invokeRemotely(members, commitCommand, asyncUnordered);

					// If the transaction is not successful, *RegionAccessStrategy would not be called, therefore
					// we have to end invalidation from here manually (in successful case as well)
					for (Object key : keys) {
						putFromLoadValidator.endInvalidatingKey(globalTransaction, key);
					}
				}
			}
		}
		finally {
			return invokeNextInterceptor(ctx, command);
		}
	}
}
