/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.access;

import org.infinispan.commands.write.PutKeyValueCommand;
import org.infinispan.context.Flag;
import org.infinispan.context.InvocationContext;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.annotations.Start;
import org.infinispan.interceptors.distribution.NonTxDistributionInterceptor;
import org.infinispan.remoting.inboundhandler.DeliverOrder;
import org.infinispan.remoting.rpc.ResponseMode;
import org.infinispan.remoting.rpc.RpcOptions;
import org.infinispan.remoting.transport.Address;
import org.infinispan.statetransfer.OutdatedTopologyException;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.util.List;

/**
 * Since the data handled in {@link TombstoneCallInterceptor} or {@link VersionedCallInterceptor}
 * does not rely on the order how these are applied (the updates are commutative), this interceptor
 * simply sends any command to all other owners without ordering them through primary owner.
 * Note that {@link LockingInterceptor} is required in the stack as locking on backup is not guaranteed
 * by primary owner.
 */
public class UnorderedDistributionInterceptor extends NonTxDistributionInterceptor {
	private static Log log = LogFactory.getLog(UnorderedDistributionInterceptor.class);
	private static final boolean trace = log.isTraceEnabled();

	private DistributionManager distributionManager;
	private RpcOptions syncRpcOptions, asyncRpcOptions;

	@Inject
	public void inject(DistributionManager distributionManager) {
		this.distributionManager = distributionManager;
	}

	@Start
	public void start() {
		syncRpcOptions = rpcManager.getRpcOptionsBuilder(ResponseMode.SYNCHRONOUS_IGNORE_LEAVERS, DeliverOrder.NONE).build();
		// We don't have to guarantee ordering even for asynchronous messages
		asyncRpcOptions = rpcManager.getRpcOptionsBuilder(ResponseMode.ASYNCHRONOUS, DeliverOrder.NONE).build();
	}

	@Override
	public Object visitPutKeyValueCommand(InvocationContext ctx, PutKeyValueCommand command) throws Throwable {
		if (command.hasFlag(Flag.CACHE_MODE_LOCAL)) {
			// for state-transfer related writes
			return invokeNextInterceptor(ctx, command);
		}
		int commandTopologyId = command.getTopologyId();
		int currentTopologyId = stateTransferManager.getCacheTopology().getTopologyId();
		if (commandTopologyId != -1 && currentTopologyId != commandTopologyId) {
			throw new OutdatedTopologyException("Cache topology changed while the command was executing: expected " +
				commandTopologyId + ", got " + currentTopologyId);
		}

		ConsistentHash writeCH = distributionManager.getWriteConsistentHash();
		List<Address> owners = null;
		if (writeCH.isReplicated()) {
			// local result is always ignored
			invokeNextInterceptor(ctx, command);
		}
		else {
			owners = writeCH.locateOwners(command.getKey());
			if (owners.contains(rpcManager.getAddress())) {
				invokeNextInterceptor(ctx, command);
			}
			else {
				log.tracef("Not invoking %s on %s since it is not an owner", command, rpcManager.getAddress());
			}
		}

		if (ctx.isOriginLocal() && command.isSuccessful()) {
			// This is called with the entry locked. In order to avoid deadlocks we must not wait for RPC while
			// holding the lock, therefore we'll return a future and wait for it in LockingInterceptor after
			// unlocking (and committing) the entry.
			return rpcManager.invokeRemotelyAsync(owners, command, isSynchronous(command) ? syncRpcOptions : asyncRpcOptions);
		}
		return null;
	}
}
