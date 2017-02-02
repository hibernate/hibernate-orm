/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.ondelete;

import java.util.List;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.stat.Statistics;
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author Gavin King
 */
public class OnDeleteTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "ondelete/Person.hbm.xml" };
	}

	@Override
	public void configure(Configuration cfg) {
		cfg.setProperty(Environment.GENERATE_STATISTICS, "true");
	}

	@Test
	@RequiresDialectFeature(
			value = DialectChecks.SupportsCircularCascadeDeleteCheck.class,
			comment = "db/dialect does not support circular cascade delete constraints"
	)
	public void testJoinedSubclass() {
		Statistics statistics = sessionFactory().getStatistics();
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

