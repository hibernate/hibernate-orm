/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.naturalid.idclass;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.FindBy;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdClass;
import org.hibernate.property.access.spi.PropertyAccessException;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ExpectedException;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {
		ToOneTests.OrderKey.class,
		ToOneTests.Customer.class,
		ToOneTests.Order.class
})
@SessionFactory
public class ToOneTests {
	@BeforeEach
	void setUp(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final Customer billys = new Customer( 1, "bILLY bOB'S Steak House and Grill and Bar and Sushi" );
			session.persist( billys );

			final Order billys1001 = new Order( 1, billys, 1001, Instant.now() );
			session.persist(  billys1001 );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	@ExpectedException(PropertyAccessException.class)
	void testWouldBeNiceBaseline(SessionFactoryScope factoryScope) {
		// This would be functionally equivalent to defining
		// OrderKey with `int customer` and simply using the Customer id.
		// However, this is not supported atm.
		// See comments in `CompoundNaturalIdMapping.ToOneAttributeMapperImpl#extractFrom`.
		factoryScope.inTransaction( (session) -> {
			session.find(
					Order.class,
					Map.of( "customer", 1, "invoiceNumber", 1001 ),
					FindBy.NATURAL_ID
			);
		} );
	}

	@Test
	void testCurrentReality(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			// the current reality is that we need a reference to the associated entity
			// rather than just its id
			var customer = session.getReference( Customer.class, 1 );
			session.find( Order.class, new OrderKey(customer, 1001), FindBy.NATURAL_ID );
		} );
	}

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

	public record OrderKey(Customer customer, int invoiceNumber) {
	}
}
