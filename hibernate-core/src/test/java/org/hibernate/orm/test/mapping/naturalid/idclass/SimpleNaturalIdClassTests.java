/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.naturalid.idclass;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.KeyType;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdClass;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {
		SimpleNaturalIdClassTests.OrderKey.class,
		SimpleNaturalIdClassTests.Customer.class,
		SimpleNaturalIdClassTests.Order.class,
		SimpleNaturalIdClassTests.SystemUserKey.class,
		SimpleNaturalIdClassTests.User.class,
		SimpleNaturalIdClassTests.SystemUser.class
})
@Jira( "https://hibernate.atlassian.net/browse/HHH-16383" )
@SessionFactory(generateStatistics = true)
public class SimpleNaturalIdClassTests {
	@BeforeEach
	void setUp(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.persist( new SystemUser( 1, "steve", "ci", "abc" ) );
			session.persist( new User( 1, "steve", "def" ) );

			final Customer billys = new Customer( 1, "bILLY bOB'S Steak House and Grill and Bar and Sushi" );
			session.persist( billys );

			final Order billys1001 = new Order( 1, billys, 1001, Instant.now() );
			session.persist( billys1001 );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	void testFindBySimple(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var result = session.find( User.class, "steve", KeyType.NATURAL );
			assertEquals( 1, result.id );
		} );
	}

	@Test
	void testFindMultipleBySimple(SessionFactoryScope factoryScope) {
		// baseline
		factoryScope.inTransaction( (session) -> {
			var results = session.findMultiple( User.class, List.of( 1, 2 ), KeyType.IDENTIFIER );
			assertThat( results ).hasSize( 2 );
			assertEquals( 1,  results.get( 0 ).id );
			assertNull( results.get( 1 ) );
		} );

		factoryScope.inTransaction( (session) -> {
			var results = session.findMultiple( User.class, List.of( "steve", "john" ), KeyType.NATURAL );
			assertThat( results ).hasSize( 2 );
			assertEquals( 1,  results.get( 0 ).id );
			assertNull( results.get( 1 ) );
		} );
	}

	@Test
	void testFindByClass(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var result = session.find( SystemUser.class, new SystemUserKey("steve", "ci"), KeyType.NATURAL );
			assertEquals( 1, result.id );
		} );
	}

	@Test
	void testFindMultipleByClass(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var results = session.findMultiple( SystemUser.class, List.of( new SystemUserKey("steve", "ci") ), KeyType.NATURAL );
			assertThat( results ).hasSize( 1 );
			assertEquals( 1,  results.get( 0 ).id );
		} );
	}

	@Test
	void testNormalization(SessionFactoryScope factoryScope) {
		var stats = factoryScope.getSessionFactory().getStatistics();
		stats.clear();

		factoryScope.inTransaction( (session) -> {
			session.find( SystemUser.class, new SystemUserKey("ci", "steve"), KeyType.NATURAL );
			assertEquals( 1, stats.getNaturalIdStatistics( SystemUser.class.getName() ).getNormalizationCount() );
		} );
	}

	@Test
	void testFindByClassWithToOne(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			// the current reality is that we need a reference to the associated entity
			// rather than just its id
			var customer = session.getReference( Customer.class, 1 );
			session.find( Order.class, new OrderKey(customer, 1001), KeyType.NATURAL );
		} );
	}

	@Test
	void testFindByCompositeNaturalIdForms(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			// by idclass is tested in other methods...

			var customer = session.getReference( Customer.class, 1 );

			// by map
			session.find( SystemUser.class, Map.of( "system", "ci", "username", "steve" ), KeyType.NATURAL );
			session.find( Order.class, Map.of( "customer", customer, "invoiceNumber", 1001), KeyType.NATURAL );

			// by array
			session.find( SystemUser.class, new Object[] {"ci","steve"}, KeyType.NATURAL );
			session.find( Order.class, new Object[] {customer,1001}, KeyType.NATURAL );
		} );
	}

	@SuppressWarnings("FieldCanBeLocal")
	@Entity(name="User")
	@Table(name="t_users")
	public static class User {
		@Id
		private Integer id;
		@NaturalId
		private String username;
		private String stuff;

		public User() {
		}

		public User(Integer id, String username, String stuff) {
			this.id = id;
			this.username = username;
			this.stuff = stuff;
		}
	}

	public record SystemUserKey(String system, String username) {
	}

	@SuppressWarnings("FieldCanBeLocal")
	//tag::naturalidclass-mapping-example[]
	@Entity(name="SystemUser")
	@Table(name="t_sys_users")
	@NaturalIdClass(SystemUserKey.class)
	public static class SystemUser {
		@Id
		private Integer id;
		@NaturalId
		@Column(name = "sys")
		private String system;
		@NaturalId
		private String username;
	//end::naturalidclass-mapping-example[]
		private String stuff;

		public SystemUser() {
		}

		public SystemUser(Integer id, String system, String username, String stuff) {
			this.id = id;
			this.system = system;
			this.username = username;
			this.stuff = stuff;
		}
	//tag::naturalidclass-mapping-example[]
	}
	//end::naturalidclass-mapping-example[]

	@SuppressWarnings("FieldCanBeLocal")
	@Entity(name="Customer")
	@Table(name="customers")
	public static class Customer {
		@Id
		private Integer id;
		private String name;

		public Customer() {
		}

		public Customer(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	public record OrderKey(Customer customer, int invoiceNumber) {
	}

	@SuppressWarnings("FieldCanBeLocal")
	@Entity(name="Order")
	@Table(name="orders")
	@NaturalIdClass(OrderKey.class)
	public static class Order {
		@Id
		private Integer id;
		@NaturalId
		@ManyToOne
		@JoinColumn(name = "customer_fk")
		private Customer customer;
		@NaturalId
		int invoiceNumber;
		private Instant timestamp;

		public Order() {
		}

		public Order(Integer id, Customer customer, int invoiceNumber, Instant timestamp) {
			this.id = id;
			this.customer = customer;
			this.invoiceNumber = invoiceNumber;
			this.timestamp = timestamp;
		}
	}
}
