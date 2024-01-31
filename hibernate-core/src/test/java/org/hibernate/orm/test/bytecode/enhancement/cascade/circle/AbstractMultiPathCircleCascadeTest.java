/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.bytecode.enhancement.cascade.circle;

import java.util.Iterator;

import org.hibernate.JDBCException;
import org.hibernate.PropertyValueException;
import org.hibernate.Session;
import org.hibernate.TransientPropertyValueException;
import org.hibernate.orm.test.cascade.circle.Node;
import org.hibernate.orm.test.cascade.circle.Route;
import org.hibernate.orm.test.cascade.circle.Tour;
import org.hibernate.orm.test.cascade.circle.Transport;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Test;

import jakarta.persistence.PersistenceException;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Andrea Boriero
 */
public abstract class AbstractMultiPathCircleCascadeTest extends BaseCoreFunctionalTestCase {
	private interface EntityOperation {
		boolean isLegacy();

		Object doEntityOperation(Object entity, Session s);
	}

	private static EntityOperation MERGE_OPERATION =
			new EntityOperation() {
				@Override
				public boolean isLegacy() {
					return false;
				}

				@Override
				public Object doEntityOperation(Object entity, Session s) {
					return s.merge( entity );
				}
			};
	private static EntityOperation SAVE_OPERATION =
			new EntityOperation() {
				@Override
				public boolean isLegacy() {
					return true;
				}

				@Override
				public Object doEntityOperation(Object entity, Session s) {
					s.save( entity );
					return entity;
				}
			};
	private static EntityOperation SAVE_UPDATE_OPERATION =
			new EntityOperation() {
				@Override
				public boolean isLegacy() {
					return true;
				}

				@Override
				public Object doEntityOperation(Object entity, Session s) {
					s.saveOrUpdate( entity );
					return entity;
				}
			};

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

