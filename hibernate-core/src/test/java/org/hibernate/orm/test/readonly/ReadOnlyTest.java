/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.readonly;

import java.math.BigDecimal;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gavin King
 * @author Gail Badner
 */
@DomainModel(
		xmlMappings = {
				"org/hibernate/orm/test/readonly/DataPoint.hbm.xml",
				"org/hibernate/orm/test/readonly/TextHolder.hbm.xml"
		}
)
public class ReadOnlyTest extends AbstractReadOnlyTest {

	@Test
	public void testReadOnlyOnProxies(SessionFactoryScope scope) {
		clearCounts( scope );

		long dpId = scope.fromTransaction(
				session -> {
					DataPoint dp = new DataPoint();
					dp.setX( new BigDecimal( 0.1d ).setScale( 19, BigDecimal.ROUND_DOWN ) );
					dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale(
							19,
							BigDecimal.ROUND_DOWN
					) );
					dp.setDescription( "original" );
					session.persist( dp );
					return dp.getId();
				}
		);

		assertInsertCount( 1, scope );
		assertUpdateCount( 0, scope );
		clearCounts( scope );

		scope.inTransaction(
				session -> {
					DataPoint dp = session.getReference( DataPoint.class, new Long( dpId ) );
					assertFalse( Hibernate.isInitialized( dp ), "was initialized" );
					session.setReadOnly( dp, true );
					assertFalse( Hibernate.isInitialized( dp ), "was initialized during setReadOnly" );
					dp.setDescription( "changed" );
					assertTrue( Hibernate.isInitialized( dp ), "was not initialized during mod" );
					assertEquals( "changed", dp.getDescription(), "desc not changed in memory" );
					session.flush();
				}
		);

		assertUpdateCount( 0, scope );

		scope.inTransaction(
				session -> {
					List list = session.createQuery( "from DataPoint where description = 'changed'" ).list();
					assertEquals( 0, list.size(), "change written to database" );
					assertEquals( 1, session.createQuery( "delete from DataPoint" ).executeUpdate() );

				}
		);

