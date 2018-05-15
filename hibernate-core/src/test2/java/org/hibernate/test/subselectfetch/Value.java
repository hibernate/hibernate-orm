/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.subselectfetch;

import java.io.Serializable;

public class Value implements Serializable {
   private int id;
   private Name name;
   private String value;

   public int getId() { return id; }
   public Name getName() { return name; }
   public String getValue() { return value; }

   public void setId(int id) { this.id = id; }
   public void setName(Name name) { this.name = name; }
   public void setValue(String value) { this.value = value; }

   public boolean equals(Object obj) {
      if (!(obj instanceof Value )) return false;
      Value other = (Value) obj;
      return other.id == this.id;
   }

   public int hashCode() { return id; }
}
