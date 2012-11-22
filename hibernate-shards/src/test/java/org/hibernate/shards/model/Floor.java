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
import java.math.BigDecimal;
import java.util.List;

/**
 * @author maxr@google.com (Max Ross)
 */
public class Floor {

  private Serializable floorId;
  private int number;
  private BigDecimal squareFeet;
  private List<Tenant> tenants;
  private List<Office> offices = Lists.newArrayList();
  private List<Elevator> elevators = Lists.newArrayList();
  private Escalator goingUp;
  private Escalator goingDown;
  private Building building;

  public Serializable getFloorId() {
    return floorId;
  }

  void setFloorId(Serializable floorId) {
    this.floorId = floorId;
  }

  public int getNumber() {
    return number;
  }

  public void setNumber(int number) {
    this.number = number;
  }

  public BigDecimal getSquareFeet() {
    return squareFeet;
  }

  public void setSquareFeet(BigDecimal squareFeet) {
    this.squareFeet = squareFeet;
  }

  public List<Tenant> getTenants() {
    return tenants;
  }

  public void setTenants(List<Tenant> tenants) {
    this.tenants = tenants;
  }

  public List<Office> getOffices() {
    return offices;
  }

  public void setOffices(List<Office> offices) {
    this.offices = offices;
  }

  public List<Elevator> getElevators() {
    return elevators;
  }

  public void setElevators(List<Elevator> elevators) {
    this.elevators = elevators;
  }

  public Escalator getGoingUp() {
    return goingUp;
  }

  public void setGoingUp(Escalator goingUp) {
    this.goingUp = goingUp;
  }

  public Escalator getGoingDown() {
    return goingDown;
  }

  public void setGoingDown(Escalator goingDown) {
    this.goingDown = goingDown;
  }

  public Building getBuilding() {
    return building;
  }

  public void setBuilding(Building building) {
    this.building = building;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final Floor floor = (Floor)o;

    if (floorId != null ? !floorId.equals(floor.floorId) : floor.floorId != null)
    {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return (floorId != null ? floorId.hashCode() : 0);
  }
}
