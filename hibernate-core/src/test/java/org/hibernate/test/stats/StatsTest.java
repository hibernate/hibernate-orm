/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.stats;

import java.util.HashSet;
import java.util.Iterator;

import org.hibernate.Hibernate;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.stat.QueryStatistics;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Show the difference between fetch and load
 *
 * @author Emmanuel Bernard
 */
public class StatsTest extends BaseUnitTestCase {
	public String[] getMappings() {
		return new String[] { "stats/Continent.hbm.xml" };
	}

	private Configuration buildBaseConfiguration() {
		return new Configuration()
				.addResource( "org/hibernate/test/stats/Continent.hbm.xml" )
				.setProperty( AvailableSettings.GENERATE_STATISTICS, "true" );
	}
//
//	@Test
//	@SuppressWarnings( {"UnusedAssignment"})
//	public void testCollectionFetchVsLoad() throws Exception {
//		SessionFactory sf = buildBaseConfiguration()
//				.setProperty( AvailableSettings.HBM2DDL_AUTO, "create-drop" )
//				.buildSessionFactory();
//
//		Session s = sf.openSession();
//		Transaction tx = s.beginTransaction();
//		Continent europe = fillDb(s);
//		tx.commit();
//		s.close();
//
//		s = sf.openSession();
//		tx = s.beginTransaction();
//		assertEquals(0, sf.getStatistics().getCollectionLoadCount() );
//		assertEquals(0,  sf.getStatistics().getCollectionFetchCount() );
//		Continent europe2 = (Continent) s.get( Continent.class, europe.getId() );
//		assertEquals("Lazy true: no collection should be loaded", 0, sf.getStatistics().getCollectionLoadCount() );
//		assertEquals( 0, sf.getStatistics().getCollectionFetchCount() );
//		europe2.getCountries().size();
//		assertEquals( 1, sf.getStatistics().getCollectionLoadCount() );
//		assertEquals("Explicit fetch of the collection state", 1, sf.getStatistics().getCollectionFetchCount() );
//		tx.commit();
//		s.close();
//
//		sf.getStatistics().clear();
//
//		s = sf.openSession();
//		tx = s.beginTransaction();
//		europe = fillDb(s);
//		tx.commit();
//		s.clear();
//		tx = s.beginTransaction();
//		assertEquals( 0, sf.getStatistics().getCollectionLoadCount() );
//		assertEquals( 0, sf.getStatistics().getCollectionFetchCount() );
//		europe2 = (Continent) s.createQuery(
//				"from " + Continent.class.getName() + " a join fetch a.countries where a.id = " + europe.getId()
//			).uniqueResult();
//		assertEquals( 1, sf.getStatistics().getCollectionLoadCount() );
//		assertEquals( "collection should be loaded in the same query as its parent", 0, sf.getStatistics().getCollectionFetchCount() );
//		tx.commit();
//		s.close();
//
//		// open a new SF
//		sf.close();
//		Configuration cfg = buildBaseConfiguration().setProperty( AvailableSettings.HBM2DDL_AUTO, "create-drop" );
//		cfg.buildMappings();
//		Collection coll = cfg.getCollectionMapping(Continent.class.getName() + ".countries");
//		coll.setFetchMode(FetchMode.JOIN);
//		coll.setLazy(false);
//		sf = cfg.buildSessionFactory();
//
//		s = sf.openSession();
//		tx = s.beginTransaction();
//		europe = fillDb(s);
//		tx.commit();
//		s.close();
//
//		s = sf.openSession();
//		tx = s.beginTransaction();
//		assertEquals( 0, sf.getStatistics().getCollectionLoadCount() );
//		assertEquals( 0, sf.getStatistics().getCollectionFetchCount() );
//		europe2 = (Continent) s.get( Continent.class, europe.getId() );
//		assertEquals( 1, sf.getStatistics().getCollectionLoadCount() );
//		assertEquals( "Should do direct load, not indirect second load when lazy false and JOIN", 0, sf.getStatistics().getCollectionFetchCount() );
//		tx.commit();
//		s.close();
//		sf.close();
//
//		// open yet another SF
//		sf.close();
//		cfg = buildBaseConfiguration().setProperty( AvailableSettings.HBM2DDL_AUTO, "create-drop" );
//		cfg.buildMappings();
//		coll = cfg.getCollectionMapping( Continent.class.getName() + ".countries" );
//		coll.setFetchMode(FetchMode.SELECT);
//		coll.setLazy(false);
//		sf = cfg.buildSessionFactory();
//
//		s = sf.openSession();
//		tx = s.beginTransaction();
//		europe = fillDb(s);
//		tx.commit();
//		s.close();
//
//		s = sf.openSession();
//		tx = s.beginTransaction();
//		assertEquals( 0, sf.getStatistics().getCollectionLoadCount() );
//		assertEquals( 0, sf.getStatistics().getCollectionFetchCount() );
//		europe2 = (Continent) s.get( Continent.class, europe.getId() );
//		assertEquals( 1, sf.getStatistics().getCollectionLoadCount() );
//		assertEquals( "Should do explicit collection load, not part of the first one", 1, sf.getStatistics().getCollectionFetchCount() );
//		for ( Object o : europe2.getCountries() ) {
//			s.delete( o );
//		}
//		cleanDb( s );
//		tx.commit();
//		s.close();
//
//		sf.close();
//	}

