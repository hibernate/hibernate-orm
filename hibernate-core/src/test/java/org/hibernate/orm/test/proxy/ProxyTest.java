/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.proxy;

import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.hibernate.LockMode;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.SessionImpl;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Gavin King
 */
public class ProxyTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "proxy/DataPoint.hbm.xml" };
	}

	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/orm/test/";
	}

	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.STATEMENT_BATCH_SIZE, 0 ); // problem on HSQLDB (go figure)
	}

	@Test
	public void testFinalizeFiltered() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		DataPoint dp = new DataPoint();
		dp.setDescription("a data point");
		dp.setX( new BigDecimal("1.0") );
		dp.setY( new BigDecimal("2.0") );
		s.persist(dp);
		s.flush();
		s.clear();

		dp = s.getReference(DataPoint.class, dp.getId());
		assertFalse( Hibernate.isInitialized(dp) );

		try {
			dp.getClass().getDeclaredMethod( "finalize", (Class[]) null );
			fail();

		}
		catch (NoSuchMethodException e) {}

		s.remove(dp);
		t.commit();
		s.close();

	}

	@Test
	public void testProxyException() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		DataPoint dp = new DataPoint();
		dp.setDescription("a data point");
		dp.setX( new BigDecimal("1.0") );
		dp.setY( new BigDecimal("2.0") );
		s.persist(dp);
		s.flush();
		s.clear();

		dp = s.getReference( DataPoint.class, dp.getId() );
		assertFalse( Hibernate.isInitialized(dp) );

		try {
			dp.exception();
			fail();
		}
		catch (Exception e) {
			assertTrue( e.getClass()==Exception.class );
		}
		s.remove(dp);
		t.commit();
		s.close();
	}

	@Test
	public void testProxyExceptionWithNewGetReference() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		DataPoint dp = new DataPoint();
		dp.setDescription("a data point");
		dp.setX( new BigDecimal("1.0") );
		dp.setY( new BigDecimal("2.0") );
		s.persist(dp);
		s.flush();
		s.clear();

		dp = s.getReference(dp);
		assertFalse( Hibernate.isInitialized(dp) );

		try {
			dp.exception();
			fail();
		}
		catch (Exception e) {
			assertTrue( e.getClass()==Exception.class );
		}
		s.remove(dp);
		t.commit();
		s.close();
	}

	@Test
	public void testProxyExceptionWithOldGetReference() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		DataPoint dp = new DataPoint();
		dp.setDescription("a data point");
		dp.setX( new BigDecimal("1.0") );
		dp.setY( new BigDecimal("2.0") );
		s.persist(dp);
		s.flush();
		s.clear();

		dp = s.getReference( DataPoint.class, dp.getId() );
		assertFalse( Hibernate.isInitialized(dp) );

		try {
			dp.exception();
			fail();
		}
		catch (Exception e) {
			assertTrue( e.getClass()==Exception.class );
		}
		s.remove(dp);
		t.commit();
		s.close();
	}

	@Test
	public void testProxySerializationAfterSessionClosed() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		DataPoint dp = new DataPoint();
		dp.setDescription("a data point");
		dp.setX( new BigDecimal("1.0") );
		dp.setY( new BigDecimal("2.0") );
		s.persist(dp);
		s.flush();
		s.clear();

		dp = s.getReference( DataPoint.class, dp.getId());
		assertFalse( Hibernate.isInitialized(dp) );
		s.close();
		SerializationHelper.clone( dp );

		s = openSession();
		t = s.beginTransaction();
		s.remove( dp );
		t.commit();
		s.close();
	}

	@Test
	public void testInitializedProxySerializationAfterSessionClosed() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		DataPoint dp = new DataPoint();
		dp.setDescription("a data point");
		dp.setX( new BigDecimal("1.0") );
		dp.setY( new BigDecimal("2.0") );
		s.persist(dp);
		s.flush();
		s.clear();

		dp = s.getReference( DataPoint.class, dp.getId());
		assertFalse( Hibernate.isInitialized(dp) );
		Hibernate.initialize( dp );
		assertTrue( Hibernate.isInitialized(dp) );
		s.close();
		SerializationHelper.clone( dp );

		s = openSession();
		t = s.beginTransaction();
		s.remove( dp );
		t.commit();
		s.close();
	}

	@Test
	public void testProxySerialization() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		DataPoint dp = new DataPoint();
		dp.setDescription("a data point");
		dp.setX( new BigDecimal("1.0") );
		dp.setY( new BigDecimal("2.0") );
		s.persist(dp);
		s.flush();
		s.clear();

		dp = s.getReference( DataPoint.class, dp.getId());
		assertFalse( Hibernate.isInitialized(dp) );
		dp.getId();
		assertFalse( Hibernate.isInitialized(dp) );
		dp.getDescription();
		assertTrue( Hibernate.isInitialized(dp) );
		Object none = s.getReference( DataPoint.class, 666L);
		assertFalse( Hibernate.isInitialized(none) );

		t.commit();
		s.unwrap( SessionImplementor.class ).getJdbcCoordinator().getLogicalConnection().manualDisconnect();

		Object[] holder = new Object[] { s, dp, none };

		holder = (Object[]) SerializationHelper.clone(holder);
		Session sclone = (Session) holder[0];
		dp = (DataPoint) holder[1];
		none = holder[2];

		//close the original:
		s.close();

		t = sclone.beginTransaction();

		DataPoint sdp = sclone.getReference( DataPoint.class, dp.getId());
		assertSame(dp, sdp);
		assertFalse(sdp instanceof HibernateProxy);
		Object snone = sclone.getReference( DataPoint.class, 666L);
		assertSame(none, snone);
		assertTrue(snone instanceof HibernateProxy);

		sclone.remove(dp);

		t.commit();
		sclone.close();

	}

	@Test
	public void testProxy() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		DataPoint dp = new DataPoint();
		dp.setDescription("a data point");
		dp.setX( new BigDecimal("1.0") );
		dp.setY( new BigDecimal("2.0") );
		s.persist(dp);
		s.flush();
		s.clear();

		dp = s.getReference( DataPoint.class, dp.getId());
		assertFalse( Hibernate.isInitialized(dp) );
		DataPoint dp2 = s.get( DataPoint.class, dp.getId());
		assertSame(dp, dp2);
		assertTrue( Hibernate.isInitialized(dp) );
		s.clear();

		dp = s.getReference( DataPoint.class, dp.getId());
		assertFalse( Hibernate.isInitialized(dp) );
		dp2 = s.getReference( DataPoint.class, dp.getId() );
		assertSame(dp, dp2);
		assertFalse( Hibernate.isInitialized(dp) );
		s.clear();

		dp = s.getReference( DataPoint.class, dp.getId());
		assertFalse( Hibernate.isInitialized(dp) );
		dp2 = s.get( DataPoint.class, dp.getId(), LockMode.READ );
		assertSame(dp, dp2);
		assertTrue( Hibernate.isInitialized(dp) );
		s.clear();

		dp = s.getReference( DataPoint.class, dp.getId());
		assertFalse( Hibernate.isInitialized(dp) );
		dp2 = s.find( DataPoint.class, dp.getId(), LockMode.READ );
		assertSame(dp, dp2);
		assertTrue( Hibernate.isInitialized(dp) );
		s.clear();

		dp = s.getReference( DataPoint.class, dp.getId());
		assertFalse( Hibernate.isInitialized(dp) );
		dp2 = (DataPoint) s.createQuery("from DataPoint").uniqueResult();
		assertSame(dp, dp2);
		assertTrue( Hibernate.isInitialized(dp) );
		s.remove( dp );
		t.commit();
		s.close();
	}

	@Test
	public void testProxyWithGetReference() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		DataPoint dp = new DataPoint();
		dp.setDescription("a data point");
		dp.setX( new BigDecimal("1.0") );
		dp.setY( new BigDecimal("2.0") );
		s.persist(dp);
		s.flush();
		s.clear();

		dp = s.getReference( DataPoint.class, dp.getId() );
		assertFalse( Hibernate.isInitialized(dp) );
		DataPoint dp2 = s.get( DataPoint.class, dp.getId() );
		assertSame(dp, dp2);
		assertTrue( Hibernate.isInitialized(dp) );
		s.clear();

		dp = s.getReference( DataPoint.class, dp.getId() );
		assertFalse( Hibernate.isInitialized(dp) );
		dp2 = s.getReference( DataPoint.class, dp.getId() );
		assertSame(dp, dp2);
		assertFalse( Hibernate.isInitialized(dp) );
		s.clear();

		dp = s.getReference( dp );
		assertFalse( Hibernate.isInitialized(dp) );
		dp2 = s.getReference( dp );
		assertSame(dp, dp2);
		assertFalse( Hibernate.isInitialized(dp) );
		s.clear();

		dp = s.getReference( DataPoint.class, dp.getId() );
		assertFalse( Hibernate.isInitialized(dp) );
		dp2 = s.find( DataPoint.class, dp.getId(), LockMode.READ );
		assertSame(dp, dp2);
		assertTrue( Hibernate.isInitialized(dp) );
		s.clear();

		dp = s.getReference( DataPoint.class, dp.getId() );
		assertFalse( Hibernate.isInitialized(dp) );
		dp2 = (DataPoint) s.createQuery("from DataPoint").uniqueResult();
		assertSame(dp, dp2);
		assertTrue( Hibernate.isInitialized(dp) );
		s.remove( dp );
		t.commit();
		s.close();
	}

	@Test
	public void testSubsequentNonExistentProxyAccess() {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		DataPoint proxy = s.getReference( DataPoint.class, (long) -1);
		assertFalse( Hibernate.isInitialized( proxy ) );
		try {
			proxy.getDescription();
			fail( "proxy access did not fail on non-existent proxy" );
		}
		catch( ObjectNotFoundException onfe ) {
			// expected
		}
		catch( Throwable e ) {
			fail( "unexpected exception type on non-existent proxy access : " + e );
		}
		// try it a second (subsequent) time...
		try {
			proxy.getDescription();
			fail( "proxy access did not fail on non-existent proxy" );
		}
		catch( ObjectNotFoundException onfe ) {
			// expected
		}
		catch( Throwable e ) {
			fail( "unexpected exception type on non-existent proxy access : " + e );
		}

		t.commit();
		s.close();
	}

	@SuppressWarnings( {"unchecked"})
	@Test
	public void testProxyEviction() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Container container = new Container( "container" );
		container.setOwner( new Owner( "owner" ) );
		container.setInfo( new Info( "blah blah blah" ) );
		container.getDataPoints().add( new DataPoint( new BigDecimal( 1 ), new BigDecimal( 1 ), "first data point" ) );
		container.getDataPoints().add( new DataPoint( new BigDecimal( 2 ), new BigDecimal( 2 ), "second data point" ) );
		s.persist( container );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		Container c = s.getReference( Container.class, container.getId() );
		assertFalse( Hibernate.isInitialized( c ) );
		s.evict( c );
		try {
			c.getName();
			fail( "expecting LazyInitializationException" );
		}
		catch( LazyInitializationException e ) {
			// expected result
		}

		c = s.getReference( Container.class, container.getId() );
		assertFalse( Hibernate.isInitialized( c ) );
		Info i = c.getInfo();
		assertTrue( Hibernate.isInitialized( c ) );
		assertFalse( Hibernate.isInitialized( i ) );
		s.evict( c );
		try {
			i.getDetails();
			fail( "expecting LazyInitializationException" );
		}
		catch( LazyInitializationException e ) {
			// expected result
		}

		s.remove( c );

		t.commit();
		s.close();
	}

	@Test
	public void testFullyLoadedPCSerialization() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Long lastContainerId = null;
		int containerCount = 10;
		int nestedDataPointCount = 5;
		for ( int c_indx = 0; c_indx < containerCount; c_indx++ ) {
			Owner owner = new Owner( "Owner #" + c_indx );
			Container container = new Container( "Container #" + c_indx );
			container.setOwner( owner );
			for ( int dp_indx = 0; dp_indx < nestedDataPointCount; dp_indx++ ) {
				DataPoint dp = new DataPoint();
				dp.setDescription( "data-point [" + c_indx + ", " + dp_indx + "]" );
// more HSQLDB fun...
//				dp.setX( new BigDecimal( c_indx ) );
				dp.setX( new BigDecimal( c_indx + dp_indx ) );
				dp.setY( new BigDecimal( dp_indx ) );
				container.getDataPoints().add( dp );
			}
			s.persist( container );
			lastContainerId = container.getId();
		}
		t.commit();
		s.close();

		s = openSession();
		s.setHibernateFlushMode( FlushMode.MANUAL );
		t = s.beginTransaction();
		// load the last container as a proxy
		Container proxy = s.getReference( Container.class, lastContainerId );
		assertFalse( Hibernate.isInitialized( proxy ) );
		// load the rest back into the PC
		List all = s.createQuery( "from Container as c inner join fetch c.owner inner join fetch c.dataPoints where c.id <> :l" )
				.setParameter( "l", lastContainerId.longValue() )
				.list();
		Container container = ( Container ) all.get( 0 );
		s.remove( container );
		// force a snapshot retrieval of the proxied container
		SessionImpl sImpl = ( SessionImpl ) s;
		sImpl.getPersistenceContext().getDatabaseSnapshot(
				lastContainerId,
				sImpl.getFactory().getMappingMetamodel().getEntityDescriptor(Container.class.getName())
		);
		assertFalse( Hibernate.isInitialized( proxy ) );
		t.commit();

