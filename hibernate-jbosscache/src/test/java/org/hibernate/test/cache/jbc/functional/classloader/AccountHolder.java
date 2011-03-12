/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.test.cache.jbc.functional.classloader;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

/**
 * Comment
 * 
 * @author Brian Stansberry
 * @version $Revision: 60233 $
 */
public class AccountHolder implements Serializable
{   
   private static final long serialVersionUID = 1L;
   
   private String lastName;
   private String ssn;
   private transient boolean deserialized;
   
   public AccountHolder( ) {
      this("Stansberry", "123-456-7890");
   }
   
   public AccountHolder(String lastName, String ssn)
   {
      this.lastName = lastName;
      this.ssn = ssn;
   }
   
   public String getLastName( ) { return this.lastName; }
   public void setLastName(String lastName) { this.lastName = lastName; }
   
   public String getSsn( ) { return ssn; }
   public void setSsn(String ssn) { this.ssn = ssn; }
   
   public boolean equals(Object obj)
   {
      if (obj == this) return true;
      if (!(obj instanceof AccountHolder)) return false;
      AccountHolder pk = (AccountHolder)obj;
      if (!lastName.equals(pk.lastName)) return false;
      if (!ssn.equals(pk.ssn)) return false;
      return true;
   }
   
   public int hashCode( )
   {
      int result = 17;
      result = result * 31 + lastName.hashCode();
      result = result * 31 + ssn.hashCode();
      return result;
   }
   
   public String toString()
   {
      StringBuffer sb = new StringBuffer(getClass().getName());
      sb.append("[lastName=");
      sb.append(lastName);
      sb.append(",ssn=");
      sb.append(ssn);
      sb.append(",deserialized=");
      sb.append(deserialized);
      sb.append("]");
      return sb.toString();
   }
   
   private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException
   {
      ois.defaultReadObject();
      deserialized = true;
   }

}
