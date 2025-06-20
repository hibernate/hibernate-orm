/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance.join;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel( annotatedClasses = {
		ReloadMultipleCollectionElementsTest.Flight.class,
		ReloadMultipleCollectionElementsTest.Ticket.class,
		ReloadMultipleCollectionElementsTest.Customer.class,
		ReloadMultipleCollectionElementsTest.Company.class
} )
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-18493")
public class ReloadMultipleCollectionElementsTest {

	@BeforeEach
	public void setup(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Flight f2 = new Flight();
			f2.setId(1L);
			f2.setName("Flight two");

			Company us = new Company();
			us.setName("Airline 2");
			f2.setCompany(us);

			Customer c1 = new Customer();
			c1.setId( 1L );
			c1.setName("Tom");

			Customer c2 = new Customer();
			c2.setId( 2L );
			c2.setName("Pete");

			Ticket t1 = new Ticket();
			t1.setId(1L);
			t1.setCustomer(c2);
			t1.setNumber( "123" );

			Ticket t2 = new Ticket();
			t2.setId(2L);
			t2.setCustomer(c2);
			t2.setNumber( "456" );

			f2.setCustomers(Set.of(c1, c2));

			s.persist(c1);
			s.persist(c2);
			s.persist(f2);
			s.persist(t1);
			s.persist(t2);
		} );
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testResolveElementOfInitializedCollection(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			// First load all customers with their flights collection and corresponding customers
			List<Customer> customers = s.createQuery(
					"from Customer c join fetch c.flights f join fetch f.customers order by c.id",
					Customer.class
			).getResultList();
			assertEquals( 2, customers.size() );
			assertFalse( Hibernate.isInitialized( customers.get( 0 ).getTickets() ) );
			assertFalse( Hibernate.isInitialized( customers.get( 1 ).getTickets() ) );

			// Then load all flights with their customers collection, but in addition, also the customers tickets
			// This will trigger resolveInstance(Object, Data) with the existing collection and will
			// fetch tickets data into existing customers
			s.createQuery( "from Flight f join fetch f.customers c left join fetch c.tickets", Flight.class ).getResultList();

			assertTrue( Hibernate.isInitialized( customers.get( 0 ).getTickets() ) );
			assertTrue( Hibernate.isInitialized( customers.get( 1 ).getTickets() ) );
			assertEquals( 0, customers.get( 0 ).getTickets().size() );
			assertEquals( 2, customers.get( 1 ).getTickets().size() );
		} );
	}

	@Entity( name = "Flight" )
	public static class Flight {
		private Long id;
		private String name;
		private Company company;
		private Set<Customer> customers;

		public Flight() {
		}

		@Id
		@Column(name = "flight_id")
		public Long getId() {
			return id;
		}

		public void setId(Long long1) {
			id = long1;
		}

		@Column(updatable = false, name = "flight_name", nullable = false, length = 50)
		public String getName() {
			return name;
		}

		public void setName(String string) {
			name = string;
		}


		@ManyToOne(cascade = {CascadeType.ALL})
		@JoinColumn(name = "comp_id")
		public Company getCompany() {
			return company;
		}

		public void setCompany(Company company) {
			this.company = company;
		}

		@ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER)
		public Set<Customer> getCustomers() {
			return customers;
		}

		public void setCustomers(Set<Customer> customers) {
			this.customers = customers;
		}
	}

	@Entity( name = "Ticket" )
	public static class Ticket {

		Long id;
		String number;
		Customer customer;

		public Ticket() {
		}

		@Id
		public Long getId() {
			return id;
		}

		public void setId(Long long1) {
			id = long1;
		}

		@Column(name = "nr")
		public String getNumber() {
			return number;
		}

		public void setNumber(String string) {
			number = string;
		}

		@ManyToOne(cascade = CascadeType.ALL)
		@JoinColumn(name = "CUSTOMER_ID")
		public Customer getCustomer() {
			return customer;
		}

		public void setCustomer(Customer customer) {
			this.customer = customer;
		}
	}

	@Entity( name = "Customer" )
	public static class Customer {
		private Long id;
		private String name;
		private String address;
		private Set<Ticket> tickets;
		private Set<Flight> flights;

		// Address address;

		public Customer() {
		}

		@Id
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

		public String getAddress() {
			return address;
		}

		public void setAddress(String address) {
			this.address = address;
		}

		@OneToMany(cascade = CascadeType.ALL, mappedBy = "customer")
		public Set<Ticket> getTickets() {
			return tickets;
		}

		public void setTickets(Set<Ticket> tickets) {
			this.tickets = tickets;
		}

		@ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, mappedBy = "customers")
		public Set<Flight> getFlights() {
			return flights;
		}

		public void setFlights(Set<Flight> flights) {
			this.flights = flights;
		}
	}

	@Entity( name = "Company" )
	public static class Company {

		private Long id;
		private String name;
		private Set<Flight> flights = new HashSet<Flight>();

		public Company() {
		}

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		@Column(name = "comp_id")
		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setId(Long newId) {
			id = newId;
		}

		public void setName(String string) {
			name = string;
		}

		@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "company")
		@Column(name = "flight_id")
		public Set<Flight> getFlights() {
			return flights;
		}

		public void setFlights(Set<Flight> flights) {
			this.flights = flights;
		}
	}
}
