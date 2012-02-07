/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.annotations.naturalid;

import java.util.List;

import org.jboss.logging.Logger;
import org.junit.Test;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.stat.Statistics;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test case for NaturalId annotation. See ANN-750.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
@SuppressWarnings("unchecked")
@TestForIssue( jiraKey = "ANN-750" )
public class NaturalIdOnSingleManyToOneTest extends BaseCoreFunctionalTestCase {
	private static final Logger log = Logger.getLogger( NaturalIdOnSingleManyToOneTest.class );

	@Test
	public void testMappingProperties() {
        log.warn("Commented out test");

		ClassMetadata metaData = sessionFactory().getClassMetadata(
				NaturalIdOnManyToOne.class
		);
		assertTrue(
				"Class should have a natural key", metaData
						.hasNaturalIdentifier()
		);
		int[] propertiesIndex = metaData.getNaturalIdentifierProperties();
		assertTrue( "Wrong number of elements", propertiesIndex.length == 1 );
	}

	@Test
	public void testManyToOneNaturalIdCached() {
		NaturalIdOnManyToOne singleManyToOne = new NaturalIdOnManyToOne();
		Citizen c1 = new Citizen();
		c1.setFirstname( "Emmanuel" );
		c1.setLastname( "Bernard" );
		c1.setSsn( "1234" );

		State france = new State();
		france.setName( "Ile de France" );
		c1.setState( france );

		singleManyToOne.setCitizen( c1 );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( france );
		s.persist( c1 );
		s.persist( singleManyToOne );
		tx.commit();
		s.close();
		
		//Clear naturalId cache that was populated when putting the data
		s.getSessionFactory().getCache().evictNaturalIdRegions();
		
		s = openSession();
		tx = s.beginTransaction();
		Criteria criteria = s.createCriteria( NaturalIdOnManyToOne.class );
		criteria.add( Restrictions.naturalId().set( "citizen", c1 ) );
		criteria.setCacheable( true );

		Statistics stats = sessionFactory().getStatistics();
		stats.setStatisticsEnabled( true );
		stats.clear();
		assertEquals(
				"Cache hits should be empty", 0, stats
						.getNaturalIdCacheHitCount()
		);

		// first query
		List results = criteria.list();
		assertEquals( 1, results.size() );
		assertEquals(
				"Cache hits should be empty", 0, stats
						.getNaturalIdCacheHitCount()
		);
		assertEquals(
				"First query should be a miss", 1, stats
						.getNaturalIdCacheMissCount()
		);
		assertEquals(
				"Query result should be added to cache", 1, stats
						.getNaturalIdCachePutCount()
		);
		assertEquals(
				"Query count should be one", 1, stats
						.getNaturalIdQueryExecutionCount()
		);

		// query a second time - result should be in session cache
		criteria.list();
		assertEquals(
				"Cache hits should be empty", 0, stats
						.getNaturalIdCacheHitCount()
		);
		assertEquals(
				"Second query should not be a miss", 1, stats
						.getNaturalIdCacheMissCount()
		);
		assertEquals(
				"Query count should be one", 1, stats
						.getNaturalIdQueryExecutionCount()
		);

		// cleanup
		tx.rollback();
		s.close();
	}

	@Override
    protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Citizen.class, State.class,
				NaturalIdOnManyToOne.class
		};
	}

	@Override
    protected void configure(Configuration cfg) {
		cfg.setProperty( "hibernate.cache.use_query_cache", "true" );
	}
}
