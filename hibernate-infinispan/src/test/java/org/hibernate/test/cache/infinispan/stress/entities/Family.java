/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other
 * contributors as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a full listing of
 * individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.hibernate.test.cache.infinispan.stress.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;

@Entity
public final class Family {

   @Id
   @GeneratedValue
   private int id;
   private String name;
   private String secondName;
   @OneToMany
   private Set<Person> members;
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

      if (id != family.id) return false;
      if (version != family.version) return false;
      if (members != null ? !members.equals(family.members) : family.members != null)
         return false;
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
      result = 31 * result + (members != null ? members.hashCode() : 0);
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
