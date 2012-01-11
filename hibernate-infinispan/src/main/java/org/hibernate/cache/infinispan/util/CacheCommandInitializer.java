package org.hibernate.cache.infinispan.util;

import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.infinispan.commands.ReplicableCommand;
import org.infinispan.commands.module.ModuleCommandInitializer;

/**
 * Command initializer
 *
 * @author Galder Zamarreño
 * @since 4.0
 */
public class CacheCommandInitializer implements ModuleCommandInitializer {

   private InfinispanRegionFactory regionFactory;

   public void setRegionFactory(InfinispanRegionFactory regionFactory) {
      this.regionFactory = regionFactory;
   }

   public EvictAllCommand buildEvictAllCommand(String regionName) {
      return new EvictAllCommand(regionName, regionFactory);
   }

   @Override
   public void initializeReplicableCommand(ReplicableCommand c, boolean isRemote) {
      // No need to initialize...
   }

}
