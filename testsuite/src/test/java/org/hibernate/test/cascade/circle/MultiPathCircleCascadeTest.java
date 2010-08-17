//$Id: $
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */

package org.hibernate.test.cascade.circle;

import java.util.Iterator;

import junit.framework.Test;

import org.hibernate.JDBCException;
import org.hibernate.PropertyValueException;
import org.hibernate.Session;
import org.hibernate.TransientObjectException;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

/**
 * The test case uses the following model:
 *
 *                          <-    ->
 *                      -- (N : 0,1) -- Tour
 *                      |    <-   ->
 *                      | -- (1 : N) -- (pickup) ----
 *               ->     | |                          |
 * Route -- (1 : N) -- Node                      Transport
 *                      |  <-   ->                |
 *                      -- (1 : N) -- (delivery) --
 *
 *  Arrows indicate the direction of cascade-merge.
 *
 * It reproduced the following issues:
 *    http://opensource.atlassian.com/projects/hibernate/browse/HHH-3046
 *    http://opensource.atlassian.com/projects/hibernate/browse/HHH-3810
 *
 * This tests that merge is cascaded properly from each entity.
 * 
 * @author Pavol Zibrita, Gail Badner
 */
public class MultiPathCircleCascadeTest extends FunctionalTestCase {

	public MultiPathCircleCascadeTest(String string) {
		super(string);
	}

	public void configure(Configuration cfg) {
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true");
		cfg.setProperty( Environment.STATEMENT_BATCH_SIZE, "0" );
	}

	public String[] getMappings() {
		return new String[] {
				"cascade/circle/MultiPathCircleCascade.hbm.xml"
		};
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( MultiPathCircleCascadeTest.class );
	}
	
	protected void cleanupTest() {
		Session s = openSession();
		s.beginTransaction();
		s.createQuery( "delete from Transport" );
		s.createQuery( "delete from Tour" );
		s.createQuery( "delete from Node" );
		s.createQuery( "delete from Route" );
	}
	
	public void testMergeEntityWithNonNullableTransientEntity()
	{
		Route route = getUpdatedDetachedEntity();

		Node node = ( Node ) route.getNodes().iterator().next();
		route.getNodes().remove( node );

		Route routeNew = new Route();
		routeNew.setName( "new route" );
		routeNew.getNodes().add( node );
		node.setRoute( routeNew );

		Session s = openSession();
		s.beginTransaction();

		try {
			s.merge( node );
			fail( "should have thrown an exception" );
		}
		catch ( Exception ex ) {
			if ( ( ( SessionImplementor ) s ).getFactory().getSettings().isCheckNullability() ) {
				assertTrue( ex instanceof TransientObjectException );
			}
			else {
				assertTrue( ex instanceof JDBCException );
			}
		}
		finally {
			s.getTransaction().rollback();
			s.close();
		}
	}

	public void testMergeEntityWithNonNullableEntityNull()
	{
		Route route = getUpdatedDetachedEntity();

		Node node = ( Node ) route.getNodes().iterator().next();
		route.getNodes().remove( node );
		node.setRoute( null );

		Session s = openSession();
		s.beginTransaction();

		try {
			s.merge( node );
			fail( "should have thrown an exception" );
		}
		catch ( Exception ex ) {
			if ( ( ( SessionImplementor ) s ).getFactory().getSettings().isCheckNullability() ) {
				assertTrue( ex instanceof PropertyValueException );
			}
			else {
				assertTrue( ex instanceof JDBCException );
			}
		}
		finally {
			s.getTransaction().rollback();
			s.close();
		}
	}

	public void testMergeEntityWithNonNullablePropSetToNull()
	{
		Route route = getUpdatedDetachedEntity();
		Node node = ( Node ) route.getNodes().iterator().next();
		node.setName( null );

		Session s = openSession();
		s.beginTransaction();

		try {
			s.merge( route );
			fail( "should have thrown an exception" );
		}
		catch ( Exception ex ) {
			if ( ( ( SessionImplementor ) s ).getFactory().getSettings().isCheckNullability() ) {
				assertTrue( ex instanceof PropertyValueException );
			}
			else {
				assertTrue( ex instanceof JDBCException );
			}
		}
		finally {
			s.getTransaction().rollback();
			s.close();
		}
	}

	public void testMergeRoute()
	{

		Route route = getUpdatedDetachedEntity();

		clearCounts();

		Session s = openSession();
		s.beginTransaction();

		s.merge(route);

		s.getTransaction().commit();
		s.close();

		assertInsertCount( 4 );
		assertUpdateCount( 1 );

		s = openSession();
		s.beginTransaction();
		route = ( Route ) s.get( Route.class, route.getRouteID() );
		checkResults( route, true );
		s.getTransaction().commit();
		s.close();
	}

