/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.test.cache.infinispan.stress.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Version;

import java.util.Date;

@Entity
public class Person {

   @Id
   @GeneratedValue
   private int id;
   private String firstName;
   @ManyToOne
   private Family family;
   private Date birthDate;
   @ManyToOne
   private Address address;
   private boolean checked;
   @Version
   private int version;

   public Person(String firstName, Family family) {
      this.firstName = firstName;
      this.family = family;
      this.birthDate = null;
      this.address = null;
      this.checked = false;
      this.id = 0;
      this.version = 0;
      this.family.addMember(this);
   }

   protected Person() {
      this.firstName = null;
      this.family = null;
      this.birthDate = null;
      this.address = null;
      this.checked = false;
      this.id = 0;
      this.version = 0;
   }

   public String getFirstName() {
      return firstName;
   }

   public Family getFamily() {
      return family;
   }

   public Date getBirthDate() {
      return birthDate;
   }

   public void setBirthDate(Date birthDate) {
      this.birthDate = birthDate;
   }

   public Address getAddress() {
      return address;
   }

   public void setAddress(Address address) {
      // To skip Hibernate BUG with access.PROPERTY : the rest should be done in DAO
      //		this.address = address;
      // Hibernate BUG : if we update the relation on 2 sides
      if (this.address != address) {
         if (this.address != null) this.address.remInhabitant(this);
         this.address = address;
         if (this.address != null) this.address.addInhabitant(this);
      }
   }

   public boolean isChecked() {
      return checked;
   }

   public void setChecked(boolean checked) {
      this.checked = checked;
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

   protected void setFirstName(String firstName) {
      this.firstName = firstName;
   }

   protected void setFamily(Family family) {
      this.family = family;
   }

   protected void setVersion(Integer version) {
      this.version = version;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Person person = (Person) o;

      if (checked != person.checked) return false;
      if (id != person.id) return false;
      if (version != person.version) return false;
      if (address != null ? !address.equals(person.address) : person.address != null)
         return false;
      if (birthDate != null ? !birthDate.equals(person.birthDate) : person.birthDate != null)
         return false;
      if (family != null ? !family.equals(person.family) : person.family != null)
         return false;
      if (firstName != null ? !firstName.equals(person.firstName) : person.firstName != null)
         return false;

      return true;
   }

   @Override
   public int hashCode() {
      int result = firstName != null ? firstName.hashCode() : 0;
      result = 31 * result + (family != null ? family.hashCode() : 0);
      result = 31 * result + (birthDate != null ? birthDate.hashCode() : 0);
      result = 31 * result + (address != null ? address.hashCode() : 0);
      result = 31 * result + (checked ? 1 : 0);
      result = 31 * result + id;
      result = 31 * result + version;
      return result;
   }

   @Override
   public String toString() {
      return "Person{" +
            "address=" + address +
            ", firstName='" + firstName + '\'' +
            ", family=" + family +
            ", birthDate=" + birthDate +
            ", checked=" + checked +
            ", id=" + id +
            ", version=" + version +
            '}';
   }

}
