/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cascade;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertFalse;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.Hibernate;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

@TestForIssue(jiraKey = "HHH-12867")
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
			session.save( invoice );

			session.save( new Line( "1 pen - 5€", invoice ) );
			session.save( new Tax( "21%", invoice ) );
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
