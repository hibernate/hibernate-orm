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
package org.hibernate.test.stats;

import java.util.HashSet;
import java.util.Iterator;

import org.junit.Test;

import org.hibernate.FetchMode;
import org.hibernate.Hibernate;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.mapping.Collection;
import org.hibernate.stat.QueryStatistics;
import org.hibernate.stat.Statistics;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Show the difference between fetch and load
 *
 * @author Emmanuel Bernard
 */
public class StatsTest extends BaseCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[] { "stats/Continent.hbm.xml" };
	}

	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
	}

	@Test
	@SuppressWarnings( {"UnusedAssignment"})
	public void testCollectionFetchVsLoad() throws Exception {
		Statistics stats = sessionFactory().getStatistics();
		stats.clear();

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Continent europe = fillDb(s);
		tx.commit();
		s.clear();

		tx = s.beginTransaction();
		assertEquals(0, stats.getCollectionLoadCount() );
		assertEquals(0,  stats.getCollectionFetchCount() );
		Continent europe2 = (Continent) s.get( Continent.class, europe.getId() );
		assertEquals("Lazy true: no collection should be loaded", 0, stats.getCollectionLoadCount() );
		assertEquals( 0, stats.getCollectionFetchCount() );
		europe2.getCountries().size();
		assertEquals( 1, stats.getCollectionLoadCount() );
		assertEquals("Explicit fetch of the collection state", 1, stats.getCollectionFetchCount() );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		stats.clear();
		europe = fillDb(s);
		tx.commit();
		s.clear();
		tx = s.beginTransaction();
		assertEquals( 0, stats.getCollectionLoadCount() );
		assertEquals( 0, stats.getCollectionFetchCount() );
		europe2 = (Continent) s.createQuery(
				"from " + Continent.class.getName() + " a join fetch a.countries where a.id = " + europe.getId()
			).uniqueResult();
		assertEquals( 1, stats.getCollectionLoadCount() );
		assertEquals( "collection should be loaded in the same query as its parent", 0, stats.getCollectionFetchCount() );
		tx.commit();
		s.close();

		// open second SessionFactory
		Collection coll = configuration().getCollectionMapping(Continent.class.getName() + ".countries");
		coll.setFetchMode(FetchMode.JOIN);
		coll.setLazy(false);
		SessionFactory sf = configuration().buildSessionFactory();
		stats = sf.getStatistics();
		stats.clear();
		stats.setStatisticsEnabled(true);
		s = sf.openSession();
		tx = s.beginTransaction();
		europe = fillDb(s);
		tx.commit();
		s.clear();
		tx = s.beginTransaction();
		assertEquals( 0, stats.getCollectionLoadCount() );
		assertEquals( 0, stats.getCollectionFetchCount() );
		europe2 = (Continent) s.get( Continent.class, europe.getId() );
		assertEquals( 1, stats.getCollectionLoadCount() );
		assertEquals( "Should do direct load, not indirect second load when lazy false and JOIN", 0, stats.getCollectionFetchCount() );
		tx.commit();
		s.close();
		sf.close();

		// open third SessionFactory
		coll = configuration().getCollectionMapping(Continent.class.getName() + ".countries");
		coll.setFetchMode(FetchMode.SELECT);
		coll.setLazy(false);
		sf = configuration().buildSessionFactory();
		stats = sf.getStatistics();
		stats.clear();
		stats.setStatisticsEnabled(true);
		s = sf.openSession();
		tx = s.beginTransaction();
		europe = fillDb(s);
		tx.commit();
		s.clear();
		tx = s.beginTransaction();
		assertEquals( 0, stats.getCollectionLoadCount() );
		assertEquals( 0, stats.getCollectionFetchCount() );
		europe2 = (Continent) s.get( Continent.class, europe.getId() );
		assertEquals( 1, stats.getCollectionLoadCount() );
		assertEquals( "Should do explicit collection load, not part of the first one", 1, stats.getCollectionFetchCount() );
		for ( Object o : europe2.getCountries() ) {
			s.delete( o );
		}
		cleanDb( s );
		tx.commit();
		s.close();
	}

	@Test
	public void testQueryStatGathering() {
		Statistics stats = sessionFactory().getStatistics();
		stats.clear();

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		fillDb(s);
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		final String continents = "from Continent";
		int results = s.createQuery( continents ).list().size();
		QueryStatistics continentStats = stats.getQueryStatistics( continents );
		assertNotNull( "stats were null",  continentStats );
		assertEquals( "unexpected execution count", 1, continentStats.getExecutionCount() );
		assertEquals( "unexpected row count", results, continentStats.getExecutionRowCount() );
		long maxTime = continentStats.getExecutionMaxTime();
		assertEquals( maxTime, stats.getQueryExecutionMaxTime() );
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
		// if data is not read before closing the ResultSet
		while ( scrollableResults.next() ) {
			// do nothing
		}
		scrollableResults.close();
		tx.commit();
		s.close();

		// explicitly check that statistics for "split queries" get collected
		// under the original query
		stats.clear();
		s = openSession();
		tx = s.beginTransaction();
		final String localities = "from Locality";
		results = s.createQuery( localities ).list().size();
		QueryStatistics localityStats = stats.getQueryStatistics( localities );
		assertNotNull( "stats were null",  localityStats );
		// ...one for each split query
		assertEquals( "unexpected execution count", 2, localityStats.getExecutionCount() );
		assertEquals( "unexpected row count", results, localityStats.getExecutionRowCount() );
		maxTime = localityStats.getExecutionMaxTime();
		assertEquals( maxTime, stats.getQueryExecutionMaxTime() );
//		assertEquals( localities, stats.getQueryExecutionMaxTimeQueryString() );
		tx.commit();
		s.close();
		assertFalse( s.isOpen() );

		// native sql queries
		stats.clear();
		s = openSession();
		tx = s.beginTransaction();
		final String sql = "select id, name from Country";
		results = s.createSQLQuery( sql ).addEntity( Country.class ).list().size();
		QueryStatistics sqlStats = stats.getQueryStatistics( sql );
		assertNotNull( "sql stats were null", sqlStats );
		assertEquals( "unexpected execution count", 1, sqlStats.getExecutionCount() );
		assertEquals( "unexpected row count", results, sqlStats.getExecutionRowCount() );
		maxTime = sqlStats.getExecutionMaxTime();
		assertEquals( maxTime, stats.getQueryExecutionMaxTime() );
//		assertEquals( sql, stats.getQueryExecutionMaxTimeQueryString() );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		cleanDb( s );
		tx.commit();
		s.close();
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
