/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.functional.cluster;


/**
 * RepeatableSessionRefreshTest.
 * 
 * @author Galder Zamarreño
 * @since 3.5
 */
public class RepeatableSessionRefreshTest extends SessionRefreshTestCase {
   private static final String CACHE_CONFIG = "entity-repeatable";

   @Override
   protected String getEntityCacheConfigName() {
       return CACHE_CONFIG;
   } 

}
