/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.association.toone;

import java.util.List;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@Jpa(
		annotatedClasses = {
				CriteriaJoinFetchTest.Customer.class,
				CriteriaJoinFetchTest.Address.class,
				CriteriaJoinFetchTest.Note.class
		},
		useCollectingStatementInspector = true
)
public class CriteriaJoinFetchTest {

	private static SQLStatementInspector statementInspector;

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		statementInspector = scope.getCollectingStatementInspector();

		scope.inTransaction(
				entityManager -> {

					Customer customer0 = new Customer( 1, "William P. Keaton" );
					Customer customer1 = new Customer( 2, "Kate P. Hudson" );

					entityManager.persist( customer0 );
					entityManager.persist( customer1 );

					Note note0 = new Note( 3, "Note for address 0" );

					Note note1 = new Note( 4, "Note for address 1" );

					Address address0 = new Address( 5, "Flit street", "London", note0, customer0 );
					Address address1 = new Address( 6, "via Marconi", "Pavia", note1, customer1 );

					entityManager.persist( address0 );
					entityManager.persist( address1 );


					customer0.setAddress( address0 );
					customer1.setAddress( address1 );
				}
		);
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testCriteriaFetchSingularAttribute(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					statementInspector.clear();
					final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
					final CriteriaQuery<Customer> criteriaQuery = criteriaBuilder.createQuery( Customer.class );

					final From<Customer, Customer> customer = criteriaQuery.from( Customer.class );

					final EntityType<Customer> customerEntityType = entityManager.getMetamodel()
							.entity( Customer.class );

					final SingularAttribute<? super Customer, Address> address = (SingularAttribute<? super Customer, Address>) customerEntityType.getSingularAttribute(
							"address" );
					customer.fetch( address, JoinType.INNER );
					criteriaQuery.select( customer );

					final TypedQuery<Customer> query = entityManager.createQuery( criteriaQuery );
					List<Customer> result = query.getResultList();
					assertThat( result.size(), is( 2 ) );
					assertThat( statementInspector.getSqlQueries().size(), is( 3 ) );
					Customer customer1 = result.get( 0 );
					Note note = customer1.getAddress().getNote();
					assertThat( note, notNullValue() );
					if ( customer1.getId() == 1 ) {
						assertThat( note.getId(), is( 3 ) );
					}
					else {
						assertThat( note.getId(), is( 4 ) );
					}
				}
		);
	}

	@Test
	public void testCriteriaFetchSingularAttribute2(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					statementInspector.clear();
					final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
					final CriteriaQuery<Customer> criteriaQuery = criteriaBuilder.createQuery( Customer.class );

					final From<Customer, Customer> customer = criteriaQuery.from( Customer.class );

					final EntityType<Customer> customerEntityType = entityManager.getMetamodel()
							.entity( Customer.class );

					final SingularAttribute<? super Customer, Address> address = (SingularAttribute<? super Customer, Address>) customerEntityType.getSingularAttribute(
							"address" );
					final Fetch<Customer, Address> fetch = customer.fetch( address, JoinType.INNER );

					fetch.fetch( entityManager.getMetamodel()
										.entity( Address.class ).getSingularAttribute( "note" ), JoinType.INNER );
					criteriaQuery.select( customer );

					final TypedQuery<Customer> query = entityManager.createQuery( criteriaQuery );

					final List<Customer> result = query.getResultList();

					assertThat( result.size(), is( 2 ) );
					assertThat( statementInspector.getSqlQueries().size(), is( 1 ) );

					final Customer customer1 = result.get( 0 );
					final Note note = customer1.getAddress().getNote();

					assertThat( note, notNullValue() );
					if ( customer1.getId() == 1 ) {
						assertThat( note.getId(), is( 3 ) );
					}
					else {
						assertThat( note.getId(), is( 4 ) );
					}
					assertThat( statementInspector.getSqlQueries().size(), is( 1 ) );
				}
		);
	}


	@Test
	public void testFind(EntityManagerFactoryScope scope) {
		statementInspector.clear();
		scope.inTransaction(
				entityManager -> {
					Customer customer = entityManager.find( Customer.class, 2 );
					final Note note = customer.getAddress().getNote();

					assertThat( note.getId(), is( 4 ) );
					assertThat( statementInspector.getSqlQueries().size(), is( 1 ) );
				}
		);
	}

	@Entity(name = "Customer")
	@Table(name = "CUSTOMER_TABLE")
	public static class Customer {

		@Id
		private Integer id;

		private String name;

		@OneToOne(cascade = CascadeType.ALL, mappedBy = "customer")
		private Address address;

		public Customer() {
		}

		public Customer(Integer id, String name) {
			this.id = id;
			this.name = name;
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

		public Address getAddress() {
			return address;
		}

		public void setAddress(Address address) {
			this.address = address;
		}
	}


	@Entity(name = "Address")
	@Table(name = "ADDRESS_TABLE")
	public static class Address {

		@Id
		private Integer id;

		private String street;

		private String city;

		@OneToOne(cascade = CascadeType.ALL)
		@JoinColumn(name = "NOTE_FK")
		private Note note;

		@OneToOne(cascade = CascadeType.ALL)
		@JoinColumn(name = "CUSTOMER_FK")
		private Customer customer;

		public Address() {
		}

		public Address(
				Integer id,
				String street,
				String city,
				Note note,
				Customer customer) {
			this.id = id;
			this.street = street;
			this.city = city;
			this.note = note;
			this.note.setAddress( this );
			this.customer = customer;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
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

		public Note getNote() {
			return note;
		}

		public void setNote(Note note) {
			this.note = note;
		}

		public Customer getCustomer() {
			return customer;
		}

		public void setCustomer(Customer customer) {
			this.customer = customer;
		}
	}

	@Entity(name = "Note")
	@Table(name = "NOTE_TABLE")
	public static class Note {

		@Id
		private Integer id;

		private String line;

		@OneToOne(mappedBy = "note")
		private Address address;

		public Note() {
		}

		public Note(Integer id, String line) {
			this.id = id;
			this.line = line;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}


		public Address getAddress() {
			return address;
		}

		public void setAddress(Address address) {
			this.address = address;
		}
	}

}
