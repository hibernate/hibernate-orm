/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.joincolumn;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel(annotatedClasses = {
		StringToCharArrayJoinColumnTest.Vehicle.class,
		StringToCharArrayJoinColumnTest.VehicleInvoice.class
})
@JiraKey("HHH-16040")
public class StringToCharArrayJoinColumnTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Vehicle vehicle = new Vehicle();
			vehicle.setId( 1L );
			vehicle.setCharArrayProp( "2020".toCharArray() );
			session.persist( vehicle );
			VehicleInvoice invoice = new VehicleInvoice();
			invoice.setId( "2020" );
			invoice.setVehicle( vehicle );
			vehicle.getInvoices().add( invoice );
			session.persist( invoice );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from VehicleInvoice" ).executeUpdate();
			session.createMutationQuery( "delete from Vehicle" ).executeUpdate();
		} );
	}

	@Test
	public void testAssociation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final VehicleInvoice vehicleInvoice = session.createQuery(
					"from VehicleInvoice",
					VehicleInvoice.class
			).getSingleResult();
			assertEquals( 1L, vehicleInvoice.getVehicle().getId() );
			assertEquals( "2020", new String( vehicleInvoice.getVehicle().getCharArrayProp() ) );
		} );
	}

	@Test
	public void testInverse(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Vehicle vehicle = session.createQuery(
					"from Vehicle",
					Vehicle.class
			).getSingleResult();
			assertEquals( 1, vehicle.getInvoices().size() );
			assertEquals( "2020", vehicle.getInvoices().get( 0 ).getId() );
		} );
	}


	@Entity(name = "VehicleInvoice")
	public static class VehicleInvoice {
		@Id
		@Column(name = "string_col")
		private String id;

		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumn(name = "string_col", referencedColumnName = "char_array_col", insertable = false, updatable = false)
		private Vehicle vehicle;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public Vehicle getVehicle() {
			return vehicle;
		}

		public void setVehicle(Vehicle vehicle) {
			this.vehicle = vehicle;
		}
	}

	@Entity(name = "Vehicle")
	public static class Vehicle implements Serializable {
		@Id
		private Long id;

		@Column(name = "char_array_col", nullable = false)
		private char[] charArrayProp;

		@OneToMany(mappedBy = "vehicle")
		private List<VehicleInvoice> invoices;

		public Vehicle() {
			this.invoices = new ArrayList<>();
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public char[] getCharArrayProp() {
			return charArrayProp;
		}

		public void setCharArrayProp(char[] charArrayProp) {
			this.charArrayProp = charArrayProp;
		}

		public List<VehicleInvoice> getInvoices() {
			return invoices;
		}

		public void setInvoices(List<VehicleInvoice> invoices) {
			this.invoices = invoices;
		}
	}
}
