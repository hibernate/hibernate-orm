/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
// AnnotationSourceProcessor (incorrectly) requires this to be transient; see HHH-4819 and HHH-4820
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
