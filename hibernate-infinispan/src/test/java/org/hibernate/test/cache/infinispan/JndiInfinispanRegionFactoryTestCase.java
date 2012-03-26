package org.hibernate.test.cache.infinispan;

import java.util.Properties;

import org.junit.Test;

import org.hibernate.cfg.SettingsFactory;

/**
 * // TODO: Document this
 *
 * @author Galder Zamarreño
 * @since 3.5
 */
public class JndiInfinispanRegionFactoryTestCase {
   @Test
   public void testConstruction() {
      Properties p = new Properties();
      p.setProperty("hibernate.cache.region.factory_class", "org.hibernate.cache.infinispan.JndiInfinispanRegionFactory");
      SettingsFactory.createRegionFactory(p, true);
   }
}
