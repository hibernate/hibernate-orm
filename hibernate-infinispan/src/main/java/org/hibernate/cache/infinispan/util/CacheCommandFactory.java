/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.hibernate.cache.infinispan.impl.BaseRegion;

import org.infinispan.commands.ReplicableCommand;
import org.infinispan.commands.module.ExtendedModuleCommandFactory;
import org.infinispan.commands.remote.CacheRpcCommand;

/**
 * Command factory
 *
 * @author Galder Zamarreño
 * @since 4.0
 */
public class CacheCommandFactory implements ExtendedModuleCommandFactory {

   /**
    * Keeps track of regions to which second-level cache specific
    * commands have been plugged.
    */
	private ConcurrentMap<String, BaseRegion> allRegions =
			new ConcurrentHashMap<String, BaseRegion>();

   /**
    * Add region so that commands can be cleared on shutdown.
    *
    * @param region instance to keep track of
    */
	public void addRegion(BaseRegion region) {
		allRegions.put( region.getName(), region );
	}

   /**
    * Clear all regions from this command factory.
    *
    * @param regions collection of regions to clear
    */
	public void clearRegions(Collection<BaseRegion> regions) {
		regions.forEach( region -> allRegions.remove( region.getName() ) );
	}

	@Override
	public Map<Byte, Class<? extends ReplicableCommand>> getModuleCommands() {
		final Map<Byte, Class<? extends ReplicableCommand>> map = new HashMap<Byte, Class<? extends ReplicableCommand>>( 3 );
		map.put( CacheCommandIds.EVICT_ALL, EvictAllCommand.class );
		map.put( CacheCommandIds.END_INVALIDATION, EndInvalidationCommand.class );
		map.put( CacheCommandIds.BEGIN_INVALIDATION, BeginInvalidationCommand.class );
		return map;
	}

	@Override
	public CacheRpcCommand fromStream(byte commandId, Object[] args, String cacheName) {
		CacheRpcCommand c;
		switch ( commandId ) {
			case CacheCommandIds.EVICT_ALL:
				c = new EvictAllCommand( cacheName, allRegions.get( cacheName ) );
				break;
			case CacheCommandIds.END_INVALIDATION:
				c = new EndInvalidationCommand(cacheName);
				break;
			default:
				throw new IllegalArgumentException( "Not registered to handle command id " + commandId );
		}
		c.setParameters( commandId, args );
		return c;
	}

	@Override
	public ReplicableCommand fromStream(byte commandId, Object[] args) {
		ReplicableCommand c;
		switch ( commandId ) {
			case CacheCommandIds.BEGIN_INVALIDATION:
				c = new BeginInvalidationCommand();
				break;
			default:
				throw new IllegalArgumentException( "Not registered to handle command id " + commandId );
		}
		c.setParameters( commandId, args );
		return c;
	}

}
