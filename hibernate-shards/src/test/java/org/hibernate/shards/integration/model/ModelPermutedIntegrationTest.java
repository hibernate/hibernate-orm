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
import org.hibernate.LockOptions;
import org.hibernate.ReplicationMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.TransactionException;
import org.hibernate.criterion.Projections;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.shards.ShardId;
import org.hibernate.shards.integration.BaseShardingIntegrationTestCase;
import org.hibernate.shards.integration.MemoryLeakPlugger;
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
import org.junit.Assert;
import org.junit.Test;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hibernate.shards.integration.model.ModelDataFactory.building;
import static org.hibernate.shards.integration.model.ModelDataFactory.elevator;
import static org.hibernate.shards.integration.model.ModelDataFactory.escalator;
import static org.hibernate.shards.integration.model.ModelDataFactory.floor;
import static org.hibernate.shards.integration.model.ModelDataFactory.office;
import static org.hibernate.shards.integration.model.ModelDataFactory.person;
import static org.hibernate.shards.integration.model.ModelDataFactory.tenant;
import static org.hibernate.shards.integration.model.ModelDataFactory.window;

/**
 * @author maxr@google.com (Max Ross)
 */
public class ModelPermutedIntegrationTest extends BaseShardingIntegrationTestCase {

    @Test
    public void testMapping() {
        // if we succeed, our model is ok
    }

    @Test
    public void testBuildingLifecycle() {
        session.beginTransaction();
        Set<Building> buildings = Sets.newHashSet();
        // we're using the round robin shard selector so we should
        // end up with 2 buildings per shard
        for (int i = 0; i < getNumShards() * 2; i++) {
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
        List<Integer> counts = list(session.createCriteria(Building.class).setProjection(Projections.rowCount()));
        int total = 0;
        for (Integer count : counts) {
            total += count;
        }
        Assert.assertEquals(getNumShards() * 2, total);
        session.beginTransaction();
        Map<ShardId, List<Serializable>> shards = Maps.newHashMap();
        for (Building b : buildings) {
            Building bReloaded = reloadAssertNotNull(b);
            Assert.assertEquals(b.getName(), bReloaded.getName());
            Assert.assertEquals(b.getFloors().size(), bReloaded.getFloors().size());
            for (int i = 0; i < b.getFloors().size(); i++) {
                Floor bFloor = b.getFloors().get(i);
                Floor bReloadedFloor = bReloaded.getFloors().get(i);
                Assert.assertEquals(bFloor.getNumber(), bReloadedFloor.getNumber());
                Assert.assertEquals(2, bReloadedFloor.getElevators().size());
            }
            Assert.assertNotNull(bReloaded.getFloors().get(0).getGoingUp());
            Assert.assertNull(bReloaded.getFloors().get(0).getGoingDown());
            Assert.assertNull(bReloaded.getFloors().get(1).getGoingUp());
            Assert.assertNotNull(bReloaded.getFloors().get(1).getGoingDown());

            Assert.assertEquals(b.getElevators().size(), bReloaded.getElevators().size());
            ShardId shardIdForObject = getShardIdForObject(bReloaded);
            Assert.assertNotNull(shardIdForObject);
            assertBuildingSubObjectsOnSameShard(bReloaded);
            List<Serializable> idList = shards.get(shardIdForObject);
            if (idList == null) {
                idList = Lists.newArrayList();
                shards.put(shardIdForObject, idList);
            }
            idList.add(bReloaded.getBuildingId());
            bReloaded.setName(bReloaded.getName() + " updated");
            for (Floor newFloor : bReloaded.getFloors()) {
                newFloor.setNumber(newFloor.getNumber() + 33);
            }
        }
        Assert.assertEquals(getNumShards(), shards.size());
        for (List<Serializable> idList : shards.values()) {
            Assert.assertEquals(2, idList.size());
        }
        commitAndResetSession();
        session.beginTransaction();
        for (Building b : buildings) {
            Building bReloaded = reloadAssertNotNull(b);
            Assert.assertNotNull(bReloaded);
            Assert.assertEquals(b.getName() + " updated", bReloaded.getName());
            for (int i = 0; i < b.getFloors().size(); i++) {
                Floor bFloor = b.getFloors().get(i);
                Floor bReloadedFloor = bReloaded.getFloors().get(i);
                Assert.assertEquals(33 + bFloor.getNumber(), bReloadedFloor.getNumber());
            }
            ShardId shardIdForObject = getShardIdForObject(bReloaded);
            // make sure the object resides on the same shard as before
            Assert.assertTrue(shards.get(shardIdForObject).contains(b.getBuildingId()));
            assertBuildingSubObjectsOnSameShard(bReloaded);
            // now let's get rid of the buildings
            session.delete(bReloaded);
        }
        commitAndResetSession();
        for (Building b : buildings) {
            Assert.assertNull(reload(b));
            for (Floor f : b.getFloors()) {
                Assert.assertNull(reload(f));
            }
        }
    }

    @Test
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
        Assert.assertEquals("b2", b.getName());
    }

