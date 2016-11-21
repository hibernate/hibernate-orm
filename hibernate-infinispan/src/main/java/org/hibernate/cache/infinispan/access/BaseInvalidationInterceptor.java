/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.access;

import org.infinispan.commands.CommandsFactory;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.annotations.Start;
import org.infinispan.interceptors.base.BaseRpcInterceptor;
import org.infinispan.jmx.JmxStatisticsExposer;
import org.infinispan.jmx.annotations.DataType;
import org.infinispan.jmx.annotations.ManagedAttribute;
import org.infinispan.jmx.annotations.ManagedOperation;
import org.infinispan.jmx.annotations.MeasurementType;
import org.infinispan.jmx.annotations.Parameter;
import org.infinispan.remoting.inboundhandler.DeliverOrder;
import org.infinispan.remoting.rpc.ResponseMode;
import org.infinispan.remoting.rpc.RpcOptions;
import org.infinispan.remoting.transport.Address;
import org.infinispan.statetransfer.StateTransferManager;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public abstract class BaseInvalidationInterceptor extends BaseRpcInterceptor implements JmxStatisticsExposer {
	private final AtomicLong invalidations = new AtomicLong(0);
	protected CommandsFactory commandsFactory;
	protected StateTransferManager stateTransferManager;
	protected boolean statisticsEnabled;
	protected RpcOptions syncRpcOptions;
	protected RpcOptions asyncRpcOptions;

	@Inject
	public void injectDependencies(CommandsFactory commandsFactory, StateTransferManager stateTransferManager) {
		this.commandsFactory = commandsFactory;
		this.stateTransferManager = stateTransferManager;
	}

	@Start
	private void start() {
		this.setStatisticsEnabled(cacheConfiguration.jmxStatistics().enabled());
		syncRpcOptions = rpcManager.getRpcOptionsBuilder(ResponseMode.SYNCHRONOUS_IGNORE_LEAVERS, DeliverOrder.NONE).build();
		asyncRpcOptions = rpcManager.getDefaultRpcOptions(false);
	}

	@ManagedOperation(
			description = "Resets statistics gathered by this component",
			displayName = "Reset statistics"
	)
	public void resetStatistics() {
		invalidations.set(0);
	}

	@ManagedAttribute(
			displayName = "Statistics enabled",
			description = "Enables or disables the gathering of statistics by this component",
			dataType = DataType.TRAIT,
			writable = true
	)
	public boolean getStatisticsEnabled() {
		return this.statisticsEnabled;
	}

	public void setStatisticsEnabled(@Parameter(name = "enabled", description = "Whether statistics should be enabled or disabled (true/false)") boolean enabled) {
		this.statisticsEnabled = enabled;
	}

	@ManagedAttribute(
			description = "Number of invalidations",
			displayName = "Number of invalidations",
			measurementType = MeasurementType.TRENDSUP
	)
	public long getInvalidations() {
		return invalidations.get();
	}

	protected void incrementInvalidations() {
		if (statisticsEnabled) {
			invalidations.incrementAndGet();
		}
	}

	protected List<Address> getMembers() {
		return stateTransferManager.getCacheTopology().getMembers();
	}
}
