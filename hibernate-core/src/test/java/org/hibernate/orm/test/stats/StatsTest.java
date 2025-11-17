/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stats;

import java.util.HashSet;

import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.stat.QueryStatistics;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Show the difference between fetch and load
 *
 * @author Emmanuel Bernard
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(xmlMappings = "org/hibernate/orm/test/stats/Continent.hbm.xml")
@SessionFactory(generateStatistics = true)
public class StatsTest {

	@Test
	public void testQueryStatGathering(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			final String continents = "from Continent";
			int results = session.createQuery( continents ).list().size();
			final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
			QueryStatistics continentStats = statistics.getQueryStatistics( continents );
			assertNotNull( continentStats, "stats were null" );
			assertEquals( 1, continentStats.getExecutionCount(), "unexpected execution count" );
			assertEquals( results, continentStats.getExecutionRowCount(), "unexpected row count" );
			long maxTime = continentStats.getExecutionMaxTime();
			assertEquals( maxTime, statistics.getQueryExecutionMaxTime() );

			session.createQuery( continents ).list().iterator();
			assertEquals( 2, continentStats.getExecutionCount(), "unexpected execution count" );
			assertEquals( 2, continentStats.getExecutionRowCount(), "unexpected row count" );


			try (ScrollableResults scrollableResults = session.createQuery( continents ).scroll()) {
				// same deal with scroll()...
				assertEquals( 2, continentStats.getExecutionCount(), "unexpected execution count" );
				assertEquals( 2, continentStats.getExecutionRowCount(), "unexpected row count" );
				// scroll through data because SybaseASE15Dialect throws NullPointerException
				// if data is not read before closing the ResultSet
				while ( scrollableResults.next() ) {
					// do nothing
				}
			}
		} );

		// explicitly check that statistics for "split queries" get collected
		// under the original query
		scope.getSessionFactory().getStatistics().clear();

		scope.inTransaction(session -> {
			final String localities = "from Locality";
			int results = session.createQuery( localities ).list().size();
			final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
			QueryStatistics localityStats = statistics.getQueryStatistics( localities );
			assertNotNull( localityStats, "stats were null" );
			// ...one for each split query
			assertEquals( 2, localityStats.getExecutionCount(), "unexpected execution count" );
			assertEquals( results, localityStats.getExecutionRowCount(), "unexpected row count" );
			long maxTime = localityStats.getExecutionMaxTime();
			assertEquals( maxTime, statistics.getQueryExecutionMaxTime() );
		} );

		// native sql queries
		scope.getSessionFactory().getStatistics().clear();

		scope.inTransaction( session -> {
			final String sql = "select id, name from Country";
			int results = session.createNativeQuery( sql ).addEntity( Country.class ).list().size();
			final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
			QueryStatistics sqlStats = statistics.getQueryStatistics( sql );
			assertNotNull( sqlStats, "sql stats were null" );
			assertEquals( 1, sqlStats.getExecutionCount(), "unexpected execution count" );
			assertEquals( results, sqlStats.getExecutionRowCount(), "unexpected row count" );
			long maxTime = sqlStats.getExecutionMaxTime();
			assertEquals( maxTime, statistics.getQueryExecutionMaxTime() );
		} );
	}

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( this::fillDb );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	private Continent fillDb(Session s) {
		Continent europe = new Continent();
		europe.setName( "Europe" );
		Country france = new Country();
		france.setName( "France" );
		europe.setCountries( new HashSet() );
		europe.getCountries().add( france );
		s.persist( france );
		s.persist( europe );
		return europe;
	}
}
