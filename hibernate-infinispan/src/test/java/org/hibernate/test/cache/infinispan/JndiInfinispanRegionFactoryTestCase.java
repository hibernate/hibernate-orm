package org.hibernate.test.cache.infinispan;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.infinispan.JndiInfinispanRegionFactory;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cfg.AvailableSettings;

import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;

/**
 * // TODO: Document this
 *
 * @author Galder Zamarreño
 * @since 3.5
 */
public class JndiInfinispanRegionFactoryTestCase {
   @Test
   public void testConstruction() {
      StandardServiceRegistry ssr = new StandardServiceRegistryBuilder()
              .applySetting( AvailableSettings.CACHE_REGION_FACTORY, JndiInfinispanRegionFactory.class.getName() )
              .build();
      try {
         RegionFactory regionFactory = ssr.getService( RegionFactory.class );
         assertTyping( JndiInfinispanRegionFactory.class, regionFactory );
      }
      finally {
         StandardServiceRegistryBuilder.destroy( ssr );
      }
   }
}
