/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jcache;

import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.cache.spi.support.AbstractReadWriteAccess;
import org.hibernate.cache.spi.support.DomainDataRegionTemplate;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.orm.test.jcache.domain.Event;
import org.hibernate.orm.test.jcache.domain.EventManager;
import org.hibernate.orm.test.jcache.domain.Item;
import org.hibernate.orm.test.jcache.domain.Person;
import org.hibernate.orm.test.jcache.domain.PhoneNumber;
import org.hibernate.orm.test.jcache.domain.VersionedItem;
import org.hibernate.stat.CacheRegionStatistics;
import org.hibernate.stat.QueryStatistics;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.orm.junit.ExtraAssertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Chris Dennis
 * @author Brett Meyer
 */
public class HibernateCacheTest extends BaseFunctionalTest {
	@Test
	public void testQueryCacheInvalidation() {
		Session s = sessionFactory().openSession();
		Transaction t = s.beginTransaction();
		Item i = new Item();
		i.setName( "widget" );
		i.setDescription( "A really top-quality, full-featured widget." );
		s.persist( i );
		t.commit();
		s.close();

		CacheRegionStatistics slcs = sessionFactory()
				.getStatistics()
				.getDomainDataRegionStatistics( Item.class.getName() );

		assertThat( slcs.getPutCount(), equalTo( 1L ) );
		assertTrue( sessionFactory().getCache().containsEntity( Item.class, i.getId() ) );

		s = sessionFactory().openSession();
		t = s.beginTransaction();
		i = s.get( Item.class, i.getId() );

		assertThat( slcs.getHitCount(), equalTo( 1L ) );
		assertThat( slcs.getMissCount(), equalTo( 0L ) );

		i.setDescription( "A bog standard item" );

		t.commit();
		s.close();

		assertThat( slcs.getPutCount(), equalTo( 2L ) );
		assertTrue( sessionFactory().getCache().containsEntity( Item.class, i.getId() ) );

		final DomainDataRegionTemplate region = (DomainDataRegionTemplate) sessionFactory().getMappingMetamodel()
				.getEntityDescriptor( Item.class )
				.getCacheAccessStrategy()
				.getRegion();
		final Object fromCache = region.getCacheStorageAccess().getFromCache(
				region.getEffectiveKeysFactory().createEntityKey(
						i.getId(),
						sessionFactory().getMappingMetamodel().getEntityDescriptor( Item.class ),
						sessionFactory(),
						null
				),
				(SharedSessionContractImplementor) s
		);
		assertNotNull( fromCache );
		ExtraAssertions.assertTyping( AbstractReadWriteAccess.Item.class, fromCache );
//		assertThat( (String) map.get( "description" ), equalTo( "A bog standard item" ) );
//		assertThat( (String) map.get( "name" ), equalTo( "widget" ) );

		// cleanup
		s = sessionFactory().openSession();
		t = s.beginTransaction();
		s.remove( i );
		t.commit();
		s.close();
	}

//	@Test
//	public void testEmptySecondLevelCacheEntry() throws Exception {
//		sessionFactory().getCache().evictEntityRegion( Item.class.getName() );
//		Statistics stats = sessionFactory().getStatistics();
//		stats.clear();
//		CacheRegionStatistics statistics = stats.getDomainDataRegionStatistics( Item.class.getName() );
//		Map cacheEntries = statistics.getEntries();
//		assertThat( cacheEntries.size(), equalTo( 0 ) );
//	}