    @Test
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
        Assert.assertEquals("b2", b.getName());
    }

    @Test
    public void testSavingOneToManyChildViaCascade() {
        session.beginTransaction();
        Building b = building("awesome building");
        Floor f = floor(b, 23);
        session.save(b);
        session.getTransaction().commit();
        assertBuildingSubObjectsOnSameShard(b);
        assertOnSameShard(b, f);
        resetSession();
        Assert.assertNotNull(reload(b));
        Assert.assertNotNull(reload(f));
    }

    /**
     * In this test we demonstrate our ability to create a parent and a child by
     * calling save on them individually (no cascading involved).  The key here
     * is that both objects need to end up in the same shard.
     */
    @Test
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
    @Test
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
    @Test
    public void testSavingOneToOneChildWithoutAssociationFails() {
        session.beginTransaction();
        Escalator esc = escalator(null, null);
        try {
            session.save(esc);
            Assert.fail("expected he");
        } catch (HibernateException he) {
            // good;
        }
    }

    /**
     * In this test we demonstrate that if you try to save an entity that doesn't
     * support top-level saves after associating some other object with that
     * entity, you'll be cool.
     */
    @Test
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

    @Test
    public void testUpdatingOneToOneWithObjectFromWrongShardFailsOnSave() {
        // test only applies if num dbs > 1
        if (getNumShards() == 1) {
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
        Assert.assertFalse("Should have been on different shards!", getShardIdForObject(f2).equals(getShardIdForObject(f.getGoingUp())));
        resetSession();
        session.beginTransaction();
        f = reloadAssertNotNull(f);
        f2 = reloadAssertNotNull(f2);
        f2.setGoingUp(f.getGoingUp());
        try {
            session.save(f2);
            session.getTransaction().commit();
            Assert.fail("expected Hibernate Exception");
        } catch (HibernateException he) {
            // good
        }
        session.getTransaction().rollback();
    }

    @Test
    public void testUpdatingOneToOneWithObjectFromWrongShardFailsOnCommitWithUnfetchedTarget() {
        // test only applies if num dbs > 1
        if (getNumShards() == 1) {
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
        Assert.assertFalse("Should have been on different shards!", getShardIdForObject(f2).equals(getShardIdForObject(f.getGoingUp())));
        resetSession();
        session.beginTransaction();
        f = reloadAssertNotNull(f);
        f2 = reloadAssertNotNull(f2);
        f2.setGoingUp(f.getGoingUp());
        Assert.assertTrue(f.getGoingUp() instanceof HibernateProxy);
        Assert.assertTrue(((HibernateProxy) f.getGoingUp()).getHibernateLazyInitializer().isUninitialized());
        // note that we don't touch the escalator itself so that it is still a proxy
        try {
            session.getTransaction().commit();
            Assert.fail("expected Transaction Exception");
        } catch (TransactionException te) {
            // good
        }
    }

    @Test
    public void testUpdatingOneToOneWithObjectFromWrongShardFailsOnCommitWithFetchedTarget() {
        // test only applies if num dbs > 1
        if (getNumShards() == 1) {
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
        Assert.assertFalse("Should have been on different shards!", getShardIdForObject(f2).equals(getShardIdForObject(f.getGoingUp())));
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
            Assert.fail("expected Transaction Exception");
        } catch (TransactionException tc) {
            // good
        }
    }

    @Test
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
        Assert.assertEquals(2, b.getFloors().size());
        Assert.assertNotNull(b.getFloors().get(0).getGoingUp());
        Assert.assertNotNull(b.getFloors().get(1).getGoingDown());
        assertBuildingSubObjectsOnSameShard(b);
        assertOnSameShard(b, f1, f2, e);
    }

    @Test
    public void testUpdatingOneToManyWithObjectFromWrongShardFailsOnSave() {
        // test only applies if num dbs > 1
        if (getNumShards() == 1) {
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
        Assert.assertFalse("Should have been on different shards!", getShardIdForObject(b).equals(getShardIdForObject(f2)));
        resetSession();
        session.beginTransaction();
        b = reloadAssertNotNull(b);
        f2 = reloadAssertNotNull(f2);
        b.getFloors().add(f2);
        try {
            session.save(b);
            session.getTransaction().commit();
            Assert.fail("expected Hibernate Exception");
        } catch (HibernateException he) {
            // good
        }
    }

    @Test
    public void testUpdatingOneToManyWithObjectFromWrongShardFailsOnCommit() {
        // test only applies if num dbs > 1
        if (getNumShards() == 1) {
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
        Assert.assertFalse("Should have been on different shards!", getShardIdForObject(f2).equals(getShardIdForObject(b)));
        resetSession();
        session.beginTransaction();
        b = reloadAssertNotNull(b);
        f2 = reloadAssertNotNull(f2);
        b.getFloors().add(f2);
        // note that we don't touch the floor itself so that it is still a proxy
        try {
            session.getTransaction().commit();
            Assert.fail("expected Transaction Exception");
        } catch (TransactionException te) {
            // good
        }
    }

    @Test
    public void testUpdatingManyToOneWithObjectFromWrongShardFailsOnSave() {
        // test only applies if num dbs > 1
        if (getNumDatabases() == 1) {
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
        Assert.assertFalse("Should have been on different shards!", getShardIdForObject(f2).equals(getShardIdForObject(b)));
        resetSession();
        session.beginTransaction();
        b = reloadAssertNotNull(b);
        f2 = reloadAssertNotNull(f2);
        f2.setBuilding(b);
        try {
            session.save(f2);
            session.getTransaction().commit();
            Assert.fail("expected Hibernate Exception");
        } catch (HibernateException he) {
            // good, Hibernate detects that we're attempting to associate a collection
            // with multiple sessions
        }
    }

    @Test
    public void testUpdatingManyToOneWithObjectFromWrongShardFailsOnCommit() {
        // test only applies if num dbs > 1
        if (getNumShards() == 1) {
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
        Assert.assertFalse("Should have been on different shards!", getShardIdForObject(f2).equals(getShardIdForObject(b)));
        resetSession();
        session.beginTransaction();
        b = reloadAssertNotNull(b);
        f2 = reloadAssertNotNull(f2);
        f2.setBuilding(b);
        // note that we don't touch the floor itself so that it is still a proxy
        try {
            session.getTransaction().commit();
            Assert.fail("expected Transaction Exception");
        } catch (TransactionException te) {
            // good
        }
    }

    @Test
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

    @Test
    public void testPerson() {
        session.beginTransaction();
        Person p = person("max", null);
        Tenant t = tenant("tenant", null, Lists.newArrayList(p));
        session.save(t);
        session.getTransaction().commit();
        resetSession();
        p = reloadAssertNotNull(p);
        Assert.assertEquals("max", p.getName());
        t = reloadAssertNotNull(t);
        assertOnSameShard(p, t);
        Assert.assertTrue(t.getEmployees().contains(p));
        // calling through to getTenantId() because of some weird cglib magic
        Assert.assertEquals(p.getEmployer().getTenantId(), t.getTenantId());
    }

    @Test
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
        Assert.assertEquals("whaptech", t.getName());
        b = reloadAssertNotNull(b);
        assertOnSameShard(b, t);
        Assert.assertTrue(b.getTenants().contains(t));
        Assert.assertTrue(t.getBuildings().contains(b));
    }

    @Test
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
        Assert.assertEquals(1, b.getElevators().size());
        Assert.assertEquals(b.getElevators().get(0), b.getFloors().get(0).getElevators().get(0));
        resetSession();
    }

    @Test
    public void testSavingManyToManyOnDifferentShardsFailsOnSave() {
        if (getNumDatabases() == 1) {
            return;
        }
        session.beginTransaction();
        Tenant t = tenant("t", Collections.<Building>emptyList(), Collections.<Person>emptyList());
        session.save(t);
        Building b = building("b");
        session.save(b);
        Serializable bId = b.getBuildingId();
        session.getTransaction().commit();
        Assert.assertFalse("Should have been on different shards!", getShardIdForObject(b).equals(getShardIdForObject(t)));
        resetSession();
        session.beginTransaction();
        b = reloadAssertNotNull(b);
        t = reloadAssertNotNull(t);
        b.getTenants().add(t);
        try {
            session.save(b);
            session.getTransaction().commit();
            Assert.fail("expected Hibernate Exception");
        } catch (HibernateException he) {
            // good, Hibernate should recognize this as an attempt to associate
            // a collection with two open sessions
        }
        resetSession();
        b = (Building) session.get(Building.class, bId);
        Assert.assertTrue(b.getTenants().isEmpty());
    }

    @Test
    public void testSavingManyToManyOnDifferentShardsFailsOnCommit() {
        if (getNumDatabases() == 1) {
            return;
        }
        session.beginTransaction();
        Tenant t = tenant("t", Collections.<Building>emptyList(), Collections.<Person>emptyList());
        session.save(t);
        Building b = building("b");
        session.save(b);
        Serializable bId = b.getBuildingId();
        session.getTransaction().commit();
        Assert.assertFalse("Should have been on different shards!", getShardIdForObject(t).equals(getShardIdForObject(b)));
        resetSession();
        session.beginTransaction();
        b = reloadAssertNotNull(b);
        t = reloadAssertNotNull(t);
        b.getTenants().add(t);
        try {
            session.getTransaction().commit();
            Assert.fail("Expected Transaction Exception");
        } catch (TransactionException te) {
            // good
        }
        resetSession();
        b = (Building) session.get(Building.class, bId);
        Assert.assertTrue(b.getTenants().isEmpty());
    }

    @Test
    public void testParticularShardIds() throws Exception {
        // store a couple of buildings on different shards
        openSession();
        for (int i = 0; i < getNumShards(); i++) {
            saveBuilding("building-" + i);
        }
        List<Building> buildings = list(session.createQuery("from Building"));
        Assert.assertEquals(getNumShards(), buildings.size());
        resetSession();

        ShardedSessionFactory ssf = (ShardedSessionFactory) session.getSessionFactory();

        // desired shards
        List<ShardId> shardIds = Lists.newArrayList(new ShardId((short) 0));

        List<SessionFactory> allFactories = ssf.getSessionFactories();

        MyShardStrategyFactory strategyFactoryMock = getMyMockStrategyFactory(shardIds);
        ShardedSessionFactory ssfWithParticularShards = ssf.getSessionFactory(shardIds, strategyFactoryMock);

        // need to make sure closing functionality is disabled.
        Assert.assertTrue(ssfWithParticularShards instanceof SubsetShardedSessionFactoryImpl);
        Assert.assertFalse(ssfWithParticularShards.isClosed());
        ssfWithParticularShards.close();
        Assert.assertFalse(ssfWithParticularShards.isClosed());

        Assert.assertNotNull(ssfWithParticularShards);
        // should not have kept track of this special factory
        Assert.assertEquals(allFactories.size(), ssfWithParticularShards.getSessionFactories().size());

        // did we use the new strategies?
        Assert.assertEquals(shardIds, strategyFactoryMock.shardIds);

        // do we only search certain shards?
        // we should only get back the buildings that were on shard 0, i.e. a single building
        Session sessionWithParticularShards = ssfWithParticularShards.openSession();
        List<Building> buildingsFromDesiredShards = list(sessionWithParticularShards.createQuery("from Building"));

        if (!isVirtualShardingEnabled()) {
            Assert.assertEquals(1, buildingsFromDesiredShards.size());
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

    @Test
    public void testBadShardIds() throws Exception {
        ShardedSessionFactory ssf = (ShardedSessionFactory) session.getSessionFactory();
        // this shardId doesn't exist
        List<ShardId> shardIds = Lists.newArrayList(new ShardId((short) -1));
        try {
            ssf.getSessionFactory(shardIds, new MyShardStrategyFactory());
            Assert.fail("ShardedSessionFactory accepted an invalid shardId");
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

    @Test
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
        Assert.assertNotNull(b.getFloors().get(0).getOffices().get(0).getWindow());
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
        Assert.assertNotNull(b.getFloors().get(0).getOffices().get(0).getWindow());
    }

    @Test
    public void testBuildingWithWindowNoCascadeFailsOnSave() {
        // test only applies if num dbs > 1
        if (getNumDatabases() == 1) {
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
        Assert.assertFalse("Should have been on different shards!", getShardIdForObject(o).equals(getShardIdForObject(w)));
        o.setWindow(w);
        try {
            session.save(o);
            session.getTransaction().commit();
            Assert.fail("expected Hibernate Exception");
        } catch (HibernateException he) {
            // good
        }
    }

    @Test
    public void testBuildingWithWindowNoCascadeFailsOnCommit() {
        // test only applies if num dbs > 1
        if (getNumDatabases() == 1) {
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
        Assert.assertFalse("Should have been on different shards!", getShardIdForObject(b).equals(getShardIdForObject(w)));
        b.getFloors().get(0).getOffices().get(0).setWindow(w);
        try {
            session.getTransaction().commit();
            Assert.fail("expected Transaction Exception");
        } catch (TransactionException te) {
            // good
        }
    }

    private void assertOnSameShard(Object... objs) {
        for (int i = 0; i < objs.length - 1; i++) {
            Assert.assertEquals(getShardIdForObject(objs[i]), getShardIdForObject(objs[i + 1]));
        }
    }

    private void assertBuildingSubObjectsOnSameShard(Building b) {
        List<Object> subObjects = Lists.<Object>newArrayList(b);
        subObjects.addAll(b.getFloors());
        for (Floor f : b.getFloors()) {
            addIfNotNull(subObjects, f.getGoingUp());
            addIfNotNull(subObjects, f.getGoingDown());
            subObjects.addAll(f.getOffices());
        }
        subObjects.addAll(b.getElevators());
        subObjects.addAll(b.getTenants());
        subObjects.addAll(b.getTenants());
        for (Tenant t : b.getTenants()) {
            subObjects.addAll(t.getEmployees());
        }
        assertOnSameShard(subObjects);
    }

    private void addIfNotNull(List<Object> subObjects, Object obj) {
        if (obj != null) {
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
        Assert.assertEquals("other name", b.getName());
    }

    // calling update on a detached entity should actually result in a merge
    @Test
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
        Assert.assertNotNull(b);
        Assert.assertEquals("a different name", b.getName());
    }

    @Test
    public void testLoad() {
        session.beginTransaction();
        Building b = building("b1");
        session.save(b);
        commitAndResetSession();
        Building loadedB = (Building) session.load(b.getClass(), b.getBuildingId());
        Assert.assertNotNull(loadedB);
        Assert.assertEquals("b1", loadedB.getName());
    }

    @Test
    public void testLoadIntoObject() {
        session.beginTransaction();
        Building b = building("b1");
        session.save(b);
        commitAndResetSession();
        Building loadedB = new Building();
        session.load(loadedB, b.getBuildingId());
        Assert.assertNotNull(loadedB);
        Assert.assertEquals("b1", loadedB.getName());
    }

    @Test
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
            Building loadedB = (Building) session.load(Building.class, id);
            loadedB.getName();
            Assert.fail();
        } catch (HibernateException he) {
            // good
        }
    }

    @Test
    public void testMergeUnpersisted() {
        session.beginTransaction();
        Building b = building("b1");
        Building returnedB = (Building) session.merge(b);
        commitAndResetSession();
        Building mergedB = (Building) session.get(Building.class, returnedB.getBuildingId());
        Assert.assertNotNull(mergedB);
        Assert.assertEquals("b1", mergedB.getName());
    }

    @Test
    public void testMergePersisted() {
        session.beginTransaction();
        Building b = building("b1");
        session.save(b);
        commitAndResetSession();
        session.evict(b);

        session.beginTransaction();
        b.setName("b2");
        session.merge(b);
        Assert.assertFalse(session.contains(b));
        commitAndResetSession();

        Building mergedB = (Building) session.get(Building.class, b.getBuildingId());
        Assert.assertNotNull(mergedB);
        Assert.assertEquals("b2", mergedB.getName());
    }

    @Test
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
        Building replicatedB = (Building) session.get(Building.class, id);
        Assert.assertNotNull(replicatedB);
        Assert.assertEquals("b1", replicatedB.getName());
    }

    @Test
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
        Building replicatedB2 = (Building) session.get(Building.class, b1.getBuildingId());
        Assert.assertNotNull(replicatedB2);
        Assert.assertEquals("b2", replicatedB2.getName());
    }

    @Test
    public void testPersist() {
        session.beginTransaction();
        Building b = building("b1");
        session.persist(b);
        commitAndResetSession();
        Building returnedB = (Building) session.get(b.getClass(), b.getBuildingId());
        Assert.assertNotNull(returnedB);
        Assert.assertEquals("b1", returnedB.getName());
    }

    @Test
    public void testRefresh() {
        session.beginTransaction();
        Building b = building("b1");
        session.save(b);
        commitAndResetSession();
        Building reloadedB = reload(b);
        reloadedB.setName("b2");
        commitAndResetSession();
        Assert.assertEquals("b1", b.getName());
        session.refresh(b);
        Assert.assertEquals("b2", b.getName());
    }

    // calling update on a nonexistent entity should result in an exception
    @Test
    public void testUpdateOfNonexistentEntity() {
        session.beginTransaction();
        Building b = building("b1");
        try {
            session.update(b);
            Assert.fail("expected HE");
        } catch (HibernateException he) {
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
            Assert.fail("expected he");
        } catch (HibernateException he) {
            // good
        }
    }

    @Test
    public void testDeleteOfAttachedEntity() {
        session.beginTransaction();
        Building b = building("b1");
        session.save(b);
        commitAndResetSession();
        b = reload(b);
        session.delete(b);
        commitAndResetSession();
        b = reload(b);
        Assert.assertNull(b);
    }

    @Test
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
        Assert.assertNull(b);
    }

    @Test
    public void testGetEntityName() {
        Building b = building("b1");
        try {
            session.getEntityName(b);
            Assert.fail("expected he");
        } catch (HibernateException he) {
            // good
        }
        session.save(b);
        Assert.assertEquals(Building.class.getName(), session.getEntityName(b));
    }

    @Test
    public void testGetCurrentLockMode() {
        session.beginTransaction();
        Building b = building("b1");
        try {
            session.getCurrentLockMode(b);
            Assert.fail("expected he");
        } catch (HibernateException he) {
            // good
        }
        session.save(b);
        Assert.assertEquals(LockMode.WRITE, session.getCurrentLockMode(b));
    }

    @Test
    public void testLock() {
        session.beginTransaction();
        Building b = building("b1");
        try {
            session.buildLockRequest(LockOptions.READ).lock(b);
            Assert.fail("expected he");
        } catch (HibernateException he) {
            // good
        }
        session.save(b);
        commitAndResetSession();
        b = reload(b);
        session.buildLockRequest(LockOptions.UPGRADE).lock(b);
        Assert.assertEquals(LockOptions.UPGRADE, session.getCurrentLockMode(b));
    }

    // this is a really good way to shake out synchronization bugs
    @Test
    public void xtestOverAndOver() throws Exception {
        final boolean[] go = {true};
        Runnable r = new Runnable() {
            public void run() {
                while (go[0]) {
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
        for (int i = 0; i < 100; i++) {
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
