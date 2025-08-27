/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cascade.circle;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
		annotatedClasses = {
				CascadeManagedAndTransientTest.Node.class,
				CascadeManagedAndTransientTest.Route.class,
				CascadeManagedAndTransientTest.Tour.class,
				CascadeManagedAndTransientTest.Transport.class,
				CascadeManagedAndTransientTest.Vehicle.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = Environment.CHECK_NULLABILITY, value = "true")
)
@JiraKey("HHH-9512")
public class CascadeManagedAndTransientTest {

	@AfterEach
	public void cleanupTest(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testAttachedChildInMerge(SessionFactoryScope scope) {
		fillInitialData( scope );

		scope.inTransaction(
				session -> {
					Route route = session.createQuery( "FROM Route WHERE name = :name", Route.class )
							.setParameter( "name", "Route 1" )
							.uniqueResult();
					Node n2 = session.createQuery( "FROM Node WHERE name = :name", Node.class )
							.setParameter( "name", "Node 2" )
							.uniqueResult();
					Node n3 = session.createQuery( "FROM Node WHERE name = :name", Node.class )
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

					vehicle.setTransports( Set.of( $2to3 ) );

					// Try to save graph of transient entities (vehicle, transport) which contains attached entities (node2, node3)
					Vehicle managedVehicle = (Vehicle) session.merge( vehicle );
					checkNewVehicle( managedVehicle );

					session.flush();
					session.clear();

					assertEquals( 3, session.createQuery( "FROM Transport", Transport.class ).list().size() );
					assertEquals( 2, session.createQuery( "FROM Vehicle", Vehicle.class ).list().size() );
					assertEquals( 4, session.createQuery( "FROM Node", Node.class ).list().size() );

					Vehicle newVehicle = session.createQuery( "FROM Vehicle WHERE name = :name", Vehicle.class )
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
		route.setVehicles( Set.of( vehicle ) );
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

	@Entity(name = "Route")
	@Table(name = "HB_Route")
	public static class Route {

		@Id
		@GeneratedValue
		private Long routeID;

		private long version;

		@OneToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH}, mappedBy = "route")
		private Set<Node> nodes = new HashSet<>();

		@OneToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH}, mappedBy = "route")
		private Set<Vehicle> vehicles = new HashSet<>();

		@Basic(optional = false)
		private String name;

		@Transient
		private String transientField = null;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Set<Node> getNodes() {
			return nodes;
		}

		protected void setNodes(Set<Node> nodes) {
			this.nodes = nodes;
		}

		protected Set<Vehicle> getVehicles() {
			return vehicles;
		}

		protected void setVehicles(Set<Vehicle> vehicles) {
			this.vehicles = vehicles;
		}

		protected void setRouteID(Long routeID) {
			this.routeID = routeID;
		}

		public Long getRouteID() {
			return routeID;
		}

		public long getVersion() {
			return version;
		}

		protected void setVersion(long version) {
			this.version = version;
		}

		public String getTransientField() {
			return transientField;
		}

		public void setTransientField(String transientField) {
			this.transientField = transientField;
		}
	}

	@Entity(name = "Tour")
	@Table(name = "HB_Tour")
	public static class Tour {

		@Id
		@GeneratedValue
		private Long tourID;

		@Version
		private long version;

		@Basic(optional = false)
		private String name;

