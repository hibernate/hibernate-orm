/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				EmbeddableWithToOneAssociation2Test.Employee.class,
				EmbeddableWithToOneAssociation2Test.ParkingSpot.class
		}
)
@SessionFactory(
		statementInspectorClass = SQLStatementInspector.class
)
public class EmbeddableWithToOneAssociation2Test {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Employee employee = new Employee( 1, "Fab" );
					ParkingSpot parkingSpot = new ParkingSpot( 1, "park 1", employee );

					ParkingSpot parkingSpot2 = new ParkingSpot( 2, "park 2", null );

					employee.setParkingSpot( parkingSpot2 );

					LocationDetails details = new LocationDetails( 1, parkingSpot );
					employee.setLocation( details );

					session.persist( employee );
					session.persist( parkingSpot );
					session.persist( parkingSpot2 );
				}
		);
	}

	@Test
	public void testGet(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					Employee employee = session.get( Employee.class, 1 );
					assertThat( employee ).isNotNull();
					ParkingSpot parkingSpot = employee.getLocation().getParkingSpot();
					assertThat( parkingSpot ).isNotNull();
					assertThat( parkingSpot.getAssignedTo() ).isEqualTo( employee );
					assertThat( employee.getParkingSpot() ).isNotEqualTo( parkingSpot );

					assertThat( statementInspector.getSqlQueries().size() ).isEqualTo( 1 );
					assertThat( statementInspector.getNumberOfJoins( 0 ) ).isEqualTo( 3 );
				}
		);
		statementInspector.clear();

		scope.inTransaction(
				session -> {
					ParkingSpot parkingSpot = session.get( ParkingSpot.class, 1 );
					assertThat( parkingSpot ).isNotNull();
					Employee employee = parkingSpot.getAssignedTo();
					assertThat( employee ).isNotNull();
					assertThat( employee.getLocation().getParkingSpot() ).isEqualTo( parkingSpot );

					assertThat( employee.getParkingSpot() ).isNotEqualTo( parkingSpot );

					assertThat( statementInspector.getSqlQueries().size() ).isEqualTo( 2 );
					assertThat( statementInspector.getNumberOfJoins( 0 ) ).isEqualTo( 2 );
					assertThat( statementInspector.getNumberOfJoins( 1 ) ).isEqualTo( 3 );
				}
		);
	}

	@Entity(name = "Employee")
	public static class Employee {
		@Id
		int id;

		String name;

		@ManyToOne
		@JoinColumn(name = "parking_spot_id1")
		ParkingSpot parkingSpot;

		@Embedded
		LocationDetails location;

		public Employee() {
		}

		public Employee(int id, String name) {
			this.id = id;
			this.name = name;
		}

		public int getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public LocationDetails getLocation() {
			return location;
		}

		public void setLocation(LocationDetails location) {
			this.location = location;
		}

		public ParkingSpot getParkingSpot() {
			return parkingSpot;
		}

		public void setParkingSpot(ParkingSpot parkingSpot) {
			this.parkingSpot = parkingSpot;
		}
	}

	@Embeddable
	public static class LocationDetails {
		int officeNumber;

		@OneToOne
		@JoinColumn(name = "parking_spot_id2")
		ParkingSpot parkingSpot;

		public LocationDetails() {
		}

		public LocationDetails(int officeNumber, ParkingSpot parkingSpot) {
			this.officeNumber = officeNumber;
			this.parkingSpot = parkingSpot;
		}

		public int getOfficeNumber() {
			return officeNumber;
		}

		public ParkingSpot getParkingSpot() {
			return parkingSpot;
		}
	}

	@Entity(name = "ParkingSpot")
	public static class ParkingSpot {
		@Id
		int id;

		String garage;

		@OneToOne(mappedBy = "location.parkingSpot")
		Employee assignedTo;

		public ParkingSpot() {
		}

		public ParkingSpot(int id, String garage, Employee assignedTo) {
			this.id = id;
			this.garage = garage;
			this.assignedTo = assignedTo;
		}

		public int getId() {
			return id;
		}

		public String getGarage() {
			return garage;
		}

		public Employee getAssignedTo() {
			return assignedTo;
		}
	}
}
