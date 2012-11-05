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

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.ReplicationMode;
import org.hibernate.SessionFactory;
import org.hibernate.TransactionException;
import org.hibernate.classic.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.shards.ShardId;
import org.hibernate.shards.integration.BaseShardingIntegrationTestCase;
import org.hibernate.shards.integration.MemoryLeakPlugger;
import static org.hibernate.shards.integration.model.ModelDataFactory.building;
import static org.hibernate.shards.integration.model.ModelDataFactory.elevator;
import static org.hibernate.shards.integration.model.ModelDataFactory.escalator;
import static org.hibernate.shards.integration.model.ModelDataFactory.floor;
import static org.hibernate.shards.integration.model.ModelDataFactory.office;
import static org.hibernate.shards.integration.model.ModelDataFactory.person;
import static org.hibernate.shards.integration.model.ModelDataFactory.tenant;
import static org.hibernate.shards.integration.model.ModelDataFactory.window;
import org.hibernate.shards.model.Building;
import org.hibernate.shards.model.Escalator;
import org.hibernate.shards.model.Floor;
import org.hibernate.shards.model.Office;
import org.hibernate.shards.model.Person;
import org.hibernate.shards.model.Tenant;
import org.hibernate.shards.model.Window;
import org.hibernate.shards.session.ShardedSessionFactory;
import org.hibernate.shards.session.ShardedSessionImpl;
import org.hibernate.shards.session.SubsetShardedSessionFactoryImpl;
import org.hibernate.shards.strategy.ShardStrategy;
import org.hibernate.shards.strategy.ShardStrategyFactory;
import org.hibernate.shards.strategy.ShardStrategyFactoryDefaultMock;
import org.hibernate.shards.util.Lists;
import org.hibernate.shards.util.Maps;
import org.hibernate.shards.util.Sets;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author maxr@google.com (Max Ross)
 */
public class ModelPermutedIntegrationTest extends BaseShardingIntegrationTestCase {

  /*
  @Override
  protected int getNumDatabases() {
    return 3;
  }

  @Override
  protected int getNumShards() {
    return 9;
  }

  @Override
  protected boolean isVirtualShardingEnabled() {
    return true;
  }

  @Override
  protected IdGenType getIdGenType() {
    return IdGenType.SIMPLE;
  }
  */

  public void testMapping() {
    // if we succeed, our model is ok
  }

  public void testBuildingLifecycle() {
    session.beginTransaction();
    Set<Building> buildings = Sets.newHashSet();
    // we're using the round robin shard selector so we should
    // end up with 2 buildings per shard
    for(int i = 0; i < getNumShards() * 2; i++) {
      Building b = building("building" + i);
      Floor f1 = floor(b, 1);
      Floor f2 = floor(b, 2);
      Floor f3 = floor(b, 3);
      escalator(f1, f2);
      elevator(b, f1, f2, f3);
      elevator(b, f1, f2, f3);
      session.save(b);
      buildings.add(b);
    }
    commitAndResetSession();
    List<Integer> counts =  list(session.createCriteria(Building.class).setProjection(Projections.rowCount()));
    int total = 0;
    for(Integer count : counts) {
      total += count;
    }
    assertEquals(getNumShards() * 2, total);
    session.beginTransaction();
    Map<ShardId, List<Serializable>> shards = Maps.newHashMap();
    for(Building b : buildings) {
      Building bReloaded = reloadAssertNotNull(b);
      assertEquals(b.getName(), bReloaded.getName());
      assertEquals(b.getFloors().size(), bReloaded.getFloors().size());
      for(int i = 0; i < b.getFloors().size(); i++) {
        Floor bFloor = b.getFloors().get(i);
        Floor bReloadedFloor = bReloaded.getFloors().get(i);
        assertEquals(bFloor.getNumber(), bReloadedFloor.getNumber());
        assertEquals(2, bReloadedFloor.getElevators().size());
      }
      assertNotNull(bReloaded.getFloors().get(0).getGoingUp());
      assertNull(bReloaded.getFloors().get(0).getGoingDown());
      assertNull(bReloaded.getFloors().get(1).getGoingUp());
      assertNotNull(bReloaded.getFloors().get(1).getGoingDown());

      assertEquals(b.getElevators().size(), bReloaded.getElevators().size());
      ShardId shardIdForObject = getShardIdForObject(bReloaded);
      assertNotNull(shardIdForObject);
      assertBuildingSubObjectsOnSameShard(bReloaded);
      List<Serializable> idList = shards.get(shardIdForObject);
      if(idList == null) {
        idList = Lists.newArrayList();
        shards.put(shardIdForObject, idList);
      }
      idList.add(bReloaded.getBuildingId());
      bReloaded.setName(bReloaded.getName() + " updated");
      for(Floor newFloor : bReloaded.getFloors()) {
        newFloor.setNumber(newFloor.getNumber() + 33);
      }
    }
    assertEquals(getNumShards(), shards.size());
    for(List<Serializable> idList : shards.values()) {
      assertEquals(2, idList.size());
    }
    commitAndResetSession();
    session.beginTransaction();
    for(Building b : buildings) {
      Building bReloaded = reloadAssertNotNull(b);
      assertNotNull(bReloaded);
      assertEquals(b.getName() + " updated", bReloaded.getName());
      for(int i = 0; i < b.getFloors().size(); i++) {
        Floor bFloor = b.getFloors().get(i);
        Floor bReloadedFloor = bReloaded.getFloors().get(i);
        assertEquals(33 + bFloor.getNumber(), bReloadedFloor.getNumber());
      }
      ShardId shardIdForObject = getShardIdForObject(bReloaded);
      // make sure the object resides on the same shard as before
      assertTrue(shards.get(shardIdForObject).contains(b.getBuildingId()));
      assertBuildingSubObjectsOnSameShard(bReloaded);
      // now let's get rid of the buildings
      session.delete(bReloaded);
    }
    commitAndResetSession();
    for(Building b : buildings) {
      assertNull(reload(b));
      for(Floor f : b.getFloors()) {
        assertNull(reload(f));
      }
    }
  }

