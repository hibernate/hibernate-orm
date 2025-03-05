/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.joincolumn.embedded;

import java.io.Serializable;

import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinColumnsOrFormulas;
import org.hibernate.annotations.JoinFormula;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel(annotatedClasses = {
		CharArrayToStringInEmbeddedJoinColumnOrFormulaTest.Vehicle.class,
		CharArrayToStringInEmbeddedJoinColumnOrFormulaTest.VehicleInvoice.class
})
@JiraKey("HHH-15916")
public class CharArrayToStringInEmbeddedJoinColumnOrFormulaTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Vehicle vehicle = new Vehicle();
			vehicle.setId( 1L );
			vehicle.setStringProp1( "VO" );
			vehicle.setStringProp2( "2020" );
			session.persist( vehicle );
			VehicleInvoice invoice = new VehicleInvoice();
			invoice.setId( new VehicleInvoiceId( "VO".toCharArray(), "2020".toCharArray() ) );
			invoice.setVehicle( vehicle );
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
			assertEquals( "VO", vehicleInvoice.getVehicle().getStringProp1() );
			assertEquals( "2020", vehicleInvoice.getVehicle().getStringProp2() );
		} );
	}

	@Embeddable
	public static class VehicleInvoiceId implements Serializable {
		@Column(name = "char_array_col_1")
		private char[] charArrayProp1;

		@Column(name = "char_array_col_2")
		private char[] charArrayProp2;

		public VehicleInvoiceId() {
		}

		public VehicleInvoiceId(char[] charArrayProp1, char[] charArrayProp2) {
			this.charArrayProp1 = charArrayProp1;
			this.charArrayProp2 = charArrayProp2;
		}
	}

	@Entity(name = "VehicleInvoice")
	public static class VehicleInvoice {
		@EmbeddedId
		private VehicleInvoiceId id;

		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumnsOrFormulas({
				@JoinColumnOrFormula(formula = @JoinFormula(value = "char_array_col_1", referencedColumnName = "string_col_1")),
				@JoinColumnOrFormula(column = @JoinColumn(name = "char_array_col_2", referencedColumnName = "string_col_2", insertable = false, updatable = false))
		})
		private Vehicle vehicle;

		public VehicleInvoiceId getId() {
			return id;
		}

		public void setId(VehicleInvoiceId id) {
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

		@Column(name = "string_col_1", nullable = false)
		private String stringProp1;

		@Column(name = "string_col_2", nullable = false)
		private String stringProp2;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getStringProp1() {
			return stringProp1;
		}

		public void setStringProp1(String stringProp1) {
			this.stringProp1 = stringProp1;
		}

		public String getStringProp2() {
			return stringProp2;
		}

		public void setStringProp2(String stringProp2) {
			this.stringProp2 = stringProp2;
		}
	}
}
