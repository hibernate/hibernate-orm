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
import java.util.Iterator;
import java.util.Set;

@Entity
public final class Address {

   @Id
   @GeneratedValue
   private int id;
   private int streetNumber;
   private String streetName;
   private String cityName;
   private String countryName;
   private String zipCode;
   @OneToMany
   private Set<Person> inhabitants;
   private int version;

   public Address(int streetNumber, String streetName, String cityName, String countryName) {
      this.streetNumber = streetNumber;
      this.streetName = streetName;
      this.cityName = cityName;
      this.countryName = countryName;
      this.zipCode = null;
      this.inhabitants = new HashSet<Person>();
      this.id = 0;
      this.version = 0;
   }

   protected Address() {
      this.streetNumber = 0;
      this.streetName = null;
      this.cityName = null;
      this.countryName = null;
      this.zipCode = null;
      this.inhabitants = new HashSet<Person>();
      this.id = 0;
      this.version = 0;
   }

   public int getStreetNumber() {
      return streetNumber;
   }

   public String getStreetName() {
      return streetName;
   }

   public String getCityName() {
      return cityName;
   }

   public String getCountryName() {
      return countryName;
   }

   public String getZipCode() {
      return zipCode;
   }

   public void setZipCode(String zipCode) {
      this.zipCode = zipCode;
   }

   public Set<Person> getInhabitants() {
      return inhabitants;
   }

   public boolean addInhabitant(Person inhabitant) {
      boolean done = false;
      if (inhabitants.add(inhabitant)) {
         inhabitant.setAddress(this);
         done = true;
      }
      return done;
   }

   public boolean remInhabitant(Person inhabitant) {
      boolean done = false;
      if (inhabitants.remove(inhabitant)) {
         inhabitant.setAddress(null);
         done = true;
      }
      return done;
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

   protected void removeAllInhabitants() {
      // inhabitants relation is not CASCADED, we must delete the relation on other side by ourselves
      for (Iterator<Person> iterator = inhabitants.iterator(); iterator.hasNext(); ) {
         Person p = iterator.next();
         p.setAddress(null);
      }
   }

   protected void setStreetNumber(int streetNumber) {
      this.streetNumber = streetNumber;
   }

   protected void setStreetName(String streetName) {
      this.streetName = streetName;
   }

   protected void setCityName(String cityName) {
      this.cityName = cityName;
   }

   protected void setCountryName(String countryName) {
      this.countryName = countryName;
   }

   protected void setInhabitants(Set<Person> inhabitants) {
      if (inhabitants == null) {
         this.inhabitants = new HashSet<Person>();
      } else {
         this.inhabitants = inhabitants;
      }
   }

   protected void setVersion(Integer version) {
      this.version = version;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Address address = (Address) o;

      if (id != address.id) return false;
      if (streetNumber != address.streetNumber) return false;
      if (version != address.version) return false;
      if (cityName != null ? !cityName.equals(address.cityName) : address.cityName != null)
         return false;
      if (countryName != null ? !countryName.equals(address.countryName) : address.countryName != null)
         return false;
      if (inhabitants != null ? !inhabitants.equals(address.inhabitants) : address.inhabitants != null)
         return false;
      if (streetName != null ? !streetName.equals(address.streetName) : address.streetName != null)
         return false;
      if (zipCode != null ? !zipCode.equals(address.zipCode) : address.zipCode != null)
         return false;

      return true;
   }

   @Override
   public int hashCode() {
      int result = streetNumber;
      result = 31 * result + (streetName != null ? streetName.hashCode() : 0);
      result = 31 * result + (cityName != null ? cityName.hashCode() : 0);
      result = 31 * result + (countryName != null ? countryName.hashCode() : 0);
      result = 31 * result + (zipCode != null ? zipCode.hashCode() : 0);
      result = 31 * result + (inhabitants != null ? inhabitants.hashCode() : 0);
      result = 31 * result + id;
      result = 31 * result + version;
      return result;
   }

   @Override
   public String toString() {
      return "Address{" +
            "cityName='" + cityName + '\'' +
            ", streetNumber=" + streetNumber +
            ", streetName='" + streetName + '\'' +
            ", countryName='" + countryName + '\'' +
            ", zipCode='" + zipCode + '\'' +
            ", inhabitants=" + inhabitants +
            ", id=" + id +
            ", version=" + version +
            '}';
   }

}
