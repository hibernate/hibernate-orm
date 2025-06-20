/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.attributeOverrides;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Table;

import org.hibernate.boot.spi.MetadataImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.orm.test.util.SchemaUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				MappedSuperclassOverrideTests.Customer.class,
				MappedSuperclassOverrideTests.DomesticCustomer.class,
				MappedSuperclassOverrideTests.ForeignCustomer.class
		}
)
@ServiceRegistry
@SessionFactory
public class MappedSuperclassOverrideTests {

	@Test
	public void testSchema(SessionFactoryScope scope) {
		MetadataImplementor metadata = scope.getMetadataImplementor();
		assertTrue( SchemaUtil.isColumnPresent( "DOMESTIC_CUSTOMER", "DC_address_street", metadata ) );
		assertTrue( SchemaUtil.isColumnPresent( "FOREIGN_CUSTOMER", "STREET", metadata ) );
	}

	@Test
	public void testQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					DomesticCustomer domesticCustomer = session.createQuery(
							"from DomesticCustomer c",
							DomesticCustomer.class
					).getSingleResult();
					assertThat( domesticCustomer.getName(), is( "domestic" ) );
					assertThat( domesticCustomer.getAddress().getCity(), is( "London" ) );

					ForeignCustomer foreignCustomer = session.createQuery(
							"from ForeignCustomer c",
							ForeignCustomer.class
					).getSingleResult();
					assertThat( foreignCustomer.getName(), is( "foreign" ) );
					assertThat( foreignCustomer.getAddress(), is( nullValue() ) );
				}
		);
	}

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Address address = new Address( "Kennington road", "London" );
					DomesticCustomer domestic = new DomesticCustomer( 1, "domestic", "123" );
					domestic.setAddress( address );
					session.persist( domestic );
					session.persist( new ForeignCustomer( 2, "foreign", "987" ) );
				}
		);
	}

	@AfterEach
	public void cleanupTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Embeddable
	public static class Address {
		private String street;
		private String city;

		public Address() {
		}

		public Address(String street, String city) {
			this.street = street;
			this.city = city;
		}

		@Column(name = "STREET")
		public String getStreet() {
			return street;
		}

		public void setStreet(String street) {
			this.street = street;
		}

		public String getCity() {
			return city;
		}

		public void setCity(String city) {
			this.city = city;
		}
	}


	@MappedSuperclass
	public static abstract class Customer {
		private Integer id;
		private String name;
		private Address address;

		public Customer() {
		}

		public Customer(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Embedded
		public Address getAddress() {
			return address;
		}

		public void setAddress(Address address) {
			this.address = address;
		}
	}

	@Entity(name = "DomesticCustomer")
	@AttributeOverrides(
			value = {
					@AttributeOverride(
							name = "name",
							column = @Column(name = "DC_name")),
					@AttributeOverride(
							name = "address.street",
							column = @Column(name = "DC_address_street")),
			}
	)
	@Table(name = "DOMESTIC_CUSTOMER")
	public static class DomesticCustomer extends Customer {
		private String taxId;

		public DomesticCustomer() {
		}

		public DomesticCustomer(Integer id, String name, String taxId) {
			super( id, name );
			this.taxId = taxId;
		}


		public String getTaxId() {
			return taxId;
		}

		public void setTaxId(String taxId) {
			this.taxId = taxId;
		}
	}

	@Entity(name = "ForeignCustomer")
	@Table(name = "FOREIGN_CUSTOMER")
	public static class ForeignCustomer extends Customer {
		private String vat;

		public ForeignCustomer() {
		}

		public ForeignCustomer(Integer id, String name, String vat) {
			super( id, name );
			this.vat = vat;
		}

		public String getVat() {
			return vat;
		}

		public void setVat(String vat) {
			this.vat = vat;
		}
	}
}