  public void testSaveOrUpdateAttached() {
    session.beginTransaction();
    Building b = building("b");
    floor(b, 23);
    session.saveOrUpdate(b);
    commitAndResetSession();
    b = reload(b);
    b.setName("b2");
    session.saveOrUpdate(b);
    commitAndResetSession();
    b = reload(b);
    assertEquals("b2", b.getName());
  }

  public void testSaveOrUpdateDetached() {
    session.beginTransaction();
    Building b = building("b");
    floor(b, 23);
    session.saveOrUpdate(b);
    commitAndResetSession();
    // let's do saveOrUpdate on a detached entity
    Building transientB = building("b2");
    transientB.setBuildingId(b.getBuildingId());
    session.saveOrUpdate(transientB);
    commitAndResetSession();
    b = reload(b);
    assertEquals("b2", b.getName());
  }

  public void testSavingOneToManyChildViaCascade() {
    session.beginTransaction();
    Building b = building("awesome building");
    Floor f = floor(b, 23);
    session.save(b);
    session.getTransaction().commit();
    assertBuildingSubObjectsOnSameShard(b);
    assertOnSameShard(b, f);
    resetSession();
    assertNotNull(reload(b));
    assertNotNull(reload(f));
  }

  /**
   * In this test we demonstrate our ability to create a parent and a child by
   * calling save on them individually (no cascading involved).  The key here
   * is that both objects need to end up in the same shard.
   */
  public void testSavingOneToManyChildSeparatelyInSameSession() {
    session.beginTransaction();
    Building b = building("awesome building");
    session.save(b);
    Floor f = floor(b, 23);
    session.save(f);
    session.getTransaction().commit();
    assertBuildingSubObjectsOnSameShard(b);
    assertOnSameShard(b, f);
  }

  /**
   * In this test we demonstrate our ability to create a parent and a child by
   * calling save on them individually in separate sessions(no cascading involved).  The key here
   * is that both objects need to end up in the same shard.
   */
  public void testSavingOneToManyChildSeparatelyInDifferentSessions() {
    session.beginTransaction();
    Building b = building("awesome building");
    session.save(b);
    session.getTransaction().commit();
    resetSession();
    session.beginTransaction();
    b = reloadAssertNotNull(b);
    Floor f = floor(b, 23);
    session.save(f);
    session.getTransaction().commit();
    assertBuildingSubObjectsOnSameShard(b);
    assertOnSameShard(b, f);
  }

  /**
   * In this test we demonstrate that if you try to save an entity that doesn't
   * support top-level saves before associating some other object with that
   * entity, you'll get an exception.
   */
  public void testSavingOneToOneChildWithoutAssociationFails() {
    session.beginTransaction();
    Escalator esc = escalator(null, null);
    try {
      session.save(esc);
      fail("expected he");
    } catch (HibernateException he) {
      // good;
    }
  }

