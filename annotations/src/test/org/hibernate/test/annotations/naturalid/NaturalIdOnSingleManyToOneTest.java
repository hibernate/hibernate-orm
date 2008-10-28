//$Id$
package org.hibernate.test.annotations.naturalid;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.stat.Statistics;
import org.hibernate.test.annotations.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test case for NaturalId annotation. See ANN-750.
 * 
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
@SuppressWarnings("unchecked")
public class NaturalIdOnSingleManyToOneTest extends TestCase {
	
	private Logger log = LoggerFactory.getLogger(NaturalIdOnManyToOne.class);

	public void testMappingProperties() {
		log.warn("Commented out test");
		
		ClassMetadata metaData = getSessions().getClassMetadata(
				NaturalIdOnManyToOne.class);
		assertTrue("Class should have a natural key", metaData
				.hasNaturalIdentifier());
		int[] propertiesIndex = metaData.getNaturalIdentifierProperties();
		assertTrue("Wrong number of elements", propertiesIndex.length == 1);
	}

	public void testManyToOneNaturalIdCached() {
		NaturalIdOnManyToOne singleManyToOne = new NaturalIdOnManyToOne();
		Citizen c1 = new Citizen();
		c1.setFirstname("Emmanuel");
		c1.setLastname("Bernard");
		c1.setSsn("1234");

		State france = new State();
		france.setName("Ile de France");
		c1.setState(france);

		singleManyToOne.setCitizen(c1);

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist(france);
		s.persist(c1);
		s.persist(singleManyToOne);
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		Criteria criteria = s.createCriteria(NaturalIdOnManyToOne.class);
		criteria.add(Restrictions.naturalId().set("citizen", c1));
		criteria.setCacheable(true);

		Statistics stats = getSessions().getStatistics();
		stats.setStatisticsEnabled(true);
		stats.clear();
		assertEquals("Cache hits should be empty", 0, stats
				.getQueryCacheHitCount());

		// first query
		List results = criteria.list();
		assertEquals(1, results.size());
		assertEquals("Cache hits should be empty", 0, stats
				.getQueryCacheHitCount());
		assertEquals("First query should be a miss", 1, stats
				.getQueryCacheMissCount());
		assertEquals("Query result should be added to cache", 1, stats
				.getQueryCachePutCount());

		// query a second time - result should be cached
		results = criteria.list();
		assertEquals("Cache hits should be empty", 1, stats
				.getQueryCacheHitCount());

		// cleanup
		tx.rollback();
		s.close();
	}

	protected Class[] getMappings() {
		return new Class[] { Citizen.class, State.class,
				NaturalIdOnManyToOne.class };
	}
}
