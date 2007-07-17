//$Id: PaginationTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.pagination;

import java.math.BigDecimal;

import junit.framework.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.criterion.Order;
import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Gavin King
 */
public class PaginationTest extends FunctionalTestCase {
	
	public PaginationTest(String str) {
		super(str);
	}

	public String[] getMappings() {
		return new String[] { "pagination/DataPoint.hbm.xml" };
	}

	public void configure(Configuration cfg) {
		cfg.setProperty(Environment.STATEMENT_BATCH_SIZE, "20");
	}

	public String getCacheConcurrencyStrategy() {
		return null;
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( PaginationTest.class );
	}
	
	public void testPagination() {
		Session s = openSession();
		Transaction t = s.beginTransaction();		
		for ( int i=0; i<10; i++ ) {
			DataPoint dp = new DataPoint();
			dp.setX( new BigDecimal(i * 0.1d).setScale(19, BigDecimal.ROUND_DOWN) );
			dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale(19, BigDecimal.ROUND_DOWN) );
			s.persist(dp);
		}
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		int size = s.createSQLQuery("select id, xval, yval, description from DataPoint order by xval, yval")
			.addEntity(DataPoint.class)
			.setMaxResults(5)
			.list().size();
		assertEquals(size, 5);
		size = s.createQuery("from DataPoint order by x, y")
			.setFirstResult(5)
			.setMaxResults(2)
			.list().size();
		assertEquals(size, 2);
		size = s.createCriteria(DataPoint.class)
			.addOrder( Order.asc("x") )
			.addOrder( Order.asc("y") )
			.setFirstResult(8)
			.list().size();
		assertEquals(size, 2);
		t.commit();
		s.close();
		
	}
}

