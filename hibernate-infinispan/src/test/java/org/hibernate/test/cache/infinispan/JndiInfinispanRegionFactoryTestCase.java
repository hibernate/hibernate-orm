package org.hibernate.test.cache.infinispan;

import junit.framework.TestCase;
import org.hibernate.cfg.SettingsFactory;

import java.util.Properties;

/**
 * // TODO: Document this
 *
 * @author Galder Zamarreño
 * @since 3.5
 */
public class JndiInfinispanRegionFactoryTestCase extends TestCase {

   public void testConstruction() {
      Properties p = new Properties();
      p.setProperty("hibernate.cache.region.factory_class", "org.hibernate.cache.infinispan.JndiInfinispanRegionFactory");
      SettingsFactory.createRegionFactory(p, true);
   }
}
