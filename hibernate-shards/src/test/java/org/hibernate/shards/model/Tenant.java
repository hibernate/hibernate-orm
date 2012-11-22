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
import java.util.List;

/**
 * @author maxr@google.com (Max Ross)
 */
public class Tenant {

  private Serializable tenantId;
  private String name;
  private List<Building> buildings;
  private List<Person> employees;

  public Serializable getTenantId() {
    return tenantId;
  }

  void setTenantId(Serializable tenantId) {
    this.tenantId = tenantId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<Building> getBuildings() {
    return buildings;
  }

  public void setBuildings(List<Building> buildings) {
    this.buildings = buildings;
  }

  public List<Person> getEmployees() {
    return employees;
  }

  public void setEmployees(List<Person> employees) {
    this.employees = employees;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final Tenant tenant = (Tenant)o;

    if (tenantId != null ? !tenantId.equals(tenant.tenantId)
        : tenant.tenantId != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return (tenantId != null ? tenantId.hashCode() : 0);
  }
}
