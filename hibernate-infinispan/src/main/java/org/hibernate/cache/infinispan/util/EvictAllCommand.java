package org.hibernate.cache.infinispan.util;

import org.hibernate.cache.infinispan.InfinispanRegionFactory;
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

   private InfinispanRegionFactory regionFactory;

   public EvictAllCommand(String regionName, InfinispanRegionFactory regionFactory) {
      super(regionName); // region name and cache names are the same...
      this.regionFactory = regionFactory;
   }

   @Override
   public Object perform(InvocationContext ctx) throws Throwable {
      BaseRegion region = regionFactory.getRegion(cacheName);
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
