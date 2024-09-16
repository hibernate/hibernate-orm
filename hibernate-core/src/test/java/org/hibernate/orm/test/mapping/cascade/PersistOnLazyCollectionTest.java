/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.cascade;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.LazyInitializationException;
import org.hibernate.Session;
import org.hibernate.stat.SessionStatistics;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Testing relationships between components: example invoice -> invoice line
 *
 * @author Jan-Oliver Lustig, Sebastian Viefhaus
 */
@DomainModel(
		annotatedClasses = {
				PersistOnLazyCollectionTest.Invoice.class,
				PersistOnLazyCollectionTest.InvoiceLine.class,
				PersistOnLazyCollectionTest.Receipt.class
		}
)
@SessionFactory
public class PersistOnLazyCollectionTest {

	static String RECEIPT_A = "Receipt A";
	static String INVOICE_A = "Invoice A";
	static String INVOICELINE_A = "InvoiceLine A";
	static String INVOICELINE_B = "InvoiceLine B";

	public Invoice createInvoiceWithTwoInvoiceLines(Session session) {
		InvoiceLine lineA = new InvoiceLine( INVOICELINE_A );
		InvoiceLine lineB = new InvoiceLine( INVOICELINE_B );

		Invoice invoice = new Invoice( INVOICE_A );
		invoice.addInvoiceLine( lineA );
		invoice.addInvoiceLine( lineB );

		session.persist( invoice );

		return invoice;
	}

	@Test
	@JiraKey(value = "HHH-11916")
	public void testPersistOnAlreadyPersistentEntityWithUninitializedLazyCollection(SessionFactoryScope scope) {

		final Invoice _invoice = scope.fromTransaction( session -> createInvoiceWithTwoInvoiceLines( session ) );

		Invoice invoiceAfter = scope.fromTransaction( session -> {
			SessionStatistics stats = session.getStatistics();

			// load invoice, invoiceLines should not be loaded
			Invoice invoice = session.get( Invoice.class, _invoice.getId() );
			assertEquals(
					1,
					stats.getEntityCount(),
					"Invoice lines should not be initialized while loading the invoice, because of the lazy association."
			);

			invoice.setName( invoice.getName() + " !" );

			return invoice;
		} );

		try {
			invoiceAfter.getInvoiceLines().size();
			fail( "Should throw LazyInitializationException" );
		}
		catch (LazyInitializationException expected) {
		}
	}

	@Test
	@JiraKey(value = "HHH-11916")
	public void testPersistOnNewEntityRelatedToAlreadyPersistentEntityWithUninitializedLazyCollection(
			SessionFactoryScope scope) {
		final Invoice _invoice = scope.fromTransaction( session -> createInvoiceWithTwoInvoiceLines( session ) );

		Invoice invoiceAfter = scope.fromTransaction( session -> {
			SessionStatistics stats = session.getStatistics();

			// load invoice, invoiceLines should not be loaded
			Invoice invoice = session.get( Invoice.class, _invoice.getId() );
			assertEquals( 1,
						stats.getEntityCount(),
						"Invoice lines should not be initialized while loading the invoice, because of the lazy association."
			);

			Receipt receipt = new Receipt( RECEIPT_A );

			receipt.setInvoice( invoice );
			session.persist( receipt );

			return invoice;
		} );

		try {
			invoiceAfter.getInvoiceLines().size();
			fail( "Should throw LazyInitializationException" );
		}
		catch (LazyInitializationException expected) {
		}
	}

	@Entity
	@Table(name = "OTM_Invoice")
	public static class Invoice {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		@Column(length = 50, nullable = false)
		private String name;

		@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
		@JoinColumn(name = "INVOICE_ID", nullable = false)
		private final Set<InvoiceLine> invoiceLines = new HashSet<>();

		public Invoice() {
			super();
		}

		public Invoice(String name) {
			super();
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Set<InvoiceLine> getInvoiceLines() {
			return invoiceLines;
		}

		public void addInvoiceLine(InvoiceLine invoiceLine) {
			invoiceLines.add( invoiceLine );
		}
	}

	@Entity
	@Table(name = "OTM_InvoiceLine")
	public static class InvoiceLine {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		@Column(length = 50, nullable = false)
		private String name;

		public InvoiceLine() {
			super();
		}

		public InvoiceLine(String name) {
			super();
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

	}

	@Entity
	@Table(name = "OTM_Receipt")
	public static class Receipt {

		@OneToOne(cascade = { CascadeType.PERSIST })
		private Invoice invoice;

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		@Column(length = 50, nullable = false)
		private String name;

		public Receipt() {
			super();
		}

		public Receipt(String name) {
			super();
			this.name = name;
		}

		public void setInvoice(Invoice invoice) {
			this.invoice = invoice;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

	}
}
