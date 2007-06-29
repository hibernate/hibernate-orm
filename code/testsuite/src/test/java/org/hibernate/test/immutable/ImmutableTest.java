//$Id: ImmutableTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.immutable;

import junit.framework.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Projections;
import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Gavin King
 */
public class ImmutableTest extends FunctionalTestCase {

	public ImmutableTest(String str) {
		super(str);
	}

	public String[] getMappings() {
		return new String[] { "immutable/ContractVariation.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( ImmutableTest.class );
	}

	public void testImmutable() {
		Contract c = new Contract("gavin", "phone");
		ContractVariation cv1 = new ContractVariation(1, c);
		cv1.setText("expensive");
		ContractVariation cv2 = new ContractVariation(2, c);
		cv2.setText("more expensive");
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(c);
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria(Contract.class).uniqueResult();
		c.setCustomerName("foo bar");
		c.getVariations().add( new ContractVariation(3, c) );
		cv1 = (ContractVariation) c.getVariations().iterator().next();
		cv1.setText("blah blah");
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria(Contract.class).uniqueResult();
		assertEquals( c.getCustomerName(), "gavin" );
		assertEquals( c.getVariations().size(), 2 );
		cv1 = (ContractVariation) c.getVariations().iterator().next();
		assertEquals( cv1.getText(), "expensive" );
		s.delete(c);
		assertEquals( s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult(), new Integer(0) );
		assertEquals( s.createCriteria(ContractVariation.class).setProjection( Projections.rowCount() ).uniqueResult(), new Integer(0) );
		t.commit();
		s.close();
	}

}

