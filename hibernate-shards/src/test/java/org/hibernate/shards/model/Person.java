/**
 * Copyright (C) 2007 Google Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package org.hibernate.shards.model;

import java.io.Serializable;

/**
 * @author maxr@google.com (Max Ross)
 */
public class Person {

  private Serializable personId;
  private String name;
  private Office office;
  private Tenant employer;

  public Serializable getPersonId() {
    return personId;
  }

  void setPersonId(Serializable personId) {
    this.personId = personId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Office getOffice() {
    return office;
  }

  public void setOffice(Office office) {
    this.office = office;
  }

  public Tenant getEmployer() {
    return employer;
  }

  public void setEmployer(Tenant employer) {
    this.employer = employer;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final Person person = (Person)o;

    if (personId != null ? !personId.equals(person.personId)
        : person.personId != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return (personId != null ? personId.hashCode() : 0);
  }
}
