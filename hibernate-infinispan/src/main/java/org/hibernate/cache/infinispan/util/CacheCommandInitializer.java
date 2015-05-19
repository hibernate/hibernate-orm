/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.util;

import org.infinispan.commands.ReplicableCommand;
import org.infinispan.commands.module.ModuleCommandInitializer;

/**
 * Command initializer
 *
 * @author Galder Zamarreño
 * @since 4.0
 */
public class CacheCommandInitializer implements ModuleCommandInitializer {

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

	@Override
	public void initializeReplicableCommand(ReplicableCommand c, boolean isRemote) {
		// No need to initialize...
	}

}