		assertUpdateCount( 0, scope );
		//deletes from Query.executeUpdate() are not tracked
		//assertDeleteCount( 1 );
	}

	@Test
	public void testReadOnlyMode(SessionFactoryScope scope) {
		clearCounts( scope );

		scope.inTransaction(
				session -> {
					for ( int i = 0; i < 100; i++ ) {
						DataPoint dp = new DataPoint();
						dp.setX( new BigDecimal( i * 0.1d ).setScale( 19, BigDecimal.ROUND_DOWN ) );
						dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale(
								19,
								BigDecimal.ROUND_DOWN
						) );
						session.persist( dp );
					}
				}
		);

		assertInsertCount( 100, scope );
		assertUpdateCount( 0, scope );
		clearCounts( scope );

		scope.inSession(
				session -> {
					try {
						session.getTransaction().begin();
						int i = 0;
						try (ScrollableResults sr = session.createQuery( "from DataPoint dp order by dp.x asc" )
								.setReadOnly( true )
								.scroll( ScrollMode.FORWARD_ONLY )) {
							while ( sr.next() ) {
								DataPoint dp = (DataPoint) sr.get();
								if ( ++i == 50 ) {
									session.setReadOnly( dp, false );
								}
								dp.setDescription( "done!" );
							}
						}
						session.getTransaction().commit();

						assertUpdateCount( 1, scope );
						clearCounts( scope );

						session.clear();
						session.getTransaction().begin();

						List single = session.createQuery( "from DataPoint where description='done!'" ).list();
						assertEquals( single.size(), 1 );
						assertEquals( 100, session.createQuery( "delete from DataPoint" ).executeUpdate() );
						session.getTransaction().commit();
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}

				}
		);

		assertUpdateCount( 0, scope );
		//deletes from Query.executeUpdate() are not tracked
		//assertDeleteCount( 100 );
	}

	@Test
	public void testReadOnlyModeAutoFlushOnQuery(SessionFactoryScope scope) {
		clearCounts( scope );

		scope.inTransaction(
				session -> {
					DataPoint dpFirst = null;
					for ( int i = 0; i < 100; i++ ) {
						DataPoint dp = new DataPoint();
						dp.setX( new BigDecimal( i * 0.1d ).setScale( 19, BigDecimal.ROUND_DOWN ) );
						dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale(
								19,
								BigDecimal.ROUND_DOWN
						) );
						session.persist( dp );
					}

					assertInsertCount( 0, scope );
					assertUpdateCount( 0, scope );

					try (ScrollableResults sr = session.createQuery( "from DataPoint dp order by dp.x asc" )
							.setReadOnly( true )
							.scroll( ScrollMode.FORWARD_ONLY )) {

						assertInsertCount( 100, scope );
						assertUpdateCount( 0, scope );
						clearCounts( scope );

						while ( sr.next() ) {
							DataPoint dp = (DataPoint) sr.get();
							assertFalse( session.isReadOnly( dp ) );
							session.remove( dp );
						}
					}
				}
		);

		assertUpdateCount( 0, scope );
		assertDeleteCount( 100, scope );
	}

	@Test
	public void testSaveReadOnlyModifyInSaveTransaction(SessionFactoryScope scope) {
		clearCounts( scope );

		DataPoint d = new DataPoint();
		scope.inTransaction(
				session -> {
					d.setDescription( "original" );
					d.setX( new BigDecimal( 0.1d ).setScale( 19, BigDecimal.ROUND_DOWN ) );
					d.setY( new BigDecimal( Math.cos( d.getX().doubleValue() ) ).setScale(
							19,
							BigDecimal.ROUND_DOWN
					) );
					session.persist( d );
					session.setReadOnly( d, true );
					d.setDescription( "different" );
				}
		);

		assertInsertCount( 1, scope );
		assertUpdateCount( 0, scope );
		clearCounts( scope );

		scope.inSession(
				session -> {
					try {
						session.beginTransaction();
						DataPoint dp = session.get( DataPoint.class, d.getId() );
						session.setReadOnly( dp, true );
						assertEquals( "original", dp.getDescription() );
						dp.setDescription( "changed" );
						assertEquals( "changed", dp.getDescription() );
						session.refresh( dp );
						assertEquals( "original", dp.getDescription() );
						dp.setDescription( "changed" );
						assertEquals( "changed", dp.getDescription() );
						session.getTransaction().commit();

						assertInsertCount( 0, scope );
						assertUpdateCount( 0, scope );

						session.clear();
						session.beginTransaction();
						dp = session.get( DataPoint.class, dp.getId() );
						assertEquals( "original", dp.getDescription() );
						session.remove( dp );
						session.getTransaction().commit();
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);

		assertUpdateCount( 0, scope );
		assertDeleteCount( 1, scope );
		clearCounts( scope );
	}

	@Test
	public void testReadOnlyRefresh(SessionFactoryScope scope) {
		clearCounts( scope );

		DataPoint d = new DataPoint();
		scope.inTransaction(
				session -> {
					d.setDescription( "original" );
					d.setX( new BigDecimal( 0.1d ).setScale( 19, BigDecimal.ROUND_DOWN ) );
					d.setY( new BigDecimal( Math.cos( d.getX().doubleValue() ) ).setScale(
							19,
							BigDecimal.ROUND_DOWN
					) );
					session.persist( d );
				}
		);

		assertInsertCount( 1, scope );
		assertUpdateCount( 0, scope );
		clearCounts( scope );

		scope.inSession(
				session -> {
					try {
						session.beginTransaction();

						DataPoint dp = session.get( DataPoint.class, d.getId() );
						session.setReadOnly( dp, true );
						assertEquals( "original", dp.getDescription() );
						dp.setDescription( "changed" );
						assertEquals( "changed", dp.getDescription() );
						session.refresh( dp );
						assertEquals( "original", dp.getDescription() );
						dp.setDescription( "changed" );
						assertEquals( "changed", dp.getDescription() );
						session.getTransaction().commit();

						assertInsertCount( 0, scope );
						assertUpdateCount( 0, scope );

						session.clear();
						session.beginTransaction();
						dp = session.get( DataPoint.class, dp.getId() );
						assertEquals( "original", dp.getDescription() );
						session.remove( dp );
						session.getTransaction().commit();
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);

		assertUpdateCount( 0, scope );
		assertDeleteCount( 1, scope );
		clearCounts( scope );
	}

	@Test
	public void testReadOnlyDelete(SessionFactoryScope scope) {
		clearCounts( scope );

		Session s = openSession( scope );
		Transaction t = s.beginTransaction();
		DataPoint dp = new DataPoint();
		dp.setX( new BigDecimal( 0.1d ).setScale( 19, BigDecimal.ROUND_DOWN ) );
		dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale( 19, BigDecimal.ROUND_DOWN ) );
		s.persist( dp );
		t.commit();
		s.close();

		assertInsertCount( 1, scope );
		assertUpdateCount( 0, scope );
		clearCounts( scope );

		s = openSession( scope );
		t = s.beginTransaction();
		dp = s.get( DataPoint.class, dp.getId() );
		s.setReadOnly( dp, true );
		s.remove( dp );
		t.commit();
		s.close();

		assertUpdateCount( 0, scope );
		assertDeleteCount( 1, scope );

		s = openSession( scope );
		t = s.beginTransaction();
		List list = s.createQuery( "from DataPoint where description='done!'" ).list();
		assertTrue( list.isEmpty() );
		t.commit();
		s.close();

	}

	@Test
	public void testReadOnlyGetModifyAndDelete(SessionFactoryScope scope) {
		clearCounts( scope );

		Session s = openSession( scope );
		Transaction t = s.beginTransaction();
		DataPoint dp = new DataPoint();
		dp.setX( new BigDecimal( 0.1d ).setScale( 19, BigDecimal.ROUND_DOWN ) );
		dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale( 19, BigDecimal.ROUND_DOWN ) );
		s.persist( dp );
		t.commit();
		s.close();

		assertInsertCount( 1, scope );
		assertUpdateCount( 0, scope );
		clearCounts( scope );

		s = openSession( scope );
		t = s.beginTransaction();
		dp = s.get( DataPoint.class, dp.getId() );
		s.setReadOnly( dp, true );
		dp.setDescription( "a DataPoint" );
		s.remove( dp );
		t.commit();
		s.close();

		assertUpdateCount( 0, scope );
		assertDeleteCount( 1, scope );
		clearCounts( scope );

		s = openSession( scope );
		t = s.beginTransaction();
		List list = s.createQuery( "from DataPoint where description='done!'" ).list();
		assertTrue( list.isEmpty() );
		t.commit();
		s.close();
	}

	@Test
	public void testReadOnlyModeWithExistingModifiableEntity(SessionFactoryScope scope) {
		clearCounts( scope );

		Session s = openSession( scope );
		Transaction t = s.beginTransaction();
		DataPoint dp = null;
		for ( int i = 0; i < 100; i++ ) {
			dp = new DataPoint();
			dp.setX( new BigDecimal( i * 0.1d ).setScale( 19, BigDecimal.ROUND_DOWN ) );
			dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale( 19, BigDecimal.ROUND_DOWN ) );
			s.persist( dp );
		}
		t.commit();
		s.close();

		assertInsertCount( 100, scope );
		assertUpdateCount( 0, scope );
		clearCounts( scope );

		s = openSession( scope );
		t = s.beginTransaction();
		DataPoint dpLast = (DataPoint) s.get( DataPoint.class, dp.getId() );
		assertFalse( s.isReadOnly( dpLast ) );
		int i = 0;
		int nExpectedChanges = 0;
		try (ScrollableResults sr = s.createQuery( "from DataPoint dp order by dp.x asc" )
				.setReadOnly( true )
				.scroll( ScrollMode.FORWARD_ONLY )) {
			while ( sr.next() ) {
				dp = (DataPoint) sr.get();
				if ( dp.getId() == dpLast.getId() ) {
					//dpLast existed in the session before executing the read-only query
					assertFalse( s.isReadOnly( dp ) );
				}
				else {
					assertTrue( s.isReadOnly( dp ) );
				}
				if ( ++i == 50 ) {
					s.setReadOnly( dp, false );
					nExpectedChanges = ( dp == dpLast ? 1 : 2 );
				}
				dp.setDescription( "done!" );
			}
		}
		t.commit();
		s.clear();

		assertInsertCount( 0, scope );
		assertUpdateCount( nExpectedChanges, scope );
		clearCounts( scope );

		t = s.beginTransaction();
		List list = s.createQuery( "from DataPoint where description='done!'" ).list();
		assertEquals( list.size(), nExpectedChanges );
		assertEquals( 100, s.createQuery( "delete from DataPoint" ).executeUpdate() );
		t.commit();
		s.close();

		assertUpdateCount( 0, scope );
	}

	@Test
	public void testModifiableModeWithExistingReadOnlyEntity(SessionFactoryScope scope) {
		clearCounts( scope );

		Session s = openSession( scope );
		Transaction t = s.beginTransaction();
		DataPoint dp = null;
		for ( int i = 0; i < 100; i++ ) {
			dp = new DataPoint();
			dp.setX( new BigDecimal( i * 0.1d ).setScale( 19, BigDecimal.ROUND_DOWN ) );
			dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale( 19, BigDecimal.ROUND_DOWN ) );
			s.persist( dp );
		}
		t.commit();
		s.close();

		assertInsertCount( 100, scope );
		assertUpdateCount( 0, scope );
		clearCounts( scope );

		s = openSession( scope );
		t = s.beginTransaction();
		DataPoint dpLast = (DataPoint) s.get( DataPoint.class, dp.getId() );
		assertFalse( s.isReadOnly( dpLast ) );
		s.setReadOnly( dpLast, true );
		assertTrue( s.isReadOnly( dpLast ) );
		dpLast.setDescription( "oy" );
		int i = 0;

		assertUpdateCount( 0, scope );

		int nExpectedChanges = 0;
		try (ScrollableResults sr = s.createQuery( "from DataPoint dp order by dp.x asc" )
				.setReadOnly( false )
				.scroll( ScrollMode.FORWARD_ONLY )) {
			while ( sr.next() ) {
				dp = (DataPoint) sr.get();
				if ( dp.getId() == dpLast.getId() ) {
					//dpLast existed in the session before executing the read-only query
					assertTrue( s.isReadOnly( dp ) );
				}
				else {
					assertFalse( s.isReadOnly( dp ) );
				}
				if ( ++i == 50 ) {
					s.setReadOnly( dp, true );
					nExpectedChanges = ( dp == dpLast ? 99 : 98 );
				}
				dp.setDescription( "done!" );
			}
		}
		t.commit();
		s.clear();

		assertUpdateCount( nExpectedChanges, scope );
		clearCounts( scope );

		t = s.beginTransaction();
		List list = s.createQuery( "from DataPoint where description='done!'" ).list();
		assertEquals( list.size(), nExpectedChanges );
		assertEquals( 100, s.createQuery( "delete from DataPoint" ).executeUpdate() );
		t.commit();
		s.close();

		assertUpdateCount( 0, scope );
	}

	@Test
	public void testReadOnlyOnTextType(SessionFactoryScope scope) {
		final String origText = "some huge text string";
		final String newText = "some even bigger text string";

		clearCounts( scope );

		Session s = openSession( scope );
		s.beginTransaction();
		TextHolder holder = new TextHolder( origText );
		s.persist( holder );
		Long id = holder.getId();
		s.getTransaction().commit();
		s.close();

		assertInsertCount( 1, scope );
		assertUpdateCount( 0, scope );
		clearCounts( scope );

		s = openSession( scope );
		s.beginTransaction();
		holder = s.get( TextHolder.class, id );
		s.setReadOnly( holder, true );
		holder.setTheText( newText );
		s.flush();
		s.getTransaction().commit();
		s.close();

		assertUpdateCount( 0, scope );

		s = openSession( scope );
		s.beginTransaction();
		holder = s.get( TextHolder.class, id );
		assertEquals( origText, holder.getTheText(), "change written to database" );
		s.remove( holder );
		s.getTransaction().commit();
		s.close();

		assertUpdateCount( 0, scope );
		assertDeleteCount( 1, scope );
	}

	@Test
	public void testMergeWithReadOnlyEntity(SessionFactoryScope scope) {
		clearCounts( scope );

		Session s = openSession( scope );
		Transaction t = s.beginTransaction();
		DataPoint dp = new DataPoint();
		dp.setX( new BigDecimal( 0.1d ).setScale( 19, BigDecimal.ROUND_DOWN ) );
		dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale( 19, BigDecimal.ROUND_DOWN ) );
		s.persist( dp );
		t.commit();
		s.close();

		assertInsertCount( 1, scope );
		assertUpdateCount( 0, scope );
		clearCounts( scope );

		dp.setDescription( "description" );

		s = openSession( scope );
		t = s.beginTransaction();
		DataPoint dpManaged = s.get( DataPoint.class, new Long( dp.getId() ) );
		s.setReadOnly( dpManaged, true );
		DataPoint dpMerged = s.merge( dp );
		assertSame( dpManaged, dpMerged );
		t.commit();
		s.close();

		assertUpdateCount( 0, scope );

		s = openSession( scope );
		t = s.beginTransaction();
		dpManaged = s.get( DataPoint.class, new Long( dp.getId() ) );
		assertNull( dpManaged.getDescription() );
		s.remove( dpManaged );
		t.commit();
		s.close();

		assertUpdateCount( 0, scope );
		assertDeleteCount( 1, scope );
	}

	private Session openSession(SessionFactoryScope scope) {
		return scope.getSessionFactory().openSession();
	}
}
