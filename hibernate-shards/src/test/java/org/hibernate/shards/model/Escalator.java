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
public class Escalator {

  private Serializable escalatorId;
  private Floor bottomFloor;
  private Floor topFloor;

  public Serializable getEscalatorId() {
    return escalatorId;
  }

  void setEscalatorId(Serializable escalatorId) {
    this.escalatorId = escalatorId;
  }

  public Floor getBottomFloor() {
    return bottomFloor;
  }

  public void setBottomFloor(Floor bottomFloor) {
    this.bottomFloor = bottomFloor;
  }

  public Floor getTopFloor() {
    return topFloor;
  }

  public void setTopFloor(Floor topFloor) {
    this.topFloor = topFloor;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final Escalator escalator = (Escalator)o;

    if (escalatorId != null ? !escalatorId.equals(escalator.escalatorId)
        : escalator.escalatorId != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return (escalatorId != null ? escalatorId.hashCode() : 0);
  }
}