//		int iterations = 50;
//		long cumulativeTime = 0;
//		long cumulativeSize = 0;
//		for ( int i = 0; i < iterations; i++ ) {
//			final long start = System.currentTimeMillis();
//			byte[] bytes = SerializationHelper.serialize( s );
//			SerializationHelper.deserialize( bytes );
//			final long end = System.currentTimeMillis();
//			cumulativeTime += ( end - start );
//			int size = bytes.length;
//			cumulativeSize += size;
////			System.out.println( "Iteration #" + i + " took " + ( end - start ) + " ms : size = " + size + " bytes" );
//		}
//		System.out.println( "Average time : " + ( cumulativeTime / iterations ) + " ms" );
//		System.out.println( "Average size : " + ( cumulativeSize / iterations ) + " bytes" );

		byte[] bytes = SerializationHelper.serialize( s );
		SerializationHelper.deserialize( bytes );

		t = s.beginTransaction();
		int count = s.createQuery( "delete DataPoint" ).executeUpdate();
		assertEquals( "unexpected DP delete count", ( containerCount * nestedDataPointCount ), count );
		count = s.createQuery( "delete Container" ).executeUpdate();
		assertEquals( "unexpected container delete count", containerCount, count );
		count = s.createQuery( "delete Owner" ).executeUpdate();
		assertEquals( "unexpected owner delete count", containerCount, count );
		t.commit();
		s.close();
	}

	@Test
	public void testRefreshLockInitializedProxy() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		DataPoint dp = newPersistentDataPoint( s );

		dp = s.getReference( DataPoint.class, dp.getId());
		dp.getX();
		assertTrue( Hibernate.isInitialized( dp ) );

		s.refresh( dp, LockMode.PESSIMISTIC_WRITE );
		assertSame( LockMode.PESSIMISTIC_WRITE, s.getCurrentLockMode( dp ) );

		s.remove( dp );
		t.commit();
		s.close();
	}

	@Test
	@JiraKey( "HHH-1645" )
	public void testRefreshLockUninitializedProxy() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		DataPoint dp = newPersistentDataPoint( s );

		dp = s.getReference( DataPoint.class, dp.getId());
		assertFalse( Hibernate.isInitialized( dp ) );

		s.refresh( dp, LockMode.PESSIMISTIC_WRITE );
		assertSame( LockMode.PESSIMISTIC_WRITE, s.getCurrentLockMode( dp ) );

		s.remove( dp );
		t.commit();
		s.close();
	}

	private static DataPoint newPersistentDataPoint(Session s) {
		DataPoint dp = new DataPoint();
		dp.setDescription( "a data point" );
		dp.setX( new BigDecimal("1.0") );
		dp.setY( new BigDecimal("2.0") );
		s.persist( dp );
		s.flush();
		s.clear();
		return dp;
	}

	@Test
	@JiraKey( "HHH-1645" )
	public void testRefreshLockUninitializedProxyThenRead() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		DataPoint dp = newPersistentDataPoint( s );

		dp = s.getReference( DataPoint.class, dp.getId());
		assertFalse( Hibernate.isInitialized( dp ) );
		s.refresh( dp, LockMode.PESSIMISTIC_WRITE );
		dp.getX();
		assertSame( LockMode.PESSIMISTIC_WRITE, s.getCurrentLockMode( dp ) );

		s.remove( dp );
		t.commit();
		s.close();
	}

	@Test
	public void testLockUninitializedProxy() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		DataPoint dp = newPersistentDataPoint( s );

		dp = s.getReference( DataPoint.class, dp.getId());
		assertFalse( Hibernate.isInitialized( dp ) );
		s.lock( dp, LockMode.PESSIMISTIC_WRITE );
		assertSame( LockMode.PESSIMISTIC_WRITE, s.getCurrentLockMode( dp ) );

		s.remove( dp );
		t.commit();
		s.close();
	}
}
