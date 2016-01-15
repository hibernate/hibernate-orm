/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.access;

import org.hibernate.cache.infinispan.util.CacheCommandInitializer;
import org.hibernate.cache.infinispan.util.InfinispanMessageLogger;
import org.infinispan.commands.CommandsFactory;
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
import org.infinispan.factories.annotations.Start;
import org.infinispan.interceptors.InvalidationInterceptor;
import org.infinispan.interceptors.base.BaseRpcInterceptor;
import org.infinispan.jmx.JmxStatisticsExposer;
import org.infinispan.jmx.annotations.DataType;
import org.infinispan.jmx.annotations.MBean;
import org.infinispan.jmx.annotations.ManagedAttribute;
import org.infinispan.jmx.annotations.ManagedOperation;
import org.infinispan.jmx.annotations.MeasurementType;
import org.infinispan.jmx.annotations.Parameter;

import java.util.concurrent.atomic.AtomicLong;

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
public class NonTxInvalidationInterceptor extends BaseRpcInterceptor implements JmxStatisticsExposer {
	private final AtomicLong invalidations = new AtomicLong(0);
	private final PutFromLoadValidator putFromLoadValidator;
	private CommandsFactory commandsFactory;
	private CacheCommandInitializer commandInitializer;
	private boolean statisticsEnabled;

	private static final InfinispanMessageLogger log = InfinispanMessageLogger.Provider.getLog(InvalidationInterceptor.class);

	public NonTxInvalidationInterceptor(PutFromLoadValidator putFromLoadValidator) {
		this.putFromLoadValidator = putFromLoadValidator;
	}

	@Inject
	public void injectDependencies(CommandsFactory commandsFactory, CacheCommandInitializer commandInitializer) {
		this.commandsFactory = commandsFactory;
		this.commandInitializer = commandInitializer;
	}

	@Start
	private void start() {
		this.setStatisticsEnabled(cacheConfiguration.jmxStatistics().enabled());
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
				rpcManager.invokeRemotely(null, command, rpcManager.getDefaultRpcOptions(defaultSynchronous));
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

			rpcManager.invokeRemotely(null, invalidateCommand, rpcManager.getDefaultRpcOptions(isSynchronous(command)));
		}
	}

	private void incrementInvalidations() {
		if (statisticsEnabled) {
			invalidations.incrementAndGet();
		}
	}

	private boolean isPutForExternalRead(FlagAffectedCommand command) {
		if (command.hasFlag(Flag.PUT_FOR_EXTERNAL_READ)) {
			log.trace("Put for external read called.  Suppressing clustered invalidation.");
			return true;
		}
		return false;
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

}