	public void testMergePickupNode()
	{

		Route route = getUpdatedDetachedEntity();

		clearCounts();

		Session s = openSession();
		s.beginTransaction();

		Iterator it=route.getNodes().iterator();
		Node node = ( Node ) it.next();
		Node pickupNode;
		if ( node.getName().equals( "pickupNodeB") ) {
			pickupNode = node;
		}
		else {
			node = ( Node ) it.next();
			assertEquals( "pickupNodeB", node.getName() );
			pickupNode = node;
		}

		pickupNode = ( Node ) s.merge( pickupNode );

		s.getTransaction().commit();
		s.close();

		assertInsertCount( 4 );
		assertUpdateCount( 0 );

		s = openSession();
		s.beginTransaction();
		route = ( Route ) s.get( Route.class, route.getRouteID() );
		checkResults( route, false );
		s.getTransaction().commit();
		s.close();
	}

	public void testMergeDeliveryNode()
	{

		Route route = getUpdatedDetachedEntity();

		clearCounts();

		Session s = openSession();
		s.beginTransaction();

		Iterator it=route.getNodes().iterator();
		Node node = ( Node ) it.next();
		Node deliveryNode;
		if ( node.getName().equals( "deliveryNodeB") ) {
			deliveryNode = node;
		}
		else {
			node = ( Node ) it.next();
			assertEquals( "deliveryNodeB", node.getName() );
			deliveryNode = node;
		}

		deliveryNode = ( Node ) s.merge( deliveryNode );

		s.getTransaction().commit();
		s.close();

		assertInsertCount( 4 );
		assertUpdateCount( 0 );

		s = openSession();
		s.beginTransaction();
		route = ( Route ) s.get( Route.class, route.getRouteID() );
		checkResults( route, false );
		s.getTransaction().commit();
		s.close();
	}

	public void testMergeTour()
	{

		Route route = getUpdatedDetachedEntity();

		clearCounts();

		Session s = openSession();
		s.beginTransaction();

		Tour tour = ( Tour ) s.merge( ( ( Node ) route.getNodes().toArray()[0]).getTour() );

		s.getTransaction().commit();
		s.close();

		assertInsertCount( 4 );
		assertUpdateCount( 0 );

		s = openSession();
		s.beginTransaction();
		route = ( Route ) s.get( Route.class, route.getRouteID() );
		checkResults( route, false );
		s.getTransaction().commit();
		s.close();
	}

	public void testMergeTransport()
	{

		Route route = getUpdatedDetachedEntity();

		clearCounts();

		Session s = openSession();
		s.beginTransaction();

		Node node = ( ( Node ) route.getNodes().toArray()[0]);
		Transport transport;
		if ( node.getPickupTransports().size() == 1 ) {
			transport = ( Transport ) node.getPickupTransports().toArray()[0];
		}
		else {
			transport = ( Transport ) node.getDeliveryTransports().toArray()[0];
		}

		transport = ( Transport ) s.merge( transport  );

		s.getTransaction().commit();
		s.close();

		assertInsertCount( 4 );
		assertUpdateCount( 0 );

		s = openSession();
		s.beginTransaction();
		route = ( Route ) s.get( Route.class, route.getRouteID() );
		checkResults( route, false );
		s.getTransaction().commit();
		s.close();
	}

	private Route getUpdatedDetachedEntity() {

		Session s = openSession();
		s.beginTransaction();

		Route route = new Route();
		route.setName("routeA");

		s.save( route );
		s.getTransaction().commit();
		s.close();

		route.setName( "new routeA" );
		route.setTransientField(new String("sfnaouisrbn"));

		Tour tour = new Tour();
		tour.setName("tourB");

		Transport transport = new Transport();
		transport.setName("transportB");

		Node pickupNode = new Node();
		pickupNode.setName("pickupNodeB");

		Node deliveryNode = new Node();
		deliveryNode.setName("deliveryNodeB");

		pickupNode.setRoute(route);
		pickupNode.setTour(tour);
		pickupNode.getPickupTransports().add(transport);
		pickupNode.setTransientField("pickup node aaaaaaaaaaa");

		deliveryNode.setRoute(route);
		deliveryNode.setTour(tour);
		deliveryNode.getDeliveryTransports().add(transport);
		deliveryNode.setTransientField("delivery node aaaaaaaaa");

		tour.getNodes().add(pickupNode);
		tour.getNodes().add(deliveryNode);

		route.getNodes().add(pickupNode);
		route.getNodes().add(deliveryNode);

		transport.setPickupNode(pickupNode);
		transport.setDeliveryNode(deliveryNode);
		transport.setTransientField("aaaaaaaaaaaaaa");

		return route;
	}

