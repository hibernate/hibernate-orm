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
public class Elevator {

  private Serializable elevatorId;
  private List<Floor> floors = Lists.newArrayList();
  private Building building;

  public Serializable getElevatorId() {
    return elevatorId;
  }

  public void setElevatorId(Serializable elevatorId) {
    this.elevatorId = elevatorId;
  }

  public List<Floor> getFloors() {
    return floors;
  }

  public void setFloors(List<Floor> floors) {
    this.floors = floors;
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

    final Elevator elevator = (Elevator)o;

    if (elevatorId != null ? !elevatorId.equals(elevator.elevatorId)
        : elevator.elevatorId != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return (elevatorId != null ? elevatorId.hashCode() : 0);
  }
}
