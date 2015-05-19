/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