	private void testEntityWithNonNullableTransientEntity( EntityOperation operation) {

		Route route = getUpdatedDetachedEntity();

		Node node = (Node) route.getNodes().iterator().next();
		route.getNodes().remove( node );

		Route routeNew = new Route();
		routeNew.setName( "new route" );
		routeNew.getNodes().add( node );
		node.setRoute( routeNew );

		inSession(
				session -> {
					session.beginTransaction();
					try {
						operation.doEntityOperation( node, session );
						session.getTransaction().commit();
						fail( "should have thrown an exception" );
					}
					catch (Exception ex) {
						checkExceptionFromNullValueForNonNullable(
								ex,
								session.getFactory().getSessionFactoryOptions().isCheckNullability(),
								false,
								operation.isLegacy()
						);
					}
					finally {
						session.getTransaction().rollback();
					}
				}
		);

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

	private void testEntityWithNonNullableEntityNull( EntityOperation operation) {
		Route route = getUpdatedDetachedEntity();

		Node node = (Node) route.getNodes().iterator().next();
		route.getNodes().remove( node );
		node.setRoute( null );

		inSession(
				session -> {
					session.beginTransaction();
					try {
						operation.doEntityOperation( node, session );
						session.getTransaction().commit();
						fail( "should have thrown an exception" );
					}
					catch (Exception ex) {
						checkExceptionFromNullValueForNonNullable(
								ex,
								session.getFactory().getSessionFactoryOptions().isCheckNullability(),
								true,
								operation.isLegacy()
						);
					}
					finally {
						session.getTransaction().rollback();
					}
				}
		);
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

	private void testEntityWithNonNullablePropSetToNull( EntityOperation operation) {
		Route route = getUpdatedDetachedEntity();
		Node node = (Node) route.getNodes().iterator().next();
		node.setName( null );

		inSession(
				session -> {
					session.beginTransaction();

					try {
						operation.doEntityOperation( route, session );
						session.getTransaction().commit();
						fail( "should have thrown an exception" );
					}
					catch (Exception ex) {
						checkExceptionFromNullValueForNonNullable(
								ex,
								session.getFactory().getSessionFactoryOptions().isCheckNullability(),
								true,
								operation.isLegacy()
						);
					}
					finally {
						session.getTransaction().rollback();
					}
				}
		);

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

	private void testRoute( EntityOperation operation) {

		Route r = getUpdatedDetachedEntity();

		clearCounts();

		inTransaction(
				session ->
						operation.doEntityOperation( r, session )
		);

		assertInsertCount( 4 );
		assertUpdateCount( 1 );

		inTransaction(
				session -> {
					Route route = session.get( Route.class, r.getRouteID() );
					checkResults( route, true );
				}
		);
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

	private void testPickupNode( EntityOperation operation) {

		Route r = getUpdatedDetachedEntity();

		clearCounts();

		inTransaction(
				session -> {
					Iterator it = r.getNodes().iterator();
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

					operation.doEntityOperation( pickupNode, session );
				}
		);

		assertInsertCount( 4 );
		assertUpdateCount( 0 );

		inTransaction(
				session -> {
					Route route = session.get( Route.class, r.getRouteID() );
					checkResults( route, false );
				}
		);
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

	private void testDeliveryNode( EntityOperation operation) {

		Route r = getUpdatedDetachedEntity();

		clearCounts();

		inTransaction(
				session -> {
					Iterator it = r.getNodes().iterator();
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

					operation.doEntityOperation( deliveryNode, session );
				}
		);


		assertInsertCount( 4 );
		assertUpdateCount( 0 );

		inTransaction(
				session -> {
					Route route = session.get( Route.class, r.getRouteID() );
					checkResults( route, false );
				}
		);
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

	private void testTour( EntityOperation operation) {

		Route r = getUpdatedDetachedEntity();

		clearCounts();

		inTransaction(
				session ->
						operation.doEntityOperation( ( (Node) r.getNodes().toArray()[0] ).getTour(), session )
		);

		assertInsertCount( 4 );
		assertUpdateCount( 0 );

		inTransaction(
				session -> {
					Route route = session.get( Route.class, r.getRouteID() );
					checkResults( route, false );
				}
		);
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

	private void testTransport( EntityOperation operation) {

		Route r = getUpdatedDetachedEntity();

		clearCounts();

		inTransaction(
				session -> {
					Transport transport;
					Node node = ( (Node) r.getNodes().toArray()[0] );
					if ( node.getPickupTransports().size() == 1 ) {
						transport = (Transport) node.getPickupTransports().toArray()[0];
					}
					else {
						transport = (Transport) node.getDeliveryTransports().toArray()[0];
					}

					operation.doEntityOperation( transport, session );
				}
		);

		assertInsertCount( 4 );
		assertUpdateCount( 0 );

		inTransaction(
				session -> {
					Route route = session.get( Route.class, r.getRouteID() );
					checkResults( route, false );
				}
		);
	}

	private Node getSimpleUpdatedDetachedEntity() {

		Node deliveryNode = new Node();
		deliveryNode.setName( "deliveryNodeB" );
		return deliveryNode;
	}

	private Route getUpdatedDetachedEntity() {

		Route route = new Route();
		inTransaction(
				session -> {
					route.setName( "routeA" );

					session.save( route );
				}
		);

		route.setName( "new routeA" );
		route.setTransientField( "sfnaouisrbn" );

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

	@After
	public void cleanup() {
		inTransaction(
				session -> {
					session.createQuery( "delete from Transport" );
					session.createQuery( "delete from Tour" );
					session.createQuery( "delete from Node" );
					session.createQuery( "delete from Route" );
				}
		);
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

	private void testData3Nodes( EntityOperation operation) {

		Route r = new Route();
		inTransaction(
				session -> {
					r.setName( "routeA" );

					session.save( r );
				}
		);

		clearCounts();

		inTransaction(
				session -> {
					Route route = session.get( Route.class, r.getRouteID() );
					route.setName( "new routA" );

					route.setTransientField( "sfnaouisrbn" );

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

					operation.doEntityOperation( route, session );
				}
		);

		assertInsertCount( 6 );
		assertUpdateCount( 1 );
	}

	protected void checkExceptionFromNullValueForNonNullable(
			Exception ex, boolean checkNullability, boolean isNullValue, boolean isLegacy
	) {
		if ( isNullValue ) {
			if ( checkNullability ) {
				if ( isLegacy ) {
					assertTyping( PropertyValueException.class, ex );
				}
				else {
					assertTyping( PersistenceException.class, ex );
				}
			}
			else {
				assertTrue( ( ex instanceof JDBCException ) || ( ex.getCause() instanceof JDBCException ) );
			}
		}
		else {
			if ( isLegacy ) {
				assertTyping( TransientPropertyValueException.class, ex );
			}
			else {
				assertTyping( IllegalStateException.class, ex );
			}
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
