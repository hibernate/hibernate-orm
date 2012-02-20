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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Immutable;
import org.hibernate.cfg.Configuration;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * Test case for NaturalId annotation on an {@link Immutable} entity
 *
 * @author Eric Dalquist
 * @see https://hibernate.onjira.com/browse/HHH-7085
 */
@SuppressWarnings("unchecked")
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
	public void testImmutableNaturalIdLifecycle() {
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
		
//		Statistics stats = sessionFactory().getStatistics();
//		assertEquals(
//				"Cache hits should be empty", 0, stats
//						.getNaturalIdCacheHitCount()
//		);
//		assertEquals(
//				"First load should be a miss", 1, stats
//						.getNaturalIdCacheMissCount()
//		);
//		assertEquals(
//				"Query result should be added to cache", 3, stats
//						.getNaturalIdCachePutCount()
//		);
//
//		Session s = openSession();
//		Transaction tx = s.beginTransaction();
//		State france = ( State ) s.load( State.class, 2 );
//		final NaturalIdLoadAccess naturalIdLoader = s.byNaturalId( Citizen.class );
//		naturalIdLoader.using( "ssn", "1234" ).using( "state", france );
//
//		//Not clearing naturalId caches, should be warm from entity loading
//		stats.setStatisticsEnabled( true );
//		stats.clear();
//		assertEquals(
//				"Cache hits should be empty", 0, stats
//						.getNaturalIdCacheHitCount()
//		);
//
//		// first query
//		Citizen citizen = (Citizen)naturalIdLoader.load();
//		assertNotNull( citizen );
//		assertEquals(
//				"Cache hits should be empty", 1, stats
//						.getNaturalIdCacheHitCount()
//		);
//		assertEquals(
//				"First load should be a miss", 0, stats
//						.getNaturalIdCacheMissCount()
//		);
//		assertEquals(
//				"Query result should be added to cache", 0, stats
//						.getNaturalIdCachePutCount()
//		);
//
//		// cleanup
//		tx.rollback();
//		s.close();
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
