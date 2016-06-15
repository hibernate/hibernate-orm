/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.naturalid.immutableentity;

import org.hibernate.NaturalIdLoadAccess;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Immutable;
import org.hibernate.cfg.Configuration;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.tuple.entity.EntityMetamodel;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test case for NaturalId annotation on an {@link Immutable} entity
 *
 * @author Eric Dalquist
 */
@SuppressWarnings("unchecked")
@TestForIssue( jiraKey = "HHH-7085" )
public class ImmutableEntityNaturalIdTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testMappingProperties() {
		ClassMetadata metaData = sessionFactory().getClassMetadata(
				Building.class
		);
		assertTrue(
				"Class should have a natural key", metaData
						.hasNaturalIdentifier()
		);
		int[] propertiesIndex = metaData.getNaturalIdentifierProperties();
		assertEquals( "Wrong number of elements", 3, propertiesIndex.length );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-10360")
	public void testNaturalIdNullability() {
		final EntityPersister persister = sessionFactory().getEntityPersister( Building.class.getName() );
		final EntityMetamodel entityMetamodel = persister.getEntityMetamodel();
		// nullability is not specified, so they should be nullable by annotations-specific default
		assertTrue( persister.getPropertyNullability()[entityMetamodel.getPropertyIndex( "address" )] );
		assertTrue( persister.getPropertyNullability()[entityMetamodel.getPropertyIndex( "city" )] );
		assertTrue( persister.getPropertyNullability()[entityMetamodel.getPropertyIndex( "state" )] );
	}

	@Test
	public void testImmutableNaturalIdLifecycle() {
		Statistics stats = sessionFactory().getStatistics();
		stats.setStatisticsEnabled( true );
		stats.clear();

		assertEquals( "Cache hits should be empty", 0, stats.getNaturalIdCacheHitCount() );
		assertEquals( "Cache misses should be empty", 0, stats.getNaturalIdCacheMissCount() );
		assertEquals( "Cache put should be empty", 0, stats.getNaturalIdCachePutCount() );
		assertEquals( "Query count should be empty", 0, stats.getNaturalIdQueryExecutionCount() );
		
		Building b1 = new Building();
		b1.setName( "Computer Science" );
		b1.setAddress( "1210 W. Dayton St." );
		b1.setCity( "Madison" );
		b1.setState( "WI" );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( b1 );
		tx.commit();
		s.close();

		assertEquals( "Cache hits should be empty", 0, stats.getNaturalIdCacheHitCount() );
		assertEquals( "Cache misses should be empty", 0, stats.getNaturalIdCacheMissCount() );
		assertEquals( "Cache put should be one afterQuery insert", 1, stats.getNaturalIdCachePutCount() );
		assertEquals( "Query count should be empty", 0, stats.getNaturalIdQueryExecutionCount() );
		
		s = openSession();
		tx = s.beginTransaction();
		
		//Clear caches and reset cache stats
		s.getSessionFactory().getCache().evictNaturalIdRegions();
		stats.clear();
		
		NaturalIdLoadAccess naturalIdLoader = s.byNaturalId( Building.class );
		naturalIdLoader.using( "address", "1210 W. Dayton St." ).using( "city", "Madison" ).using( "state", "WI" );

		// first query
		Building building = (Building) naturalIdLoader.load();
		assertNotNull( building );
		assertEquals( "Cache hits should be empty", 0, stats.getNaturalIdCacheHitCount() );
		assertEquals( "Cache misses should be one afterQuery first query", 1, stats.getNaturalIdCacheMissCount() );
		assertEquals( "Cache put should be one afterQuery first query", 1, stats.getNaturalIdCachePutCount() );
		assertEquals( "Query count should be one afterQuery first query", 1, stats.getNaturalIdQueryExecutionCount() );

		// cleanup
		tx.rollback();
		s.close();
		
		//Try two, should be a cache hit
		
		s = openSession();
		tx = s.beginTransaction();
		naturalIdLoader = s.byNaturalId( Building.class );
		naturalIdLoader.using( "address", "1210 W. Dayton St." ).using( "city", "Madison" ).using( "state", "WI" );

		// second query
		building = (Building) naturalIdLoader.load();
		assertNotNull( building );
		assertEquals( "Cache hits should be one afterQuery second query", 1, stats.getNaturalIdCacheHitCount() );
		assertEquals( "Cache misses should be one afterQuery second query", 1, stats.getNaturalIdCacheMissCount() );
		assertEquals( "Cache put should be one afterQuery second query", 1, stats.getNaturalIdCachePutCount() );
		assertEquals( "Query count should be one afterQuery second query", 1, stats.getNaturalIdQueryExecutionCount() );
		
		// Try Deleting
		s.delete( building );
		
		// third query
		building = (Building) naturalIdLoader.load();
		assertNull( building );
		assertEquals( "Cache hits should be one afterQuery second query", 1, stats.getNaturalIdCacheHitCount() );
		assertEquals( "Cache misses should be two afterQuery second query", 2, stats.getNaturalIdCacheMissCount() );
		assertEquals( "Cache put should be one afterQuery second query", 2, stats.getNaturalIdCachePutCount() );
		assertEquals( "Query count should be two afterQuery second query", 2, stats.getNaturalIdQueryExecutionCount() );

		// cleanup
		tx.commit();
		s.close();
		
		//Try three, should be db lookup and miss
		
		s = openSession();
		tx = s.beginTransaction();
		naturalIdLoader = s.byNaturalId( Building.class );
		naturalIdLoader.using( "address", "1210 W. Dayton St." ).using( "city", "Madison" ).using( "state", "WI" );

		// second query
		building = (Building) naturalIdLoader.load();
		assertNull( building );
		assertEquals( "Cache hits should be one afterQuery third query", 1, stats.getNaturalIdCacheHitCount() );
		assertEquals( "Cache misses should be one afterQuery third query", 3, stats.getNaturalIdCacheMissCount() );
		assertEquals( "Cache put should be one afterQuery third query", 2, stats.getNaturalIdCachePutCount() );
		assertEquals( "Query count should be one afterQuery third query", 3, stats.getNaturalIdQueryExecutionCount() );

		// cleanup
		tx.rollback();
		s.close();
	}
	