  /**
   * In this test we demonstrate that if you try to save an entity that doesn't
   * support top-level saves after associating some other object with that
   * entity, you'll be cool.
   */
  public void testSavingOneToOneChildWithAssociationSucceeds() {
    session.beginTransaction();
    Building b = building("hi");
    Floor f = floor(b, 23);
    session.save(b);
    // now there is a session associated with the building and the floor
    Escalator esc = escalator(f, null);
    session.save(esc);
    assertBuildingSubObjectsOnSameShard(b);
    assertOnSameShard(b, f, esc);
    session.getTransaction().commit();
  }

  public void testUpdatingOneToOneWithObjectFromWrongShardFailsOnSave() {
    // test only applies if num dbs > 1
    if(getNumShards() == 1) {
      return;
    }
    session.beginTransaction();
    Building b = building("hi");
    Floor f = floor(b, 23);
    session.save(b);
    // now there is a session associated with the building and the floor
    Escalator esc = escalator(f, null);
    session.save(esc);

    Building b2 = building("hi2");
    Floor f2 = floor(b2, 24);
    session.save(b2);
    session.getTransaction().commit();
    assertFalse("Should have been on different shards!", getShardIdForObject(f2).equals(getShardIdForObject(f.getGoingUp())));
    resetSession();
    session.beginTransaction();
    f = reloadAssertNotNull(f);
    f2 = reloadAssertNotNull(f2);
    f2.setGoingUp(f.getGoingUp());
    try {
      session.save(f2);
      session.getTransaction().commit();
      fail("expected Hibernate Exception");
    } catch (HibernateException he) {
      // good
    }
    session.getTransaction().rollback();
  }

  public void testUpdatingOneToOneWithObjectFromWrongShardFailsOnCommitWithUnfetchedTarget() {
    // test only applies if num dbs > 1
    if(getNumShards() == 1) {
      return;
    }
    session.beginTransaction();
    Building b = building("hi");
    Floor f = floor(b, 23);
    session.save(b);
    // now there is a session associated with the building and the floor
    Escalator esc = escalator(f, null);
    session.save(esc);

    Building b2 = building("hi2");
    Floor f2 = floor(b2, 24);
    session.save(b2);
    session.getTransaction().commit();
    assertFalse("Should have been on different shards!", getShardIdForObject(f2).equals(getShardIdForObject(f.getGoingUp())));
    resetSession();
    session.beginTransaction();
    f = reloadAssertNotNull(f);
    f2 = reloadAssertNotNull(f2);
    f2.setGoingUp(f.getGoingUp());
    assertTrue(f.getGoingUp() instanceof HibernateProxy);
    assertTrue(((HibernateProxy)f.getGoingUp()).getHibernateLazyInitializer().isUninitialized());
    // note that we don't touch the escalator itself so that it is still a proxy
    try {
      session.getTransaction().commit();
      fail("expected Transaction Exception");
    } catch (TransactionException te) {
      // good
    }
  }

  public void testUpdatingOneToOneWithObjectFromWrongShardFailsOnCommitWithFetchedTarget() {
    // test only applies if num dbs > 1
    if(getNumShards() == 1) {
      return;
    }
    session.beginTransaction();
    Building b = building("hi");
    Floor f = floor(b, 23);
    session.save(b);
    // now there is a session associated with the building and the floor
    Escalator esc = escalator(f, null);
    session.save(esc);

    Building b2 = building("hi2");
    Floor f2 = floor(b2, 24);
    session.save(b2);
    session.getTransaction().commit();
    assertFalse("Should have been on different shards!", getShardIdForObject(f2).equals(getShardIdForObject(f.getGoingUp())));
    resetSession();
    session.beginTransaction();
    f = reloadAssertNotNull(f);
    f2 = reloadAssertNotNull(f2);
    f2.setGoingUp(f.getGoingUp());
    // forces lazy object to initialize.  Sadly, this has a meaningful effect
    // on the behavior
    f.getGoingUp().getBottomFloor();
    try {
      session.getTransaction().commit();
      fail("expected Transaction Exception");
    } catch (TransactionException tc) {
      // good
    }
  }

  public void testManyToOneCascade() {
    session.beginTransaction();
    Building b = building("b");
    Floor f1 = floor(b, 1);
    Floor f2 = floor(b, 2);
    Escalator e = escalator(f1, f2);
    session.save(b);
    session.getTransaction().commit();
    resetSession();
    b = reloadAssertNotNull(b);
    f1 = reloadAssertNotNull(f1);
    f2 = reloadAssertNotNull(f2);
    e = reloadAssertNotNull(e);
    assertEquals(2, b.getFloors().size());
    assertNotNull(b.getFloors().get(0).getGoingUp());
    assertNotNull(b.getFloors().get(1).getGoingDown());
    assertBuildingSubObjectsOnSameShard(b);
    assertOnSameShard(b, f1, f2, e);
  }