	@Test
	public void testStaleWritesLeaveCacheConsistent() {
		Session s = sessionFactory().openSession();
		Transaction txn = s.beginTransaction();
		VersionedItem item = new VersionedItem();
		item.setName( "steve" );
		item.setDescription( "steve's item" );
		s.persist( item );
		txn.commit();
		s.close();

		Long initialVersion = item.getVersion();

		// manually revert the version property
		item.setVersion( item.getVersion() - 1 );

		try {
			s = sessionFactory().openSession();
			txn = s.beginTransaction();
			s.merge( item );
			txn.commit();
			s.close();
			fail( "expected stale write to fail" );
		}
		catch ( Throwable expected ) {
			// expected behavior here
			if ( txn != null ) {
				try {
					txn.rollback();
				}
				catch ( Throwable ignore ) {
				}
			}
			if ( expected instanceof AssertionError ) {
				throw (AssertionError) expected;
			}
		}
		finally {
			if ( s != null && s.isOpen() ) {
				try {
					s.close();
				}
				catch ( Throwable ignore ) {
				}
			}
		}

//		// check the version value in the cache...
//		CacheRegionStatistics slcs = sessionFactory().getStatistics()
//				.getDomainDataRegionStatistics( VersionedItem.class.getName() );
//		assertNotNull(slcs);
//		final Map entries = slcs.getEntries();
//		Object entry = entries.get( item.getId() );
//		Long cachedVersionValue;
//		if ( entry instanceof SoftLock ) {
//			//FIXME don't know what to test here
//			//cachedVersionValue = new Long( ( (ReadWriteCache.Lock) entry).getUnlockTimestamp() );
//		}
//		else {
//			cachedVersionValue = (Long) ( (Map) entry ).get( "_version" );
//			assertThat( initialVersion, equalTo( cachedVersionValue ) );
//		}

		final DomainDataRegionTemplate region = (DomainDataRegionTemplate) sessionFactory().getMappingMetamodel()
				.getEntityDescriptor( Item.class )
				.getCacheAccessStrategy()
				.getRegion();
		final Object fromCache = region.getCacheStorageAccess().getFromCache(
				region.getEffectiveKeysFactory().createEntityKey(
						item.getId(),
						sessionFactory().getMappingMetamodel().getEntityDescriptor( Item.class ),
						sessionFactory(),
						null
				),
				(SharedSessionContractImplementor) s
		);
		assertTrue(
				fromCache == null || fromCache instanceof SoftLock
		);

		// cleanup
		s = sessionFactory().openSession();
		txn = s.beginTransaction();
		item = s.getReference( VersionedItem.class, item.getId() );
		s.remove( item );
		txn.commit();
		s.close();

	}

	@Test
	public void testGeneralUsage() {
		EventManager mgr = new EventManager( sessionFactory() );
		Statistics stats = sessionFactory().getStatistics();

		// create 3 persons Steve, Orion, Tim
		Person stevePerson = new Person();
		stevePerson.setFirstname( "Steve" );
		stevePerson.setLastname( "Harris" );
		Long steveId = mgr.createAndStorePerson( stevePerson );
		mgr.addEmailToPerson( steveId, "steve@tc.com" );
		mgr.addEmailToPerson( steveId, "sharrif@tc.com" );
		mgr.addTalismanToPerson( steveId, "rabbit foot" );
		mgr.addTalismanToPerson( steveId, "john de conqueroo" );

		PhoneNumber p1 = new PhoneNumber();
		p1.setNumberType( "Office" );
		p1.setPhone( 111111 );
		mgr.addPhoneNumberToPerson( steveId, p1 );

		PhoneNumber p2 = new PhoneNumber();
		p2.setNumberType( "Home" );
		p2.setPhone( 222222 );
		mgr.addPhoneNumberToPerson( steveId, p2 );

		Person orionPerson = new Person();
		orionPerson.setFirstname( "Orion" );
		orionPerson.setLastname( "Letizi" );
		Long orionId = mgr.createAndStorePerson( orionPerson );
		mgr.addEmailToPerson( orionId, "orion@tc.com" );
		mgr.addTalismanToPerson( orionId, "voodoo doll" );

		Long timId = mgr.createAndStorePerson( "Tim", "Teck" );
		mgr.addEmailToPerson( timId, "teck@tc.com" );
		mgr.addTalismanToPerson( timId, "magic decoder ring" );

		Long engMeetingId = mgr.createAndStoreEvent( "Eng Meeting", stevePerson, new Date() );
		mgr.addPersonToEvent( steveId, engMeetingId );
		mgr.addPersonToEvent( orionId, engMeetingId );
		mgr.addPersonToEvent( timId, engMeetingId );

		Long docMeetingId = mgr.createAndStoreEvent( "Doc Meeting", orionPerson, new Date() );
		mgr.addPersonToEvent( steveId, docMeetingId );
		mgr.addPersonToEvent( orionId, docMeetingId );

		for ( Event event : (List<Event>) mgr.listEvents() ) {
			mgr.listEmailsOfEvent( event.getId() );
		}

		QueryStatistics queryStats = stats.getQueryStatistics( "from Event" );
		assertThat( "Cache Miss Count", queryStats.getCacheMissCount(), equalTo( 1L ) );
		assertThat( "Cache Hit Count", queryStats.getCacheHitCount(), equalTo( 0L ) );
		assertThat( "Cache Put Count", queryStats.getCachePutCount(), equalTo( 1L ) );
	}
}
