package org.hibernate.cache.infinispan.util;

import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.infinispan.commands.ReplicableCommand;
import org.infinispan.commands.module.ExtendedModuleCommandFactory;
import org.infinispan.commands.remote.CacheRpcCommand;

import java.util.HashMap;
import java.util.Map;

/**
 * Command factory
 *
 * @author Galder Zamarreño
 * @since 4.0
 */
public class CacheCommandFactory implements ExtendedModuleCommandFactory {

   private InfinispanRegionFactory regionFactory;

   public void setRegionFactory(InfinispanRegionFactory regionFactory) {
      this.regionFactory = regionFactory;
   }

   @Override
   public Map<Byte, Class<? extends ReplicableCommand>> getModuleCommands() {
      Map<Byte, Class<? extends ReplicableCommand>> map = new HashMap<Byte, Class<? extends ReplicableCommand>>(3);
      map.put(CacheCommandIds.EVICT_ALL, EvictAllCommand.class);
      return map;
   }

   @Override
   public CacheRpcCommand fromStream(byte commandId, Object[] args, String cacheName) {
      CacheRpcCommand c;
      switch (commandId) {
         case CacheCommandIds.EVICT_ALL:
            c = new EvictAllCommand(cacheName, regionFactory);
            break;
         default:
            throw new IllegalArgumentException("Not registered to handle command id " + commandId);
      }
      c.setParameters(commandId, args);
      return c;
   }

   @Override
   public ReplicableCommand fromStream(byte commandId, Object[] args) {
      // Should not be called while this factory only
      // provides cache specific replicable commands.
      return null;
   }

}
