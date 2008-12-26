//$Id: OnDeleteTest.java 15728 2008-12-26 22:59:25Z gbadner $
package org.hibernate.test.ondelete;

import java.util.List;

import junit.framework.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.MySQLInnoDBDialect;
import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.stat.Statistics;

/**
 * @author Gavin King
 */
public class OnDeleteTest extends FunctionalTestCase {
	
	public OnDeleteTest(String str) {
		super(str);
	}

	public String[] getMappings() {
		return new String[] { "ondelete/Person.hbm.xml" };
	}

	public void configure(Configuration cfg) {
		cfg.setProperty(Environment.GENERATE_STATISTICS, "true");
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( OnDeleteTest.class );
	}
	
	public void testJoinedSubclass() {
		if ( ! supportsCircularCascadeDelete() ) {
			return;
		}

		Statistics statistics = getSessions().getStatistics();
		statistics.clear();
		
		Session s = openSession();
		Transaction t = s.beginTransaction();
		
		Salesperson mark = new Salesperson();
		mark.setName("Mark");
		mark.setTitle("internal sales");
		mark.setSex('M');
		mark.setAddress("buckhead");
		mark.setZip("30305");
		mark.setCountry("USA");
		
		Person joe = new Person();
		joe.setName("Joe");
		joe.setAddress("San Francisco");
		joe.setZip("XXXXX");
		joe.setCountry("USA");
		joe.setSex('M');
		joe.setSalesperson(mark);
		mark.getCustomers().add(joe);
				
		s.save(mark);
		
		t.commit();
		
		assertEquals( statistics.getEntityInsertCount(), 2 );
		assertEquals( statistics.getPrepareStatementCount(), 5 );
		
		statistics.clear();
		
		t = s.beginTransaction();
		s.delete(mark);
		t.commit();

		assertEquals( statistics.getEntityDeleteCount(), 2 );
		if ( getDialect().supportsCascadeDelete() ) {
			assertEquals( statistics.getPrepareStatementCount(), 1 );
		}
		
		t = s.beginTransaction();
		List names = s.createQuery("select name from Person").list();
		assertTrue( names.isEmpty() );
		t.commit();

		s.close();
	}

}

