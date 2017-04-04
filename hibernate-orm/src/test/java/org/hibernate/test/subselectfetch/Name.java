/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.subselectfetch;

import java.io.Serializable;
import java.util.List;

public class Name implements Serializable {
   private int id;
   private String name;
   private int nameLength;
   private List values;

   public int getId() { return id; }
   public String getName() { return name; }
   public int getNameLength() { return nameLength; }
   public List getValues() { return values; }

   public void setId(int id) { this.id = id; }
   public void setName(String name) { this.name = name; }
   public void setNameLength(int nameLength) { this.nameLength = nameLength; }
   public void setValues(List values) { this.values = values; }

   public boolean equals(Object obj) {
      if (!(obj instanceof Name )) return false;
      Name other = (Name) obj;
      return other.id == this.id;
   }

   public int hashCode() { return id; }
}
