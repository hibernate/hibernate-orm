/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.cid.aggregated;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Simple tests that aggregated id mappings work at a basic level
 *
 * @author Steve Ebersole
 */
@ServiceRegistry
@DomainModel(
		annotatedClasses = {
				SmokeTests.Order.class,
				SmokeTests.LineItem.class,
				SmokeTests.LineItemId.class
		}
)
@SessionFactory
public class SmokeTests {
	@Test
	public void simpleTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "select i from LineItem i" ).list();
				}
		);
	}

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Order order = new Order( 1, "123-abc" );
					session.persist( order );

					session.persist( new LineItem( order, 1, "xyz", 500 ) );
					session.persist( new LineItem( order, 2, "tuv", 60 ) );
					session.persist( new LineItem( order, 3, "def", 350 ) );
				}
		);
	}

	@AfterEach
	public void cleanUpTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity( name = "Order" )
	@Table( name = "orders" )
	public static class Order {
		private Integer id;
		private String invoice;

		public Order() {
		}

		public Order(Integer id, String invoice) {
			this.id = id;
			this.invoice = invoice;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getInvoice() {
			return invoice;
		}

		public void setInvoice(String invoice) {
			this.invoice = invoice;
		}
	}

	@Embeddable
	public static class LineItemId implements Serializable {
		private Order order;
		private Integer lineNumber;

		public LineItemId() {
		}

		public LineItemId(Order order, Integer lineNumber) {
			this.order = order;
			this.lineNumber = lineNumber;
		}

		@ManyToOne
		@JoinColumn( name = "order_id" )
		public Order getOrder() {
			return order;
		}

		public void setOrder(Order order) {
			this.order = order;
		}

		@Column( name = "line_number" )
		public Integer getLineNumber() {
			return lineNumber;
		}

		public void setLineNumber(Integer lineNumber) {
			this.lineNumber = lineNumber;
		}
	}

	@Entity( name = "LineItem" )
	@Table( name = "line_items" )
	public static class LineItem {
		private LineItemId id;
		private String sku;
		private int quantity;

		public LineItem() {
		}

		public LineItem(LineItemId id, String sku, int quantity) {
			this.id = id;
			this.sku = sku;
			this.quantity = quantity;
		}

		public LineItem(Order order, int lineNumber, String sku, int quantity) {
			this.id = new LineItemId( order, lineNumber );
			this.sku = sku;
			this.quantity = quantity;
		}

		@EmbeddedId
		public LineItemId getId() {
			return id;
		}

		public void setId(LineItemId id) {
			this.id = id;
		}

		public String getSku() {
			return sku;
		}

		public void setSku(String sku) {
			this.sku = sku;
		}

		public int getQuantity() {
			return quantity;
		}

		public void setQuantity(int quantity) {
			this.quantity = quantity;
		}
	}
}
