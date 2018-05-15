/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.readonly;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;

import org.hibernate.CacheMode;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.proxy.HibernateProxy;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
public class ReadOnlySessionTest extends AbstractReadOnlyTest {
	@Override
	public String[] getMappings() {
		return new String[] { "readonly/DataPoint.hbm.xml", "readonly/TextHolder.hbm.xml" };
	}

	@Test
	public void testReadOnlyOnProxies() {
		Session s = openSession();
		s.setCacheMode( CacheMode.IGNORE );
		s.beginTransaction();
		DataPoint dp = new DataPoint();
		dp.setX( new BigDecimal( 0.1d ).setScale(19, BigDecimal.ROUND_DOWN) );
		dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale(19, BigDecimal.ROUND_DOWN) );
		dp.setDescription( "original" );
		s.save( dp );
		long dpId = dp.getId();
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		s.beginTransaction();
		s.setDefaultReadOnly( true );
		assertTrue( s.isDefaultReadOnly() );
		dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpId ) );
		s.setDefaultReadOnly( false );
		assertFalse( "was initialized", Hibernate.isInitialized( dp ) );
		assertTrue( s.isReadOnly( dp ) );
		assertFalse( "was initialized during isReadOnly", Hibernate.isInitialized( dp ) );
		dp.setDescription( "changed" );
		assertTrue( "was not initialized during mod", Hibernate.isInitialized( dp ) );
		assertEquals( "desc not changed in memory", "changed", dp.getDescription() );
		s.flush();
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		List list = s.createQuery( "from DataPoint where description = 'changed'" ).list();
		assertEquals( "change written to database", 0, list.size() );
		s.createQuery("delete from DataPoint").executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testReadOnlySessionDefaultQueryScroll() {
		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		Transaction t = s.beginTransaction();
		for ( int i=0; i<100; i++ ) {
			DataPoint dp = new DataPoint();
			dp.setX( new BigDecimal(i * 0.1d).setScale(19, BigDecimal.ROUND_DOWN) );
			dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale(19, BigDecimal.ROUND_DOWN) );
			s.save(dp);
		}
		t.commit();
		s.close();

		s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		t = s.beginTransaction();
		s.setDefaultReadOnly( true );
		int i = 0;
		ScrollableResults sr = s.createQuery("from DataPoint dp order by dp.x asc")
				.scroll(ScrollMode.FORWARD_ONLY);
		s.setDefaultReadOnly( false );
		while ( sr.next() ) {
			DataPoint dp = (DataPoint) sr.get(0);
			if (++i==50) {
				s.setReadOnly(dp, false);
			}
			dp.setDescription("done!");
		}
		t.commit();
		s.clear();
		t = s.beginTransaction();
		List single = s.createQuery("from DataPoint where description='done!'").list();
		assertEquals( 1, single.size() );
		s.createQuery("delete from DataPoint").executeUpdate();
		t.commit();
		s.close();
	}

	@Test
	public void testReadOnlySessionModifiableQueryScroll() {
		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		Transaction t = s.beginTransaction();
		for ( int i=0; i<100; i++ ) {
			DataPoint dp = new DataPoint();
			dp.setX( new BigDecimal(i * 0.1d).setScale(19, BigDecimal.ROUND_DOWN) );
			dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale(19, BigDecimal.ROUND_DOWN) );
			s.save(dp);
		}
		t.commit();
		s.close();

		s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		t = s.beginTransaction();
		s.setDefaultReadOnly( true );
		int i = 0;
		ScrollableResults sr = s.createQuery("from DataPoint dp order by dp.x asc")
				.setReadOnly( false )
				.scroll(ScrollMode.FORWARD_ONLY);
		while ( sr.next() ) {
			DataPoint dp = (DataPoint) sr.get(0);
			if (++i==50) {
				s.setReadOnly(dp, true);
			}
			dp.setDescription("done!");
		}
		t.commit();
		s.clear();
		t = s.beginTransaction();
		List list = s.createQuery("from DataPoint where description='done!'").list();
		assertEquals( 99, list.size() );
		s.createQuery("delete from DataPoint").executeUpdate();
		t.commit();
		s.close();
	}

	@Test
	public void testModifiableSessionReadOnlyQueryScroll() {
		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		Transaction t = s.beginTransaction();
		for ( int i=0; i<100; i++ ) {
			DataPoint dp = new DataPoint();
			dp.setX( new BigDecimal(i * 0.1d).setScale(19, BigDecimal.ROUND_DOWN) );
			dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale(19, BigDecimal.ROUND_DOWN) );
			s.save(dp);
		}
		t.commit();
		s.close();

		s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		t = s.beginTransaction();
		assertFalse( s.isDefaultReadOnly() );
		int i = 0;
		ScrollableResults sr = s.createQuery("from DataPoint dp order by dp.x asc")
				.setReadOnly( true )
				.scroll(ScrollMode.FORWARD_ONLY);
		while ( sr.next() ) {
			DataPoint dp = (DataPoint) sr.get(0);
			if (++i==50) {
				s.setReadOnly(dp, false);
			}
			dp.setDescription("done!");
		}
		t.commit();
		s.clear();
		t = s.beginTransaction();
		List single = s.createQuery("from DataPoint where description='done!'").list();
		assertEquals( 1, single.size() );
		s.createQuery("delete from DataPoint").executeUpdate();
		t.commit();
		s.close();
	}

	@Test
	public void testModifiableSessionDefaultQueryReadOnlySessionScroll() {
		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		Transaction t = s.beginTransaction();
		for ( int i=0; i<100; i++ ) {
			DataPoint dp = new DataPoint();
			dp.setX( new BigDecimal(i * 0.1d).setScale(19, BigDecimal.ROUND_DOWN) );
			dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale(19, BigDecimal.ROUND_DOWN) );
			s.save(dp);
		}
		t.commit();
		s.close();

		s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		t = s.beginTransaction();
		s.setDefaultReadOnly( false );
		int i = 0;
		Query query = s.createQuery("from DataPoint dp order by dp.x asc");
		s.setDefaultReadOnly( true );
		ScrollableResults sr = query.scroll(ScrollMode.FORWARD_ONLY);
		s.setDefaultReadOnly( false );
		while ( sr.next() ) {
			DataPoint dp = (DataPoint) sr.get(0);
			if (++i==50) {
				s.setReadOnly(dp, false);
			}
			dp.setDescription("done!");
		}
		t.commit();
		s.clear();
		t = s.beginTransaction();
		List single = s.createQuery("from DataPoint where description='done!'").list();
		assertEquals( 1, single.size() );
		s.createQuery("delete from DataPoint").executeUpdate();
		t.commit();
		s.close();
	}

	@Test
	public void testQueryReadOnlyScroll() {
		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		Transaction t = s.beginTransaction();
		DataPoint dp = null;
		for ( int i=0; i<100; i++ ) {
			dp = new DataPoint();
			dp.setX( new BigDecimal(i * 0.1d).setScale(19, BigDecimal.ROUND_DOWN) );
			dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale(19, BigDecimal.ROUND_DOWN) );
			s.save(dp);
		}
		t.commit();
		s.close();

		s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		t = s.beginTransaction();
		s.setDefaultReadOnly( false );
		int i = 0;
		Query query = s.createQuery("from DataPoint dp order by dp.x asc");
		assertFalse( query.isReadOnly() );
		s.setDefaultReadOnly( true );
		assertTrue( query.isReadOnly() );
		s.setDefaultReadOnly( false );
		assertFalse( query.isReadOnly() );
		query.setReadOnly( true );
		assertTrue( query.isReadOnly() );
		s.setDefaultReadOnly( true );
		assertTrue( query.isReadOnly() );
		s.setDefaultReadOnly( false );
		assertTrue( query.isReadOnly() );
		query.setReadOnly( false );
		assertFalse( query.isReadOnly() );
		s.setDefaultReadOnly( true );
		assertFalse( query.isReadOnly() );
		query.setReadOnly( true );
		assertTrue( query.isReadOnly() );
		s.setDefaultReadOnly( false );
		assertFalse( s.isDefaultReadOnly() );
		ScrollableResults sr = query.scroll(ScrollMode.FORWARD_ONLY);
		assertFalse( s.isDefaultReadOnly() );
		assertTrue( query.isReadOnly() );
		DataPoint dpLast = ( DataPoint ) s.get( DataPoint.class, dp.getId() );		
		assertFalse( s.isReadOnly( dpLast ) );
		query.setReadOnly( false );
		assertFalse( query.isReadOnly() );
		int nExpectedChanges = 0;
		assertFalse( s.isDefaultReadOnly() );
		while ( sr.next() ) {
			assertFalse( s.isDefaultReadOnly() );
			dp = (DataPoint) sr.get(0);
			if ( dp.getId() == dpLast.getId() ) {
				//dpLast existed in the session before executing the read-only query
				assertFalse( s.isReadOnly( dp ) );
			}
			else {
				assertTrue( s.isReadOnly( dp ) );
			}
			if (++i==50) {
				s.setReadOnly(dp, false);
				nExpectedChanges = ( dp == dpLast ? 1 : 2 );
			}
			dp.setDescription("done!");
		}
		assertFalse( s.isDefaultReadOnly() );
		t.commit();
		s.clear();
		t = s.beginTransaction();
		List list = s.createQuery("from DataPoint where description='done!'").list();
		assertEquals( nExpectedChanges, list.size() );
		s.createQuery("delete from DataPoint").executeUpdate();
		t.commit();
		s.close();
	}

	@Test
	public void testQueryModifiableScroll() {
		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		Transaction t = s.beginTransaction();
		DataPoint dp = null;
		for ( int i=0; i<100; i++ ) {
			dp = new DataPoint();
			dp.setX( new BigDecimal(i * 0.1d).setScale(19, BigDecimal.ROUND_DOWN) );
			dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale(19, BigDecimal.ROUND_DOWN) );
			s.save(dp);
		}
		t.commit();
		s.close();

		s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		t = s.beginTransaction();
		s.setDefaultReadOnly( true );
		int i = 0;
		Query query = s.createQuery("from DataPoint dp order by dp.x asc");
		assertTrue( query.isReadOnly() );
		s.setDefaultReadOnly( false );
		assertFalse( query.isReadOnly() );
		s.setDefaultReadOnly( true );
		assertTrue( query.isReadOnly() );
		query.setReadOnly( false );
		assertFalse( query.isReadOnly() );
		s.setDefaultReadOnly( false );
		assertFalse( query.isReadOnly() );
		s.setDefaultReadOnly( true );
		assertFalse( query.isReadOnly() );
		query.setReadOnly( true );
		assertTrue( query.isReadOnly() );
		s.setDefaultReadOnly( false );
		assertTrue( query.isReadOnly() );
		query.setReadOnly( false );
		assertFalse( query.isReadOnly() );
		s.setDefaultReadOnly( true );
		assertTrue( s.isDefaultReadOnly() );
		ScrollableResults sr = query.scroll(ScrollMode.FORWARD_ONLY);
		assertFalse( query.isReadOnly() );
		DataPoint dpLast = ( DataPoint ) s.get( DataPoint.class, dp.getId() );
		assertTrue( s.isReadOnly( dpLast ) );
		query.setReadOnly( true );
		assertTrue( query.isReadOnly() );
		int nExpectedChanges = 0;
		assertTrue( s.isDefaultReadOnly() );
		while ( sr.next() ) {
			assertTrue( s.isDefaultReadOnly() );
			dp = (DataPoint) sr.get(0);
			if ( dp.getId() == dpLast.getId() ) {
				//dpLast existed in the session before executing the read-only query
				assertTrue( s.isReadOnly( dp ) );
			}
			else {
				assertFalse( s.isReadOnly( dp ) );
			}
			if (++i==50) {
				s.setReadOnly(dp, true);
				nExpectedChanges = ( dp == dpLast ? 99 : 98 );
			}
			dp.setDescription("done!");
		}
		assertTrue( s.isDefaultReadOnly() );
		t.commit();
		s.clear();
		t = s.beginTransaction();
		List list = s.createQuery("from DataPoint where description='done!'").list();
		assertEquals( nExpectedChanges, list.size() );
		s.createQuery("delete from DataPoint").executeUpdate();
		t.commit();
		s.close();
	}

	@Test
	public void testReadOnlySessionDefaultQueryIterate() {
		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		Transaction t = s.beginTransaction();
		for ( int i=0; i<100; i++ ) {
			DataPoint dp = new DataPoint();
			dp.setX( new BigDecimal(i * 0.1d).setScale(19, BigDecimal.ROUND_DOWN) );
			dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale(19, BigDecimal.ROUND_DOWN) );
			s.save(dp);
		}
		t.commit();
		s.close();

		s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		t = s.beginTransaction();
		s.setDefaultReadOnly( true );
		int i = 0;
		Iterator it = s.createQuery("from DataPoint dp order by dp.x asc")
				.iterate();
		s.setDefaultReadOnly( false );
		while ( it.hasNext() ) {
			DataPoint dp = (DataPoint) it.next();
			if (++i==50) {
				s.setReadOnly(dp, false);
			}
			dp.setDescription("done!");
		}
		t.commit();
		s.clear();
		t = s.beginTransaction();
		List single = s.createQuery("from DataPoint where description='done!'").list();
		assertEquals( 1, single.size() );
		s.createQuery("delete from DataPoint").executeUpdate();
		t.commit();
		s.close();
	}

	@Test
	public void testReadOnlySessionModifiableQueryIterate() {
		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		Transaction t = s.beginTransaction();
		for ( int i=0; i<100; i++ ) {
			DataPoint dp = new DataPoint();
			dp.setX( new BigDecimal(i * 0.1d).setScale(19, BigDecimal.ROUND_DOWN) );
			dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale(19, BigDecimal.ROUND_DOWN) );
			s.save(dp);
		}
		t.commit();
		s.close();

		s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		t = s.beginTransaction();
		s.setDefaultReadOnly( true );
		int i = 0;
		Iterator it = s.createQuery("from DataPoint dp order by dp.x asc")
				.setReadOnly( false )
				.iterate();
		while ( it.hasNext() ) {
			DataPoint dp = (DataPoint) it.next();
			if (++i==50) {
				s.setReadOnly(dp, true);
			}
			dp.setDescription("done!");
		}
		t.commit();
		s.clear();
		t = s.beginTransaction();
		List list = s.createQuery("from DataPoint where description='done!'").list();
		assertEquals( 99, list.size() );
		s.createQuery("delete from DataPoint").executeUpdate();
		t.commit();
		s.close();
	}

	@Test
	public void testModifiableSessionReadOnlyQueryIterate() {
		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		Transaction t = s.beginTransaction();
		for ( int i=0; i<100; i++ ) {
			DataPoint dp = new DataPoint();
			dp.setX( new BigDecimal(i * 0.1d).setScale(19, BigDecimal.ROUND_DOWN) );
			dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale(19, BigDecimal.ROUND_DOWN) );
			s.save(dp);
		}
		t.commit();
		s.close();

		s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		t = s.beginTransaction();
		assertFalse( s.isDefaultReadOnly() );
		int i = 0;
		Iterator it = s.createQuery("from DataPoint dp order by dp.x asc")
				.setReadOnly( true )
				.iterate();
		while ( it.hasNext() ) {
			DataPoint dp = (DataPoint) it.next();
			if (++i==50) {
				s.setReadOnly(dp, false);
			}
			dp.setDescription("done!");
		}
		t.commit();
		s.clear();
		t = s.beginTransaction();
		List single = s.createQuery("from DataPoint where description='done!'").list();
		assertEquals( 1, single.size() );
		s.createQuery("delete from DataPoint").executeUpdate();
		t.commit();
		s.close();
	}

	@Test
	public void testModifiableSessionDefaultQueryReadOnlySessionIterate() {
		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		Transaction t = s.beginTransaction();
		for ( int i=0; i<100; i++ ) {
			DataPoint dp = new DataPoint();
			dp.setX( new BigDecimal(i * 0.1d).setScale(19, BigDecimal.ROUND_DOWN) );
			dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale(19, BigDecimal.ROUND_DOWN) );
			s.save(dp);
		}
		t.commit();
		s.close();

		s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		t = s.beginTransaction();
		s.setDefaultReadOnly( false );
		int i = 0;
		Query query = s.createQuery("from DataPoint dp order by dp.x asc");
		s.setDefaultReadOnly( true );
		Iterator it = query.iterate();
		s.setDefaultReadOnly( false );
		while ( it.hasNext() ) {
			DataPoint dp = (DataPoint) it.next();
			if (++i==50) {
				s.setReadOnly(dp, false);
			}
			dp.setDescription("done!");
		}
		t.commit();
		s.clear();
		t = s.beginTransaction();
		List single = s.createQuery("from DataPoint where description='done!'").list();
		assertEquals( 1, single.size() );
		s.createQuery("delete from DataPoint").executeUpdate();
		t.commit();
		s.close();
	}

	@Test
	public void testQueryReadOnlyIterate() {
		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		Transaction t = s.beginTransaction();
		DataPoint dp = null;
		for ( int i=0; i<100; i++ ) {
			dp = new DataPoint();
			dp.setX( new BigDecimal(i * 0.1d).setScale(19, BigDecimal.ROUND_DOWN) );
			dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale(19, BigDecimal.ROUND_DOWN) );
			s.save(dp);
		}
		t.commit();
		s.close();

		s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		t = s.beginTransaction();
		s.setDefaultReadOnly( false );
		int i = 0;
		Query query = s.createQuery("from DataPoint dp order by dp.x asc");
		assertFalse( query.isReadOnly() );
		s.setDefaultReadOnly( true );
		assertTrue( query.isReadOnly() );
		s.setDefaultReadOnly( false );
		assertFalse( query.isReadOnly() );
		query.setReadOnly( true );
		assertTrue( query.isReadOnly() );
		s.setDefaultReadOnly( true );
		assertTrue( query.isReadOnly() );
		s.setDefaultReadOnly( false );
		assertTrue( query.isReadOnly() );
		query.setReadOnly( false );
		assertFalse( query.isReadOnly() );
		s.setDefaultReadOnly( true );
		assertFalse( query.isReadOnly() );
		query.setReadOnly( true );
		assertTrue( query.isReadOnly() );
		s.setDefaultReadOnly( false );
		assertFalse( s.isDefaultReadOnly() );
		Iterator it = query.iterate();
		assertTrue( query.isReadOnly() );
		DataPoint dpLast = ( DataPoint ) s.get( DataPoint.class, dp.getId() );
		assertFalse( s.isReadOnly( dpLast ) );
		query.setReadOnly( false );
		assertFalse( query.isReadOnly() );
		int nExpectedChanges = 0;
		assertFalse( s.isDefaultReadOnly() );
		while ( it.hasNext() ) {
			assertFalse( s.isDefaultReadOnly() );		
			dp = (DataPoint) it.next();
			assertFalse( s.isDefaultReadOnly() );
			if ( dp.getId() == dpLast.getId() ) {
				//dpLast existed in the session before executing the read-only query
				assertFalse( s.isReadOnly( dp ) );
			}
			else {
				assertTrue( s.isReadOnly( dp ) );
			}
			if (++i==50) {
				s.setReadOnly(dp, false);
				nExpectedChanges = ( dp == dpLast ? 1 : 2 );
			}
			dp.setDescription("done!");
		}
		assertFalse( s.isDefaultReadOnly() );
		t.commit();
		s.clear();
		t = s.beginTransaction();
		List list = s.createQuery("from DataPoint where description='done!'").list();
		assertEquals( nExpectedChanges, list.size() );
		s.createQuery("delete from DataPoint").executeUpdate();
		t.commit();
		s.close();
	}

	@Test
	public void testQueryModifiableIterate() {
		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		Transaction t = s.beginTransaction();
		DataPoint dp = null;
		for ( int i=0; i<100; i++ ) {
			dp = new DataPoint();
			dp.setX( new BigDecimal(i * 0.1d).setScale(19, BigDecimal.ROUND_DOWN) );
			dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale(19, BigDecimal.ROUND_DOWN) );
			s.save(dp);
		}
		t.commit();
		s.close();

		s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		t = s.beginTransaction();
		s.setDefaultReadOnly( true );
		int i = 0;
		Query query = s.createQuery("from DataPoint dp order by dp.x asc");
		assertTrue( query.isReadOnly() );
		s.setDefaultReadOnly( false );
		assertFalse( query.isReadOnly() );
		s.setDefaultReadOnly( true );
		assertTrue( query.isReadOnly() );
		query.setReadOnly( false );
		assertFalse( query.isReadOnly() );
		s.setDefaultReadOnly( false );
		assertFalse( query.isReadOnly() );
		s.setDefaultReadOnly( true );
		assertFalse( query.isReadOnly() );
		query.setReadOnly( true );
		assertTrue( query.isReadOnly() );
		s.setDefaultReadOnly( false );
		assertTrue( query.isReadOnly() );
		query.setReadOnly( false );
		assertFalse( query.isReadOnly() );
		s.setDefaultReadOnly( true );
		assertTrue( s.isDefaultReadOnly() );
		Iterator it = query.iterate();
		assertFalse( query.isReadOnly() );
		DataPoint dpLast = ( DataPoint ) s.get( DataPoint.class, dp.getId() );
		assertTrue( s.isReadOnly( dpLast ) );
		query.setReadOnly( true );
		assertTrue( query.isReadOnly() );
		int nExpectedChanges = 0;
		assertTrue( s.isDefaultReadOnly() );
		while ( it.hasNext() ) {
			assertTrue( s.isDefaultReadOnly() );
			dp = (DataPoint) it.next();
			assertTrue( s.isDefaultReadOnly() );
			if ( dp.getId() == dpLast.getId() ) {
				//dpLast existed in the session before executing the read-only query
				assertTrue( s.isReadOnly( dp ) );
			}
			else {
				assertFalse( s.isReadOnly( dp ) );
			}
			if (++i==50) {
				s.setReadOnly(dp, true);
				nExpectedChanges = ( dp == dpLast ? 99 : 98 );
			}
			dp.setDescription("done!");
		}
		assertTrue( s.isDefaultReadOnly() );
		t.commit();
		s.clear();
		t = s.beginTransaction();
		List list = s.createQuery("from DataPoint where description='done!'").list();
		assertEquals( nExpectedChanges, list.size() );
		s.createQuery("delete from DataPoint").executeUpdate();
		t.commit();
		s.close();
	}

	@Test
	public void testReadOnlyRefresh() {
		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		Transaction t = s.beginTransaction();
		DataPoint dp = new DataPoint();
		dp.setDescription( "original" );
		dp.setX( new BigDecimal(0.1d).setScale(19, BigDecimal.ROUND_DOWN) );
		dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale(19, BigDecimal.ROUND_DOWN) );
		s.save(dp);
		t.commit();
		s.close();

		s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		s.setDefaultReadOnly( true );
		t = s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dp.getId() );
		assertTrue( s.isReadOnly( dp ) );
		assertEquals( "original", dp.getDescription() );
		dp.setDescription( "changed" );
		assertEquals( "changed", dp.getDescription() );
		s.refresh( dp );
		assertTrue( s.isReadOnly( dp ) );
		assertEquals( "original", dp.getDescription() );
		dp.setDescription( "changed" );
		assertEquals( "changed", dp.getDescription() );
		s.setDefaultReadOnly( false );
		s.refresh( dp );
		assertTrue( s.isReadOnly( dp ) );
		assertEquals( "original", dp.getDescription() );
		dp.setDescription( "changed" );
		assertEquals( "changed", dp.getDescription() );
		t.commit();

		s.clear();
		t = s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dp.getId() );
		assertEquals( "original", dp.getDescription() );
		s.delete( dp );
		t.commit();
		s.close();
	}

	@Test
	public void testReadOnlyRefreshDetached() {
		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		Transaction t = s.beginTransaction();
		DataPoint dp = new DataPoint();
		dp.setDescription( "original" );
		dp.setX( new BigDecimal(0.1d).setScale(19, BigDecimal.ROUND_DOWN) );
		dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale(19, BigDecimal.ROUND_DOWN) );
		s.save(dp);
		t.commit();
		s.close();

		s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		t = s.beginTransaction();
		s.setDefaultReadOnly( false );
		dp.setDescription( "changed" );
		assertEquals( "changed", dp.getDescription() );
		s.refresh( dp );
		assertEquals( "original", dp.getDescription() );
		assertFalse( s.isReadOnly( dp ) );
		dp.setDescription( "changed" );
		assertEquals( "changed", dp.getDescription() );
		s.evict( dp );
		s.refresh( dp );
		assertEquals( "original", dp.getDescription() );
		assertFalse( s.isReadOnly( dp ) );
		dp.setDescription( "changed" );
		assertEquals( "changed", dp.getDescription() );
		s.setDefaultReadOnly( true );
		s.evict( dp );
		s.refresh( dp );
		assertEquals( "original", dp.getDescription() );
		assertTrue( s.isReadOnly( dp ) );
		dp.setDescription( "changed" );
		t.commit();

		s.clear();
		t = s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dp.getId() );
		assertEquals( "original", dp.getDescription() );
		s.delete( dp );
		t.commit();
		s.close();
	}

	@Test
	public void testReadOnlyProxyRefresh() {
		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		Transaction t = s.beginTransaction();
		DataPoint dp = new DataPoint();
		dp.setDescription( "original" );
		dp.setX( new BigDecimal(0.1d).setScale(19, BigDecimal.ROUND_DOWN) );
		dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale(19, BigDecimal.ROUND_DOWN) );
		s.save(dp);
		t.commit();
		s.close();

		s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		t = s.beginTransaction();
		s.setDefaultReadOnly( true );
		dp = ( DataPoint ) s.load( DataPoint.class, dp.getId() );
		assertTrue( s.isReadOnly( dp ) );
		assertFalse( Hibernate.isInitialized( dp ) );
		s.refresh( dp );
		assertFalse( Hibernate.isInitialized( dp ) );
		assertTrue( s.isReadOnly( dp ) );
		s.setDefaultReadOnly( false );
		s.refresh( dp );
		assertFalse( Hibernate.isInitialized( dp ) );
		assertTrue( s.isReadOnly( dp ) );
		assertEquals( "original", dp.getDescription() );
		assertTrue( Hibernate.isInitialized( dp ) );
		dp.setDescription( "changed" );
		assertEquals( "changed", dp.getDescription() );
		assertTrue( s.isReadOnly( dp ) );
		assertTrue( s.isReadOnly( ( ( HibernateProxy ) dp ).getHibernateLazyInitializer().getImplementation() ) );
		s.refresh( dp );
		assertEquals( "original", dp.getDescription() );
		assertTrue( s.isReadOnly( dp ) );
		assertTrue( s.isReadOnly( ( ( HibernateProxy ) dp ).getHibernateLazyInitializer().getImplementation() ) );
		s.setDefaultReadOnly( true );
		dp.setDescription( "changed" );
		assertEquals( "changed", dp.getDescription() );
		s.refresh( dp );
		assertTrue( s.isReadOnly( dp ) );
		assertTrue( s.isReadOnly( ( ( HibernateProxy ) dp ).getHibernateLazyInitializer().getImplementation() ) );
		assertEquals( "original", dp.getDescription() );
		dp.setDescription( "changed" );
		t.commit();

		s.clear();
		t = s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dp.getId() );
		assertEquals( "original", dp.getDescription() );
		s.delete( dp );
		t.commit();
		s.close();

	}

	@Test
	public void testReadOnlyProxyRefreshDetached() {
		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		Transaction t = s.beginTransaction();
		DataPoint dp = new DataPoint();
		dp.setDescription( "original" );
		dp.setX( new BigDecimal(0.1d).setScale(19, BigDecimal.ROUND_DOWN) );
		dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale(19, BigDecimal.ROUND_DOWN) );
		s.save(dp);
		t.commit();
		s.close();

		s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		t = s.beginTransaction();
		s.setDefaultReadOnly( true );
		dp = ( DataPoint ) s.load( DataPoint.class, dp.getId() );
		assertFalse( Hibernate.isInitialized( dp ) );
		assertTrue( s.isReadOnly( dp ) );
		s.evict( dp );
		s.refresh( dp );
		assertFalse( Hibernate.isInitialized( dp ) );
		s.setDefaultReadOnly( false );
		assertTrue( s.isReadOnly( dp ) );
		s.evict( dp );
		s.refresh( dp );
		assertFalse( Hibernate.isInitialized( dp ) );
		assertFalse( s.isReadOnly( dp ) );
		assertFalse( s.isReadOnly( ( ( HibernateProxy ) dp ).getHibernateLazyInitializer().getImplementation() ) );
		dp.setDescription( "changed" );
		assertEquals( "changed", dp.getDescription() );
		assertTrue( Hibernate.isInitialized( dp ) );
		s.evict( dp );
		s.refresh( dp );
		assertEquals( "original", dp.getDescription() );
		assertFalse( s.isReadOnly( dp ) );
		assertFalse( s.isReadOnly( ( ( HibernateProxy ) dp ).getHibernateLazyInitializer().getImplementation() ) );
		dp.setDescription( "changed" );
		assertEquals( "changed", dp.getDescription() );
		s.setDefaultReadOnly( true );
		s.evict( dp );
		s.refresh( dp );
		assertEquals( "original", dp.getDescription() );
		assertTrue( s.isReadOnly( dp ) );
		assertTrue( s.isReadOnly( ( ( HibernateProxy ) dp ).getHibernateLazyInitializer().getImplementation() ) );		
		dp.setDescription( "changed" );
		assertEquals( "changed", dp.getDescription() );
		t.commit();

		s.clear();
		t = s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dp.getId() );
		assertEquals( "original", dp.getDescription() );
		s.delete( dp );
		t.commit();
		s.close();
	}

	@Test
	public void testReadOnlyDelete() {
		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		Transaction t = s.beginTransaction();
		DataPoint dp = new DataPoint();
		dp.setX( new BigDecimal(0.1d).setScale(19, BigDecimal.ROUND_DOWN) );
		dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale(19, BigDecimal.ROUND_DOWN) );
		s.save(dp);
		t.commit();
		s.close();

		s = openSession();
		s.setDefaultReadOnly( true );
		s.setCacheMode(CacheMode.IGNORE);
		t = s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dp.getId() );
		s.setDefaultReadOnly( false );
		assertTrue( s.isReadOnly( dp ) );
		s.delete(  dp );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		List list = s.createQuery("from DataPoint where id=" + dp.getId() ).list();
		assertTrue( list.isEmpty() );
		t.commit();
		s.close();

	}

	@Test
	public void testReadOnlyGetModifyAndDelete() {
		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		Transaction t = s.beginTransaction();
		DataPoint dp = new DataPoint();
		dp.setX( new BigDecimal(0.1d).setScale(19, BigDecimal.ROUND_DOWN) );
		dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale(19, BigDecimal.ROUND_DOWN) );
		s.save(dp);
		t.commit();
		s.close();

		s = openSession();
		s.setDefaultReadOnly( true );
		s.setCacheMode(CacheMode.IGNORE);
		t = s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dp.getId() );
		s.setDefaultReadOnly( true );
		dp.setDescription( "a DataPoint" );
		s.delete(  dp );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		List list = s.createQuery("from DataPoint where id=" + dp.getId() ).list();
		assertTrue( list.isEmpty() );
		t.commit();
		s.close();
	}

	@Test
	public void testReadOnlyModeWithExistingModifiableEntity() {
		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		Transaction t = s.beginTransaction();
		DataPoint dp = null;
		for ( int i=0; i<100; i++ ) {
			dp = new DataPoint();
			dp.setX( new BigDecimal(i * 0.1d).setScale(19, BigDecimal.ROUND_DOWN) );
			dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale(19, BigDecimal.ROUND_DOWN) );
			s.save(dp);
		}
		t.commit();
		s.close();

		s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		t = s.beginTransaction();
		DataPoint dpLast = ( DataPoint ) s.get( DataPoint.class,  dp.getId() );
		assertFalse( s.isReadOnly( dpLast ) );
		s.setDefaultReadOnly( true );
		int i = 0;
		ScrollableResults sr = s.createQuery("from DataPoint dp order by dp.x asc")
				.scroll(ScrollMode.FORWARD_ONLY);
		s.setDefaultReadOnly( false );
		int nExpectedChanges = 0;
		while ( sr.next() ) {
			dp = (DataPoint) sr.get(0);
			if ( dp.getId() == dpLast.getId() ) {
				//dpLast existed in the session before executing the read-only query
				assertFalse( s.isReadOnly( dp ) );
			}
			else {
				assertTrue( s.isReadOnly( dp ) );
			}
			if (++i==50) {
				s.setReadOnly(dp, false);
				nExpectedChanges = ( dp == dpLast ? 1 : 2 );
			}
			dp.setDescription("done!");
		}
		t.commit();
		s.clear();
		t = s.beginTransaction();
		List list = s.createQuery("from DataPoint where description='done!'").list();
		assertEquals( nExpectedChanges, list.size() );
		s.createQuery("delete from DataPoint").executeUpdate();
		t.commit();
		s.close();
	}

	@Test
	public void testModifiableModeWithExistingReadOnlyEntity() {
		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		Transaction t = s.beginTransaction();
		DataPoint dp = null;
		for ( int i=0; i<100; i++ ) {
			dp = new DataPoint();
			dp.setX( new BigDecimal(i * 0.1d).setScale(19, BigDecimal.ROUND_DOWN) );
			dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale(19, BigDecimal.ROUND_DOWN) );
			s.save(dp);
		}
		t.commit();
		s.close();

		s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		t = s.beginTransaction();
		s.setDefaultReadOnly( true );
		DataPoint dpLast = ( DataPoint ) s.get( DataPoint.class,  dp.getId() );
		assertTrue( s.isReadOnly( dpLast ) );
		int i = 0;
		ScrollableResults sr = s.createQuery("from DataPoint dp order by dp.x asc")
				.setReadOnly(false)
				.scroll(ScrollMode.FORWARD_ONLY);
		int nExpectedChanges = 0;
		while ( sr.next() ) {
			dp = (DataPoint) sr.get(0);
			if ( dp.getId() == dpLast.getId() ) {
				//dpLast existed in the session before executing the read-only query
				assertTrue( s.isReadOnly( dp ) );
			}
			else {
				assertFalse( s.isReadOnly( dp ) );
			}
			if (++i==50) {
				s.setReadOnly(dp, true);
				nExpectedChanges = ( dp == dpLast ? 99 : 98 );
			}
			dp.setDescription("done!");
		}
		t.commit();
		s.clear();
		t = s.beginTransaction();
		List list = s.createQuery("from DataPoint where description='done!'").list();
		assertEquals( nExpectedChanges, list.size() );
		s.createQuery("delete from DataPoint").executeUpdate();
		t.commit();
		s.close();
	}

	@Test
	public void testReadOnlyOnTextType() {
		final String origText = "some huge text string";
		final String newText = "some even bigger text string";

		Session s = openSession();
		s.beginTransaction();
		s.setCacheMode( CacheMode.IGNORE );
		TextHolder holder = new TextHolder( origText );
		s.save( holder );
		Long id = holder.getId();
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.setDefaultReadOnly( true );
		s.setCacheMode( CacheMode.IGNORE );
		holder = ( TextHolder ) s.get( TextHolder.class, id );
		s.setDefaultReadOnly( false );
		holder.setTheText( newText );
		s.flush();
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		holder = ( TextHolder ) s.get( TextHolder.class, id );
		assertEquals( "change written to database", origText, holder.getTheText() );
		s.delete( holder );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testMergeWithReadOnlyEntity() {
		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		Transaction t = s.beginTransaction();
		DataPoint dp = new DataPoint();
		dp.setX( new BigDecimal(0.1d).setScale(19, BigDecimal.ROUND_DOWN) );
		dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale(19, BigDecimal.ROUND_DOWN) );
		s.save(dp);
		t.commit();
		s.close();

		dp.setDescription( "description" );

		s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		t = s.beginTransaction();
		s.setDefaultReadOnly( true );
		DataPoint dpManaged = ( DataPoint ) s.get( DataPoint.class, new Long( dp.getId() ) );
		DataPoint dpMerged = ( DataPoint ) s.merge( dp );
		assertSame( dpManaged, dpMerged );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		dpManaged = ( DataPoint ) s.get( DataPoint.class, new Long( dp.getId() ) );
		assertNull( dpManaged.getDescription() );
		s.delete( dpManaged );
		t.commit();
		s.close();

	}

	@Test
	public void testMergeWithReadOnlyProxy() {
		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		Transaction t = s.beginTransaction();
		DataPoint dp = new DataPoint();
		dp.setX( new BigDecimal(0.1d).setScale(19, BigDecimal.ROUND_DOWN) );
		dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale(19, BigDecimal.ROUND_DOWN) );
		s.save(dp);
		t.commit();
		s.close();

		dp.setDescription( "description" );

		s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		t = s.beginTransaction();
		s.setDefaultReadOnly( true );
		DataPoint dpProxy = ( DataPoint ) s.load( DataPoint.class, new Long( dp.getId() ) );
		assertTrue( s.isReadOnly( dpProxy ) );
		assertFalse( Hibernate.isInitialized( dpProxy ) );
		s.evict( dpProxy );
		dpProxy = ( DataPoint ) s.merge( dpProxy );
		assertTrue( s.isReadOnly( dpProxy ) );
		assertFalse( Hibernate.isInitialized( dpProxy ) );
		dpProxy = ( DataPoint ) s.merge( dp );
		assertTrue( s.isReadOnly( dpProxy ) );
		assertTrue( Hibernate.isInitialized( dpProxy ) );
		assertEquals( "description", dpProxy.getDescription() );
		s.evict( dpProxy );
		dpProxy = ( DataPoint ) s.merge( dpProxy );
		assertTrue( s.isReadOnly( dpProxy ) );
		assertTrue( Hibernate.isInitialized( dpProxy ) );
		assertEquals( "description", dpProxy.getDescription() );
		dpProxy.setDescription( null );
		dpProxy = ( DataPoint ) s.merge( dp );
		assertTrue( s.isReadOnly( dpProxy ) );
		assertTrue( Hibernate.isInitialized( dpProxy ) );
		assertEquals( "description", dpProxy.getDescription() );		
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, new Long( dp.getId() ) );
		assertNull( dp.getDescription() );
		s.delete( dp );
		t.commit();
		s.close();

	}
}
