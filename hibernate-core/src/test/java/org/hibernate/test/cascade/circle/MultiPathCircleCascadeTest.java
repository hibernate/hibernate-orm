/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cascade.circle;

import java.util.Iterator;

import org.junit.Test;

import org.hibernate.JDBCException;
import org.hibernate.PropertyValueException;
import org.hibernate.Session;
import org.hibernate.TransientPropertyValueException;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
 *  Arrows indicate the direction of cascade-merge, cascade-save, and cascade-save-or-update
 *
 * It reproduced the following issues:
 * http://opensource.atlassian.com/projects/hibernate/browse/HHH-3046
 * http://opensource.atlassian.com/projects/hibernate/browse/HHH-3810
 * <p/>
 * This tests that cascades are done properly from each entity.
 *
 * @author Pavol Zibrita, Gail Badner
 */
public class MultiPathCircleCascadeTest extends BaseCoreFunctionalTestCase {
	private static interface EntityOperation {
		Object doEntityOperation(Object entity, Session s);
	}
	private static EntityOperation MERGE_OPERATION =
			new EntityOperation() {
				@Override
				public Object doEntityOperation(Object entity, Session s) {
					return s.merge( entity );
				}
			};
	private static EntityOperation SAVE_OPERATION =
			new EntityOperation() {
				@Override
				public Object doEntityOperation(Object entity, Session s) {
					s.save( entity );
					return entity;
				}
			};
	private static EntityOperation SAVE_UPDATE_OPERATION =
			new EntityOperation() {
				@Override
				public Object doEntityOperation(Object entity, Session s) {
					s.saveOrUpdate( entity );
					return entity;
				}
			};

	@Override
	public void configure(Configuration cfg) {
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
		cfg.setProperty( Environment.STATEMENT_BATCH_SIZE, "0" );
	}

	@Override
	public String[] getMappings() {
		return new String[] {
				"cascade/circle/MultiPathCircleCascade.hbm.xml"
		};
	}

	@Test
	public void testMergeEntityWithNonNullableTransientEntity() {
		testEntityWithNonNullableTransientEntity( MERGE_OPERATION );
	}
	@Test
	public void testSaveEntityWithNonNullableTransientEntity() {
		testEntityWithNonNullableTransientEntity( SAVE_OPERATION );
	}
	@Test
	public void testSaveUpdateEntityWithNonNullableTransientEntity() {
		testEntityWithNonNullableTransientEntity( SAVE_UPDATE_OPERATION );
	}
	private void testEntityWithNonNullableTransientEntity(EntityOperation operation) {

		Route route = getUpdatedDetachedEntity();

		Node node = (Node) route.getNodes().iterator().next();
		route.getNodes().remove( node );

		Route routeNew = new Route();
		routeNew.setName( "new route" );
		routeNew.getNodes().add( node );
		node.setRoute( routeNew );

		Session s = openSession();
		s.beginTransaction();

		try {
			operation.doEntityOperation( node, s );
			s.getTransaction().commit();
			fail( "should have thrown an exception" );
		}
		catch (Exception ex) {
			checkExceptionFromNullValueForNonNullable(
					ex,
					((SessionImplementor) s).getFactory().getSettings().isCheckNullability(),
					false
			);
		}
		finally {
			s.getTransaction().rollback();
			s.close();
			cleanup();
		}
	}

	@Test
	public void testMergeEntityWithNonNullableEntityNull() {
		testEntityWithNonNullableEntityNull( MERGE_OPERATION );
	}
	@Test
	public void testSaveEntityWithNonNullableEntityNull() {
		testEntityWithNonNullableEntityNull( SAVE_OPERATION );
	}
	@Test
	public void testSaveUpdateEntityWithNonNullableEntityNull() {
		testEntityWithNonNullableEntityNull( SAVE_UPDATE_OPERATION );
	}
	private void testEntityWithNonNullableEntityNull(EntityOperation operation) {
		Route route = getUpdatedDetachedEntity();

		Node node = (Node) route.getNodes().iterator().next();
		route.getNodes().remove( node );
		node.setRoute( null );

		Session s = openSession();
		s.beginTransaction();

		try {
			operation.doEntityOperation( node, s );
			s.getTransaction().commit();
			fail( "should have thrown an exception" );
		}
		catch (Exception ex) {
			checkExceptionFromNullValueForNonNullable(
					ex,
					((SessionImplementor) s).getFactory().getSettings().isCheckNullability(),
					true
			);
		}
		finally {
			s.getTransaction().rollback();
			s.close();
			cleanup();
		}
	}

