/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.util;

import java.util.HashMap;
import java.util.List;
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
    * @param regionName name of the region
    * @param region instance to keep track of
    */
	public void addRegion(String regionName, BaseRegion region) {
		allRegions.put( regionName, region );
	}

   /**
    * Clear all regions from this command factory.
    *
    * @param regionNames collection of regions to clear
    */
	public void clearRegions(List<String> regionNames) {
		for ( String regionName : regionNames ) {
			allRegions.remove( regionName );
		}
	}

	@Override
	public Map<Byte, Class<? extends ReplicableCommand>> getModuleCommands() {
		final Map<Byte, Class<? extends ReplicableCommand>> map = new HashMap<Byte, Class<? extends ReplicableCommand>>( 3 );
		map.put( CacheCommandIds.EVICT_ALL, EvictAllCommand.class );
		return map;
	}

	@Override
	public CacheRpcCommand fromStream(byte commandId, Object[] args, String cacheName) {
		CacheRpcCommand c;
		switch ( commandId ) {
			case CacheCommandIds.EVICT_ALL:
				c = new EvictAllCommand( cacheName, allRegions.get( cacheName ) );
				break;
			default:
				throw new IllegalArgumentException( "Not registered to handle command id " + commandId );
		}
		c.setParameters( commandId, args );
		return c;
	}

	@Override
	public ReplicableCommand fromStream(byte commandId, Object[] args) {
		// Should not be called while this factory only
		// provides cache specific replicable commands.
		return null;
	}

}
