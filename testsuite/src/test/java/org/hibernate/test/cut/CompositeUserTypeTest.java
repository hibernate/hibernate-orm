//$Id: CompositeUserTypeTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.cut;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

import junit.framework.Test;

import org.hibernate.Session;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Gavin King
 */
public class CompositeUserTypeTest extends FunctionalTestCase {
	
	public CompositeUserTypeTest(String str) {
		super(str);
	}

	public String[] getMappings() {
		return new String[] { "cut/types.hbm.xml", "cut/Transaction.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( CompositeUserTypeTest.class );
	}
	
	public void testCompositeUserType() {
		Session s = openSession();
		org.hibernate.Transaction t = s.beginTransaction();
		
		Transaction tran = new Transaction();
		tran.setDescription("a small transaction");
		tran.setValue( new MonetoryAmount( new BigDecimal(1.5), Currency.getInstance("USD") ) );
		s.persist(tran);
		
		List result = s.createQuery("from Transaction tran where tran.value.amount > 1.0 and tran.value.currency = 'USD'").list();
		assertEquals( result.size(), 1 );
		tran.getValue().setCurrency( Currency.getInstance("AUD") );
		result = s.createQuery("from Transaction tran where tran.value.amount > 1.0 and tran.value.currency = 'AUD'").list();
		assertEquals( result.size(), 1 );
		
		if ( !(getDialect() instanceof HSQLDialect) ) {
		
			result = s.createQuery("from Transaction txn where txn.value = (1.5, 'AUD')").list();
			assertEquals( result.size(), 1 );
			result = s.createQuery("from Transaction where value = (1.5, 'AUD')").list();
			assertEquals( result.size(), 1 );
			
		}
		
		s.delete(tran);
		t.commit();
		s.close();
	}

}

