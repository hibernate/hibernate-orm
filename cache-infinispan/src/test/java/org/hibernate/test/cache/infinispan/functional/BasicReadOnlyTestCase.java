package org.hibernate.test.cache.infinispan.functional;

/**
 * @author Galder Zamarreño
 * @since 3.5
 */
public class BasicReadOnlyTestCase extends SingleNodeTestCase {

   public BasicReadOnlyTestCase(String string) {
      super(string);
   }

   @Override
   public String getCacheConcurrencyStrategy() {
      return "read-only";
   }

}