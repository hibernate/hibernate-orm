/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cascade;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.Hibernate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

@JiraKey(value = "HHH-12867")
@DomainModel(
		annotatedClasses = {
				RefreshLazyOneToManyTest.Invoice.class,
				RefreshLazyOneToManyTest.Tax.class,
				RefreshLazyOneToManyTest.Line.class
		}
)
@SessionFactory
public class RefreshLazyOneToManyTest {

	@Test
	@FailureExpected(jiraKey = "HHH-12867")
	public void testRefreshCascade(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Invoice invoice = new Invoice( "An invoice for John Smith" );
			session.persist( invoice );

			session.persist( new Line( "1 pen - 5€", invoice ) );
			session.persist( new Tax( "21%", invoice ) );
		} );

		scope.inTransaction( session -> {
			Invoice invoice = session.get( Invoice.class, 1 );

			assertFalse( Hibernate.isInitialized( invoice.getTaxes() ),
					"Taxes should not be initialized before refresh" );
			assertFalse( Hibernate.isInitialized( invoice.getLines() ),
					"Lines should not be initialized before refresh" );

			session.refresh( invoice );

			assertFalse( Hibernate.isInitialized( invoice.getTaxes() ),
					"Taxes should not be initialized after refresh" );
			assertFalse( Hibernate.isInitialized( invoice.getLines() ),
					"Lines should not be initialized after refresh" );
		} );
	}

	@Entity
	@Table(name = "invoice")
	public static class Invoice {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		@Column(name = "id")
		private Integer id;

		@Column(name = "description")
		private String description;

		@OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL)
		private List<Line> lines;

		@OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL)
		private List<Tax> taxes;

		public Invoice() {
		}

		public Invoice(String description) {
			this.description = description;
		}

		public List<Line> getLines() {
			return lines;
		}

		public List<Tax> getTaxes() {
			return taxes;
		}
	}

	@Entity
	@Table(name = "invoice_line")
	public static class Line {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		@Column(name = "id")
		private Integer id;

		@Column(name = "description")
		private String description;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "invoice_id")
		private Invoice invoice;

		public Line() {
		}

		public Line(String description, Invoice invoice) {
			this.description = description;
			this.invoice = invoice;
		}
	}

	@Entity
	@Table(name = "invoice_tax")
	public static class Tax {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		@Column(name = "id")
		private Integer id;

		@Column(name = "description")
		private String description;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "invoice_id")
		private Invoice invoice;

		public Tax() {
		}

		public Tax(String description, Invoice invoice) {
			this.description = description;
			this.invoice = invoice;
		}
	}
}
