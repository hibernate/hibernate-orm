/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
