/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.mapping.cascade;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.LazyInitializationException;
import org.hibernate.Session;
import org.hibernate.stat.SessionStatistics;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.fail;

/**
 * Testing relationships between components: example invoice -> invoice line
 *
 * @author Jan-Oliver Lustig, Sebastian Viefhaus
 */
public class PersistOnLazyCollectionTest extends BaseCoreFunctionalTestCase{

	static String RECEIPT_A = "Receipt A";
    static String INVOICE_A = "Invoice A";
    static String INVOICELINE_A = "InvoiceLine A";
    static String INVOICELINE_B = "InvoiceLine B";
    
    @Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Invoice.class,
				InvoiceLine.class,
				Receipt.class
		};
	}

    public Invoice createInvoiceWithTwoInvoiceLines(Session session) {
        InvoiceLine lineA = new InvoiceLine(INVOICELINE_A);
        InvoiceLine lineB = new InvoiceLine(INVOICELINE_B);

        Invoice invoice = new Invoice(INVOICE_A);
        invoice.addInvoiceLine(lineA);
        invoice.addInvoiceLine(lineB);       
        
        session.persist(invoice);

        return invoice;
    }
    
    @Test
    @TestForIssue( jiraKey = "HHH-11916" )
    public void testPersistOnAlreadyPersistentEntityWithUninitializedLazyCollection() {

		final Invoice _invoice = doInHibernate( this::sessionFactory, this::createInvoiceWithTwoInvoiceLines );

		Invoice invoiceAfter = doInHibernate( this::sessionFactory, session -> {
			SessionStatistics stats = session.getStatistics();

			// load invoice, invoiceLines should not be loaded
			Invoice invoice = session.get(Invoice.class, _invoice.getId());
			Assert.assertEquals(
				"Invoice lines should not be initialized while loading the invoice, " +
				"because of the lazy association.", 1, stats.getEntityCount());

			invoice.setName(invoice.getName() + " !");

			return invoice;
		} );

		try {
			invoiceAfter.getInvoiceLines().size();
			fail("Should throw LazyInitializationException");
		}
		catch (LazyInitializationException expected) {
		}
	}
    
    @Test
    @TestForIssue( jiraKey = "HHH-11916" )
    public void testPersistOnNewEntityRelatedToAlreadyPersistentEntityWithUninitializedLazyCollection() {
		final Invoice _invoice = doInHibernate( this::sessionFactory, this::createInvoiceWithTwoInvoiceLines );

		Invoice invoiceAfter = doInHibernate( this::sessionFactory, session -> {
			SessionStatistics stats = session.getStatistics();

			// load invoice, invoiceLines should not be loaded
			Invoice invoice = session.get(Invoice.class, _invoice.getId());
			Assert.assertEquals(
				"Invoice lines should not be initialized while loading the invoice, " +
				"because of the lazy association.", 1, stats.getEntityCount());

			Receipt receipt = new Receipt(RECEIPT_A);

			receipt.setInvoice(invoice);
			session.persist(receipt);

			return invoice;
		} );

		try {
			invoiceAfter.getInvoiceLines().size();
			fail("Should throw LazyInitializationException");
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
			invoiceLines.add(invoiceLine);
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

		@OneToOne(cascade = { CascadeType.PERSIST})
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