  public void testUpdatingOneToManyWithObjectFromWrongShardFailsOnSave() {
    // test only applies if num dbs > 1
    if(getNumShards() == 1) {
      return;
    }
    session.beginTransaction();
    Building b = building("hi");
    floor(b, 23);
    session.save(b);

    Building b2 = building("hi2");
    Floor f2 = floor(b2, 24);
    session.save(b2);
    session.getTransaction().commit();
    assertFalse("Should have been on different shards!", getShardIdForObject(b).equals(getShardIdForObject(f2)));
    resetSession();
    session.beginTransaction();
    b = reloadAssertNotNull(b);
    f2 = reloadAssertNotNull(f2);
    b.getFloors().add(f2);
    try {
      session.save(b);
      session.getTransaction().commit();
      fail("expected Hibernate Exception");
    } catch (HibernateException he) {
      // good
    }
  }

  public void testUpdatingOneToManyWithObjectFromWrongShardFailsOnCommit() {
    // test only applies if num dbs > 1
    if(getNumShards() == 1) {
      return;
    }
    session.beginTransaction();
    Building b = building("hi");
    floor(b, 23);
    session.save(b);

    Building b2 = building("hi2");
    Floor f2 = floor(b2, 24);
    session.save(b2);
    session.getTransaction().commit();
    assertFalse("Should have been on different shards!", getShardIdForObject(f2).equals(getShardIdForObject(b)));
    resetSession();
    session.beginTransaction();
    b = reloadAssertNotNull(b);
    f2 = reloadAssertNotNull(f2);
    b.getFloors().add(f2);
    // note that we don't touch the floor itself so that it is still a proxy
    try {
      session.getTransaction().commit();
      fail("expected Transaction Exception");
    } catch (TransactionException te) {
      // good
    }
  }

  public void testUpdatingManyToOneWithObjectFromWrongShardFailsOnSave() {
    // test only applies if num dbs > 1
    if(getNumDatabases() == 1) {
      return;
    }
    session.beginTransaction();
    Building b = building("hi");
    floor(b, 23);
    session.save(b);

    Building b2 = building("hi2");
    Floor f2 = floor(b2, 24);
    session.save(b2);
    session.getTransaction().commit();
    assertFalse("Should have been on different shards!", getShardIdForObject(f2).equals(getShardIdForObject(b)));
    resetSession();
    session.beginTransaction();
    b = reloadAssertNotNull(b);
    f2 = reloadAssertNotNull(f2);
    f2.setBuilding(b);
    try {
      session.save(f2);
      session.getTransaction().commit();
      fail("expected Hibernate Exception");
    } catch (HibernateException he) {
      // good, Hibernate detects that we're attempting to associate a collection
      // with multiple sessions
    }
  }

  public void testUpdatingManyToOneWithObjectFromWrongShardFailsOnCommit() {
    // test only applies if num dbs > 1
    if(getNumShards() == 1) {
      return;
    }
    session.beginTransaction();
    Building b = building("hi");
    floor(b, 23);
    session.save(b);

    Building b2 = building("hi2");
    Floor f2 = floor(b2, 24);
    session.save(b2);
    session.getTransaction().commit();
    assertFalse("Should have been on different shards!", getShardIdForObject(f2).equals(getShardIdForObject(b)));
    resetSession();
    session.beginTransaction();
    b = reloadAssertNotNull(b);
    f2 = reloadAssertNotNull(f2);
    f2.setBuilding(b);
    // note that we don't touch the floor itself so that it is still a proxy
    try {
      session.getTransaction().commit();
      fail("expected Transaction Exception");
    } catch (TransactionException te) {
      // good
    }
  }

  public void testLockShard() {
    session.beginTransaction();
    session.lockShard();
    Building b1 = building("b1");
    session.save(b1);
    Building b2 = building("b2");
    session.save(b2);
    session.getTransaction().commit();
    assertOnSameShard(b1, b2);
  }

