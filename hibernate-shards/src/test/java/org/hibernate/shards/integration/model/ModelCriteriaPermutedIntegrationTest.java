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


import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.shards.integration.BaseShardingIntegrationTestCase;
import org.hibernate.shards.model.Building;
import org.hibernate.shards.model.Floor;
import org.hibernate.shards.model.Office;
import org.hibernate.shards.util.Lists;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author maxr@google.com (Max Ross)
 */
public class ModelCriteriaPermutedIntegrationTest extends BaseShardingIntegrationTestCase {

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
    // because of the fuzziness in how avg gets computed on hsqldb
    // we need to make sure the per-shard avg is a round number, otherwise
    // our test will fail
    b1f1 = ModelDataFactory.floor(b1, 1, new BigDecimal(10.00));
    b1f2 = ModelDataFactory.floor(b1, 2, new BigDecimal(20.00));
    b1f3 = ModelDataFactory.floor(b1, 3, new BigDecimal(30.00));
    b1f3o1 = ModelDataFactory.office("NOT LAHGE", b1f3);
    b1f3o2 = ModelDataFactory.office("LAHGE", b1f3);
    session.save(b1);
    session.getTransaction().commit();

    session.beginTransaction();
    b2 = ModelDataFactory.building("b2");
    b2f1 = ModelDataFactory.floor(b2, 1, new BigDecimal(20.00));
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

  public void testLoadAllBuildings() {
    Criteria crit = session.createCriteria(Building.class);
    List<Building> buildings = list(crit);
    assertEquals(2, buildings.size());
    assertTrue(buildings.contains(b1));
    assertTrue(buildings.contains(b2));
  }

  public void testLoadAllBuildingsAfterForcingEarlyInit() {
    Criteria crit = session.createCriteria(Building.class);
    // forces us to initialize an actual Criteria object
    crit.getAlias();
    List<Building> buildings = list(crit);
    assertEquals(2, buildings.size());
    assertTrue(buildings.contains(b1));
    assertTrue(buildings.contains(b2));
  }

  public void testLoadBuildingByName() {
    Criteria crit = session.createCriteria(Building.class);
    crit.add(Restrictions.eq("name", "b2"));
    Building b2Reloaded = uniqueResult(crit);
    assertEquals(b2.getBuildingId(), b2Reloaded.getBuildingId());
  }

  public void testLoadBuildingByNameAfterForcingEarlyInit() {
    Criteria crit = session.createCriteria(Building.class);
    crit.add(Restrictions.eq("name", "b2"));
    // forces us to initialize an actual Criteria object
    crit.getAlias();
    Building b2Reloaded = uniqueResult(crit);
    assertEquals(b2.getBuildingId(), b2Reloaded.getBuildingId());
  }

  public void testLoadBuildingsByLikeName() {
    Criteria crit = session.createCriteria(Building.class);
    crit.add(Restrictions.in("name", Lists.newArrayList("b1", "b2")));
    List<Building> buildings = list(crit);
    assertEquals(2, buildings.size());
    assertTrue(buildings.contains(b1));
    assertTrue(buildings.contains(b2));
  }

  public void testLoadHighFloors() {
    Criteria crit = session.createCriteria(Floor.class);
    crit.add(Restrictions.ge("number", 3));
    List<Floor> floors = list(crit);
    assertEquals(1, floors.size());
    assertTrue(floors.contains(b1f3));
  }

  public void testLoadBuildingsWithHighFloorsViaTopLevelCriteria() {
    Criteria crit = session.createCriteria(Building.class);
    Criteria floorCrit = crit.createCriteria("floors");
    floorCrit.add(Restrictions.ge("number", 3));
    List<Building> l = list(crit);
    assertEquals(1, l.size());
  }

  public void testLoadBuildingsWithHighFloorsViaTopLevelCriteriaAfterForcingEarlyInitOnTopLevelCriteria() {
    Criteria crit = session.createCriteria(Building.class);
    Criteria floorCrit = crit.createCriteria("floors");
    floorCrit.add(Restrictions.ge("number", 3));
    // forces us to initialize an actual Criteria object
    crit.getAlias();
    List<Building> l = list(crit);
    assertEquals(1, l.size());
  }

  public void testLoadBuildingsWithHighFloorsViaTopLevelCriteriaAfterForcingEarlyInitOnSubcriteria() {
    Criteria crit = session.createCriteria(Building.class);
    Criteria floorCrit = crit.createCriteria("floors");
    floorCrit.add(Restrictions.ge("number", 3));
    // forces us to initialize an actual Criteria object
    floorCrit.getAlias();
    List<Building> l = list(crit);
    assertEquals(1, l.size());
  }

