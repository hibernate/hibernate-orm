/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.callbacks.jpa4;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListener;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = {
		EntityListenerTargetMatchingTests.Customer.class,
		EntityListenerTargetMatchingTests.PurchaseOrder.class,
		EntityListenerTargetMatchingTests.Invoice.class,
		EntityListenerTargetMatchingTests.Product.class,
		EntityListenerTargetMatchingTests.TargetMatchingListener.class
})
@SessionFactory
public class EntityListenerTargetMatchingTests {
	@BeforeEach
	void setUp() {
		TargetMatchingListener.reset();
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	void testEntityListenerCallbacksOnlyFireForCompatibleTargets(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.persist( new Customer( 1 ) );
			session.persist( new PurchaseOrder( 1 ) );
			session.persist( new Invoice( 1 ) );
		} );

		assertThat( TargetMatchingListener.namedEntityEvents ).containsExactly(
				Customer.class,
				PurchaseOrder.class
		);
		assertThat( TargetMatchingListener.customerEvents ).containsExactly( Customer.class );
		assertThat( TargetMatchingListener.invoiceEvents ).containsExactly( Invoice.class );
		assertThat( TargetMatchingListener.productEvents ).isEmpty();
	}

	interface NamedEntity {
	}

	@Entity(name = "TargetMatchingCustomer")
	public static class Customer implements NamedEntity {
		@Id
		private Integer id;

		public Customer() {
		}

		public Customer(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "TargetMatchingPurchaseOrder")
	public static class PurchaseOrder implements NamedEntity {
		@Id
		private Integer id;

		public PurchaseOrder() {
		}

		public PurchaseOrder(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "TargetMatchingInvoice")
	public static class Invoice {
		@Id
		private Integer id;

		public Invoice() {
		}

		public Invoice(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "TargetMatchingProduct")
	public static class Product {
		@Id
		private Integer id;
	}

	@EntityListener
	public static class TargetMatchingListener {
		private static final List<Class<?>> namedEntityEvents = new ArrayList<>();
		private static final List<Class<?>> customerEvents = new ArrayList<>();
		private static final List<Class<?>> invoiceEvents = new ArrayList<>();
		private static final List<Class<?>> productEvents = new ArrayList<>();

		public static void reset() {
			namedEntityEvents.clear();
			customerEvents.clear();
			invoiceEvents.clear();
			productEvents.clear();
		}

		@PrePersist
		public void prePersist(NamedEntity entity) {
			namedEntityEvents.add( entity.getClass() );
		}

		@PrePersist
		public void prePersist(Customer entity) {
			customerEvents.add( entity.getClass() );
		}

		@PrePersist
		public void prePersist(Invoice entity) {
			invoiceEvents.add( entity.getClass() );
		}

		@PrePersist
		public void prePersist(Product entity) {
			productEvents.add( entity.getClass() );
		}
	}
}
