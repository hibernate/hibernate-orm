/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cascade.circle;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * The test case uses the following model:
 *
 *                          <-    ->
 *                      -- (N : 0,1) -- Tour
 *                      |    <-   ->
 *                      | -- (1 : N) -- (pickup) ----
 *          <-   ->     | |                          |
 * Route -- (1 : N) -- Node                      Transport
 *                      |  <-   ->                |
 *                      -- (1 : N) -- (delivery) --
 *
 *  Arrows indicate the direction of cascade-merge, cascade-save, cascade-refresh and cascade-save-or-update
 *
 * It reproduces the following issues:
 * https://hibernate.atlassian.net/browse/HHH-9512
 * <p/>
 * This tests that cascades are done properly from each entity.
 *
 * @author Alex Belyaev (based on code by Pavol Zibrita and Gail Badner)
 */
public class CascadeManagedAndTransientTest extends BaseCoreFunctionalTestCase  {

    @Override
    public String[] getMappings() {
        return new String[] {
                "cascade/circle/CascadeManagedAndTransient.hbm.xml"
        };
    }

    @Override
    public void configure(Configuration cfg) {
        super.configure( cfg );
        cfg.setProperty( Environment.CHECK_NULLABILITY, "true" );
    }

    @Override
    protected void cleanupTest() {
        Session s = openSession();
        s.beginTransaction();
        s.createQuery( "delete from Transport" );
        s.createQuery( "delete from Tour" );
        s.createQuery( "delete from Node" );
        s.createQuery( "delete from Route" );
        s.createQuery( "delete from Vehicle" );
    }

    private void checkNewVehicle(Vehicle newVehicle) {
        assertEquals("Bus", newVehicle.getName());
        assertEquals(1, newVehicle.getTransports().size());
        Transport t = (Transport) newVehicle.getTransports().iterator().next();
        assertEquals("Transport 2 -> 3", t.getName());
        assertEquals("Node 2", t.getPickupNode().getName());
        assertEquals("Node 3", t.getDeliveryNode().getName());
    }

    @Test
    public void testAttachedChildInMerge() {
        fillInitialData();

        Session s = openSession();
        s.beginTransaction();

        Route route = (Route) s.createQuery("FROM Route WHERE name = :name").setString("name", "Route 1").uniqueResult();
        Node n2 = (Node) s.createQuery("FROM Node WHERE name = :name").setString("name", "Node 2").uniqueResult();
        Node n3 = (Node) s.createQuery("FROM Node WHERE name = :name").setString("name", "Node 3").uniqueResult();

        Vehicle vehicle = new Vehicle();
        vehicle.setName("Bus");
        vehicle.setRoute(route);

        Transport $2to3 = new Transport();
        $2to3.setName("Transport 2 -> 3");
        $2to3.setPickupNode(n2); n2.getPickupTransports().add($2to3);
        $2to3.setDeliveryNode(n3); n3.getDeliveryTransports().add($2to3);
        $2to3.setVehicle(vehicle);

        vehicle.setTransports(new HashSet<Transport>(Arrays.asList($2to3)));

        // Try to save graph of transient entities (vehicle, transport) which contains attached entities (node2, node3)
        Vehicle managedVehicle = (Vehicle) s.merge(vehicle);
        checkNewVehicle(managedVehicle);

        s.flush();
        s.clear();

        assertEquals(3, s.createQuery("FROM Transport").list().size());
        assertEquals(2, s.createQuery("FROM Vehicle").list().size());
        assertEquals(4, s.createQuery("FROM Node").list().size());

        Vehicle newVehicle = (Vehicle) s.createQuery("FROM Vehicle WHERE name = :name").setParameter("name", "Bus").uniqueResult();
        checkNewVehicle(newVehicle);

        s.getTransaction().commit();
        s.close();
    }

    private void fillInitialData() {
        Tour tour = new Tour();
        tour.setName("Tour 1");

        Route route = new Route();
        route.setName("Route 1");

        ArrayList<Node> nodes = new ArrayList<Node>();
        for (int i = 0; i < 4; i++) {
            Node n = new Node();
            n.setName("Node " + i);
            n.setTour(tour);
            n.setRoute(route);
            nodes.add(n);
        }

        tour.setNodes(new HashSet<Node>(nodes));
        route.setNodes(new HashSet<Node>(Arrays.asList(nodes.get(0), nodes.get(1), nodes.get(2))));

        Vehicle vehicle = new Vehicle();
        vehicle.setName("Car");
        route.setVehicles(new HashSet<Vehicle>(Arrays.asList(vehicle)));
        vehicle.setRoute(route);

        Transport $0to1 = new Transport();
        $0to1.setName("Transport 0 -> 1");
        $0to1.setPickupNode(nodes.get(0));
        $0to1.setDeliveryNode(nodes.get(1));
        $0to1.setVehicle(vehicle);

        Transport $1to2 = new Transport();
        $1to2.setName("Transport 1 -> 2");
        $1to2.setPickupNode(nodes.get(1));
        $1to2.setDeliveryNode(nodes.get(2));
        $1to2.setVehicle(vehicle);

        vehicle.setTransports(new HashSet<Transport>(Arrays.asList($0to1, $1to2)));

        Session s = openSession();
        s.beginTransaction();

        s.persist(tour);

        s.getTransaction().commit();
        s.close();
    }
}
