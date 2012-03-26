package org.hibernate.cache.infinispan.util;

import org.infinispan.commands.remote.BaseRpcCommand;
import org.infinispan.context.InvocationContext;

import org.hibernate.cache.infinispan.impl.BaseRegion;

/**
 * Evict all command
 *
 * @author Galder Zamarreño
 * @since 4.0
 */
public class EvictAllCommand extends BaseRpcCommand {

   private final BaseRegion region;

   public EvictAllCommand(String regionName, BaseRegion region) {
      super(regionName); // region name and cache names are the same...
      this.region = region;
   }

   public EvictAllCommand(String regionName) {
      this(regionName, null);
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
