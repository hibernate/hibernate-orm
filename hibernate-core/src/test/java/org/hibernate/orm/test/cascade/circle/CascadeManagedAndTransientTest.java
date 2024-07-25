/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.cascade.circle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import org.hibernate.cfg.Environment;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The test case uses the following model:
 * <p>
 * <-    ->
 * -- (N : 0,1) -- Tour
 * |    <-   ->
 * | -- (1 : N) -- (pickup) ----
 * <-   ->     | |                          |
 * Route -- (1 : N) -- Node                      Transport
 * |  <-   ->                |
 * -- (1 : N) -- (delivery) --
 * <p>
 * Arrows indicate the direction of cascade-merge, cascade-persist, cascade-refresh
 * <p>
 * It reproduces the following issues:
 * https://hibernate.atlassian.net/browse/HHH-9512
 * <p>
 * This tests that cascades are done properly from each entity.
 *
 * @author Alex Belyaev (based on code by Pavol Zibrita and Gail Badner)
 */
@DomainModel(
		xmlMappings = {
				"org/hibernate/orm/test/cascade/circle/CascadeManagedAndTransient.hbm.xml"
		}
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = Environment.CHECK_NULLABILITY, value = "true")
)
public class CascadeManagedAndTransientTest {

	@AfterEach
	public void cleanupTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from Transport" );
					session.createQuery( "delete from Tour" );
					session.createQuery( "delete from Node" );
					session.createQuery( "delete from Route" );
					session.createQuery( "delete from Vehicle" );
				}
		);
	}

	@Test
	public void testAttachedChildInMerge(SessionFactoryScope scope) {
		fillInitialData( scope );

		scope.inTransaction(
				session -> {
					Route route = (Route) session.createQuery( "FROM Route WHERE name = :name" )
							.setParameter( "name", "Route 1" )
							.uniqueResult();
					Node n2 = (Node) session.createQuery( "FROM Node WHERE name = :name" )
							.setParameter( "name", "Node 2" )
							.uniqueResult();
					Node n3 = (Node) session.createQuery( "FROM Node WHERE name = :name" )
							.setParameter( "name", "Node 3" )
							.uniqueResult();

					Vehicle vehicle = new Vehicle();
					vehicle.setName( "Bus" );
					vehicle.setRoute( route );

					Transport $2to3 = new Transport();
					$2to3.setName( "Transport 2 -> 3" );
					$2to3.setPickupNode( n2 );
					n2.getPickupTransports().add( $2to3 );
					$2to3.setDeliveryNode( n3 );
					n3.getDeliveryTransports().add( $2to3 );
					$2to3.setVehicle( vehicle );

					vehicle.setTransports( new HashSet<Transport>( Arrays.asList( $2to3 ) ) );

					// Try to save graph of transient entities (vehicle, transport) which contains attached entities (node2, node3)
					Vehicle managedVehicle = (Vehicle) session.merge( vehicle );
					checkNewVehicle( managedVehicle );

					session.flush();
					session.clear();

					assertEquals( 3, session.createQuery( "FROM Transport" ).list().size() );
					assertEquals( 2, session.createQuery( "FROM Vehicle" ).list().size() );
					assertEquals( 4, session.createQuery( "FROM Node" ).list().size() );

					Vehicle newVehicle = (Vehicle) session.createQuery( "FROM Vehicle WHERE name = :name" )
							.setParameter( "name", "Bus" )
							.uniqueResult();
					checkNewVehicle( newVehicle );
				}
		);
	}

	private void checkNewVehicle(Vehicle newVehicle) {
		assertEquals( "Bus", newVehicle.getName() );
		assertEquals( 1, newVehicle.getTransports().size() );
		Transport t = (Transport) newVehicle.getTransports().iterator().next();
		assertEquals( "Transport 2 -> 3", t.getName() );
		assertEquals( "Node 2", t.getPickupNode().getName() );
		assertEquals( "Node 3", t.getDeliveryNode().getName() );
	}

	private void fillInitialData(SessionFactoryScope scope) {
		Tour tour = new Tour();
		tour.setName( "Tour 1" );

		Route route = new Route();
		route.setName( "Route 1" );

		ArrayList<Node> nodes = new ArrayList<Node>();
		for ( int i = 0; i < 4; i++ ) {
			Node n = new Node();
			n.setName( "Node " + i );
			n.setTour( tour );
			n.setRoute( route );
			nodes.add( n );
		}

		tour.setNodes( new HashSet<>( nodes ) );
		route.setNodes( new HashSet<>( Arrays.asList( nodes.get( 0 ), nodes.get( 1 ), nodes.get( 2 ) ) ) );

		Vehicle vehicle = new Vehicle();
		vehicle.setName( "Car" );
		route.setVehicles( new HashSet<>( Arrays.asList( vehicle ) ) );
		vehicle.setRoute( route );

		Transport $0to1 = new Transport();
		$0to1.setName( "Transport 0 -> 1" );
		$0to1.setPickupNode( nodes.get( 0 ) );
		$0to1.setDeliveryNode( nodes.get( 1 ) );
		$0to1.setVehicle( vehicle );

		Transport $1to2 = new Transport();
		$1to2.setName( "Transport 1 -> 2" );
		$1to2.setPickupNode( nodes.get( 1 ) );
		$1to2.setDeliveryNode( nodes.get( 2 ) );
		$1to2.setVehicle( vehicle );

		vehicle.setTransports( new HashSet<>( Arrays.asList( $0to1, $1to2 ) ) );

		scope.inTransaction(
				session ->
						session.persist( tour )
		);
	}
}
