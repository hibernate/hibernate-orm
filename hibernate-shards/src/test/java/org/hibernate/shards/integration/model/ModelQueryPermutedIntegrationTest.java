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

import org.hibernate.Query;
import org.hibernate.QueryException;
import org.hibernate.shards.integration.BaseShardingIntegrationTestCase;
import org.hibernate.shards.model.Building;
import org.hibernate.shards.model.Floor;
import org.hibernate.shards.model.Office;
import org.hibernate.shards.util.Lists;

import java.util.Collection;
import java.util.List;

/**
 * @author Maulik Shah
 */
public class ModelQueryPermutedIntegrationTest extends BaseShardingIntegrationTestCase {

  private Building b1;
  private Floor b1f1;
  private Floor b1f2;
  private Floor b1f3;
  private Office b1f3o1;
  private Office b1f3o2;

  private Building b2;
  private Floor b2f1;
  private Office b2f1o1;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    session.beginTransaction();
    b1 = ModelDataFactory.building("b1");
    b1f1 = ModelDataFactory.floor(b1, 1);
    b1f2 = ModelDataFactory.floor(b1, 2);
    b1f3 = ModelDataFactory.floor(b1, 3);
    b1f3o1 = ModelDataFactory.office("NOT LAHGE", b1f3);
    b1f3o2 = ModelDataFactory.office("LAHGE", b1f3);
    session.save(b1);

    b2 = ModelDataFactory.building("b2");
    b2f1 = ModelDataFactory.floor(b2, 1);
    b2f1o1 = ModelDataFactory.office("LAHGE", b2f1);
    session.save(b2);
    session.getTransaction().commit();
    resetSession();
    b1 = reload(b1);
    b1f1 = reload(b1f1);
    b1f2 = reload(b1f2);
    b1f3 = reload(b1f3);
    b1f3o1 = reload(b1f3o1);
    b1f3o2 = reload(b1f3o2);
    b2 = reload(b2);
    b2f1 = reload(b2f1);
    b2f1o1 = reload(b2f1o1);
  }

  @Override
  protected void tearDown() throws Exception {
    b1 = null;
    b1f1 = null;
    b1f2 = null;
    b1f3 = null;
    b1f3o1 = null;
    b1f3o2 = null;
    b2 = null;
    b2f1 = null;
    b2f1o1 = null;
    super.tearDown();
  }

  public void testLoadAllBuildings()  {
    String queryString = "from Building";
    Query query = session.createQuery(queryString);
    @SuppressWarnings("unchecked")
    List<Building> buildings = query.list();
    assertEquals(2, buildings.size());
    assertTrue(buildings.contains(b1));
    assertTrue(buildings.contains(b2));
  }

  public void testLoadBuildingByName()  {
    String queryString = "from Building as b where b.name=:name";
    Query query = session.createQuery(queryString).setString("name", "b2");
    Building b2Reloaded = (Building) query.uniqueResult();
    assertEquals(b2.getBuildingId(), b2Reloaded.getBuildingId());
  }

  public void testLoadBuildingsByLikeName()  {
    String queryString = "from Building as b where b.name like :name";
    Query query = session.createQuery(queryString).setString("name", "b%");
    @SuppressWarnings("unchecked")
    List<Building> buildings = query.list();
    assertEquals(2, buildings.size());
    assertTrue(buildings.contains(b1));
    assertTrue(buildings.contains(b2));
  }

  public void testLoadHighFloors()  {
    String queryString = "from Floor as f where f.number >= 3";
    Query query = session.createQuery(queryString);
    @SuppressWarnings("unchecked")
    List<Floor> floors = query.list();
    assertEquals(1, floors.size());
  }

  public void testLoadBuildingsWithHighFloors() {
    String queryString = "from Floor as f where f.number >= 3";
    Query query = session.createQuery(queryString);
    @SuppressWarnings("unchecked")
    List<Floor> floors = query.list();
    assertEquals(1, floors.size());
  }

  public void testLoadBuildingsWithHighFloorsAndLargeOffices() {
    String queryString = "from Building b join b.floors floor join floor.offices office where office.label = 'LAHGE'";
    Query query = session.createQuery(queryString);
    @SuppressWarnings("unchecked")
    List<Building> buildings = query.list();
    assertEquals(2, buildings.size());
  }

  public void testNamedQuery() {
    Query query = session.getNamedQuery("SelectFloorsHigherThan");
    query.setInteger("lowestFloor", 3);
    @SuppressWarnings("unchecked")
    List<Floor> floors = query.list();
    assertEquals(1, floors.size());
  }

  public void testSetMaxResults() {
    String queryString = "from Floor";
    Query query = session.createQuery(queryString).setMaxResults(1);
    @SuppressWarnings("unchecked")
    List<Floor> floors = query.list();
    assertEquals(1, floors.size());
  }

  public void testSetFirstResultQuery() {
    String queryString = "from Floor";
    Query query = session.createQuery(queryString).setFirstResult(2);
    @SuppressWarnings("unchecked")
    List<Floor> floors = query.list();
    assertEquals(2, floors.size());
  }

  public void xtestAggregating()  {
    //TODO(maulik) make this test work
    String queryString = "min(floor.number) from Floor floor";
    Query query = session.createQuery(queryString);
    @SuppressWarnings("unchecked")
    List<Building> buildings = query.list();
    assertEquals(2, buildings.size());
  }

  public void testCreateSimpleFilter() {
    Building b = (Building) session.get(Building.class, b1.getBuildingId());
    assertNotNull(b);
    Collection<Floor> coll = b.getFloors();
    Query query = session.createFilter(coll, "");
    @SuppressWarnings("unchecked")
    List<Floor> filteredFloors = query.list();
    assertEquals(3, filteredFloors.size());
  }

  public void testCreateFancyFilter() {
    Building b = (Building) session.get(Building.class, b1.getBuildingId());
    assertNotNull(b);
    Collection<Floor> coll = b.getFloors();
    Query query = session.createFilter(coll, "where number > 2");
    @SuppressWarnings("unchecked")
    List<Floor> filteredFloors = query.list();
    assertEquals(1, filteredFloors.size());
  }

  public void testCreateFilterForUnattachedCollection() {
    List<Floor> floors = Lists.newArrayList();
    try {
      session.createFilter(floors, "");
      fail("expected query exception");
    } catch (QueryException qe) {
      // good
    }
  }
}