	@Test
	@TestForIssue( jiraKey = "HHH-7371" )
	public void testImmutableNaturalIdLifecycle2() {
		Building b1 = new Building();
		b1.setName( "Computer Science" );
		b1.setAddress( "1210 W. Dayton St." );
		b1.setCity( "Madison" );
		b1.setState( "WI" );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( b1 );
		tx.commit();
		s.close();


		s = openSession();
		tx = s.beginTransaction();
		NaturalIdLoadAccess naturalIdLoader = s.byNaturalId( Building.class );
		naturalIdLoader.using( "address", "1210 W. Dayton St." ).using( "city", "Madison" ).using( "state", "WI" );
		Building building = (Building) naturalIdLoader.getReference();
		assertNotNull( building );

		s.delete( building );
		building = (Building) naturalIdLoader.load();
		//org.hibernate.ObjectNotFoundException: No row with the given identifier exists: [org.hibernate.test.naturalid.immutableentity.Building#1]
//		at org.hibernate.internal.SessionFactoryImpl$1$1.handleEntityNotFound(SessionFactoryImpl.java:247)
//		at org.hibernate.event.internal.DefaultLoadEventListener.returnNarrowedProxy(DefaultLoadEventListener.java:282)
//		at org.hibernate.event.internal.DefaultLoadEventListener.proxyOrLoad(DefaultLoadEventListener.java:248)
//		at org.hibernate.event.internal.DefaultLoadEventListener.onLoad(DefaultLoadEventListener.java:148)
//		at org.hibernate.internal.SessionImpl.fireLoad(SessionImpl.java:1079)
//		at org.hibernate.internal.SessionImpl.access$13(SessionImpl.java:1075)
//		at org.hibernate.internal.SessionImpl$IdentifierLoadAccessImpl.load(SessionImpl.java:2425)
//		at org.hibernate.internal.SessionImpl$NaturalIdLoadAccessImpl.load(SessionImpl.java:2586)
//		at org.hibernate.test.naturalid.immutableentity.ImmutableEntityNaturalIdTest.testImmutableNaturalIdLifecycle2(ImmutableEntityNaturalIdTest.java:188)

		assertNull( building );

		tx.commit();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Building.class
		};
	}

	@Override
	protected void configure(Configuration cfg) {
		cfg.setProperty( "hibernate.cache.use_query_cache", "true" );
	}
}
