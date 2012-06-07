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
package org.hibernate.test.stats;

import java.util.HashSet;

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.stat.SessionStatistics;
import org.hibernate.stat.Statistics;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Emmanuel Bernard
 */
public class SessionStatsTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "stats/Continent2.hbm.xml" };
	}

	@Test
	public void testSessionStatistics() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Statistics stats = sessionFactory().getStatistics();
		stats.clear();
		boolean isStats = stats.isStatisticsEnabled();
		stats.setStatisticsEnabled(true);
		Continent europe = fillDb(s);
		tx.commit();
		s.clear();
		tx = s.beginTransaction();
		SessionStatistics sessionStats = s.getStatistics();
		assertEquals( 0, sessionStats.getEntityKeys().size() );
		assertEquals( 0, sessionStats.getEntityCount() );
		assertEquals( 0, sessionStats.getCollectionKeys().size() );
		assertEquals( 0, sessionStats.getCollectionCount() );
		europe = (Continent) s.get( Continent.class, europe.getId() );
		Hibernate.initialize( europe.getCountries() );
		Hibernate.initialize( europe.getCountries().iterator().next() );
		assertEquals( 2, sessionStats.getEntityKeys().size() );
		assertEquals( 2, sessionStats.getEntityCount() );
		assertEquals( 1, sessionStats.getCollectionKeys().size() );
		assertEquals( 1, sessionStats.getCollectionCount() );
		tx.commit();
		s.close();

		stats.setStatisticsEnabled( isStats);

	}

	private Continent fillDb(Session s) {
		Continent europe = new Continent();
		europe.setName("Europe");
		Country france = new Country();
		france.setName("France");
		europe.setCountries( new HashSet() );
		europe.getCountries().add(france);
		s.persist(france);
		s.persist(europe);
		return europe;
	}

}
