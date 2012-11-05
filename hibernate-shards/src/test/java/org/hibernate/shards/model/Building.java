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

import org.hibernate.shards.util.Lists;

import java.io.Serializable;
import java.util.List;

/**
 * @author maxr@google.com (Max Ross)
 */
public class Building {

  private Serializable buildingId;
  private String name;
  private List<Floor> floors = Lists.newArrayList();
  private List<Tenant> tenants = Lists.newArrayList();
  private List<Elevator> elevators = Lists.newArrayList();

  public Serializable getBuildingId() {
    return buildingId;
  }

  public void setBuildingId(Serializable buildingId) {
    this.buildingId = buildingId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<Floor> getFloors() {
    return floors;
  }

  public void setFloors(List<Floor> floors) {
    this.floors = floors;
  }

  public List<Tenant> getTenants() {
    return tenants;
  }

  public void setTenants(List<Tenant> tenants) {
    this.tenants = tenants;
  }

  public List<Elevator> getElevators() {
    return elevators;
  }

  public void setElevators(List<Elevator> elevators) {
    this.elevators = elevators;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final Building building = (Building)o;

    if (buildingId != null ? !buildingId.equals(building.buildingId)
        : building.buildingId != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return (buildingId != null ? buildingId.hashCode() : 0);
  }
}
