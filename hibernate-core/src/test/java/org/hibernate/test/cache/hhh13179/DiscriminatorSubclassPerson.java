/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.hhh13179;

public abstract class DiscriminatorSubclassPerson {

   private Long oid;

   public Long getOid() {
      return oid;
   }

   public void setOid(Long oid) {
      this.oid = oid;
   }
}
