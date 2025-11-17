/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.cid.nonaggregated;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;


import org.hibernate.testing.orm.junit.DomainModel;
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
@DomainModel( annotatedClasses = {
		SmokeTests.SystemAccess.class,
		SmokeTests.Order.class,
		SmokeTests.LineItem.class
} )
@SessionFactory
public class SmokeTests {
	@Test
	public void simpleTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createQuery( "select a from SystemAccess a" ).list();
		} );
	}

	@Test
	public void keyManyToOneTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createQuery( "select i from LineItem i" ).list();
		} );
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
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity( name = "SystemAccess" )
	@Table( name = "`access`" )
	public static class SystemAccess implements Serializable {
		private String system;
		private String userId;
		private String accessCode;

		@Id
		@Column(name = "system_id")
		public String getSystem() {
			return system;
		}

		public void setSystem(String system) {
			this.system = system;
		}

		@Id
		public String getUserId() {
			return userId;
		}

		public void setUserId(String userId) {
			this.userId = userId;
		}

		public String getAccessCode() {
			return accessCode;
		}

		public void setAccessCode(String accessCode) {
			this.accessCode = accessCode;
		}
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

	@Entity( name = "LineItem" )
	@Table( name = "line_items" )
	public static class LineItem implements Serializable {
		private Order order;
		private Integer lineNumber;
		private String sku;
		private int quantity;

		public LineItem() {
		}

		public LineItem(Order order, int lineNumber, String sku, int quantity) {
			this.order = order;
			this.lineNumber = lineNumber;
			this.sku = sku;
			this.quantity = quantity;
		}

		@Id
		@ManyToOne
		@JoinColumn( name = "order_id" )
		public Order getOrder() {
			return order;
		}

		public void setOrder(Order order) {
			this.order = order;
		}

		@Id
		@Column( name = "line_number" )
		public Integer getLineNumber() {
			return lineNumber;
		}

		public void setLineNumber(Integer lineNumber) {
			this.lineNumber = lineNumber;
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
