/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.util;

import org.hibernate.cache.infinispan.access.PutFromLoadValidator;
import org.infinispan.commands.CommandInvocationId;
import org.infinispan.commands.ReplicableCommand;
import org.infinispan.commands.module.ModuleCommandInitializer;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.context.Flag;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.interceptors.locking.ClusteringDependentLogic;
import org.infinispan.notifications.cachelistener.CacheNotifier;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Command initializer
 *
 * @author Galder Zamarreño
 * @since 4.0
 */
public class CacheCommandInitializer implements ModuleCommandInitializer {

	private final ConcurrentHashMap<String, PutFromLoadValidator> putFromLoadValidators
			= new ConcurrentHashMap<String, PutFromLoadValidator>();
	private CacheNotifier notifier;
	private Configuration configuration;
	private ClusteringDependentLogic clusteringDependentLogic;

	@Inject
	public void injectDependencies(CacheNotifier notifier, Configuration configuration, ClusteringDependentLogic clusteringDependentLogic) {
		this.notifier = notifier;
		this.configuration = configuration;
		this.clusteringDependentLogic = clusteringDependentLogic;
	}

	public void addPutFromLoadValidator(String cacheName, PutFromLoadValidator putFromLoadValidator) {
		// there could be two instances of PutFromLoadValidator bound to the same cache when
		// there are two JndiInfinispanRegionFactories bound to the same cacheManager via JNDI.
		// In that case, as putFromLoadValidator does not really own the pendingPuts cache,
		// it's safe to have more instances.
		putFromLoadValidators.put(cacheName, putFromLoadValidator);
	}

	public PutFromLoadValidator removePutFromLoadValidator(String cacheName) {
		return putFromLoadValidators.remove(cacheName);
	}

	/**
    * Build an instance of {@link EvictAllCommand} for a given region.
    *
    * @param regionName name of region for {@link EvictAllCommand}
    * @return a new instance of {@link EvictAllCommand}
    */
	public EvictAllCommand buildEvictAllCommand(String regionName) {
		// No need to pass region factory because no information on that object
		// is sent around the cluster. However, when the command factory builds
		// and evict all command remotely, it does need to initialize it with
		// the right region factory so that it can call it back.
		return new EvictAllCommand( regionName );
	}

	public BeginInvalidationCommand buildBeginInvalidationCommand(Set<Flag> flags, Object[] keys, Object lockOwner) {
		return new BeginInvalidationCommand(notifier, flags, CommandInvocationId.generateId(clusteringDependentLogic.getAddress()), keys, lockOwner);
	}

	public EndInvalidationCommand buildEndInvalidationCommand(String cacheName, Object[] keys, Object lockOwner) {
		return new EndInvalidationCommand( cacheName, keys, lockOwner );
	}

	@Override
	public void initializeReplicableCommand(ReplicableCommand c, boolean isRemote) {
		switch (c.getCommandId()) {
			case CacheCommandIds.END_INVALIDATION:
				EndInvalidationCommand endInvalidationCommand = (EndInvalidationCommand) c;
				endInvalidationCommand.setPutFromLoadValidator(putFromLoadValidators.get(endInvalidationCommand.getCacheName()));
				break;
			case CacheCommandIds.BEGIN_INVALIDATION:
				BeginInvalidationCommand beginInvalidationCommand = (BeginInvalidationCommand) c;
				beginInvalidationCommand.init(notifier, configuration);
				break;
		}
	}
}