  public void testPerson() {
    session.beginTransaction();
    Person p = person("max", null);
    Tenant t = tenant("tenant", null, Lists.newArrayList(p));
    session.save(t);
    session.getTransaction().commit();
    resetSession();
    p = reloadAssertNotNull(p);
    assertEquals("max", p.getName());
    t = reloadAssertNotNull(t);
    assertOnSameShard(p, t);
    assertTrue(t.getEmployees().contains(p));
    // calling through to getTenantId() because of some weird cglib magic
    assertEquals(p.getEmployer().getTenantId(), t.getTenantId());
  }

  public void testTenant() {
    session.beginTransaction();
    Tenant t = tenant("whaptech", null, null);
    session.save(t);
    Building b = building("b");
    b.getTenants().add(t);
    session.save(b);
    session.getTransaction().commit();
    resetSession();
    t = reloadAssertNotNull(t);
    assertEquals("whaptech", t.getName());
    b = reloadAssertNotNull(b);
    assertOnSameShard(b, t);
    assertTrue(b.getTenants().contains(t));
    assertTrue(t.getBuildings().contains(b));
  }

  public void testElevator() {
    Building b = building("hi");
    floor(b, 33);
    session.beginTransaction();
    session.save(b);
    session.getTransaction().commit();

    resetSession();
    session.beginTransaction();
    b = reloadAssertNotNull(session, b);
    Floor f = b.getFloors().get(0);
    elevator(b, f);
    session.getTransaction().commit();
    resetSession();

    b = reloadAssertNotNull(session, b);
    assertEquals(1, b.getElevators().size());
    assertEquals(b.getElevators().get(0), b.getFloors().get(0).getElevators().get(0));
    resetSession();
  }

  public void testSavingManyToManyOnDifferentShardsFailsOnSave() {
    if(getNumDatabases() == 1) {
      return;
    }
    session.beginTransaction();
    Tenant t = tenant("t", Collections.<Building>emptyList(), Collections.<Person>emptyList());
    session.save(t);
    Building b = building("b");
    session.save(b);
    Serializable bId = b.getBuildingId();
    session.getTransaction().commit();
    assertFalse("Should have been on different shards!", getShardIdForObject(b).equals(getShardIdForObject(t)));
    resetSession();
    session.beginTransaction();
    b = reloadAssertNotNull(b);
    t = reloadAssertNotNull(t);
    b.getTenants().add(t);
    try {
      session.save(b);
      session.getTransaction().commit();
      fail("expected Hibernate Exception");
    } catch (HibernateException he) {
      // good, Hibernate should recognize this as an attempt to associate
      // a collection with two open sessions
    }
    resetSession();
    b = (Building) session.get(Building.class, bId);
    assertTrue(b.getTenants().isEmpty());
  }

  public void testSavingManyToManyOnDifferentShardsFailsOnCommit() {
    if(getNumDatabases() == 1) {
      return;
    }
    session.beginTransaction();
    Tenant t = tenant("t", Collections.<Building>emptyList(), Collections.<Person>emptyList());
    session.save(t);
    Building b = building("b");
    session.save(b);
    Serializable bId = b.getBuildingId();
    session.getTransaction().commit();
    assertFalse("Should have been on different shards!", getShardIdForObject(t).equals(getShardIdForObject(b)));
    resetSession();
    session.beginTransaction();
    b = reloadAssertNotNull(b);
    t = reloadAssertNotNull(t);
    b.getTenants().add(t);
    try {
      session.getTransaction().commit();
      fail("Expected Transaction Exception");
    } catch (TransactionException te) {
      // good
    }
    resetSession();
    b = (Building) session.get(Building.class, bId);
    assertTrue(b.getTenants().isEmpty());
  }

