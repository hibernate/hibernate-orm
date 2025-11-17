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
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.orm.test.util.SchemaUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				EmbeddedOverrideTests.DomesticCustomer.class,
				EmbeddedOverrideTests.ForeignCustomer.class
		}
)
@ServiceRegistry
@SessionFactory
public class EmbeddedOverrideTests {
	@Test
	@JiraKey(value = "HHH-8630")
	public void testModel(DomainModelScope scope) {
		assertTrue( SchemaUtil.isColumnPresent( "DOMESTIC_CUSTOMER", "DC_address_street", scope.getDomainModel() ) );
		assertTrue( SchemaUtil.isColumnPresent( "FOREIGN_CUSTOMER", "FC_address_street", scope.getDomainModel() ) );

		assertStreetAttributeColumnMapping( DomesticCustomer.class, "DC_address_street", scope.getDomainModel() );
		assertStreetAttributeColumnMapping( ForeignCustomer.class, "FC_address_street", scope.getDomainModel() );
	}

	private void assertStreetAttributeColumnMapping(
			Class customerClass,
			String expectedColumnName,
			MetadataImplementor domainModel) {
		final PersistentClass entityBinding = domainModel.getEntityBinding( customerClass.getName() );

		final Property addressProperty = entityBinding.getProperty( "address" );
		assertThat( addressProperty.getValue(), instanceOf( Component.class ) );
		final Component addressMapping = (Component) addressProperty.getValue();

		final Property streetProperty = addressMapping.getProperty( "street" );
		assertThat( streetProperty.getValue(), instanceOf( BasicValue.class ) );
		final BasicValue streetMapping = (BasicValue) streetProperty.getValue();
		assertThat(
				( (org.hibernate.mapping.Column) streetMapping.getColumn() ).getName(),
				is( expectedColumnName )
		);
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
					assertThat( foreignCustomer.getAddress().getCity(), is( "Roma" ) );
				}
		);
	}

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					DomesticCustomer domestic = new DomesticCustomer(
							"domestic",
							"123",
							new Address( "Kennington road", "London" )
					);
					session.persist( domestic );
					ForeignCustomer foreign = new ForeignCustomer(
							"foreign",
							"987",
							new Address( "Via Milano", "Roma" )
					);
					session.persist( foreign );
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

	@AttributeOverrides(
			value = {
					@AttributeOverride(
							name = "address.street",
							column = @Column(name = "DC_address_street")),
			}
	)
	@Entity(name = "DomesticCustomer")
	@Table(name = "DOMESTIC_CUSTOMER")
	public static class DomesticCustomer {
		@Id
		@GeneratedValue
		private Integer id;
		private String name;
		private String taxId;
		@Embedded
		private Address address;

		public DomesticCustomer() {
		}

		public DomesticCustomer(
				String name,
				String taxId,
				Address address) {
			this.id = id;
			this.name = name;
			this.taxId = taxId;
			this.address = address;
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

		public String getTaxId() {
			return taxId;
		}

		public void setTaxId(String taxId) {
			this.taxId = taxId;
		}

		public Address getAddress() {
			return address;
		}

		public void setAddress(Address address) {
			this.address = address;
		}
	}

	@AttributeOverrides(
			value = {
					@AttributeOverride(
							name = "address.street",
							column = @Column(name = "FC_address_street")),
			}
	)
	@Entity(name = "ForeignCustomer")
	@Table(name = "FOREIGN_CUSTOMER")
	public static class ForeignCustomer {
		@Id
		@GeneratedValue
		private Integer id;
		private String name;
		private String vat;
		private Address address;

		public ForeignCustomer() {
		}

		public ForeignCustomer(
				String name,
				String vat,
				Address address) {
			this.id = id;
			this.name = name;
			this.vat = vat;
			this.address = address;
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

		public String getVat() {
			return vat;
		}

		public void setVat(String vat) {
			this.vat = vat;
		}

		public Address getAddress() {
			return address;
		}

		public void setAddress(Address address) {
			this.address = address;
		}
	}
}