	@Test
	public void testQueryStatGathering() {
		SessionFactory sf = buildBaseConfiguration()
				.setProperty( AvailableSettings.HBM2DDL_AUTO, "create-drop" )
				.buildSessionFactory();

		Session s = sf.openSession();
		Transaction tx = s.beginTransaction();
		fillDb(s);
		tx.commit();
		s.close();

		s = sf.openSession();
		tx = s.beginTransaction();
		final String continents = "from Continent";
		int results = s.createQuery( continents ).list().size();
		QueryStatistics continentStats = sf.getStatistics().getQueryStatistics( continents );
		assertNotNull( "stats were null",  continentStats );
		assertEquals( "unexpected execution count", 1, continentStats.getExecutionCount() );
		assertEquals( "unexpected row count", results, continentStats.getExecutionRowCount() );
		long maxTime = continentStats.getExecutionMaxTime();
		assertEquals( maxTime, sf.getStatistics().getQueryExecutionMaxTime() );
//		assertEquals( continents, stats.getQueryExecutionMaxTimeQueryString() );

		Iterator itr = s.createQuery( continents ).iterate();
		// iterate() should increment the execution count
		assertEquals( "unexpected execution count", 2, continentStats.getExecutionCount() );
		// but should not effect the cumulative row count
		assertEquals( "unexpected row count", results, continentStats.getExecutionRowCount() );
		Hibernate.close( itr );

		ScrollableResults scrollableResults = s.createQuery( continents ).scroll();
		// same deal with scroll()...
		assertEquals( "unexpected execution count", 3, continentStats.getExecutionCount() );
		assertEquals( "unexpected row count", results, continentStats.getExecutionRowCount() );
		// scroll through data because SybaseASE15Dialect throws NullPointerException
		// if data is not read beforeQuery closing the ResultSet
		while ( scrollableResults.next() ) {
			// do nothing
		}
		scrollableResults.close();
		tx.commit();
		s.close();

		// explicitly check that statistics for "split queries" get collected
		// under the original query
		sf.getStatistics().clear();

		s = sf.openSession();
		tx = s.beginTransaction();
		final String localities = "from Locality";
		results = s.createQuery( localities ).list().size();
		QueryStatistics localityStats = sf.getStatistics().getQueryStatistics( localities );
		assertNotNull( "stats were null",  localityStats );
		// ...one for each split query
		assertEquals( "unexpected execution count", 2, localityStats.getExecutionCount() );
		assertEquals( "unexpected row count", results, localityStats.getExecutionRowCount() );
		maxTime = localityStats.getExecutionMaxTime();
		assertEquals( maxTime, sf.getStatistics().getQueryExecutionMaxTime() );
//		assertEquals( localities, stats.getQueryExecutionMaxTimeQueryString() );
		tx.commit();
		s.close();
		assertFalse( s.isOpen() );

		// native sql queries
		sf.getStatistics().clear();

		s = sf.openSession();
		tx = s.beginTransaction();
		final String sql = "select id, name from Country";
		results = s.createSQLQuery( sql ).addEntity( Country.class ).list().size();
		QueryStatistics sqlStats = sf.getStatistics().getQueryStatistics( sql );
		assertNotNull( "sql stats were null", sqlStats );
		assertEquals( "unexpected execution count", 1, sqlStats.getExecutionCount() );
		assertEquals( "unexpected row count", results, sqlStats.getExecutionRowCount() );
		maxTime = sqlStats.getExecutionMaxTime();
		assertEquals( maxTime, sf.getStatistics().getQueryExecutionMaxTime() );
//		assertEquals( sql, stats.getQueryExecutionMaxTimeQueryString() );
		tx.commit();
		s.close();

		s = sf.openSession();
		tx = s.beginTransaction();
		cleanDb( s );
		tx.commit();
		s.close();
		sf.close();
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

	private void cleanDb(Session s) {
		s.createQuery( "delete Locality" ).executeUpdate();
		s.createQuery( "delete Country" ).executeUpdate();
		s.createQuery( "delete Continent" ).executeUpdate();
	}

}