  public void testParticularShardIds() throws Exception {
    // store a couple of buildings on different shards
    openSession();
    for(int i=0; i<getNumShards(); i++) {
      saveBuilding("building-"+i);
    }
    List<Building> buildings = list(session.createQuery("from Building"));
    assertEquals(getNumShards(), buildings.size());
    resetSession();

    ShardedSessionFactory ssf = (ShardedSessionFactory) session.getSessionFactory();

    // desired shards
    List<ShardId> shardIds = Lists.newArrayList(new ShardId((short) 0));

    List<SessionFactory> allFactories = ssf.getSessionFactories();

    MyShardStrategyFactory strategyFactoryMock = getMyMockStrategyFactory(shardIds);
    ShardedSessionFactory ssfWithParticularShards = ssf.getSessionFactory(shardIds, strategyFactoryMock);

    // need to make sure closing functionality is disabled.
    assertTrue(ssfWithParticularShards instanceof SubsetShardedSessionFactoryImpl);
    assertFalse(ssfWithParticularShards.isClosed());
    ssfWithParticularShards.close();
    assertFalse(ssfWithParticularShards.isClosed());

    assertNotNull(ssfWithParticularShards);
    // should not have kept track of this special factory
    assertEquals(allFactories.size(), ssfWithParticularShards.getSessionFactories().size());

    // did we use the new strategies?
    assertEquals(shardIds, strategyFactoryMock.shardIds);

    // do we only search certain shards?
    // we should only get back the buildings that were on shard 0, i.e. a single building
    Session sessionWithParticularShards = ssfWithParticularShards.openSession();
    List<Building> buildingsFromDesiredShards = list(sessionWithParticularShards.createQuery("from Building"));

    if (!isVirtualShardingEnabled()) {
      assertEquals(1, buildingsFromDesiredShards.size());
    }

    // clean up memory for this new unrecorded session
    MemoryLeakPlugger.plug((ShardedSessionImpl) sessionWithParticularShards);
    sessionWithParticularShards.close();
  }

  private void saveBuilding(String name) {
    session.beginTransaction();
    Building bForShard0 = building(name);
    session.save(bForShard0);
    session.getTransaction().commit();
    resetSession();
  }

  private MyShardStrategyFactory getMyMockStrategyFactory(List<ShardId> shardIds) {
    ShardStrategyFactory shardStrategyFactory = buildShardStrategyFactory();
    MyShardStrategyFactory strategyFactoryMock = new MyShardStrategyFactory();
    strategyFactoryMock.shardStrategyToReturn = shardStrategyFactory.newShardStrategy(shardIds);
    return strategyFactoryMock;
  }

  public void testBadShardIds() throws Exception {
    ShardedSessionFactory ssf = (ShardedSessionFactory) session.getSessionFactory();
    // this shardId doesn't exist
    List<ShardId> shardIds = Lists.newArrayList(new ShardId((short) -1));
    try {
      ssf.getSessionFactory(shardIds, new MyShardStrategyFactory());
      fail("ShardedSessionFactory accepted an invalid shardId");
    } catch (IllegalStateException e) {
      // good
    }
  }

  static class MyShardStrategyFactory extends ShardStrategyFactoryDefaultMock {

    List<ShardId> shardIds;
    ShardStrategy shardStrategyToReturn = null;

    @Override

    public ShardStrategy newShardStrategy(List<ShardId> shardIds) {
      this.shardIds = shardIds;
      return shardStrategyToReturn;
    }
  }


  public void testBuildingWithWindowCascadeOnSave() {
    session.beginTransaction();
    Building b = building("b1");
    Floor f = floor(b, 23);
    Office o = office("88-b", f);
    o.setWindow(window(false));
    session.save(b);
    session.getTransaction().commit();
    resetSession();
    b = reload(b);
    assertNotNull(b.getFloors().get(0).getOffices().get(0).getWindow());
  }

  public void testBuildingWithWindowNoCascadeOnUpdate() {
    session.beginTransaction();
    Building b = building("b1");
    Floor f = floor(b, 23);
    office("88-b", f);
    session.save(b);
    session.getTransaction().commit();
    resetSession();
    session.beginTransaction();
    b = reload(b);
    b.getFloors().get(0).getOffices().get(0).setWindow(window(false));
    session.getTransaction().commit();
    b = reload(b);
    assertNotNull(b.getFloors().get(0).getOffices().get(0).getWindow());
  }

  public void testBuildingWithWindowNoCascadeFailsOnSave() {
    // test only applies if num dbs > 1
    if(getNumDatabases() == 1) {
      return;
    }
    session.beginTransaction();
    Building b = building("b1");
    Floor f = floor(b, 23);
    office("88-b", f);
    session.save(b);
    session.getTransaction().commit();
    resetSession();
    session.beginTransaction();
    b = reload(b);
    Window w = window(false);
    session.save(w);
    Office o = b.getFloors().get(0).getOffices().get(0);
    assertFalse("Should have been on different shards!", getShardIdForObject(o).equals(getShardIdForObject(w)));
    o.setWindow(w);
    try {
      session.save(o);
      session.getTransaction().commit();
      fail("expected Hibernate Exception");
    } catch (HibernateException he) {
      // good
    }
  }

