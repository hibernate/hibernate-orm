/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.legacy;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.stat.Statistics;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Emmanuel Bernard
 */
public class StatisticsTest extends LegacyTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "legacy/ABC.hbm.xml", "legacy/ABCExtends.hbm.xml" };
	}

	@Test
	public void testSessionStats() throws Exception {
		SessionFactory sf = sessionFactory();
		Statistics stats = sf.getStatistics();
		boolean isStats = stats.isStatisticsEnabled();
		stats.clear();
		stats.setStatisticsEnabled(true);
		Session s = sf.openSession();
		assertEquals( 1, stats.getSessionOpenCount() );
		s.close();
		assertEquals( 1, stats.getSessionCloseCount() );
		s = sf.openSession();
		Transaction tx = s.beginTransaction();
		A a = new A();
		a.setName("mya");
		s.save(a);
		a.setName("b");
		tx.commit();
		s.close();
		assertEquals( 1, stats.getFlushCount() );
		s = sf.openSession();
		tx = s.beginTransaction();
		String hql = "from " + A.class.getName();
		Query q = s.createQuery(hql);
		q.list();
		tx.commit();
		s.close();
		assertEquals(1, stats.getQueryExecutionCount() );
		assertEquals(1, stats.getQueryStatistics(hql).getExecutionCount() );
		
		stats.setStatisticsEnabled(isStats);
	}

}
