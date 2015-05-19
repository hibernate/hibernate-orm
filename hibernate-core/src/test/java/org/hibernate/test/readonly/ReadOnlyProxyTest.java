/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.readonly;

import java.math.BigDecimal;

import org.junit.Test;

import org.hibernate.CacheMode;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionException;
import org.hibernate.Transaction;
import org.hibernate.TransientObjectException;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.testing.FailureExpected;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests making initialized and uninitialized proxies read-only/modifiable
 * 
 * @author Gail Badner
 */
public class ReadOnlyProxyTest extends AbstractReadOnlyTest {
	@Override
	public String[] getMappings() {
		return new String[] { "readonly/DataPoint.hbm.xml", "readonly/TextHolder.hbm.xml" };
	}

	@Test
	public void testReadOnlyViaSessionDoesNotInit() {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE );

		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		s.beginTransaction();
		DataPoint dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dp instanceof HibernateProxy );
		checkReadOnly( s, dp, false );
		assertFalse( Hibernate.isInitialized( dp ) );
		s.setReadOnly( dp, true );
		checkReadOnly( s, dp, true );
		assertFalse( Hibernate.isInitialized( dp ) );
		s.setReadOnly( dp, false );
		checkReadOnly( s, dp, false );
		assertFalse( Hibernate.isInitialized( dp ) );
		s.flush();
		checkReadOnly( s, dp, false );
		assertFalse( Hibernate.isInitialized( dp ) );
		s.getTransaction().commit();
		checkReadOnly( s, dp, false );
		assertFalse( Hibernate.isInitialized( dp ) );
		s.close();

		s = openSession();
		s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dpOrig.getId() );
		assertEquals( dpOrig.getId(), dp.getId() );
		assertEquals( dpOrig.getDescription(), dp.getDescription() );
		assertEquals( dpOrig.getX(), dp.getX() );
		assertEquals( dpOrig.getY(), dp.getY() );
		s.delete( dp );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testReadOnlyViaLazyInitializerDoesNotInit() {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE );

		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		s.beginTransaction();
		DataPoint dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dp instanceof HibernateProxy );
		LazyInitializer dpLI = ( ( HibernateProxy ) dp ).getHibernateLazyInitializer();
		checkReadOnly( s, dp, false );
		assertFalse( Hibernate.isInitialized( dp ) );
		dpLI.setReadOnly( true );
		checkReadOnly( s, dp, true );
		assertFalse( Hibernate.isInitialized( dp ) );
		dpLI.setReadOnly( false );
		checkReadOnly( s, dp, false );
		assertFalse( Hibernate.isInitialized( dp ) );
		s.flush();
		checkReadOnly( s, dp, false );
		assertFalse( Hibernate.isInitialized( dp ) );
		s.getTransaction().commit();
		checkReadOnly( s, dp, false );
		assertFalse( Hibernate.isInitialized( dp ) );
		s.close();

		s = openSession();
		s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dpOrig.getId() );
		assertEquals( dpOrig.getId(), dp.getId() );
		assertEquals( dpOrig.getDescription(), dp.getDescription() );
		assertEquals( dpOrig.getX(), dp.getX() );
		assertEquals( dpOrig.getY(), dp.getY() );
		s.delete( dp );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testReadOnlyViaSessionNoChangeAfterInit() {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE );

		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		s.beginTransaction();
		DataPoint dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dp instanceof HibernateProxy );
		checkReadOnly( s, dp, false );
		assertFalse( Hibernate.isInitialized( dp ) );
		Hibernate.initialize( dp );
		assertTrue( Hibernate.isInitialized( dp ) );
		checkReadOnly( s, dp, false );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		s.beginTransaction();
		dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dp instanceof HibernateProxy );
		s.setReadOnly( dp, true );
		checkReadOnly( s, dp, true );
		assertFalse( Hibernate.isInitialized( dp ) );
		Hibernate.initialize( dp );
		assertTrue( Hibernate.isInitialized( dp ) );
		checkReadOnly( s, dp, true );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		s.beginTransaction();
		dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dp instanceof HibernateProxy );
		s.setReadOnly( dp, true );
		checkReadOnly( s, dp, true );
		assertFalse( Hibernate.isInitialized( dp ) );
		s.setReadOnly( dp, false );
		checkReadOnly( s, dp, false );
		assertFalse( Hibernate.isInitialized( dp ) );
		Hibernate.initialize( dp );
		assertTrue( Hibernate.isInitialized( dp ) );
		checkReadOnly( s, dp, false );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dpOrig.getId() );
		assertEquals( dpOrig.getId(), dp.getId() );
		assertEquals( dpOrig.getDescription(), dp.getDescription() );
		assertEquals( dpOrig.getX(), dp.getX() );
		assertEquals( dpOrig.getY(), dp.getY() );
		s.delete( dp );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testReadOnlyViaLazyInitializerNoChangeAfterInit() {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE );

		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		s.beginTransaction();
		DataPoint dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dp instanceof HibernateProxy );
		LazyInitializer dpLI = ( ( HibernateProxy ) dp ).getHibernateLazyInitializer();
		checkReadOnly( s, dp, false );
		assertTrue( dpLI.isUninitialized() );
		Hibernate.initialize( dp );
		assertFalse( dpLI.isUninitialized() );
		checkReadOnly( s, dp, false );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		s.beginTransaction();
		dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dp instanceof HibernateProxy );
		dpLI = ( ( HibernateProxy ) dp ).getHibernateLazyInitializer();
		dpLI.setReadOnly( true );
		checkReadOnly( s, dp, true );
		assertTrue( dpLI.isUninitialized() );
		Hibernate.initialize( dp );
		assertFalse( dpLI.isUninitialized() );
		checkReadOnly( s, dp, true );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		s.beginTransaction();
		dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dp instanceof HibernateProxy );
		dpLI = ( ( HibernateProxy ) dp ).getHibernateLazyInitializer();
		dpLI.setReadOnly( true );
		checkReadOnly( s, dp, true );
		assertTrue( dpLI.isUninitialized() );
		dpLI.setReadOnly( false );
		checkReadOnly( s, dp, false );
		assertTrue( dpLI.isUninitialized() );
		Hibernate.initialize( dp );
		assertFalse( dpLI.isUninitialized() );
		checkReadOnly( s, dp, false );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dpOrig.getId() );
		assertEquals( dpOrig.getId(), dp.getId() );
		assertEquals( dpOrig.getDescription(), dp.getDescription() );
		assertEquals( dpOrig.getX(), dp.getX() );
		assertEquals( dpOrig.getY(), dp.getY() );
		s.delete( dp );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testReadOnlyViaSessionBeforeInit() {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE );

		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		s.beginTransaction();
		DataPoint dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dp instanceof HibernateProxy );
		s.setReadOnly( dp, true );
		dp.setDescription( "changed" );
		assertTrue( Hibernate.isInitialized( dp ) );
		assertEquals( "changed", dp.getDescription() );
		checkReadOnly( s, dp, true );
		s.flush();
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dpOrig.getId() );
		assertEquals( dpOrig.getId(), dp.getId() );
		assertEquals( dpOrig.getDescription(), dp.getDescription() );
		assertEquals( dpOrig.getX(), dp.getX() );
		assertEquals( dpOrig.getY(), dp.getY() );
		s.delete( dp );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testModifiableViaSessionBeforeInit() {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE );

		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		s.beginTransaction();
		DataPoint dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dp instanceof HibernateProxy );
		checkReadOnly( s, dp, false );
		dp.setDescription( "changed" );
		assertTrue( Hibernate.isInitialized( dp ) );
		assertEquals( "changed", dp.getDescription() );
		checkReadOnly( s, dp, false );
		s.flush();
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dpOrig.getId() );
		assertEquals( dpOrig.getId(), dp.getId() );
		assertEquals( "changed", dp.getDescription() );
		assertEquals( dpOrig.getX(), dp.getX() );
		assertEquals( dpOrig.getY(), dp.getY() );
		s.delete( dp );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testReadOnlyViaSessionBeforeInitByModifiableQuery() {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE );

		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		s.beginTransaction();
		DataPoint dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dp instanceof HibernateProxy );
		assertFalse( Hibernate.isInitialized( dp ) );
		checkReadOnly( s, dp, false );
		s.setReadOnly( dp, true );
		assertFalse( Hibernate.isInitialized( dp ) );
		checkReadOnly( s, dp, true );
		assertFalse( Hibernate.isInitialized( dp ) );
		DataPoint dpFromQuery = ( DataPoint ) s.createQuery( "from DataPoint where id=" + dpOrig.getId() ).setReadOnly( false ).uniqueResult();
		assertTrue( Hibernate.isInitialized( dpFromQuery ) );
		assertSame( dp, dpFromQuery );
		checkReadOnly( s, dp, true );
		dp.setDescription( "changed" );
		assertEquals( "changed", dp.getDescription() );
		s.flush();
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dpOrig.getId() );
		assertEquals( dpOrig.getId(), dp.getId() );
		assertEquals( dpOrig.getDescription(), dp.getDescription() );
		assertEquals( dpOrig.getX(), dp.getX() );
		assertEquals( dpOrig.getY(), dp.getY() );
		s.delete( dp );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testReadOnlyViaSessionBeforeInitByReadOnlyQuery() {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE );

		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		s.beginTransaction();
		DataPoint dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dp instanceof HibernateProxy );
		assertFalse( Hibernate.isInitialized( dp ) );
		checkReadOnly( s, dp, false );
		s.setReadOnly( dp, true );
		checkReadOnly( s, dp, true );
		DataPoint dpFromQuery = ( DataPoint ) s.createQuery( "from DataPoint where id=" + dpOrig.getId() ).setReadOnly( true ).uniqueResult();
		assertTrue( Hibernate.isInitialized( dpFromQuery ) );
		assertSame( dp, dpFromQuery );
		checkReadOnly( s, dp, true );
		dp.setDescription( "changed" );
		assertEquals( "changed", dp.getDescription() );
		s.flush();
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dpOrig.getId() );
		assertEquals( dpOrig.getId(), dp.getId() );
		assertEquals( dpOrig.getDescription(), dp.getDescription() );
		assertEquals( dpOrig.getX(), dp.getX() );
		assertEquals( dpOrig.getY(), dp.getY() );
		s.delete( dp );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testModifiableViaSessionBeforeInitByModifiableQuery() {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE );

		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		s.beginTransaction();
		DataPoint dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dp instanceof HibernateProxy );
		assertFalse( Hibernate.isInitialized( dp ) );
		checkReadOnly( s, dp, false );
		DataPoint dpFromQuery = ( DataPoint ) s.createQuery( "from DataPoint where id=" + dpOrig.getId() ).setReadOnly( false ).uniqueResult();
		assertTrue( Hibernate.isInitialized( dpFromQuery ) );
		assertSame( dp, dpFromQuery );
		checkReadOnly( s, dp, false );
		dp.setDescription( "changed" );
		assertEquals( "changed", dp.getDescription() );
		s.flush();
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dpOrig.getId() );
		assertEquals( dpOrig.getId(), dp.getId() );
		assertEquals( "changed", dp.getDescription() );
		assertEquals( dpOrig.getX(), dp.getX() );
		assertEquals( dpOrig.getY(), dp.getY() );
		s.delete( dp );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testModifiableViaSessionBeforeInitByReadOnlyQuery() {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE );

		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		s.beginTransaction();
		DataPoint dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dp instanceof HibernateProxy );
		checkReadOnly( s, dp, false );
		assertFalse( Hibernate.isInitialized( dp ) );
		DataPoint dpFromQuery = ( DataPoint ) s.createQuery( "from DataPoint where id=" + dpOrig.getId() ).setReadOnly( true ).uniqueResult();
		assertTrue( Hibernate.isInitialized( dpFromQuery ) );
		assertSame( dp, dpFromQuery );
		checkReadOnly( s, dp, false );
		dp.setDescription( "changed" );
		assertEquals( "changed", dp.getDescription() );
		s.flush();
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dpOrig.getId() );
		assertEquals( dpOrig.getId(), dp.getId() );
		assertEquals( "changed", dp.getDescription() );
		assertEquals( dpOrig.getX(), dp.getX() );
		assertEquals( dpOrig.getY(), dp.getY() );
		s.delete( dp );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testReadOnlyViaLazyInitializerBeforeInit() {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE );

		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		s.beginTransaction();
		DataPoint dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dp instanceof HibernateProxy );
		LazyInitializer dpLI = ( ( HibernateProxy ) dp ).getHibernateLazyInitializer();
		assertTrue( dpLI.isUninitialized() );
		checkReadOnly( s, dp, false );
		dpLI.setReadOnly( true );
		checkReadOnly( s, dp, true );
		dp.setDescription( "changed" );
		assertFalse( dpLI.isUninitialized() );
		assertEquals( "changed", dp.getDescription() );
		checkReadOnly( s, dp, true );
		s.flush();
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dpOrig.getId() );
		assertEquals( dpOrig.getId(), dp.getId() );
		assertEquals( dpOrig.getDescription(), dp.getDescription() );
		assertEquals( dpOrig.getX(), dp.getX() );
		assertEquals( dpOrig.getY(), dp.getY() );
		s.delete( dp );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testModifiableViaLazyInitializerBeforeInit() {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE );

		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		s.beginTransaction();
		DataPoint dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dp instanceof HibernateProxy );
		LazyInitializer dpLI = ( ( HibernateProxy ) dp ).getHibernateLazyInitializer();
		assertTrue( dp instanceof HibernateProxy );
		assertTrue( dpLI.isUninitialized() );
		checkReadOnly( s, dp, false );
		dp.setDescription( "changed" );
		assertFalse( dpLI.isUninitialized() );
		assertEquals( "changed", dp.getDescription() );
		checkReadOnly( s, dp, false );
		s.flush();
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dpOrig.getId() );
		assertEquals( dpOrig.getId(), dp.getId() );
		assertEquals( "changed", dp.getDescription() );
		assertEquals( dpOrig.getX(), dp.getX() );
		assertEquals( dpOrig.getY(), dp.getY() );
		s.delete( dp );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testReadOnlyViaLazyInitializerAfterInit() {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE );

		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		s.beginTransaction();
		DataPoint dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dp instanceof HibernateProxy );
		LazyInitializer dpLI = ( ( HibernateProxy ) dp ).getHibernateLazyInitializer();
		assertTrue( dpLI.isUninitialized() );
		checkReadOnly( s, dp, false );
		dp.setDescription( "changed" );
		assertFalse( dpLI.isUninitialized() );
		assertEquals( "changed", dp.getDescription() );
		checkReadOnly( s, dp, false );
		dpLI.setReadOnly( true );
		checkReadOnly( s, dp, true );
		s.flush();
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dpOrig.getId() );
		assertEquals( dpOrig.getId(), dp.getId() );
		assertEquals( dpOrig.getDescription(), dp.getDescription() );
		assertEquals( dpOrig.getX(), dp.getX() );
		assertEquals( dpOrig.getY(), dp.getY() );
		s.delete( dp );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testModifiableViaLazyInitializerAfterInit() {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE );

		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		s.beginTransaction();
		DataPoint dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dp instanceof HibernateProxy );
		LazyInitializer dpLI = ( ( HibernateProxy ) dp ).getHibernateLazyInitializer();
		assertTrue( dpLI.isUninitialized() );
		checkReadOnly( s, dp, false );
		dp.setDescription( "changed" );
		assertFalse( dpLI.isUninitialized() );
		assertEquals( "changed", dp.getDescription() );
		checkReadOnly( s, dp, false );
		s.flush();
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dpOrig.getId() );
		assertEquals( dpOrig.getId(), dp.getId() );
		assertEquals( "changed", dp.getDescription() );
		assertEquals( dpOrig.getX(), dp.getX() );
		assertEquals( dpOrig.getY(), dp.getY() );
		s.delete( dp );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@FailureExpected( jiraKey = "HHH-4642" )
	public void testModifyToReadOnlyToModifiableIsUpdated() {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE );

		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		s.beginTransaction();
		DataPoint dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dp instanceof HibernateProxy );
		assertFalse( Hibernate.isInitialized( dp ));
		checkReadOnly( s, dp, false );
		dp.setDescription( "changed" );
		assertTrue( Hibernate.isInitialized( dp ) );
		assertEquals( "changed", dp.getDescription() );
		s.setReadOnly( dp, true );
		checkReadOnly( s, dp,true );
		s.setReadOnly( dp, false );
		checkReadOnly( s, dp, false );
		assertEquals( "changed", dp.getDescription() );
		s.flush();
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dpOrig.getId() );
		assertEquals( dpOrig.getId(), dp.getId() );
		assertEquals( dpOrig.getDescription(), dp.getDescription() );
		assertEquals( dpOrig.getX(), dp.getX() );
		assertEquals( dpOrig.getY(), dp.getY() );
		try {
			assertEquals( "changed", dp.getDescription() );
			// should fail due to HHH-4642
		}
		finally {
			s.getTransaction().rollback();
			s.close();
			s = openSession();
			s.beginTransaction();			
			s.delete( dp );
			s.getTransaction().commit();
			s.close();
		}
	}

	@Test
	@FailureExpected( jiraKey = "HHH-4642" )
	public void testReadOnlyModifiedToModifiableIsUpdated() {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE );

		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		s.beginTransaction();
		DataPoint dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dp instanceof HibernateProxy );
		assertFalse( Hibernate.isInitialized( dp ));
		checkReadOnly( s, dp, false );
		s.setReadOnly( dp, true );
		checkReadOnly( s, dp,true );
		dp.setDescription( "changed" );
		assertTrue( Hibernate.isInitialized( dp ) );
		assertEquals( "changed", dp.getDescription() );
		s.setReadOnly( dp, false );
		checkReadOnly( s, dp, false );
		assertEquals( "changed", dp.getDescription() );
		s.flush();
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dpOrig.getId() );
		assertEquals( dpOrig.getId(), dp.getId() );
		assertEquals( dpOrig.getDescription(), dp.getDescription() );
		assertEquals( dpOrig.getX(), dp.getX() );
		assertEquals( dpOrig.getY(), dp.getY() );
		try {
			assertEquals( "changed", dp.getDescription() );
			// should fail due to HHH-4642
		}
		finally {
			s.getTransaction().rollback();
			s.close();
			s = openSession();
			s.beginTransaction();
			s.delete( dp );
			s.getTransaction().commit();
			s.close();
		}
	}

	@Test
	public void testReadOnlyChangedEvictedUpdate() {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE );

		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		s.beginTransaction();
		DataPoint dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dp instanceof HibernateProxy );
		assertFalse( Hibernate.isInitialized( dp ));
		checkReadOnly( s, dp, false );
		s.setReadOnly( dp, true );
		checkReadOnly( s, dp,true );
		dp.setDescription( "changed" );
		assertTrue( Hibernate.isInitialized( dp ) );
		assertEquals( "changed", dp.getDescription() );
		s.evict( dp );
		assertFalse( s.contains( dp ) );
		s.update( dp );
		checkReadOnly( s, dp, false );
		assertEquals( "changed", dp.getDescription() );
		s.flush();
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dpOrig.getId() );
		assertEquals( "changed", dp.getDescription() );
		assertEquals( dpOrig.getId(), dp.getId() );
		assertEquals( dpOrig.getX(), dp.getX() );
		assertEquals( dpOrig.getY(), dp.getY() );
		s.delete( dp );
		s.getTransaction().commit();
		s.close();
	}	

	@Test
	public void testReadOnlyToModifiableInitWhenModifiedIsUpdated() {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE );

		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		s.beginTransaction();
		DataPoint dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dp instanceof HibernateProxy );
		checkReadOnly( s, dp, false );
		s.setReadOnly( dp, true );
		checkReadOnly( s, dp,true );
		s.setReadOnly( dp, false );
		checkReadOnly( s, dp, false );
		assertFalse( Hibernate.isInitialized( dp ));
		dp.setDescription( "changed" );
		assertTrue( Hibernate.isInitialized( dp ) );
		assertEquals( "changed", dp.getDescription() );
		s.flush();
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dpOrig.getId() );
		assertEquals( dpOrig.getId(), dp.getId() );
		assertEquals( "changed", dp.getDescription() );
		assertEquals( dpOrig.getX(), dp.getX() );
		assertEquals( dpOrig.getY(), dp.getY() );
		s.delete( dp );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testReadOnlyInitToModifiableModifiedIsUpdated() {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE );

		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		s.beginTransaction();
		DataPoint dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dp instanceof HibernateProxy );
		checkReadOnly( s, dp, false );
		s.setReadOnly( dp, true );
		checkReadOnly( s, dp,true );
		assertFalse( Hibernate.isInitialized( dp ));
		Hibernate.initialize( dp );
		assertTrue( Hibernate.isInitialized( dp ));
		checkReadOnly( s, dp,true );
		s.setReadOnly( dp, false );
		checkReadOnly( s, dp, false );
		dp.setDescription( "changed" );
		assertTrue( Hibernate.isInitialized( dp ) );
		assertEquals( "changed", dp.getDescription() );
		s.flush();
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dpOrig.getId() );
		assertEquals( dpOrig.getId(), dp.getId() );
		assertEquals( "changed", dp.getDescription() );
		assertEquals( dpOrig.getX(), dp.getX() );
		assertEquals( dpOrig.getY(), dp.getY() );
		s.delete( dp );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testReadOnlyModifiedUpdate() {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE );

		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		s.beginTransaction();
		DataPoint dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dp instanceof HibernateProxy );
		checkReadOnly( s, dp, false );
		s.setReadOnly( dp, true );
		checkReadOnly( s, dp,true );
		assertFalse( Hibernate.isInitialized( dp ));
		dp.setDescription( "changed" );
		assertTrue( Hibernate.isInitialized( dp ) );
		assertEquals( "changed", dp.getDescription() );
		checkReadOnly( s, dp,true );
		s.update( dp );
		s.flush();
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dpOrig.getId() );
		assertEquals( dpOrig.getId(), dp.getId() );
		assertEquals( dpOrig.getDescription(), dp.getDescription() );
		assertEquals( dpOrig.getX(), dp.getX() );
		assertEquals( dpOrig.getY(), dp.getY() );
		s.delete( dp );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testReadOnlyDelete() {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE );

		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		s.beginTransaction();
		DataPoint dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dp instanceof HibernateProxy );
		checkReadOnly( s, dp, false );
		s.setReadOnly( dp, true );
		checkReadOnly( s, dp,true );
		assertFalse( Hibernate.isInitialized( dp ));
		s.delete( dp );
		s.flush();
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dpOrig.getId() );
		assertNull( dp );
		s.getTransaction().commit();
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
		t = s.beginTransaction();
		dp = ( DataPoint ) s.load( DataPoint.class, dp.getId() );
		s.setReadOnly( dp, true );
		assertFalse( Hibernate.isInitialized( dp ) );
		s.refresh( dp );
		assertFalse( Hibernate.isInitialized( dp ) );
		assertEquals( "original", dp.getDescription() );
		assertTrue( Hibernate.isInitialized( dp ) );
		dp.setDescription( "changed" );
		assertEquals( "changed", dp.getDescription() );
		assertTrue( s.isReadOnly( dp ) );
		assertTrue( s.isReadOnly( ( ( HibernateProxy ) dp ).getHibernateLazyInitializer().getImplementation() ) );
		s.refresh( dp );
		assertEquals( "original", dp.getDescription() );
		dp.setDescription( "changed" );
		assertEquals( "changed", dp.getDescription() );
		assertTrue( s.isReadOnly( dp ) );
		assertTrue( s.isReadOnly( ( ( HibernateProxy ) dp ).getHibernateLazyInitializer().getImplementation() ) );
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
	public void testReadOnlyRefreshDeleted() {
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
		HibernateProxy dpProxy = ( HibernateProxy ) s.load( DataPoint.class, dp.getId() );
		assertFalse( Hibernate.isInitialized( dpProxy ) );
		t.commit();
		s.close();

		s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		t = s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dp.getId() );
		s.delete( dp );
		s.flush();
		try {
			s.refresh( dp );
			fail( "should have thrown UnresolvableObjectException" );
		}
		catch ( UnresolvableObjectException ex ) {
			// expected
		}
		finally {
			t.rollback();
			s.close();
		}

		s = openSession();
		t = s.beginTransaction();
		s.setCacheMode(CacheMode.IGNORE);
		DataPoint dpProxyInit = ( DataPoint ) s.load( DataPoint.class, dp.getId() );
		assertEquals( "original", dp.getDescription() );
		s.delete( dpProxyInit );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		assertTrue( dpProxyInit instanceof HibernateProxy );
		assertTrue( Hibernate.isInitialized( dpProxyInit ) );
		try {
			s.refresh( dpProxyInit );
			fail( "should have thrown UnresolvableObjectException" );
		}
		catch ( UnresolvableObjectException ex ) {
			// expected
		}
		finally {
			t.rollback();
			s.close();
		}

		s = openSession();
		t = s.beginTransaction();
		assertTrue( dpProxy instanceof HibernateProxy );
		try {
			s.refresh( dpProxy );
			assertFalse( Hibernate.isInitialized( dpProxy ) );
			Hibernate.initialize( dpProxy );
			fail( "should have thrown UnresolvableObjectException" );
		}
		catch ( UnresolvableObjectException ex ) {
			// expected
		}
		finally {
			t.rollback();
			s.close();
		}
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
		dp = ( DataPoint ) s.load( DataPoint.class, dp.getId() );
		assertFalse( Hibernate.isInitialized( dp ) );
		assertFalse( s.isReadOnly( dp ) );
		s.setReadOnly( dp, true );
		assertTrue( s.isReadOnly( dp ) );
		s.evict( dp );
		s.refresh( dp );
		assertFalse( Hibernate.isInitialized( dp ) );
		assertFalse( s.isReadOnly( dp ) );
		dp.setDescription( "changed" );
		assertEquals( "changed", dp.getDescription() );
		assertTrue( Hibernate.isInitialized( dp ) );
		s.setReadOnly( dp, true );
		s.evict( dp );
		s.refresh( dp );
		assertEquals( "original", dp.getDescription() );
		assertFalse( s.isReadOnly( dp ) );
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
	public void testReadOnlyProxyMergeDetachedProxyWithChange() {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE );

		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		s.beginTransaction();
		DataPoint dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dp instanceof HibernateProxy );
		checkReadOnly( s, dp, false );
		assertFalse( Hibernate.isInitialized( dp ));
		Hibernate.initialize( dp );
		assertTrue( Hibernate.isInitialized( dp ) );
		s.getTransaction().commit();
		s.close();

		// modify detached proxy
		dp.setDescription( "changed" );

		s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		s.beginTransaction();
		DataPoint dpLoaded = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dpLoaded instanceof HibernateProxy );
		checkReadOnly( s, dpLoaded, false );
		s.setReadOnly( dpLoaded, true );
		checkReadOnly( s, dpLoaded,true );
		assertFalse( Hibernate.isInitialized( dpLoaded ) );
		DataPoint dpMerged = ( DataPoint ) s.merge( dp );
		assertSame( dpLoaded, dpMerged );
		assertTrue( Hibernate.isInitialized( dpLoaded ) );
		assertEquals( "changed", dpLoaded.getDescription() );
		checkReadOnly( s, dpLoaded, true );
		s.flush();
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dpOrig.getId() );
		assertEquals( dpOrig.getId(), dp.getId() );
		assertEquals( dpOrig.getDescription(), dp.getDescription() );
		assertEquals( dpOrig.getX(), dp.getX() );
		assertEquals( dpOrig.getY(), dp.getY() );
		s.delete( dp );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testReadOnlyProxyInitMergeDetachedProxyWithChange() {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE );

		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		s.beginTransaction();
		DataPoint dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dp instanceof HibernateProxy );
		checkReadOnly( s, dp, false );
		assertFalse( Hibernate.isInitialized( dp ));
		Hibernate.initialize( dp );
		assertTrue( Hibernate.isInitialized( dp ) );
		s.getTransaction().commit();
		s.close();

		// modify detached proxy
		dp.setDescription( "changed" );

		s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		s.beginTransaction();
		DataPoint dpLoaded = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dpLoaded instanceof HibernateProxy );
		assertFalse( Hibernate.isInitialized( dpLoaded ) );
		Hibernate.initialize( dpLoaded );
		assertTrue( Hibernate.isInitialized( dpLoaded ) );
		checkReadOnly( s, dpLoaded, false );
		s.setReadOnly( dpLoaded, true );
		checkReadOnly( s, dpLoaded,true );
		DataPoint dpMerged = ( DataPoint ) s.merge( dp );
		assertSame( dpLoaded, dpMerged );
		assertEquals( "changed", dpLoaded.getDescription() );
		checkReadOnly( s, dpLoaded, true );
		s.flush();
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dpOrig.getId() );
		assertEquals( dpOrig.getId(), dp.getId() );
		assertEquals( dpOrig.getDescription(), dp.getDescription() );
		assertEquals( dpOrig.getX(), dp.getX() );
		assertEquals( dpOrig.getY(), dp.getY() );
		s.delete( dp );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testReadOnlyProxyMergeDetachedEntityWithChange() {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE );

		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		s.beginTransaction();
		DataPoint dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dp instanceof HibernateProxy );
		checkReadOnly( s, dp, false );
		assertFalse( Hibernate.isInitialized( dp ));
		Hibernate.initialize( dp );
		assertTrue( Hibernate.isInitialized( dp ) );
		s.getTransaction().commit();
		s.close();

		// modify detached proxy target
		DataPoint dpEntity = ( DataPoint ) ( ( HibernateProxy ) dp ).getHibernateLazyInitializer().getImplementation();
		dpEntity.setDescription( "changed" );

		s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		s.beginTransaction();
		DataPoint dpLoaded = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dpLoaded instanceof HibernateProxy );
		checkReadOnly( s, dpLoaded, false );
		s.setReadOnly( dpLoaded, true );
		checkReadOnly( s, dpLoaded,true );
		assertFalse( Hibernate.isInitialized( dpLoaded ) );
		DataPoint dpMerged = ( DataPoint ) s.merge( dpEntity );
		assertSame( dpLoaded, dpMerged );
		assertTrue( Hibernate.isInitialized( dpLoaded ) );
		assertEquals( "changed", dpLoaded.getDescription() );
		checkReadOnly( s, dpLoaded, true );
		s.flush();
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dpOrig.getId() );
		assertEquals( dpOrig.getId(), dp.getId() );
		assertEquals( dpOrig.getDescription(), dp.getDescription() );
		assertEquals( dpOrig.getX(), dp.getX() );
		assertEquals( dpOrig.getY(), dp.getY() );
		s.delete( dp );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testReadOnlyProxyInitMergeDetachedEntityWithChange() {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE );

		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		s.beginTransaction();
		DataPoint dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dp instanceof HibernateProxy );
		checkReadOnly( s, dp, false );
		assertFalse( Hibernate.isInitialized( dp ));
		Hibernate.initialize( dp );
		assertTrue( Hibernate.isInitialized( dp ) );
		s.getTransaction().commit();
		s.close();

		// modify detached proxy target
		DataPoint dpEntity = ( DataPoint ) ( ( HibernateProxy ) dp ).getHibernateLazyInitializer().getImplementation();
		dpEntity.setDescription( "changed" );

		s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		s.beginTransaction();
		DataPoint dpLoaded = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dpLoaded instanceof HibernateProxy );
		assertFalse( Hibernate.isInitialized( dpLoaded ) );
		Hibernate.initialize( dpLoaded );
		assertTrue( Hibernate.isInitialized( dpLoaded ) );
		checkReadOnly( s, dpLoaded, false );
		s.setReadOnly( dpLoaded, true );
		checkReadOnly( s, dpLoaded,true );
		DataPoint dpMerged = ( DataPoint ) s.merge( dpEntity );
		assertSame( dpLoaded, dpMerged );
		assertEquals( "changed", dpLoaded.getDescription() );
		checkReadOnly( s, dpLoaded, true );
		s.flush();
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dpOrig.getId() );
		assertEquals( dpOrig.getId(), dp.getId() );
		assertEquals( dpOrig.getDescription(), dp.getDescription() );
		assertEquals( dpOrig.getX(), dp.getX() );
		assertEquals( dpOrig.getY(), dp.getY() );
		s.delete( dp );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testReadOnlyEntityMergeDetachedProxyWithChange() {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE );

		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		s.beginTransaction();
		DataPoint dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dp instanceof HibernateProxy );
		checkReadOnly( s, dp, false );
		assertFalse( Hibernate.isInitialized( dp ));
		Hibernate.initialize( dp );
		assertTrue( Hibernate.isInitialized( dp ) );
		s.getTransaction().commit();
		s.close();

		// modify detached proxy
		dp.setDescription( "changed" );

		s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		s.beginTransaction();
		DataPoint dpEntity = ( DataPoint ) s.get( DataPoint.class, new Long( dpOrig.getId() ) );
		assertFalse( dpEntity instanceof HibernateProxy );
		assertFalse( s.isReadOnly( dpEntity ) );
		s.setReadOnly( dpEntity, true );
		assertTrue( s.isReadOnly( dpEntity ) );
		DataPoint dpMerged = ( DataPoint ) s.merge( dp );
		assertSame( dpEntity, dpMerged );
		assertEquals( "changed", dpEntity.getDescription() );
		assertTrue( s.isReadOnly( dpEntity ) );
		s.flush();
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dpOrig.getId() );
		assertEquals( dpOrig.getId(), dp.getId() );
		assertEquals( dpOrig.getDescription(), dp.getDescription() );
		assertEquals( dpOrig.getX(), dp.getX() );
		assertEquals( dpOrig.getY(), dp.getY() );
		s.delete( dp );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testSetReadOnlyInTwoTransactionsSameSession() {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE );

		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);

		s.beginTransaction();
		DataPoint dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dp instanceof HibernateProxy );
		assertFalse( Hibernate.isInitialized( dp ) );
		checkReadOnly( s, dp, false );
		s.setReadOnly( dp, true );
		checkReadOnly( s, dp, true );
		assertFalse( Hibernate.isInitialized( dp ) );
		dp.setDescription( "changed" );
		assertTrue( Hibernate.isInitialized( dp ) );
		assertEquals( "changed", dp.getDescription() );
		s.flush();
		s.getTransaction().commit();

		checkReadOnly( s, dp, true );

		s.beginTransaction();
		checkReadOnly( s, dp, true );
		dp.setDescription( "changed again" );
		assertEquals( "changed again", dp.getDescription() );
		s.flush();
		s.getTransaction().commit();

		s.close();

		s = openSession();
		s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dpOrig.getId() );
		assertEquals( dpOrig.getId(), dp.getId() );
		assertEquals( dpOrig.getDescription(), dp.getDescription() );
		assertEquals( dpOrig.getX(), dp.getX() );
		assertEquals( dpOrig.getY(), dp.getY() );
		s.delete( dp );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testSetReadOnlyBetweenTwoTransactionsSameSession() {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE );

		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);

		s.beginTransaction();
		DataPoint dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dp instanceof HibernateProxy );
		assertFalse( Hibernate.isInitialized( dp ) );
		checkReadOnly( s, dp, false );
		dp.setDescription( "changed" );
		assertTrue( Hibernate.isInitialized( dp ) );
		assertEquals( "changed", dp.getDescription() );
		checkReadOnly( s, dp, false );
		s.flush();
		s.getTransaction().commit();

		checkReadOnly( s, dp, false );
		s.setReadOnly( dp, true );
		checkReadOnly( s, dp, true );

		s.beginTransaction();
		checkReadOnly( s, dp, true );
		dp.setDescription( "changed again" );
		assertEquals( "changed again", dp.getDescription() );
		s.flush();
		s.getTransaction().commit();

		s.close();

		s = openSession();
		s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dpOrig.getId() );
		assertEquals( dpOrig.getId(), dp.getId() );
		assertEquals( "changed", dp.getDescription() );
		assertEquals( dpOrig.getX(), dp.getX() );
		assertEquals( dpOrig.getY(), dp.getY() );
		s.delete( dp );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testSetModifiableBetweenTwoTransactionsSameSession() {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE );

		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);

		s.beginTransaction();
		DataPoint dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dp instanceof HibernateProxy );
		assertFalse( Hibernate.isInitialized( dp ) );
		checkReadOnly( s, dp, false );
		s.setReadOnly( dp, true );
		checkReadOnly( s, dp, true );
		dp.setDescription( "changed" );
		assertTrue( Hibernate.isInitialized( dp ) );
		assertEquals( "changed", dp.getDescription() );
		checkReadOnly( s, dp, true );
		s.flush();
		s.getTransaction().commit();

		checkReadOnly( s, dp, true );
		s.setReadOnly( dp, false );
		checkReadOnly( s, dp, false );

		s.beginTransaction();
		checkReadOnly( s, dp, false );
		assertEquals( "changed", dp.getDescription() );
		s.refresh( dp );
		assertEquals( dpOrig.getDescription(), dp.getDescription() );
		checkReadOnly( s, dp, false );
		dp.setDescription( "changed again" );
		assertEquals( "changed again", dp.getDescription() );
		s.flush();
		s.getTransaction().commit();

		s.close();

		s = openSession();
		s.beginTransaction();
		dp = ( DataPoint ) s.get( DataPoint.class, dpOrig.getId() );
		assertEquals( dpOrig.getId(), dp.getId() );
		assertEquals( "changed again", dp.getDescription() );
		assertEquals( dpOrig.getX(), dp.getX() );
		assertEquals( dpOrig.getY(), dp.getY() );
		s.delete( dp );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testIsReadOnlyAfterSessionClosed() {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE );

		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);

		s.beginTransaction();
		DataPoint dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dp instanceof HibernateProxy );
		assertFalse( Hibernate.isInitialized( dp ) );
		checkReadOnly( s, dp, false );
		s.getTransaction().commit();
		s.close();

		try {
 			s.isReadOnly( dp );
			fail( "should have failed because session was closed" );
		}
		catch ( SessionException ex) {
			// expected
			assertFalse( ( ( HibernateProxy ) dp ).getHibernateLazyInitializer().isReadOnlySettingAvailable() );
		}
		finally {
			s = openSession();
			s.beginTransaction();
			s.delete( dp );
			s.getTransaction().commit();
			s.close();
		}
	}

	@Test
	public void testIsReadOnlyAfterSessionClosedViaLazyInitializer() {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE );

		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);

		s.beginTransaction();
		DataPoint dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dp instanceof HibernateProxy );
		assertFalse( Hibernate.isInitialized( dp ) );
		checkReadOnly( s, dp, false );
		s.getTransaction().commit();
		assertTrue( s.contains( dp ) );
		s.close();

		assertNull( ( ( HibernateProxy ) dp ).getHibernateLazyInitializer().getSession() );
		try {
 			( ( HibernateProxy ) dp ).getHibernateLazyInitializer().isReadOnly();
			fail( "should have failed because session was detached" );
		}
		catch ( TransientObjectException ex) {
			// expected
			assertFalse( ( ( HibernateProxy ) dp ).getHibernateLazyInitializer().isReadOnlySettingAvailable() );
		}
		finally {
			s = openSession();
			s.beginTransaction();
			s.delete( dp );
			s.getTransaction().commit();
			s.close();
		}
	}

	@Test
	public void testDetachedIsReadOnlyAfterEvictViaSession() {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE );

		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);

		s.beginTransaction();
		DataPoint dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dp instanceof HibernateProxy );
		assertFalse( Hibernate.isInitialized( dp ) );
		checkReadOnly( s, dp, false );
		assertTrue( s.contains( dp ) );
		s.evict( dp );
		assertFalse( s.contains( dp ) );
		assertNull( ( ( HibernateProxy ) dp ).getHibernateLazyInitializer().getSession() );

		try {
 			s.isReadOnly( dp );
			fail( "should have failed because proxy was detached" );
		}
		catch ( TransientObjectException ex) {
			// expected
			assertFalse( ( ( HibernateProxy ) dp ).getHibernateLazyInitializer().isReadOnlySettingAvailable() );
		}
		finally {
			s.delete( dp );
			s.getTransaction().commit();
			s.close();
		}
	}

	@Test
	public void testDetachedIsReadOnlyAfterEvictViaLazyInitializer() {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE );

		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);

		s.beginTransaction();
		DataPoint dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dp instanceof HibernateProxy );
		assertFalse( Hibernate.isInitialized( dp ) );
		checkReadOnly( s, dp, false );
		s.evict( dp );
		assertFalse( s.contains( dp ) );
		assertNull( ( ( HibernateProxy ) dp ).getHibernateLazyInitializer().getSession() );
		try {
 			( ( HibernateProxy ) dp ).getHibernateLazyInitializer().isReadOnly();
			fail( "should have failed because proxy was detached" );
		}
		catch ( TransientObjectException ex) {
			// expected
			assertFalse( ( ( HibernateProxy ) dp ).getHibernateLazyInitializer().isReadOnlySettingAvailable() );
		}
		finally {
			s.delete( dp );
			s.getTransaction().commit();
			s.close();
		}
	}

	@Test
	public void testSetReadOnlyAfterSessionClosed() {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE );

		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);

		s.beginTransaction();
		DataPoint dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dp instanceof HibernateProxy );
		assertFalse( Hibernate.isInitialized( dp ) );
		checkReadOnly( s, dp, false );
		s.getTransaction().commit();
		s.close();

		try {
 			s.setReadOnly( dp, true );
			fail( "should have failed because session was closed" );
		}
		catch ( SessionException ex) {
			// expected
			assertFalse( ( ( HibernateProxy ) dp ).getHibernateLazyInitializer().isReadOnlySettingAvailable() );
		}
		finally {
			s = openSession();
			s.beginTransaction();
			s.delete( dp );
			s.getTransaction().commit();
			s.close();
		}
	}

	@Test
	public void testSetReadOnlyAfterSessionClosedViaLazyInitializer() {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE );

		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);

		s.beginTransaction();
		DataPoint dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dp instanceof HibernateProxy );
		assertFalse( Hibernate.isInitialized( dp ) );
		checkReadOnly( s, dp, false );
		s.getTransaction().commit();
		assertTrue( s.contains( dp ) );
		s.close();

		assertNull( ( ( HibernateProxy ) dp ).getHibernateLazyInitializer().getSession() );
		try {
 			( ( HibernateProxy ) dp ).getHibernateLazyInitializer().setReadOnly( true );
			fail( "should have failed because session was detached" );
		}
		catch ( TransientObjectException ex) {
			// expected
			assertFalse( ( ( HibernateProxy ) dp ).getHibernateLazyInitializer().isReadOnlySettingAvailable() );
		}
		finally {
			s = openSession();
			s.beginTransaction();
			s.delete( dp );
			s.getTransaction().commit();
			s.close();
		}
	}

	@Test
	public void testSetClosedSessionInLazyInitializer() {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE );

		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);

		s.beginTransaction();
		DataPoint dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dp instanceof HibernateProxy );
		assertFalse( Hibernate.isInitialized( dp ) );
		checkReadOnly( s, dp, false );
		s.getTransaction().commit();
		assertTrue( s.contains( dp ) );
		s.close();

		assertNull( ( ( HibernateProxy ) dp ).getHibernateLazyInitializer().getSession() );
		assertTrue( ( (SessionImplementor) s ).isClosed() );
		try {
			( ( HibernateProxy ) dp ).getHibernateLazyInitializer().setSession( ( SessionImplementor ) s );			
			fail( "should have failed because session was closed" );
		}
		catch ( SessionException ex) {
			// expected
			assertFalse( ( ( HibernateProxy ) dp ).getHibernateLazyInitializer().isReadOnlySettingAvailable() );
		}
		finally {
			s = openSession();
			s.beginTransaction();
			s.delete( dp );
			s.getTransaction().commit();
			s.close();
		}
	}

	@Test
	public void testDetachedSetReadOnlyAfterEvictViaSession() {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE );

		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);

		s.beginTransaction();
		DataPoint dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dp instanceof HibernateProxy );
		assertFalse( Hibernate.isInitialized( dp ) );
		checkReadOnly( s, dp, false );
		assertTrue( s.contains( dp ) );
		s.evict( dp );
		assertFalse( s.contains( dp ) );
		assertNull( ( ( HibernateProxy ) dp ).getHibernateLazyInitializer().getSession() );

		try {
 			s.setReadOnly( dp, true );
			fail( "should have failed because proxy was detached" );
		}
		catch ( TransientObjectException ex) {
			// expected
			assertFalse( ( ( HibernateProxy ) dp ).getHibernateLazyInitializer().isReadOnlySettingAvailable() );
		}
		finally {
			s.delete( dp );
			s.getTransaction().commit();
			s.close();
		}
	}

	@Test
	public void testDetachedSetReadOnlyAfterEvictViaLazyInitializer() {
		DataPoint dpOrig = createDataPoint( CacheMode.IGNORE );

		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);

		s.beginTransaction();
		DataPoint dp = ( DataPoint ) s.load( DataPoint.class, new Long( dpOrig.getId() ) );
		assertTrue( dp instanceof HibernateProxy );
		assertFalse( Hibernate.isInitialized( dp ) );
		checkReadOnly( s, dp, false );
		s.evict( dp );
		assertFalse( s.contains( dp ) );
		assertNull( ( ( HibernateProxy ) dp ).getHibernateLazyInitializer().getSession() );
		try {
 			( ( HibernateProxy ) dp ).getHibernateLazyInitializer().setReadOnly( true );
			fail( "should have failed because proxy was detached" );
		}
		catch ( TransientObjectException ex) {
			// expected
			assertFalse( ( ( HibernateProxy ) dp ).getHibernateLazyInitializer().isReadOnlySettingAvailable() );
		}
		finally {
			s.delete( dp );
			s.getTransaction().commit();
			s.close();
		}
	}

	private DataPoint createDataPoint(CacheMode cacheMode) {
		Session s = openSession();
		s.setCacheMode( cacheMode );
		s.beginTransaction();
		DataPoint dp = new DataPoint();
		dp.setX( new BigDecimal( 0.1d ).setScale(19, BigDecimal.ROUND_DOWN) );
		dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale(19, BigDecimal.ROUND_DOWN) );
		dp.setDescription( "original" );
		s.save( dp );
		s.getTransaction().commit();
		s.close();
		return dp;
	}

	private void checkReadOnly(Session s, Object proxy, boolean expectedReadOnly) {
		assertTrue( proxy instanceof HibernateProxy );
		LazyInitializer li = ( ( HibernateProxy ) proxy ).getHibernateLazyInitializer();
		assertSame( s, li.getSession() );
		assertEquals( expectedReadOnly, s.isReadOnly( proxy ) );
		assertEquals( expectedReadOnly, li.isReadOnly() );
		assertEquals( Hibernate.isInitialized( proxy ), ! li.isUninitialized() );
		if ( Hibernate.isInitialized( proxy ) ) {
			assertEquals( expectedReadOnly, s.isReadOnly( li.getImplementation() ) );
		}
	}
}
