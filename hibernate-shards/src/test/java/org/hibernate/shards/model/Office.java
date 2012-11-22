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
public class Office {

  private Serializable officeId;
  private String label;
  private Floor floor;
  private Window window;
  private List<Person> people;

  public Serializable getOfficeId() {
    return officeId;
  }

  void setOfficeId(Serializable officeId) {
    this.officeId = officeId;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public Floor getFloor() {
    return floor;
  }

  public void setFloor(Floor floor) {
    this.floor = floor;
  }

  public List<Person> getPeople() {
    return people;
  }

  public void setPeople(List<Person> people) {
    this.people = people;
  }

  public Window getWindow() {
    return window;
  }

  public void setWindow(Window window) {
    this.window = window;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final Office office = (Office)o;

    return !(officeId != null ? !officeId.equals(office.officeId)
        : office.officeId != null);

  }

  @Override
  public int hashCode() {
    return (officeId != null ? officeId.hashCode() : 0);
  }
}