	@Test
	public void testMergeEntityWithNonNullablePropSetToNull() {
		testEntityWithNonNullablePropSetToNull( MERGE_OPERATION );
	}
	@Test
	public void testSaveEntityWithNonNullablePropSetToNull() {
		testEntityWithNonNullablePropSetToNull( SAVE_OPERATION );
	}
	@Test
	public void testSaveUpdateEntityWithNonNullablePropSetToNull() {
		testEntityWithNonNullablePropSetToNull( SAVE_UPDATE_OPERATION );
	}
	private void testEntityWithNonNullablePropSetToNull(EntityOperation operation) {
		Route route = getUpdatedDetachedEntity();
		Node node = (Node) route.getNodes().iterator().next();
		node.setName( null );

		Session s = openSession();
		s.beginTransaction();

		try {
			operation.doEntityOperation( route, s );
			s.getTransaction().commit();
			fail( "should have thrown an exception" );
		}
		catch (Exception ex) {
			checkExceptionFromNullValueForNonNullable(
					ex,
					((SessionImplementor) s).getFactory().getSettings().isCheckNullability(),
					true
			);
		}
		finally {
			s.getTransaction().rollback();
			s.close();
			cleanup();
		}
	}

	@Test
	public void testMergeRoute() {
		testRoute( MERGE_OPERATION );
	}
	// skip SAVE_OPERATION since Route is not transient
	@Test
	public void testSaveUpdateRoute() {
		testRoute( SAVE_UPDATE_OPERATION );
	}
	private void testRoute(EntityOperation operation) {

		Route route = getUpdatedDetachedEntity();

		clearCounts();

		Session s = openSession();
		s.beginTransaction();

		operation.doEntityOperation( route, s );

		s.getTransaction().commit();
		s.close();

		assertInsertCount( 4 );
		assertUpdateCount( 1 );

		s = openSession();
		s.beginTransaction();
		route = (Route) s.get( Route.class, route.getRouteID() );
		checkResults( route, true );
		s.getTransaction().commit();
		s.close();

		cleanup();
	}

	@Test
	public void testMergePickupNode() {
		testPickupNode( MERGE_OPERATION );
	}
	@Test
	public void testSavePickupNode() {
		testPickupNode( SAVE_OPERATION );
	}
	@Test
	public void testSaveUpdatePickupNode() {
		testPickupNode( SAVE_UPDATE_OPERATION );
	}
	private void testPickupNode(EntityOperation operation) {

		Route route = getUpdatedDetachedEntity();

		clearCounts();

		Session s = openSession();
		s.beginTransaction();

		Iterator it = route.getNodes().iterator();
		Node node = (Node) it.next();
		Node pickupNode;
		if ( node.getName().equals( "pickupNodeB" ) ) {
			pickupNode = node;
		}
		else {
			node = (Node) it.next();
			assertEquals( "pickupNodeB", node.getName() );
			pickupNode = node;
		}

		pickupNode = (Node) operation.doEntityOperation( pickupNode, s );

		s.getTransaction().commit();
		s.close();

		assertInsertCount( 4 );
		assertUpdateCount( 0 );

		s = openSession();
		s.beginTransaction();
		route = (Route) s.get( Route.class, route.getRouteID() );
		checkResults( route, false );
		s.getTransaction().commit();
		s.close();

		cleanup();
	}

