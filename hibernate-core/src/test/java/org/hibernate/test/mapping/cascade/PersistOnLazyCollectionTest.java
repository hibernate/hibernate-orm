package org.hibernate.test.mapping.cascade;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.stat.SessionStatistics;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;


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
		return new Class[] { Invoice.class, InvoiceLine.class, Receipt.class };
	}
    
    /**
     * Helping method: create invoice with two invoice lines
     * @return invoice
     */
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
    	
    	try (Session session = openSession()) {
        	Transaction tx = session.beginTransaction();
            SessionStatistics stats = session.getStatistics();

            Invoice invoice = createInvoiceWithTwoInvoiceLines(session);
            session.flush();
            session.clear();

            // load invoice, invoiceLines should not be loaded
            invoice = (Invoice) session.get(Invoice.class, invoice.getId());
            Assert.assertEquals("Invoice lines should not be initialized while loading the invoice, because of the lazy association.", 1, stats.getEntityCount());


            // make change
            invoice.setName(invoice.getName() + " !");
            //session.persist(invoice);

            session.flush();

            Assert.assertEquals(1, stats.getEntityCount()); // invoice lines should not be initialized
            tx.commit();
    	}
    }
    
    @Test
    @TestForIssue( jiraKey = "HHH-11916" )
    public void testPersistOnNewEntityRelatedToAlreadyPersistentEntityWithUninitializedLazyCollection() {
    	try (Session session = openSession()) {
    		
    		Transaction tx = session.beginTransaction();
            SessionStatistics stats = session.getStatistics();

            Invoice invoice = createInvoiceWithTwoInvoiceLines(session);
            
    		session.flush();
            session.clear();

            // load invoice, invoiceLines should not be loaded
            invoice = (Invoice) session.get(Invoice.class, invoice.getId());
            
            Assert.assertEquals("Invoice lines should not be initialized while loading the invoice, because of the lazy association.", 1, stats.getEntityCount());
            
            Receipt receipt = new Receipt(RECEIPT_A);
            
    		receipt.setInvoice(invoice);
    		session.persist(receipt);

            session.flush();
            System.out.println( stats.getEntityCount() );
            Assert.assertEquals(2, stats.getEntityCount()); // invoice lines should not be initialized, entityCount should be 2
            
            tx.commit();
    	}   	
    }
}