/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
import org.hibernate.testing.FailureExpectedWithNewUnifiedXsd;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author Gavin King
 */
@FailureExpectedWithNewUnifiedXsd
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