  public void testBuildingWithWindowNoCascadeFailsOnCommit() {
    // test only applies if num dbs > 1
    if(getNumDatabases() == 1) {
      return;
    }
    session.beginTransaction();
    Building b = building("b1");
    Floor f = floor(b, 23);
    office("88-b", f);
    session.save(b);
    session.getTransaction().commit();
    resetSession();
    session.beginTransaction();
    b = reload(b);
    Window w = window(false);
    session.save(w);
    assertFalse("Should have been on different shards!", getShardIdForObject(b).equals(getShardIdForObject(w)));
    b.getFloors().get(0).getOffices().get(0).setWindow(w);
    try {
      session.getTransaction().commit();
      fail("expected Transaction Exception");
    } catch (TransactionException te) {
      // good
    }
  }

  private void assertOnSameShard(Object... objs) {
    for(int i = 0; i < objs.length - 1; i++) {
      assertEquals(getShardIdForObject(objs[i]), getShardIdForObject(objs[i + 1]));
    }
  }

  private void assertBuildingSubObjectsOnSameShard(Building b) {
    List<Object> subObjects = Lists.<Object>newArrayList(b);
    subObjects.addAll(b.getFloors());
    for(Floor f : b.getFloors()) {
      addIfNotNull(subObjects, f.getGoingUp());
      addIfNotNull(subObjects, f.getGoingDown());
      subObjects.addAll(f.getOffices());
    }
    subObjects.addAll(b.getElevators());
    subObjects.addAll(b.getTenants());
    subObjects.addAll(b.getTenants());
    for(Tenant t : b.getTenants()) {
      subObjects.addAll(t.getEmployees());
    }
    assertOnSameShard(subObjects);
  }

  private void addIfNotNull(List<Object> subObjects, Object obj) {
    if(obj != null) {
      subObjects.add(obj);
    }
  }

  public void testUpdateOfAttachedEntity() {
    session.beginTransaction();
    Building b = building("b1");
    session.save(b);
    commitAndResetSession();
    b = reload(b);
    b.setName("other name");
    session.update(b);
    commitAndResetSession();
    b = reload(b);
    assertEquals("other name", b.getName());
  }

  // calling update on a detached entity should actually result in a merge
  public void testUpdateOfDetachedEntity() {
    session.beginTransaction();
    Building b = building("b1");
    session.save(b);
    commitAndResetSession();
    Building transientB = building("a different name");
    transientB.setBuildingId(b.getBuildingId());
    session.update(transientB);
    commitAndResetSession();
    b = (Building) session.get(Building.class, transientB.getBuildingId());
    assertNotNull(b);
    assertEquals("a different name", b.getName());
  }

  public void testLoad() {
    session.beginTransaction();
    Building b = building("b1");
    session.save(b);
    commitAndResetSession();
    Building loadedB = (Building) session.load(b.getClass(), b.getBuildingId());
    assertNotNull(loadedB);
    assertEquals("b1", loadedB.getName());
  }

  public void testLoadIntoObject() {
    session.beginTransaction();
    Building b = building("b1");
    session.save(b);
    commitAndResetSession();
    Building loadedB = new Building();
    session.load(loadedB, b.getBuildingId());
    assertNotNull(loadedB);
    assertEquals("b1", loadedB.getName());
  }

  public void testLoadNonexisting() {
    session.beginTransaction();
    Building b = building("b1");
    session.save(b);
    commitAndResetSession();
    b = reload(b);
    Serializable id = b.getBuildingId();
    session.delete(b);
    commitAndResetSession();
    try {
      Building loadedB = (Building)session.load(Building.class, id);
      loadedB.getName();
      fail();
    } catch (HibernateException he) {
      // good
    }
  }

  public void testMergeUnpersisted() {
    session.beginTransaction();
    Building b = building("b1");
    Building returnedB = (Building)session.merge(b);
    commitAndResetSession();
    Building mergedB = (Building)session.get(Building.class,
        returnedB.getBuildingId());
    assertNotNull(mergedB);
    assertEquals("b1", mergedB.getName());
  }

  public void testMergePersisted() {
    session.beginTransaction();
    Building b = building("b1");
    session.save(b);
    commitAndResetSession();
    session.evict(b);

    session.beginTransaction();
    b.setName("b2");
    session.merge(b);
    assertFalse(session.contains(b));
    commitAndResetSession();

    Building mergedB = (Building)session.get(Building.class, b.getBuildingId());
    assertNotNull(mergedB);
    assertEquals("b2", mergedB.getName());
  }