	private void checkResults(Route route, boolean isRouteUpdated) {
		// since merge is not cascaded to route, this method needs to
		// know whether route is expected to be updated
		if ( isRouteUpdated ) {
			assertEquals( "new routeA", route.getName() );
		}
		assertEquals( 2, route.getNodes().size() );
		Node deliveryNode = null;
		Node pickupNode = null;
		for( Iterator it=route.getNodes().iterator(); it.hasNext(); ) {
			Node node = ( Node ) it.next();
			if( "deliveryNodeB".equals( node.getName(  )  ) ) {
				deliveryNode = node;
			}
			else if( "pickupNodeB".equals( node.getName() ) ) {
				pickupNode = node;
			}
			else {
				fail( "unknown node");
			}
		}
		assertNotNull( deliveryNode );
		assertSame( route, deliveryNode.getRoute() );
		assertEquals( 1, deliveryNode.getDeliveryTransports().size() );
		assertEquals( 0, deliveryNode.getPickupTransports().size() );
		assertNotNull( deliveryNode.getTour() );
		assertEquals( "node original value", deliveryNode.getTransientField() );

		assertNotNull( pickupNode );
		assertSame( route, pickupNode.getRoute() );
		assertEquals( 0, pickupNode.getDeliveryTransports().size() );
		assertEquals( 1, pickupNode.getPickupTransports().size() );
		assertNotNull( pickupNode.getTour() );
		assertEquals( "node original value", pickupNode.getTransientField() );

		assertTrue( ! deliveryNode.getNodeID().equals( pickupNode.getNodeID() ) );
		assertSame( deliveryNode.getTour(), pickupNode.getTour() );
		assertSame( deliveryNode.getDeliveryTransports().iterator().next(),
				pickupNode.getPickupTransports().iterator().next() );

		Tour tour = deliveryNode.getTour();
		Transport transport = ( Transport ) deliveryNode.getDeliveryTransports().iterator().next();

		assertEquals( "tourB", tour.getName() );
		assertEquals( 2, tour.getNodes().size() );
		assertTrue( tour.getNodes().contains( deliveryNode ) );
		assertTrue( tour.getNodes().contains( pickupNode ) );

		assertEquals( "transportB", transport.getName() );
		assertSame( deliveryNode, transport.getDeliveryNode() );
		assertSame( pickupNode, transport.getPickupNode() );
		assertEquals( "transport original value", transport.getTransientField() );
	}

	public void testMergeData3Nodes()
	{

		Session s = openSession();
		s.beginTransaction();

		Route route = new Route();
		route.setName("routeA");

		s.save( route );
		s.getTransaction().commit();
		s.close();

		clearCounts();

		s = openSession();
		s.beginTransaction();

		route = (Route) s.get(Route.class, new Long(1));
		//System.out.println(route);
		route.setName( "new routA" );

		route.setTransientField(new String("sfnaouisrbn"));

		Tour tour = new Tour();
		tour.setName("tourB");

		Transport transport1 = new Transport();
		transport1.setName("TRANSPORT1");

		Transport transport2 = new Transport();
		transport2.setName("TRANSPORT2");

		Node node1 = new Node();
		node1.setName("NODE1");

		Node node2 = new Node();
		node2.setName("NODE2");

		Node node3 = new Node();
		node3.setName("NODE3");

		node1.setRoute(route);
		node1.setTour(tour);
		node1.getPickupTransports().add(transport1);
		node1.setTransientField("node 1");

		node2.setRoute(route);
		node2.setTour(tour);
		node2.getDeliveryTransports().add(transport1);
		node2.getPickupTransports().add(transport2);
		node2.setTransientField("node 2");

		node3.setRoute(route);
		node3.setTour(tour);
		node3.getDeliveryTransports().add(transport2);
		node3.setTransientField("node 3");

		tour.getNodes().add(node1);
		tour.getNodes().add(node2);
		tour.getNodes().add(node3);

		route.getNodes().add(node1);
		route.getNodes().add(node2);
		route.getNodes().add(node3);

		transport1.setPickupNode(node1);
		transport1.setDeliveryNode(node2);
		transport1.setTransientField("aaaaaaaaaaaaaa");

		transport2.setPickupNode(node2);
		transport2.setDeliveryNode(node3);
		transport2.setTransientField("bbbbbbbbbbbbb");

		Route mergedRoute = (Route) s.merge(route);

		s.getTransaction().commit();
		s.close();

		assertInsertCount( 6 );
		assertUpdateCount( 1 );
	}

	protected void clearCounts() {
		getSessions().getStatistics().clear();
	}

	protected void assertInsertCount(int expected) {
		int inserts = ( int ) getSessions().getStatistics().getEntityInsertCount();
		assertEquals( "unexpected insert count", expected, inserts );
	}

	protected void assertUpdateCount(int expected) {
		int updates = ( int ) getSessions().getStatistics().getEntityUpdateCount();
		assertEquals( "unexpected update counts", expected, updates );
	}

	protected void assertDeleteCount(int expected) {
		int deletes = ( int ) getSessions().getStatistics().getEntityDeleteCount();
		assertEquals( "unexpected delete counts", expected, deletes );
	}	
}