		@OneToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH}, mappedBy = "tour")
		private Set<Node> nodes = new HashSet<>( 0 );

		public String getName() {
			return name;
		}

		protected void setTourID(Long tourID) {
			this.tourID = tourID;
		}

		public long getVersion() {
			return version;
		}

		protected void setVersion(long version) {
			this.version = version;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Set<Node> getNodes() {
			return nodes;
		}

		public void setNodes(Set<Node> nodes) {
			this.nodes = nodes;
		}

		public Long getTourID() {
			return tourID;
		}
	}

	@Entity(name = "Transport")
	@Table(name = "HB_Transport")
	public static class Transport {

		@Id
		@GeneratedValue
		private Long transportID;

		@Version
		private long version;

		@Basic(optional = false)
		private String name;

		@ManyToOne(
				optional = false,
				cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH},
				fetch = FetchType.EAGER)
		@JoinColumn(name = "pickupNodeID", nullable = false)
		private Node pickupNode = null;

		@ManyToOne(
				optional = false,
				cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH},
				fetch = FetchType.EAGER)
		@JoinColumn(name = "deliveryNodeID", nullable = false)
		private Node deliveryNode = null;

		@ManyToOne(
				optional = false,
				cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH},
				fetch = FetchType.EAGER
		)
		private Vehicle vehicle;

		@Transient
		private String transientField = "transport original value";

		public Node getDeliveryNode() {
			return deliveryNode;
		}

		public void setDeliveryNode(Node deliveryNode) {
			this.deliveryNode = deliveryNode;
		}

		public Node getPickupNode() {
			return pickupNode;
		}

		protected void setTransportID(Long transportID) {
			this.transportID = transportID;
		}

		public void setPickupNode(Node pickupNode) {
			this.pickupNode = pickupNode;
		}

		public Vehicle getVehicle() {
			return vehicle;
		}

		public void setVehicle(Vehicle vehicle) {
			this.vehicle = vehicle;
		}

		public Long getTransportID() {
			return transportID;
		}

		public long getVersion() {
			return version;
		}

		protected void setVersion(long version) {
			this.version = version;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getTransientField() {
			return transientField;
		}

		public void setTransientField(String transientField) {
			this.transientField = transientField;
		}
	}

	@Entity(name = "Vehicle")
	@Table(name = "HB_Vehicle")
	public static class Vehicle {

		@Id
		@GeneratedValue
		private Long vehicleID;

		@Version
		private long version;

		@Basic(optional = false)
		private String name;

		@OneToMany(
				cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH},
				fetch = FetchType.EAGER
		)
		private Set<Transport> transports = new HashSet<>();

		@ManyToOne(
				cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH},
				fetch = FetchType.EAGER
		)
		private Route route;

		@Transient
		private String transientField = "vehicle original value";

		protected void setVehicleID(Long vehicleID) {
			this.vehicleID = vehicleID;
		}

		public Long getVehicleID() {
			return vehicleID;
		}

		public long getVersion() {
			return version;
		}

		protected void setVersion(long version) {
			this.version = version;
		}

		public Set<Transport> getTransports() {
			return transports;
		}

		public void setTransports(Set<Transport> transports) {
			this.transports = transports;
		}

		public Route getRoute() {
			return route;
		}

		public void setRoute(Route route) {
			this.route = route;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getTransientField() {
			return transientField;
		}

		public void setTransientField(String transientField) {
			this.transientField = transientField;
		}
	}

	@Entity(name = "Node")
	@Table(name = "HB_Node")
	public static class Node {

		@Id
		@GeneratedValue
		private Long nodeID;

		@Version
		private long version;

		@Basic(optional = false)
		private String name;

		@OneToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH}, mappedBy = "deliveryNode")
		private Set<Transport> deliveryTransports = new HashSet<>();

		@OneToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH}, mappedBy = "pickupNode")
		private Set<Transport> pickupTransports = new HashSet<>();

		@ManyToOne(
				optional = false,
				cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH},
				fetch = FetchType.EAGER
		)
		private Route route = null;

		@ManyToOne(
				optional = false,
				cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH},
				fetch = FetchType.EAGER
		)
		private Tour tour;

		@Transient
		private String transientField = "node original value";

		public Set<Transport> getDeliveryTransports() {
			return deliveryTransports;
		}

		public void setDeliveryTransports(Set<Transport> deliveryTransports) {
			this.deliveryTransports = deliveryTransports;
		}

		public Set<Transport> getPickupTransports() {
			return pickupTransports;
		}

		public void setPickupTransports(Set<Transport> pickupTransports) {
			this.pickupTransports = pickupTransports;
		}

		public Long getNodeID() {
			return nodeID;
		}

		public long getVersion() {
			return version;
		}

		protected void setVersion(long version) {
			this.version = version;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Route getRoute() {
			return route;
		}

		public void setRoute(Route route) {
			this.route = route;
		}

		public Tour getTour() {
			return tour;
		}

		public void setTour(Tour tour) {
			this.tour = tour;
		}

		public String getTransientField() {
			return transientField;
		}

		public void setTransientField(String transientField) {
			this.transientField = transientField;
		}

		protected void setNodeID(Long nodeID) {
			this.nodeID = nodeID;
		}

	}
}
