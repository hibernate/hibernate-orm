/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.envers;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.criteria.JoinType;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.strategy.ValidityAuditStrategy;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class QueryAuditAdressCountryTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Customer.class,
			Address.class,
			Country.class
		};
	}

	@Override
	protected void addConfigOptions(Map options) {
		options.put(
			EnversSettings.AUDIT_STRATEGY,
			ValidityAuditStrategy.class.getName()
		);
	}

	@Test
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Country country = new Country();
			country.setId( 1L );
			country.setName( "România" );
			entityManager.persist( country );

			Address address = new Address();
			address.setId( 1L );
			address.setCountry( country );
			address.setCity( "Cluj-Napoca" );
			address.setStreet( "Bulevardul Eroilor" );
			address.setStreetNumber( "1 A" );
			entityManager.persist( address );

			Customer customer = new Customer();
			customer.setId( 1L );
			customer.setFirstName( "John" );
			customer.setLastName( "Doe" );
			customer.setAddress( address );

			entityManager.persist( customer );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Customer customer = entityManager.find( Customer.class, 1L );
			customer.setLastName( "Doe Jr." );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Customer customer = entityManager.getReference( Customer.class, 1L );
			entityManager.remove( customer );
		} );

		List<Number> revisions = doInJPA( this::entityManagerFactory, entityManager -> {
			 return AuditReaderFactory.get( entityManager ).getRevisions(
				Customer.class,
				1L
			);
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::envers-querying-entity-relation-nested-join-restriction[]
			Customer customer = (Customer) AuditReaderFactory
			.get( entityManager )
			.createQuery()
			.forEntitiesAtRevision( Customer.class, 1 )
			.traverseRelation( "address", JoinType.INNER )
			.traverseRelation( "country", JoinType.INNER )
			.add( AuditEntity.property( "name" ).eq( "România" ) )
			.getSingleResult();
			//end::envers-querying-entity-relation-nested-join-restriction[]
		} );
	}

	@Audited
	@Entity(name = "Customer")
	public static class Customer {

		@Id
		private Long id;

		private String firstName;

		private String lastName;

		@Temporal( TemporalType.TIMESTAMP )
		@Column(name = "created_on")
		@CreationTimestamp
		private Date createdOn;

		@ManyToOne(fetch = FetchType.LAZY)
		private Address address;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}

		public Date getCreatedOn() {
			return createdOn;
		}

		public void setCreatedOn(Date createdOn) {
			this.createdOn = createdOn;
		}

		public Address getAddress() {
			return address;
		}

		public void setAddress(Address address) {
			this.address = address;
		}
	}

	@Audited
	@Entity(name = "Address")
	public static class Address {

		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		private Country country;

		private String city;

		private String street;

		private String streetNumber;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Country getCountry() {
			return country;
		}

		public void setCountry(Country country) {
			this.country = country;
		}

		public String getCity() {
			return city;
		}

		public void setCity(String city) {
			this.city = city;
		}

		public String getStreet() {
			return street;
		}

		public void setStreet(String street) {
			this.street = street;
		}

		public String getStreetNumber() {
			return streetNumber;
		}

		public void setStreetNumber(String streetNumber) {
			this.streetNumber = streetNumber;
		}
	}

	@Audited
	@Entity(name = "Country")
	public static class Country {

		@Id
		private Long id;

		private String name;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
