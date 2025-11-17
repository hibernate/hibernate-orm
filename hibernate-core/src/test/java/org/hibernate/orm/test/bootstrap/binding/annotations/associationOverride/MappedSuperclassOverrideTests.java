/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.associationOverride;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Table;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
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
				MappedSuperclassOverrideTests.ForeignCustomer.class,
				MappedSuperclassOverrideTests.Address.class
		}
)
@ServiceRegistry
@SessionFactory
public class MappedSuperclassOverrideTests {

	@Test
	public void testMapping(DomainModelScope scope) {
		assertTrue( SchemaUtil.isColumnPresent( "DOMESTIC_CUSTOMER", "dc_home_addr_id", scope.getDomainModel() ) );
		assertTrue( SchemaUtil.isColumnPresent( "FOREIGN_CUSTOMER", "fc_home_addr_id", scope.getDomainModel() ) );
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
					assertThat( domesticCustomer.getHomeAddress().getCity(), is( "London" ) );
					assertThat( domesticCustomer.getWorkAddress(), nullValue() );

					ForeignCustomer foreignCustomer = session.createQuery(
							"from ForeignCustomer c",
							ForeignCustomer.class
					).getSingleResult();
					assertThat( foreignCustomer.getName(), is( "foreign" ) );
					assertThat( foreignCustomer.getHomeAddress().getCity(), is( "London" ) );
					assertThat( foreignCustomer.getWorkAddress(), nullValue() );
				}
		);
	}

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Address address = new Address( 1, "Kennington road", "London" );

					final DomesticCustomer domestic = new DomesticCustomer(
							1,
							"domestic",
							address,
							null,
							"123"
					);

					final ForeignCustomer foreign = new ForeignCustomer(
							1,
							"foreign",
							address,
							null,
							"123"
					);

					session.persist( domestic );
					session.persist( foreign );
				}
		);
	}

	@AfterEach
	public void cleanupTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity( name = "Address" )
	public static class Address {
		@Id
		private Integer id;
		private String street;
		private String city;

		public Address() {
		}

		public Address(Integer id, String street, String city) {
			this.id = id;
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
		@Id
		private Integer id;
		private String name;
		@ManyToOne( cascade = CascadeType.ALL )
		@JoinColumn
		private Address homeAddress;
		@ManyToOne( cascade = CascadeType.ALL )
		@JoinColumn
		private Address workAddress;

		public Customer() {
		}

		public Customer(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Customer(
				Integer id,
				String name,
				Address homeAddress,
				Address workAddress) {
			this.id = id;
			this.name = name;
			this.homeAddress = homeAddress;
			this.workAddress = workAddress;
		}

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

		public Address getHomeAddress() {
			return homeAddress;
		}

		public void setHomeAddress(Address homeAddress) {
			this.homeAddress = homeAddress;
		}

		public Address getWorkAddress() {
			return workAddress;
		}

		public void setWorkAddress(Address workAddress) {
			this.workAddress = workAddress;
		}

	}

	@Entity(name = "DomesticCustomer")
	@Table(name = "DOMESTIC_CUSTOMER")
	@AssociationOverride( name = "homeAddress", joinColumns = @JoinColumn( name = "dc_home_addr_id") )
	@AssociationOverride( name = "workAddress", joinColumns = @JoinColumn( name = "dc_work_addr_id") )
	public static class DomesticCustomer extends Customer {
		private String taxId;

		public DomesticCustomer() {
		}

		public DomesticCustomer(Integer id, String name, String taxId) {
			super( id, name );
			this.taxId = taxId;
		}

		public DomesticCustomer(
				Integer id,
				String name,
				Address homeAddress,
				Address workAddress,
				String taxId) {
			super( id, name, homeAddress, workAddress );
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
	@AssociationOverride( name = "homeAddress", joinColumns = @JoinColumn( name = "fc_home_addr_id") )
	@AssociationOverride( name = "workAddress", joinColumns = @JoinColumn( name = "fc_work_addr_id") )
	public static class ForeignCustomer extends Customer {
		private String vat;

		public ForeignCustomer() {
		}

		public ForeignCustomer(Integer id, String name, String vat) {
			super( id, name );
			this.vat = vat;
		}

		public ForeignCustomer(
				Integer id,
				String name,
				Address homeAddress,
				Address workAddress,
				String vat) {
			super( id, name, homeAddress, workAddress );
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
