/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.readonly;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;

import org.hibernate.CacheMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Andrea Boriero
 */
public class ReadOnlySessionQueryIteratorTest extends AbstractReadOnlyTest {
	@Override
	public String[] getMappings() {
		return new String[] { "readonly/DataPoint.hbm.xml", "readonly/TextHolder.hbm.xml" };
	}

	@Test
	public void testReadOnlySessionDefaultQueryIterate() {
		Session s = openSession();
		s.setCacheMode( CacheMode.IGNORE );
		Transaction t = s.beginTransaction();
		for ( int i = 0; i < 100; i++ ) {
			DataPoint dp = new DataPoint();
			dp.setX( new BigDecimal( i * 0.1d ).setScale( 19, BigDecimal.ROUND_DOWN ) );
			dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale( 19, BigDecimal.ROUND_DOWN ) );
			s.save( dp );
		}
		t.commit();
		s.close();

		s = openSession();
		s.setCacheMode( CacheMode.IGNORE );
		t = s.beginTransaction();
		s.setDefaultReadOnly( true );
		int i = 0;
		Iterator it = s.createQuery( "from DataPoint dp order by dp.x asc" )
				.iterate();
		s.setDefaultReadOnly( false );
		while ( it.hasNext() ) {
			DataPoint dp = (DataPoint) it.next();
			if ( ++i == 50 ) {
				s.setReadOnly( dp, false );
			}
			dp.setDescription( "done!" );
		}
		t.commit();
		s.clear();
		t = s.beginTransaction();
		List single = s.createQuery( "from DataPoint where description='done!'" ).list();
		assertEquals( 1, single.size() );
		s.createQuery( "delete from DataPoint" ).executeUpdate();
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
		Query query = s.createQuery( "from DataPoint dp order by dp.x asc");
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



}
