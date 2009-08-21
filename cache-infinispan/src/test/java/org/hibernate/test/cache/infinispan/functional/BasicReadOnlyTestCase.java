package org.hibernate.test.cache.infinispan.functional;

/**
 * @author Galder Zamarreño
 * @since 3.5
 */
public class BasicReadOnlyTestCase extends AbstractFunctionalTestCase {

   public BasicReadOnlyTestCase(String string) {
      super(string, "read-only");
   }

}