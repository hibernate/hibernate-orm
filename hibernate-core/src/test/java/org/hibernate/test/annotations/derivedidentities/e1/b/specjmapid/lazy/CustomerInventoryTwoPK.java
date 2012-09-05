/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.test.annotations.derivedidentities.e1.b.specjmapid.lazy;

import java.io.Serializable;

public class CustomerInventoryTwoPK implements Serializable {

   private Integer id;
   private int custId;

   public CustomerInventoryTwoPK() {
   }

   public CustomerInventoryTwoPK(Integer id, int custId) {
      this.id = id;
      this.custId = custId;
   }

   public boolean equals(Object other) {
      if ( other == this ) {
         return true;
      }
      if ( other == null || getClass() != other.getClass() ) {
         return false;
      }
      CustomerInventoryTwoPK cip = ( CustomerInventoryTwoPK ) other;
      return ( custId == cip.custId && ( id == cip.id ||
            ( id != null && id.equals( cip.id ) ) ) );
   }

   public int hashCode() {
      return ( id == null ? 0 : id.hashCode() ) ^ custId;
   }

   public Integer getId() {
      return id;
   }

   public int getCustId() {
      return custId;
   }


}
