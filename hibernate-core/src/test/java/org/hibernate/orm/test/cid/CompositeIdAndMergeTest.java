/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cid;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				CompositeIdAndMergeTest.Order.class,
				CompositeIdAndMergeTest.Invoice.class,
				CompositeIdAndMergeTest.LineItem.class
		}
)
@SessionFactory
@JiraKey("HHH-18131")
public class CompositeIdAndMergeTest {

	@Test
	public void testMerge(SessionFactoryScope scope) {
		Integer lineItemIndex = 2;
		Order persistedOrder = scope.fromTransaction(
				session -> {
					Order order = new Order( "order" );
					session.persist( order );

					Invoice invoice = new Invoice( "invoice" );
					LineItem lineItem = new LineItem( lineItemIndex );
					invoice.addLine( lineItem );
					order.setInvoice( invoice );

					session.merge( order );
					return order;
				}
		);

		scope.inTransaction(
				session -> {
					Order order = session.find( Order.class, persistedOrder.getId() );
					Invoice invoice = order.getInvoice();
					assertThat( invoice ).isNotNull();
					List<LineItem> lines = invoice.getLines();
					assertThat( lines.size() ).isEqualTo( 1 );
					assertThat( lines.get( 0 ).getIndex() ).isEqualTo( lineItemIndex );
				}
		);
	}

	@Entity(name = "Order")
	@Table(name = "order_table")
	public static class Order {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		@Column(nullable = false)
		private String description;

		@ManyToOne(cascade = { CascadeType.ALL })
		private Invoice invoice;

		public Order() {
		}

		public Order(String description) {
			this.description = description;
		}

		public Long getId() {
			return id;
		}

		public Invoice getInvoice() {
			return invoice;
		}

		public void setInvoice(Invoice invoice) {
			this.invoice = invoice;
		}
	}

	@Entity(name = "Invoice")
	public static class Invoice {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		@Column(name = "number_column")
		private String number;

		@OneToMany(mappedBy = "invoice", cascade = { CascadeType.ALL }, orphanRemoval = true)
		private List<LineItem> lines = new ArrayList<>();

		public Invoice() {
		}

		public Invoice(String number) {
			this.number = number;
		}

		public void addLine(LineItem line) {
			lines.add( line );
			line.invoice = this;
		}

		public List<LineItem> getLines() {
			return lines;
		}
	}

	@Entity
	@Table(name = "invoice_lines")
	@IdClass(LineItemId.class)
	public static class LineItem {
		@Id
		@ManyToOne
		private Invoice invoice;

		@Id
		@Column(name = "index_column")
		private Integer index;

		public LineItem() {
		}

		public LineItem(Integer index) {
			this.index = index;
		}

		public Integer getIndex() {
			return index;
		}
	}

	public static class LineItemId {
		private Long invoice;
		private Integer index;

	}

}
