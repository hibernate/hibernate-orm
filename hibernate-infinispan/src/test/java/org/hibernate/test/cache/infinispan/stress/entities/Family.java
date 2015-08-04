/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.test.cache.infinispan.stress.entities;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Version;
import java.util.HashSet;
import java.util.Set;

@Entity
public final class Family {

   @Id
   @GeneratedValue
   private int id;
   private String name;
   private String secondName;
   @OneToMany(cascade = CascadeType.ALL, mappedBy = "family", orphanRemoval = true)
   private Set<Person> members;
   @Version
   private int version;

   public Family(String name) {
      this.name = name;
      this.secondName = null;
      this.members = new HashSet<Person>();
      this.id = 0;
      this.version = 0;
   }

   protected Family() {
      this.name = null;
      this.secondName = null;
      this.members = new HashSet<Person>();
      this.id = 0;
      this.version = 0;
   }

   public String getName() {
      return name;
   }

   public Set<Person> getMembers() {
      return members;
   }

   public String getSecondName() {
      return secondName;
   }

   public void setSecondName(String secondName) {
      this.secondName = secondName;
   }

   public int getId() {
      return id;
   }

   public void setId(int id) {
      this.id = id;
   }

   public int getVersion() {
      return version;
   }

   public void setName(String name) {
      this.name = name;
   }

   public void setMembers(Set<Person> members) {
      if (members == null) {
         this.members = new HashSet<Person>();
      } else {
         this.members = members;
      }
   }

   public void setVersion(Integer version) {
      this.version = version;
   }

   boolean addMember(Person member) {
      return members.add(member);
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Family family = (Family) o;

      // members must not be in the comparison since we would end up in infinite recursive call
      if (id != family.id) return false;
      if (version != family.version) return false;
      if (name != null ? !name.equals(family.name) : family.name != null)
         return false;
      if (secondName != null ? !secondName.equals(family.secondName) : family.secondName != null)
         return false;

      return true;
   }

   @Override
   public int hashCode() {
      int result = name != null ? name.hashCode() : 0;
      result = 31 * result + (secondName != null ? secondName.hashCode() : 0);
      result = 31 * result + id;
      result = 31 * result + version;
      return result;
   }

   @Override
   public String toString() {
      return "Family{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", secondName='" + secondName + '\'' +
            ", members=" + members +
            ", version=" + version +
            '}';
   }

}
