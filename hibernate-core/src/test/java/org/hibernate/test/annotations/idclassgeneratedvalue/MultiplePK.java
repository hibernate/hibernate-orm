/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.test.annotations.idclassgeneratedvalue;

import java.io.Serializable;

/**
 * MultiplePK
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Stale W. Pedersen</a>
 */
public class MultiplePK implements Serializable
{
   private final Long id1;
   private final Long id2;
   private final Long id3;
// AnnotationBinder (incorrectly) requires this to be transient; see HHH-4819 and HHH-4820
   private final transient int cachedHashCode;

   private MultiplePK()
   {
      id1 = null;
      id2 = null;
      id3 = null;
      cachedHashCode = super.hashCode();
   }
   
   public MultiplePK(Long id1, Long id2, Long id3)
   {
      this.id1 = id1;
      this.id2 = id2;
      this.id3 = id3;
      this.cachedHashCode = calculateHashCode();
   }
   

   private int calculateHashCode() {
       int result = id1.hashCode();
       result = 31 * result + id2.hashCode();
       return result;
   }

   public Long getId1() {
       return id1;
   }

   public Long getId2() {
       return id2;
   }
   
   public Long getId3() {
      return id3;
  }

   @Override
   public boolean equals(Object o) 
   {
       if ( this == o ) {
           return true;
       }
       if ( o == null || getClass() != o.getClass() ) 
       {
           return false;
       }

       MultiplePK multiplePK = (MultiplePK) o;

       return id1.equals( multiplePK.id1 )
               && id2.equals( multiplePK.id2 )
               && id3.equals( multiplePK.id3);
   }

   @Override
   public int hashCode() {
       return cachedHashCode;
   }
}
