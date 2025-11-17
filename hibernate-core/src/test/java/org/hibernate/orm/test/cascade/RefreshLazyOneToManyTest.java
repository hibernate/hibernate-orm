/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cascade;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertFalse;

import java.util.List;

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

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

@JiraKey(value = "HHH-12867")
public class RefreshLazyOneToManyTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ Invoice.class, Tax.class, Line.class };
	}

	@Test
	@FailureExpected( jiraKey = "HHH-12867")
	public void testRefreshCascade() {
		doInHibernate( this::sessionFactory, session -> {
			Invoice invoice = new Invoice( "An invoice for John Smith" );
			session.persist( invoice );

			session.persist( new Line( "1 pen - 5€", invoice ) );
			session.persist( new Tax( "21%", invoice ) );
		} );

		doInHibernate( this::sessionFactory, session -> {
			Invoice invoice = session.get( Invoice.class, 1 );

			assertFalse( "Taxes should not be initialized before refresh",
					Hibernate.isInitialized( invoice.getTaxes() ) );
			assertFalse( "Lines should not be initialized before refresh",
					Hibernate.isInitialized( invoice.getLines() ) );

			session.refresh( invoice );

			assertFalse( "Taxes should not be initialized after refresh",
					Hibernate.isInitialized( invoice.getTaxes() ) );
			assertFalse( "Lines should not be initialized after refresh",
					Hibernate.isInitialized( invoice.getLines() ) );
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
