/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.cascade;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 * @author Jan-Oliver Lustig
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-11916")
@DomainModel(
		annotatedClasses = {
				PersistOnLazyCollectionTests.Order.class,
				PersistOnLazyCollectionTests.LineItem.class,
				PersistOnLazyCollectionTests.Payment.class
		}
)
@SessionFactory(useCollectingStatementInspector = true)
public class PersistOnLazyCollectionTests {

	@Test
	public void testMutation(SessionFactoryScope scope) {
		final SQLStatementInspector sqlCollector = scope.getCollectingStatementInspector();

		final Order detached = scope.fromTransaction( (session) -> {
			sqlCollector.clear();

			final Order order = session.find( Order.class, 1 );

			// make sure lineItems is not initialized
			assertThat( Hibernate.isInitialized( order.getLineItems() ) ).isFalse();
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			assertThat( sqlCollector.getSqlQueries().get( 0 ) ).doesNotContain( " line_items " );

			// mutate order, which will trigger a flush before we return
			order.setName( "Order 00001" );

			return order;
		} );

		// make sure lineItems is still not initialized
		assertThat( Hibernate.isInitialized( detached.getLineItems() ) ).isFalse();
		try {
			//noinspection ResultOfMethodCallIgnored
			detached.getLineItems().size();
			fail( "Should throw LazyInitializationException" );
		}
		catch (LazyInitializationException expected) {
		}
	}

	@Test
	public void testCascadePersist(SessionFactoryScope scope) {
		final SQLStatementInspector sqlCollector = scope.getCollectingStatementInspector();

		final Order detached = scope.fromTransaction( (session) -> {
			sqlCollector.clear();

			final Order order = session.find( Order.class, 1 );

			// make sure lineItems is not initialized
			assertThat( Hibernate.isInitialized( order.getLineItems() ) ).isFalse();
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			assertThat( sqlCollector.getSqlQueries().get( 0 ) ).doesNotContain( " line_items " );

			// create a Payment and persist, which will cascade the persist to Order
			// - we want to make sure this won't trigger initializing the lazy collection
			final Payment payment = new Payment( 1, order, "123456789" );
			session.persist( payment );

			return order;
		} );

		// make sure lineItems is still not initialized
		assertThat( Hibernate.isInitialized( detached.getLineItems() ) ).isFalse();
		try {
			//noinspection ResultOfMethodCallIgnored
			detached.getLineItems().size();
			fail( "Should throw LazyInitializationException" );
		}
		catch (LazyInitializationException expected) {
		}
	}

	@BeforeEach
	void createTestData(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final Order order = new Order( 1, "Order 1" );

			order.addLineItem( new LineItem( 1, "Line 1" ) );
			order.addLineItem( new LineItem( 2, "Line 2" ) );

			session.persist( order );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}


	@Entity
	@Table(name = "orders")
	public static class Order {
		@Id
		private Integer id;
		private String name;

		@OneToMany( cascade = CascadeType.PERSIST )
		@JoinColumn(name = "order_fk")
		private final Set<LineItem> lineItems = new HashSet<>();

		public Order() {
			super();
		}

		public Order(Integer id, String name) {
			super();
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Set<LineItem> getLineItems() {
			return lineItems;
		}

		public void addLineItem(LineItem lineItem) {
			lineItems.add( lineItem );
		}
	}

	@Entity
	@Table(name = "line_items")
	public static class LineItem {
		@Id
		private Integer id;
		private String name;

		public LineItem() {
			super();
		}

		public LineItem(Integer id, String name) {
			super();
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity
	@Table(name = "payments")
	public static class Payment {
		@Id
		private Integer id;
		private String transactionCode;
		@OneToOne(cascade = CascadeType.PERSIST )
		private Order order;

		public Payment() {
			super();
		}

		public Payment(Integer id, Order order, String transactionCode) {
			super();
			this.id = id;
			this.order = order;
			this.transactionCode = transactionCode;
		}

		public Integer getId() {
			return id;
		}

		public Order getOrder() {
			return order;
		}

		public void setOrder(Order order) {
			this.order = order;
		}

		public String getTransactionCode() {
			return transactionCode;
		}

		public void setTransactionCode(String transactionCode) {
			this.transactionCode = transactionCode;
		}
	}
}
