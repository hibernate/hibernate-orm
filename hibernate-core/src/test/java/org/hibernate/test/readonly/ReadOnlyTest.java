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
package org.hibernate.test.readonly;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.Transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * 
 * @author Gavin King
 * @author Gail Badner
 */
public class ReadOnlyTest extends AbstractReadOnlyTest {
	@Override
	public String[] getMappings() {
		return new String[] { "readonly/DataPoint.hbm.xml", "readonly/TextHolder.hbm.xml" };
	}

	@Test
	public void testReadOnlyOnProxies() {
		clearCounts();

		Session s = openSession();
		s.beginTransaction();
		DataPoint dp = new DataPoint();
		dp.setX( new BigDecimal( 0.1d ).setScale(19, BigDecimal.ROUND_DOWN) );
		dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale(19, BigDecimal.ROUND_DOWN) );
		dp.setDescription( "original" );
		s.save( dp );
		long dpId = dp.getId();
		s.getTransaction().commit();
		s.close();

		assertInsertCount( 1 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		s.beginTransaction();
		dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpId ) );
		assertFalse( "was initialized", Hibernate.isInitialized( dp ) );
		s.setReadOnly( dp, true );
		assertFalse( "was initialized during setReadOnly", Hibernate.isInitialized( dp ) );
		dp.setDescription( "changed" );
		assertTrue( "was not initialized during mod", Hibernate.isInitialized( dp ) );
		assertEquals( "desc not changed in memory", "changed", dp.getDescription() );
		s.flush();
		s.getTransaction().commit();
		s.close();

		assertUpdateCount( 0 );

		s = openSession();
		s.beginTransaction();
		List list = s.createQuery( "from DataPoint where description = 'changed'" ).list();
		assertEquals( "change written to database", 0, list.size() );
		assertEquals( 1, s.createQuery("delete from DataPoint").executeUpdate() );
		s.getTransaction().commit();
		s.close();
		
		assertUpdateCount( 0 );
		//deletes from Query.executeUpdate() are not tracked
		//assertDeleteCount( 1 );
	}

	@Test
	public void testReadOnlyMode() {
		clearCounts();

		Session s = openSession();
		Transaction t = s.beginTransaction();
		for ( int i=0; i<100; i++ ) {
			DataPoint dp = new DataPoint();
			dp.setX( new BigDecimal(i * 0.1d).setScale(19, BigDecimal.ROUND_DOWN) );
			dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale(19, BigDecimal.ROUND_DOWN) );
			s.save(dp);
		}
		t.commit();
		s.close();

		assertInsertCount( 100 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		int i = 0;
		ScrollableResults sr = s.createQuery("from DataPoint dp order by dp.x asc")
				.setReadOnly(true)
				.scroll(ScrollMode.FORWARD_ONLY);
		while ( sr.next() ) {
			DataPoint dp = (DataPoint) sr.get(0);
			if (++i==50) {
				s.setReadOnly(dp, false);
			}
			dp.setDescription("done!");
		}
		t.commit();

		assertUpdateCount( 1 );
		clearCounts();

		s.clear();
		t = s.beginTransaction();
		List single = s.createQuery("from DataPoint where description='done!'").list();
		assertEquals( single.size(), 1 );
		assertEquals( 100, s.createQuery("delete from DataPoint").executeUpdate() );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		//deletes from Query.executeUpdate() are not tracked
		//assertDeleteCount( 100 );
	}

	@Test
	public void testReadOnlyModeAutoFlushOnQuery() {
		clearCounts();

		Session s = openSession();
		Transaction t = s.beginTransaction();
		DataPoint dpFirst = null;
		for ( int i=0; i<100; i++ ) {
			DataPoint dp = new DataPoint();
			dp.setX( new BigDecimal(i * 0.1d).setScale(19, BigDecimal.ROUND_DOWN) );
			dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale(19, BigDecimal.ROUND_DOWN) );
			s.save(dp);
		}

		assertInsertCount( 0 );
		assertUpdateCount( 0 );

		ScrollableResults sr = s.createQuery("from DataPoint dp order by dp.x asc")
				.setReadOnly(true)
				.scroll(ScrollMode.FORWARD_ONLY);

		assertInsertCount( 100 );
		assertUpdateCount( 0 );
		clearCounts();

		while ( sr.next() ) {
			DataPoint dp = (DataPoint) sr.get(0);
			assertFalse( s.isReadOnly( dp ) );
			s.delete( dp );
		}
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 100 );
	}

	@Test
	public void testSaveReadOnlyModifyInSaveTransaction() {
		clearCounts();

		Session s = openSession();
		Transaction t = s.beginTransaction();
		DataPoint dp = new DataPoint();
		dp.setDescription( "original" );
		dp.setX( new BigDecimal(0.1d).setScale(19, BigDecimal.ROUND_DOWN) );
		dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale(19, BigDecimal.ROUND_DOWN) );
		s.save(dp);
		s.setReadOnly( dp, true );
		dp.setDescription( "different" );
		t.commit();
		s.close();

		assertInsertCount( 1 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dp.getId() );
		s.setReadOnly( dp, true );
		assertEquals( "original", dp.getDescription() );
		dp.setDescription( "changed" );
		assertEquals( "changed", dp.getDescription() );
		s.refresh( dp );
		assertEquals( "original", dp.getDescription() );
		dp.setDescription( "changed" );
		assertEquals( "changed", dp.getDescription() );
		t.commit();

		assertInsertCount( 0 );
		assertUpdateCount( 0 );

		s.clear();
		t = s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dp.getId() );
		assertEquals( "original", dp.getDescription() );
		s.delete( dp );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 1 );
		clearCounts();
	}

	@Test
	public void testReadOnlyRefresh() {
		clearCounts();

		Session s = openSession();
		Transaction t = s.beginTransaction();
		DataPoint dp = new DataPoint();
		dp.setDescription( "original" );
		dp.setX( new BigDecimal(0.1d).setScale(19, BigDecimal.ROUND_DOWN) );
		dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale(19, BigDecimal.ROUND_DOWN) );
		s.save(dp);
		t.commit();
		s.close();

		assertInsertCount( 1 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dp.getId() );
		s.setReadOnly( dp, true );
		assertEquals( "original", dp.getDescription() );
		dp.setDescription( "changed" );
		assertEquals( "changed", dp.getDescription() );
		s.refresh( dp );
		assertEquals( "original", dp.getDescription() );
		dp.setDescription( "changed" );
		assertEquals( "changed", dp.getDescription() );
		t.commit();

		assertInsertCount( 0 );
		assertUpdateCount( 0 );

		s.clear();
		t = s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dp.getId() );
		assertEquals( "original", dp.getDescription() );
		s.delete( dp );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 1 );
		clearCounts();
	}

	@Test
	public void testReadOnlyRefreshDetached() {
		clearCounts();

		Session s = openSession();
		Transaction t = s.beginTransaction();
		DataPoint dp = new DataPoint();
		dp.setDescription( "original" );
		dp.setX( new BigDecimal(0.1d).setScale(19, BigDecimal.ROUND_DOWN) );
		dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale(19, BigDecimal.ROUND_DOWN) );
		s.save(dp);
		t.commit();
		s.close();

		assertInsertCount( 1 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		dp.setDescription( "changed" );
		assertEquals( "changed", dp.getDescription() );
		s.refresh( dp );
		assertEquals( "original", dp.getDescription() );
		assertFalse( s.isReadOnly( dp ) );
		s.setReadOnly( dp, true );
		dp.setDescription( "changed" );
		assertEquals( "changed", dp.getDescription() );
		s.evict( dp );
		s.refresh( dp );
		assertEquals( "original", dp.getDescription() );
		assertFalse( s.isReadOnly( dp ) );
		t.commit();

		assertInsertCount( 0 );
		assertUpdateCount( 0 );

		s.clear();
		t = s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dp.getId() );
		assertEquals( "original", dp.getDescription() );
		s.delete( dp );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 1 );
	}

	@Test
	public void testReadOnlyDelete() {
		clearCounts();

		Session s = openSession();
		Transaction t = s.beginTransaction();
		DataPoint dp = new DataPoint();
		dp.setX( new BigDecimal(0.1d).setScale(19, BigDecimal.ROUND_DOWN) );
		dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale(19, BigDecimal.ROUND_DOWN) );
		s.save(dp);
		t.commit();
		s.close();

		assertInsertCount( 1 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dp.getId() );
		s.setReadOnly( dp, true );
		s.delete(  dp );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 1 );

		s = openSession();
		t = s.beginTransaction();
		List list = s.createQuery("from DataPoint where description='done!'").list();
		assertTrue( list.isEmpty() );
		t.commit();
		s.close();

	}

	@Test
	public void testReadOnlyGetModifyAndDelete() {
		clearCounts();

		Session s = openSession();
		Transaction t = s.beginTransaction();
		DataPoint dp = new DataPoint();
		dp.setX( new BigDecimal(0.1d).setScale(19, BigDecimal.ROUND_DOWN) );
		dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale(19, BigDecimal.ROUND_DOWN) );
		s.save(dp);
		t.commit();
		s.close();

		assertInsertCount( 1 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dp.getId() );
		s.setReadOnly( dp, true );
		dp.setDescription( "a DataPoint" );
		s.delete(  dp );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 1 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		List list = s.createQuery("from DataPoint where description='done!'").list();
		assertTrue( list.isEmpty() );
		t.commit();
		s.close();

	}

	@Test
	public void testReadOnlyModeWithExistingModifiableEntity() {
		clearCounts();

		Session s = openSession();
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

		assertInsertCount( 100 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		DataPoint dpLast = ( DataPoint ) s.get( DataPoint.class,  dp.getId() );
		assertFalse( s.isReadOnly( dpLast ) );
		int i = 0;
		ScrollableResults sr = s.createQuery("from DataPoint dp order by dp.x asc")
				.setReadOnly(true)
				.scroll(ScrollMode.FORWARD_ONLY);
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

		assertInsertCount( 0 );
		assertUpdateCount( nExpectedChanges );
		clearCounts();

		t = s.beginTransaction();
		List list = s.createQuery("from DataPoint where description='done!'").list();
		assertEquals( list.size(), nExpectedChanges );
		assertEquals( 100, s.createQuery("delete from DataPoint").executeUpdate() );
		t.commit();
		s.close();

		assertUpdateCount( 0 );				
	}

	@Test
	public void testModifiableModeWithExistingReadOnlyEntity() {
		clearCounts();

		Session s = openSession();
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

		assertInsertCount( 100 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		DataPoint dpLast = ( DataPoint ) s.get( DataPoint.class,  dp.getId() );
		assertFalse( s.isReadOnly( dpLast ) );
		s.setReadOnly( dpLast, true );
		assertTrue( s.isReadOnly( dpLast ) );
		dpLast.setDescription( "oy" );
		int i = 0;

		assertUpdateCount( 0 );

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

		assertUpdateCount( nExpectedChanges );
		clearCounts();

		t = s.beginTransaction();
		List list = s.createQuery("from DataPoint where description='done!'").list();
		assertEquals( list.size(), nExpectedChanges );
		assertEquals( 100, s.createQuery("delete from DataPoint").executeUpdate() );
		t.commit();
		s.close();

		assertUpdateCount( 0 );		
	}

	@Test
	public void testReadOnlyOnTextType() {
		final String origText = "some huge text string";
		final String newText = "some even bigger text string";

		clearCounts();

		Session s = openSession();
		s.beginTransaction();
		TextHolder holder = new TextHolder( origText );
		s.save( holder );
		Long id = holder.getId();
		s.getTransaction().commit();
		s.close();

		assertInsertCount( 1 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		s.beginTransaction();
		holder = ( TextHolder ) s.get( TextHolder.class, id );
		s.setReadOnly( holder, true );
		holder.setTheText( newText );
		s.flush();
		s.getTransaction().commit();
		s.close();

		assertUpdateCount( 0 );

		s = openSession();
		s.beginTransaction();
		holder = ( TextHolder ) s.get( TextHolder.class, id );
		assertEquals( "change written to database", origText, holder.getTheText() );
		s.delete( holder );
		s.getTransaction().commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 1 );
	}

	@Test
	public void testMergeWithReadOnlyEntity() {
		clearCounts();

		Session s = openSession();
		Transaction t = s.beginTransaction();
		DataPoint dp = new DataPoint();
		dp.setX( new BigDecimal(0.1d).setScale(19, BigDecimal.ROUND_DOWN) );
		dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale(19, BigDecimal.ROUND_DOWN) );
		s.save(dp);
		t.commit();
		s.close();

		assertInsertCount( 1 );
		assertUpdateCount( 0 );
		clearCounts();

		dp.setDescription( "description" );

		s = openSession();
		t = s.beginTransaction();
		DataPoint dpManaged = ( DataPoint ) s.get( DataPoint.class, new Long( dp.getId() ) );
		s.setReadOnly( dpManaged, true );
		DataPoint dpMerged = ( DataPoint ) s.merge( dp );
		assertSame( dpManaged, dpMerged );
		t.commit();
		s.close();

		assertUpdateCount( 0 );

		s = openSession();
		t = s.beginTransaction();
		dpManaged = ( DataPoint ) s.get( DataPoint.class, new Long( dp.getId() ) );
		assertNull( dpManaged.getDescription() );
		s.delete( dpManaged );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 1 );
	}
}

