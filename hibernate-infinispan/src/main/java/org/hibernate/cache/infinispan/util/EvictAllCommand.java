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

import org.hibernate.cache.infinispan.impl.BaseRegion;

import org.infinispan.commands.remote.BaseRpcCommand;
import org.infinispan.context.InvocationContext;

/**
 * Evict all command
 *
 * @author Galder Zamarreño
 * @since 4.0
 */
public class EvictAllCommand extends BaseRpcCommand {

	private final BaseRegion region;

   /**
    * Evict all command constructor.
    *
    * @param regionName name of the region to evict
    * @param region to evict
    */
	public EvictAllCommand(String regionName, BaseRegion region) {
		// region name and cache names are the same...
		super( regionName );
		this.region = region;
	}

   /**
    * Evict all command constructor.
    *
    * @param regionName name of the region to evict
    */
	public EvictAllCommand(String regionName) {
		this( regionName, null );
	}

	@Override
	public Object perform(InvocationContext ctx) throws Throwable {
		region.invalidateRegion();
		return null;
	}

	@Override
	public byte getCommandId() {
		return CacheCommandIds.EVICT_ALL;
	}

	@Override
	public Object[] getParameters() {
		return new Object[0];
	}

	@Override
	public void setParameters(int commandId, Object[] parameters) {
		// No-op
	}

	@Override
	public boolean isReturnValueExpected() {
		return false;
	}

}