  public void testReplicate() {
    session.beginTransaction();
    Building idB = building("just need to get id");
    session.save(idB);
    commitAndResetSession();
    Serializable id = idB.getBuildingId();
    session.beginTransaction();
    session.delete(idB);
    commitAndResetSession();

    Building b = building("b1");
    b.setBuildingId(id);
    session.beginTransaction();
    session.replicate(b, ReplicationMode.IGNORE);
    commitAndResetSession();
    Building replicatedB = (Building)session.get(Building.class, id);
    assertNotNull(replicatedB);
    assertEquals("b1", replicatedB.getName());
  }

  public void testReplicatePersistedAndOverwrite() {
    session.beginTransaction();
    Building b1 = building("b1");
    session.save(b1);
    commitAndResetSession();
    session.evict(b1);
    Building b2 = building("b2");
    b2.setBuildingId(b1.getBuildingId());
    session.beginTransaction();
    session.replicate(b2, ReplicationMode.OVERWRITE);
    commitAndResetSession();
    Building replicatedB2 = (Building)session.get(Building.class, b1.getBuildingId());
    assertNotNull(replicatedB2);
    assertEquals("b2", replicatedB2.getName());
  }

  public void testPersist() {
    session.beginTransaction();
    Building b = building("b1");
    session.persist(b);
    commitAndResetSession();
    Building returnedB = (Building) session.get(b.getClass(), b.getBuildingId());
    assertNotNull(returnedB);
    assertEquals("b1", returnedB.getName());
  }

  public void testRefresh() {
    session.beginTransaction();
    Building b = building("b1");
    session.save(b);
    commitAndResetSession();
    Building reloadedB = reload(b);
    reloadedB.setName("b2");
    commitAndResetSession();
    assertEquals("b1", b.getName());
    session.refresh(b);
    assertEquals("b2", b.getName());
  }

  // calling update on a nonexistent entity should result in an exception
  public void testUpdateOfNonexistentEntity() {
    session.beginTransaction();
    Building b = building("b1");
    try {
      session.update(b);
      fail("expected HE");
    } catch  (HibernateException he) {
      // good
    }
    resetSession();
    session.beginTransaction();
    // now we assign an id and try the same thing (calling save will assign)
    session.save(b);
    session.getTransaction().rollback();
    resetSession();
    session.beginTransaction();
    session.update(b);
    try {
      session.getTransaction().commit();
      fail("expected he");
    } catch (HibernateException he) {
      // good
    }
  }

  public void testDeleteOfAttachedEntity() {
    session.beginTransaction();
    Building b = building("b1");
    session.save(b);
    commitAndResetSession();
    b = reload(b);
    session.delete(b);
    commitAndResetSession();
    b = reload(b);
    assertNull(b);
  }

  public void testDeleteOfDetachedEntity() {
    session.beginTransaction();
    Building b = building("b1");
    session.save(b);
    commitAndResetSession();
    Building detached = new Building();
    detached.setBuildingId(b.getBuildingId());
    detached.setName("harold");
    session.delete(detached);
    commitAndResetSession();
    b = reload(b);
    assertNull(b);
  }

  public void testGetEntityName() {
    Building b = building("b1");
    try {
      session.getEntityName(b);
      fail("expected he");
    } catch (HibernateException he) {
      // good
    }
    session.save(b);
    assertEquals(Building.class.getName(), session.getEntityName(b));
  }

  public void testGetCurrentLockMode() {
    session.beginTransaction();
    Building b = building("b1");
    try {
      session.getCurrentLockMode(b);
      fail("expected he");
    } catch (HibernateException he) {
      // good
    }
    session.save(b);
    assertEquals(LockMode.WRITE, session.getCurrentLockMode(b));
  }

  public void testLock() {
    session.beginTransaction();
    Building b = building("b1");
    try {
      session.lock(b, LockMode.READ);
      fail("expected he");
    } catch (HibernateException he) {
      // good
    }
    session.save(b);
    commitAndResetSession();
    b = reload(b);
    session.lock(b, LockMode.UPGRADE);
    assertEquals(LockMode.UPGRADE, session.getCurrentLockMode(b));
  }

  // this is a really good way to shake out synchronization bugs
  public void xtestOverAndOver() throws Exception {
    final boolean[] go = {true};
    Runnable r = new Runnable() {
      public void run() {
        while(go[0]) {
          try {
            Thread.sleep(5000);
          } catch (InterruptedException e) {
            // fine
          }
        }
      }
    };
    new Thread(r).start();
    tearDown();
    for(int i = 0; i < 100; i++) {
      setUp();
      try {
        testBuildingLifecycle();
      } finally {
        tearDown();
      }
    }
    go[0] = false;
  }
}


