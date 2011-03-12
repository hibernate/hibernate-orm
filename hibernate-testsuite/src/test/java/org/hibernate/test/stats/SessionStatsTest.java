//$Id: SessionStatsTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.stats;

import java.util.HashSet;

import junit.framework.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.stat.SessionStatistics;
import org.hibernate.stat.Statistics;

/**
 * @author Emmanuel Bernard
 */
public class SessionStatsTest extends FunctionalTestCase {

	public SessionStatsTest(String x) {
		super(x);
	}

	public String[] getMappings() {
		return new String[] { "stats/Continent2.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( SessionStatsTest.class );
	}

	public void testSessionStatistics() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Statistics stats = getSessions().getStatistics();
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
