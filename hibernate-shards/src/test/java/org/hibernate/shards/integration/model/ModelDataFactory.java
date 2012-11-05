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

package org.hibernate.shards.integration.model;

import org.hibernate.shards.model.Building;
import org.hibernate.shards.model.Elevator;
import org.hibernate.shards.model.Escalator;
import org.hibernate.shards.model.Floor;
import org.hibernate.shards.model.Office;
import org.hibernate.shards.model.Person;
import org.hibernate.shards.model.Tenant;
import org.hibernate.shards.model.Window;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author maxr@google.com (Max Ross)
 */
public class ModelDataFactory {
  public static Building building(String name) {
    Building b = new Building();
    b.setName(name);
    return b;
  }

  public static Escalator escalator(Floor bottom, Floor top) {
    Escalator esc = new Escalator();
    esc.setBottomFloor(bottom);
    if(bottom != null) {
      bottom.setGoingUp(esc);
    }
    esc.setTopFloor(top);
    if(top != null) {
      top.setGoingDown(esc);
    }
    return esc;
  }

  public static Floor floor(Building b, int number) {
    return floor(b, number, null, null, null);
  }

  public static Floor floor(Building b, int number, BigDecimal squareFeet) {
    return floor(b, number, squareFeet, null, null);
  }

  public static Floor floor(Building b, int number, BigDecimal squareFeet, Escalator up, Escalator down) {
    Floor f = new Floor();
    f.setBuilding(b);
    b.getFloors().add(f);
    f.setNumber(number);
    f.setSquareFeet(squareFeet);
    f.setGoingDown(down);
    f.setGoingUp(up);
    return f;
  }

  public static Elevator elevator(Building b, Floor... floors) {
    Elevator elev = new Elevator();
    elev.setBuilding(b);
    b.getElevators().add(elev);
    for(Floor f : floors) {
      f.getElevators().add(elev);
    }
    return elev;
  }

  public static Tenant tenant(String name, List<Building> buildings, List<Person> employees) {
    Tenant t = new Tenant();
    t.setName(name);
    t.setBuildings(buildings);
    if(buildings != null) {
      for(Building b : buildings) {
        b.getTenants().add(t);
      }
    }
    t.setEmployees(employees);
    if(employees != null) {
      for(Person p : employees) {
        p.setEmployer(t);
      }
    }
    return t;
  }

  public static Person person(String name, Tenant employer) {
    Person p = new Person();
    p.setName(name);
    p.setEmployer(employer);
    if(employer != null) {
      employer.getEmployees().add(p);
    }
    return p;
  }

  public static Office office(String label, Floor floor) {
    Office o = new Office();
    o.setLabel(label);
    o.setFloor(floor);
    floor.getOffices().add(o);
    return o;
  }

  public static Window window(boolean opens) {
    Window w = new Window();
    w.setOpens(opens);
    return w;
  }
}
