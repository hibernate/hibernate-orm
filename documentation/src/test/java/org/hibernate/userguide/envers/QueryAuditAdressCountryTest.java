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

import org.hibernate.test.legacy.Custom;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
			//tag::envers-querying-entity-relation-nested-join-restriction[]
			List<Customer> customers = AuditReaderFactory
			.get( entityManager )
			.createQuery()
			.forEntitiesAtRevision( Customer.class, 1 )
			.traverseRelation( "address", JoinType.INNER )
			.traverseRelation( "country", JoinType.INNER )
			.add( AuditEntity.property( "name" ).eq( "România" ) )
			.getResultList();

			assertEquals( 1, customers.size() );
			//end::envers-querying-entity-relation-nested-join-restriction[]
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::envers-querying-entity-relation-join-multiple-restrictions[]
			List<Customer> customers = AuditReaderFactory
			.get( entityManager )
			.createQuery()
			.forEntitiesAtRevision( Customer.class, 1 )
			.traverseRelation( "address", JoinType.LEFT, "a" )
			.add(
				AuditEntity.or(
					AuditEntity.property( "a", "city" ).eq( "Cluj-Napoca" ),
					AuditEntity.relatedId( "country" ).eq( null )
				)
			)
			.getResultList();
			//end::envers-querying-entity-relation-join-multiple-restrictions[]

			assertEquals( 1, customers.size() );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::envers-querying-entity-relation-nested-join-multiple-restrictions[]
			List<Customer> customers = AuditReaderFactory
			.get( entityManager )
			.createQuery()
			.forEntitiesAtRevision( Customer.class, 1 )
			.traverseRelation( "address", JoinType.INNER, "a" )
			.traverseRelation( "country", JoinType.INNER, "cn" )
			.up()
			.up()
			.add(
				AuditEntity.disjunction()
				.add( AuditEntity.property( "a", "city" ).eq( "Cluj-Napoca" ) )
				.add( AuditEntity.property( "cn", "name" ).eq( "România" ) )
			)
			.addOrder( AuditEntity.property( "createdOn" ).asc() )
			.getResultList();
			//end::envers-querying-entity-relation-nested-join-multiple-restrictions[]

			assertEquals( 1, customers.size() );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::envers-querying-entity-relation-nested-join-multiple-restrictions-combined-entities[]
			Customer customer = entityManager.createQuery(
				"select c " +
				"from Customer c " +
				"join fetch c.address a " +
				"join fetch a.country " +
				"where c.id = :id", Customer.class )
			.setParameter( "id", 1L )
			.getSingleResult();

			customer.setLastName( "Doe Sr." );

			customer.getAddress().setCity(
				customer.getAddress().getCountry().getName()
			);
			//end::envers-querying-entity-relation-nested-join-multiple-restrictions-combined-entities[]
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {

			//tag::envers-querying-entity-relation-nested-join-multiple-restrictions-combined[]
			List<Number> revisions = AuditReaderFactory.get( entityManager ).getRevisions(
				Customer.class,
				1L
			);

			List<Customer> customers = AuditReaderFactory
			.get( entityManager )
			.createQuery()
			.forEntitiesAtRevision( Customer.class, revisions.get( revisions.size() - 1 ) )
			.traverseRelation( "address", JoinType.INNER, "a" )
			.traverseRelation( "country", JoinType.INNER, "cn" )
			.up()
			.up()
			.add( AuditEntity.property( "a", "city" ).eqProperty( "cn", "name" ) )
			.getResultList();
			//end::envers-querying-entity-relation-nested-join-multiple-restrictions-combined[]

			assertEquals( 1, customers.size() );
		} );

	}

	//tag::envers-generateschema-example[]
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

		//Getters and setters omitted for brevity
	//end::envers-generateschema-example[]

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
	//tag::envers-generateschema-example[]
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

		//Getters and setters omitted for brevity
	//end::envers-generateschema-example[]
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
	//tag::envers-generateschema-example[]
	}

	@Audited
	@Entity(name = "Country")
	public static class Country {

		@Id
		private Long id;

		private String name;

		//Getters and setters omitted for brevity
	//end::envers-generateschema-example[]

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
	//tag::envers-generateschema-example[]
	}
	//end::envers-generateschema-example[]
}