  public void testLoadBuildingsWithHighFloorsViaSubcriteriaAfterForcingEarlyInitOnSubcriteria() {
    Criteria crit = session.createCriteria(Building.class);
    Criteria floorCrit = crit.createCriteria("floors");
    floorCrit.add(Restrictions.ge("number", 3));
    // forces us to initialize an actual Criteria object
    floorCrit.getAlias();
    List<Building> l = list(floorCrit);
    assertEquals(1, l.size());
  }

  public void testLoadBuildingsWithHighFloorsViaSubcriteriaAfterForcingEarlyInitOnTopLevelCriteria() {
    Criteria crit = session.createCriteria(Building.class);
    Criteria floorCrit = crit.createCriteria("floors");
    floorCrit.add(Restrictions.ge("number", 3));
    // forces us to initialize an actual Criteria object
    crit.getAlias();
    List<Building> l = list(floorCrit);
    assertEquals(1, l.size());
  }

  public void testLoadBuildingsWithHighFloorsViaSubcriteria() {
    Criteria crit = session.createCriteria(Building.class);
    Criteria floorCrit = crit.createCriteria("floors");
    floorCrit.add(Restrictions.ge("number", 3));
    // note how we execute the query via the floorCrit
    List<Building> l = list(floorCrit);
    assertEquals(1, l.size());
  }

  public void testLoadBuildingsWithLargeOfficesViaTopLevelCriteria() {
    Criteria crit = session.createCriteria(Building.class);
    Criteria floorCrit = crit.createCriteria("floors");
    Criteria officeCrit = floorCrit.createCriteria("offices");
    officeCrit.add(Restrictions.eq("label", "LAHGE"));
    List<Building> l = list(crit);
    assertEquals(2, l.size());
  }

  public void testLoadBuildingsWithLargeOfficesViaSubcriteria() {
    Criteria crit = session.createCriteria(Building.class);
    Criteria floorCrit = crit.createCriteria("floors");
    Criteria officeCrit = floorCrit.createCriteria("offices");
    officeCrit.add(Restrictions.eq("label", "LAHGE"));
    // now how we execute the query via the floorcrit
    List<Building> l = list(officeCrit);
    assertEquals(2, l.size());
  }

  public void testRowCountProjection() {
    Criteria crit = session.createCriteria(Building.class).setProjection(Projections.rowCount());
    Criteria floorCrit = crit.createCriteria("floors");
    Criteria officeCrit = floorCrit.createCriteria("offices");
    officeCrit.add(Restrictions.eq("label", "LAHGE"));
    // now how we execute the query via the floorcrit
    List<Integer> l = list(officeCrit);
    assertEquals(1, l.size());
    int total = 0;
    for(int shardTotal : l) {
      total += shardTotal;
    }
    assertEquals(2, total);
  }

  public void testAvgProjection() {
    Criteria crit = session.createCriteria(Floor.class).setProjection(Projections.avg("squareFeet"));
    List<Double> l = list(crit);
    assertEquals(1, l.size());
    assertEquals(20.0, l.get(0));
  }


  public void testMaxResults() throws Exception {
    Criteria crit = session.createCriteria(Building.class).setMaxResults(1);
    assertEquals(1, list(crit).size());
  }

  public void testAggregateProjection() throws Exception {
    Criteria crit = session.createCriteria(Floor.class).setProjection(Projections.sum("number"));
    List<Integer> l = list(crit);
    assertEquals(1, l.size());
    assertEquals(new BigDecimal(7), l.get(0));
  }

  public void testMultiExitOperations() throws Exception {
    session.beginTransaction();
    Building b = ModelDataFactory.building("Only Has Floors from 199-210");
    ModelDataFactory.floor(b, 199);
    ModelDataFactory.floor(b, 200);
    ModelDataFactory.floor(b, 201);
    session.save(b);
    session.getTransaction().commit();
    resetSession();
    Criteria crit = session.createCriteria(Floor.class);
    crit.addOrder(Order.asc("number")).setFirstResult(2).setMaxResults(3).setProjection(Projections.sum("number"));
    List<Integer> l = list(crit);
    assertEquals(1, l.size());
    assertEquals(new BigDecimal(204), l.get(0));
  }

  public void testMultiOrdering() throws Exception {
    Criteria crit = session.createCriteria(Office.class);
    crit.addOrder(Order.asc("label")).addOrder(Order.desc("floor.building.name"));
    List<Office> l = list(crit);
    List<Office> answer = Lists.newArrayList(b2f1o1, b1f3o1, b1f3o2);
    assertTrue(answer.equals(l));
  }
}

