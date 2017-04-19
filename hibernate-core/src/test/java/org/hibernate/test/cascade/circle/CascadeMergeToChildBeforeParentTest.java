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

/**
 * The test case uses the following model:
 *
 *                         <-    ->
 *                      -- (N : 0,1) -- Tour
 *                      |    <-   ->
 *                      | -- (1 : N) -- (pickup) ----
 *               ->     | |                         |
 * Route -- (1 : N) - Node                      Transport
 *   |                    |  <-   ->                |  |
 *   |                    -- (1 : N) -- (delivery) --  |
 *   |                                                 |
 *   |             ->                    ->            |
 *   -------- (1 : N) ---- Vehicle--(1 : N)------------
 *
 * Arrows indicate the direction of cascade-merge.
 * <p/>
 * I believe it reproduces the following issue:
 * http://opensource.atlassian.com/projects/hibernate/browse/HHH-3544
 *
 * @author Gail Badner (based on original model provided by Pavol Zibrita)
 */
public class CascadeMergeToChildBeforeParentTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] {
				"cascade/circle/CascadeMergeToChildBeforeParent.hbm.xml"
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

	@Test
	public void testMerge() {
		Session s = openSession();
		s.beginTransaction();

		Route route = new Route();
		route.setName( "routeA" );

		s.save( route );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();

		route = (Route) s.get( Route.class, new Long( 1 ) );

		route.setTransientField( new String( "sfnaouisrbn" ) );

		Tour tour = new Tour();
		tour.setName( "tourB" );

		Node pickupNode = new Node();
		pickupNode.setName( "pickupNodeB" );

		Node deliveryNode = new Node();
		deliveryNode.setName( "deliveryNodeB" );

		pickupNode.setRoute( route );
		pickupNode.setTour( tour );
		pickupNode.setTransientField( "pickup node aaaaaaaaaaa" );

		deliveryNode.setRoute( route );
		deliveryNode.setTour( tour );
		deliveryNode.setTransientField( "delivery node aaaaaaaaa" );

		tour.getNodes().add( pickupNode );
		tour.getNodes().add( deliveryNode );

		route.getNodes().add( pickupNode );
		route.getNodes().add( deliveryNode );

		Route mergedRoute = (Route) s.merge( route );

		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testMergeTransientChildBeforeTransientParent() {
		// This test fails because the merge algorithm tries to save a
		// transient child (transport) before cascade-merge gets its
		// transient parent (vehicle); merge does not cascade from the
		// child to the parent.
		Session s = openSession();
		s.beginTransaction();

		Route route = new Route();
		route.setName( "routeA" );

		s.save( route );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();

		route = (Route) s.get( Route.class, new Long( 1 ) );

		route.setTransientField( new String( "sfnaouisrbn" ) );

		Tour tour = new Tour();
		tour.setName( "tourB" );

		Transport transport = new Transport();
		transport.setName( "transportB" );

		Node pickupNode = new Node();
		pickupNode.setName( "pickupNodeB" );

		Node deliveryNode = new Node();
		deliveryNode.setName( "deliveryNodeB" );

		Vehicle vehicle = new Vehicle();
		vehicle.setName( "vehicleB" );

		pickupNode.setRoute( route );
		pickupNode.setTour( tour );
		pickupNode.getPickupTransports().add( transport );
		pickupNode.setTransientField( "pickup node aaaaaaaaaaa" );

		deliveryNode.setRoute( route );
		deliveryNode.setTour( tour );
		deliveryNode.getDeliveryTransports().add( transport );
		deliveryNode.setTransientField( "delivery node aaaaaaaaa" );

		tour.getNodes().add( pickupNode );
		tour.getNodes().add( deliveryNode );

		route.getNodes().add( pickupNode );
		route.getNodes().add( deliveryNode );
		route.getVehicles().add( vehicle );

		transport.setPickupNode( pickupNode );
		transport.setDeliveryNode( deliveryNode );
		transport.setVehicle( vehicle );
		transport.setTransientField( "aaaaaaaaaaaaaa" );

		vehicle.getTransports().add( transport );
		vehicle.setTransientField( "anewvalue" );
		vehicle.setRoute( route );

		Route mergedRoute = (Route) s.merge( route );

		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testMergeData3Nodes() {

		Session s = openSession();
		s.beginTransaction();

		Route route = new Route();
		route.setName( "routeA" );

		s.save( route );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();

		route = (Route) s.get( Route.class, new Long( 1 ) );

		route.setTransientField( new String( "sfnaouisrbn" ) );

		Tour tour = new Tour();
		tour.setName( "tourB" );

		Transport transport1 = new Transport();
		transport1.setName( "TRANSPORT1" );

		Transport transport2 = new Transport();
		transport2.setName( "TRANSPORT2" );

		Node node1 = new Node();
		node1.setName( "NODE1" );

		Node node2 = new Node();
		node2.setName( "NODE2" );

		Node node3 = new Node();
		node3.setName( "NODE3" );

		Vehicle vehicle = new Vehicle();
		vehicle.setName( "vehicleB" );

		node1.setRoute( route );
		node1.setTour( tour );
		node1.getPickupTransports().add( transport1 );
		node1.setTransientField( "node 1" );

		node2.setRoute( route );
		node2.setTour( tour );
		node2.getDeliveryTransports().add( transport1 );
		node2.getPickupTransports().add( transport2 );
		node2.setTransientField( "node 2" );

		node3.setRoute( route );
		node3.setTour( tour );
		node3.getDeliveryTransports().add( transport2 );
		node3.setTransientField( "node 3" );

		tour.getNodes().add( node1 );
		tour.getNodes().add( node2 );
		tour.getNodes().add( node3 );

		route.getNodes().add( node1 );
		route.getNodes().add( node2 );
		route.getNodes().add( node3 );
		route.getVehicles().add( vehicle );

		transport1.setPickupNode( node1 );
		transport1.setDeliveryNode( node2 );
		transport1.setVehicle( vehicle );
		transport1.setTransientField( "aaaaaaaaaaaaaa" );

		transport2.setPickupNode( node2 );
		transport2.setDeliveryNode( node3 );
		transport2.setVehicle( vehicle );
		transport2.setTransientField( "bbbbbbbbbbbbb" );

		vehicle.getTransports().add( transport1 );
		vehicle.getTransports().add( transport2 );
		vehicle.setTransientField( "anewvalue" );
		vehicle.setRoute( route );

		Route mergedRoute = (Route) s.merge( route );

		s.getTransaction().commit();
		s.close();
	}

}