	@Test
	public void testMergeDeliveryNode() {
		testDeliveryNode( MERGE_OPERATION );
	}
	@Test
	public void testSaveDeliveryNode() {
		testDeliveryNode( SAVE_OPERATION );
	}
	@Test
	public void testSaveUpdateDeliveryNode() {
		testDeliveryNode( SAVE_UPDATE_OPERATION );
	}
	private void testDeliveryNode(EntityOperation operation) {

		Route route = getUpdatedDetachedEntity();

		clearCounts();

		Session s = openSession();
		s.beginTransaction();

		Iterator it = route.getNodes().iterator();
		Node node = (Node) it.next();
		Node deliveryNode;
		if ( node.getName().equals( "deliveryNodeB" ) ) {
			deliveryNode = node;
		}
		else {
			node = (Node) it.next();
			assertEquals( "deliveryNodeB", node.getName() );
			deliveryNode = node;
		}

		deliveryNode = (Node) operation.doEntityOperation( deliveryNode, s );

		s.getTransaction().commit();
		s.close();

		assertInsertCount( 4 );
		assertUpdateCount( 0 );

		s = openSession();
		s.beginTransaction();
		route = (Route) s.get( Route.class, route.getRouteID() );
		checkResults( route, false );
		s.getTransaction().commit();
		s.close();

		cleanup();
	}

	@Test
	public void testMergeTour() {
		testTour( MERGE_OPERATION );
	}
	@Test
	public void testSaveTour() {
		testTour( SAVE_OPERATION );
	}
	@Test
	public void testSaveUpdateTour() {
		testTour( SAVE_UPDATE_OPERATION );
	}
	private void testTour(EntityOperation operation) {

		Route route = getUpdatedDetachedEntity();

		clearCounts();

		Session s = openSession();
		s.beginTransaction();

		Tour tour = (Tour) operation.doEntityOperation( ((Node) route.getNodes().toArray()[0]).getTour(), s );

		s.getTransaction().commit();
		s.close();

		assertInsertCount( 4 );
		assertUpdateCount( 0 );

		s = openSession();
		s.beginTransaction();
		route = (Route) s.get( Route.class, route.getRouteID() );
		checkResults( route, false );
		s.getTransaction().commit();
		s.close();

		cleanup();
	}

	@Test
	public void testMergeTransport() {
		testTransport( MERGE_OPERATION );
	}
	@Test
	public void testSaveTransport() {
		testTransport( SAVE_OPERATION );
	}
	@Test
	public void testSaveUpdateTransport() {
		testTransport( SAVE_UPDATE_OPERATION );
	}
	private void testTransport(EntityOperation operation) {

		Route route = getUpdatedDetachedEntity();

		clearCounts();

		Session s = openSession();
		s.beginTransaction();

		Node node = ((Node) route.getNodes().toArray()[0]);
		Transport transport;
		if ( node.getPickupTransports().size() == 1 ) {
			transport = (Transport) node.getPickupTransports().toArray()[0];
		}
		else {
			transport = (Transport) node.getDeliveryTransports().toArray()[0];
		}

		transport = (Transport) operation.doEntityOperation( transport, s );

		s.getTransaction().commit();
		s.close();

		assertInsertCount( 4 );
		assertUpdateCount( 0 );

		s = openSession();
		s.beginTransaction();
		route = (Route) s.get( Route.class, route.getRouteID() );
		checkResults( route, false );
		s.getTransaction().commit();
		s.close();

		cleanup();
	}

	private Node getSimpleUpdatedDetachedEntity(){

		Node deliveryNode = new Node();
		deliveryNode.setName( "deliveryNodeB" );
		return deliveryNode;
	}

	private Route getUpdatedDetachedEntity() {

		Session s = openSession();
		s.beginTransaction();

		Route route = new Route();
		route.setName( "routeA" );

		s.save( route );
		s.getTransaction().commit();
		s.close();

		route.setName( "new routeA" );
		route.setTransientField( new String( "sfnaouisrbn" ) );

		Tour tour = new Tour();
		tour.setName( "tourB" );

		Transport transport = new Transport();
		transport.setName( "transportB" );

		Node pickupNode = new Node();
		pickupNode.setName( "pickupNodeB" );

		Node deliveryNode = new Node();
		deliveryNode.setName( "deliveryNodeB" );

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

		transport.setPickupNode( pickupNode );
		transport.setDeliveryNode( deliveryNode );
		transport.setTransientField( "aaaaaaaaaaaaaa" );

		return route;
	}

	private void cleanup() {
		Session s = openSession();
		s.beginTransaction();
		s.createQuery( "delete from Transport" );
		s.createQuery( "delete from Tour" );
		s.createQuery( "delete from Node" );
		s.createQuery( "delete from Route" );
		s.getTransaction().commit();
		s.close();
	}

