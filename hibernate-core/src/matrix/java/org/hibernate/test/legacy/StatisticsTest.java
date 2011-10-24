/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2006-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.legacy;

import org.junit.Test;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.stat.Statistics;

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