	private void checkResults(Route route, boolean isRouteUpdated) {
		// since no cascaded to route, this method needs to
		// know whether route is expected to be updated
		if ( isRouteUpdated ) {
			assertEquals( "new routeA", route.getName() );
		}
		assertEquals( 2, route.getNodes().size() );
		Node deliveryNode = null;
		Node pickupNode = null;
		for ( Iterator it = route.getNodes().iterator(); it.hasNext(); ) {
			Node node = (Node) it.next();
			if ( "deliveryNodeB".equals( node.getName() ) ) {
				deliveryNode = node;
			}
			else if ( "pickupNodeB".equals( node.getName() ) ) {
				pickupNode = node;
			}
			else {
				fail( "unknown node" );
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

		assertTrue( !deliveryNode.getNodeID().equals( pickupNode.getNodeID() ) );
		assertSame( deliveryNode.getTour(), pickupNode.getTour() );
		assertSame(
				deliveryNode.getDeliveryTransports().iterator().next(),
				pickupNode.getPickupTransports().iterator().next()
		);

		Tour tour = deliveryNode.getTour();
		Transport transport = (Transport) deliveryNode.getDeliveryTransports().iterator().next();

		assertEquals( "tourB", tour.getName() );
		assertEquals( 2, tour.getNodes().size() );
		assertTrue( tour.getNodes().contains( deliveryNode ) );
		assertTrue( tour.getNodes().contains( pickupNode ) );

		assertEquals( "transportB", transport.getName() );
		assertSame( deliveryNode, transport.getDeliveryNode() );
		assertSame( pickupNode, transport.getPickupNode() );
		assertEquals( "transport original value", transport.getTransientField() );
	}

	@Test
	public void testMergeData3Nodes() {
		testData3Nodes( MERGE_OPERATION );
	}
	@Test
	public void testSaveData3Nodes() {
		testData3Nodes( SAVE_OPERATION );
	}
	@Test
	public void testSaveUpdateData3Nodes() {
		testData3Nodes( SAVE_UPDATE_OPERATION );
	}
	private void testData3Nodes(EntityOperation operation) {

		Session s = openSession();
		s.beginTransaction();

		Route route = new Route();
		route.setName( "routeA" );

		s.save( route );
		s.getTransaction().commit();
		s.close();

		clearCounts();

		s = openSession();
		s.beginTransaction();

		route = (Route) s.get( Route.class, route.getRouteID() );
		//System.out.println(route);
		route.setName( "new routA" );

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

		transport1.setPickupNode( node1 );
		transport1.setDeliveryNode( node2 );
		transport1.setTransientField( "aaaaaaaaaaaaaa" );

		transport2.setPickupNode( node2 );
		transport2.setDeliveryNode( node3 );
		transport2.setTransientField( "bbbbbbbbbbbbb" );

		operation.doEntityOperation( route, s );

		s.getTransaction().commit();
		s.close();

		assertInsertCount( 6 );
		assertUpdateCount( 1 );

		cleanup();
	}

	protected void checkExceptionFromNullValueForNonNullable(
			Exception ex, boolean checkNullability, boolean isNullValue
	) {
		if ( isNullValue ) {
			if ( checkNullability ) {
				assertTrue( ex instanceof PropertyValueException );
			}
			else {
				assertTrue( (ex instanceof JDBCException) || (ex.getCause() instanceof JDBCException) );
			}
		}
		else {
			assertTrue( ex instanceof TransientPropertyValueException );
		}
	}

	protected void clearCounts() {
		sessionFactory().getStatistics().clear();
	}

	protected void assertInsertCount(int expected) {
		int inserts = (int) sessionFactory().getStatistics().getEntityInsertCount();
		assertEquals( "unexpected insert count", expected, inserts );
	}

	protected void assertUpdateCount(int expected) {
		int updates = (int) sessionFactory().getStatistics().getEntityUpdateCount();
		assertEquals( "unexpected update counts", expected, updates );
	}

	protected void assertDeleteCount(int expected) {
		int deletes = (int) sessionFactory().getStatistics().getEntityDeleteCount();
		assertEquals( "unexpected delete counts", expected, deletes );
	}
}
