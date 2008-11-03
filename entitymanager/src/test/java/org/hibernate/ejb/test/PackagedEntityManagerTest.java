package org.hibernate.ejb.test;

import java.util.HashMap;
import java.util.HashSet;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;

import org.hibernate.ejb.HibernateEntityManagerFactory;
import org.hibernate.ejb.HibernatePersistence;
import org.hibernate.ejb.test.pack.cfgxmlpar.Morito;
import org.hibernate.ejb.test.pack.defaultpar.ApplicationServer;
import org.hibernate.ejb.test.pack.defaultpar.IncrementListener;
import org.hibernate.ejb.test.pack.defaultpar.Lighter;
import org.hibernate.ejb.test.pack.defaultpar.Money;
import org.hibernate.ejb.test.pack.defaultpar.Mouse;
import org.hibernate.ejb.test.pack.defaultpar.OtherIncrementListener;
import org.hibernate.ejb.test.pack.defaultpar.Version;
import org.hibernate.ejb.test.pack.excludehbmpar.Caipirinha;
import org.hibernate.ejb.test.pack.explodedpar.Carpet;
import org.hibernate.ejb.test.pack.explodedpar.Elephant;
import org.hibernate.ejb.test.pack.externaljar.Scooter;
import org.hibernate.ejb.test.pack.spacepar.Bug;
import org.hibernate.ejb.test.pack.various.Airplane;
import org.hibernate.ejb.test.pack.various.Seat;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.event.EventListeners;
import org.hibernate.stat.Statistics;

//$Id$

/**
 * @author Gavin King
 */
@SuppressWarnings("unchecked")
public class PackagedEntityManagerTest extends TestCase {

	public Class[] getAnnotatedClasses() {
		return new Class[] {
		};
	}

	public void setUp() {
		factory = Persistence.createEntityManagerFactory( "manager1" );
	}

	public void testDefaultPar() throws Exception {
		EntityManagerFactory emf = Persistence.createEntityManagerFactory( "defaultpar", new HashMap() );
		EntityManager em = emf.createEntityManager();
		ApplicationServer as = new ApplicationServer();
		as.setName( "JBoss AS" );
		Version v = new Version();
		v.setMajor( 4 );
		v.setMinor( 0 );
		v.setMicro( 3 );
		as.setVersion( v );
		Mouse mouse = new Mouse();
		mouse.setName( "mickey" );
		em.getTransaction().begin();
		em.persist( as );
		em.persist( mouse );
		assertEquals( 1, em.createNamedQuery( "allMouse" ).getResultList().size() );
		Lighter lighter = new Lighter();
		lighter.name = "main";
		lighter.power = " 250 W";
		em.persist( lighter );
		em.flush();
		em.remove( lighter );
		em.remove( mouse );
		assertNotNull( as.getId() );
		em.remove( as );
		em.getTransaction().commit();
		em.close();
		emf.close();
	}

	public void testListenersDefaultPar() throws Exception {
		IncrementListener.reset();
		OtherIncrementListener.reset();
		EntityManagerFactory emf = Persistence.createEntityManagerFactory( "defaultpar", new HashMap() );
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		ApplicationServer as = new ApplicationServer();
		as.setName( "JBoss AS" );
		Version v = new Version();
		v.setMajor( 4 );
		v.setMinor( 0 );
		v.setMicro( 3 );
		as.setVersion( v );
		em.persist( as );
		em.flush();
		assertEquals( "Failure in default listeners", 1, IncrementListener.getIncrement() );
		assertEquals( "Failuer in XML overriden listeners", 1, OtherIncrementListener.getIncrement() );

		Mouse mouse = new Mouse();
		mouse.setName( "mickey" );
		em.persist( mouse );
		em.flush();
		assertEquals( "Failure in @ExcludeDefaultListeners", 1, IncrementListener.getIncrement() );
		assertEquals( 1, OtherIncrementListener.getIncrement() );

		Money money = new Money();
		em.persist( money );
		em.flush();
		assertEquals( "Failure in @ExcludeDefaultListeners", 2, IncrementListener.getIncrement() );
		assertEquals( 1, OtherIncrementListener.getIncrement() );

		em.getTransaction().rollback();
		em.close();
		emf.close();
	}

	public void testExplodedPar() throws Exception {
		EntityManagerFactory emf = Persistence.createEntityManagerFactory( "explodedpar", new HashMap() );
		EntityManager em = emf.createEntityManager();
		org.hibernate.ejb.test.pack.explodedpar.Carpet carpet = new Carpet();
		Elephant el = new Elephant();
		el.setName( "Dumbo" );
		carpet.setCountry( "Turkey" );
		em.getTransaction().begin();
		em.persist( carpet );
		em.persist( el );
		assertEquals( 1, em.createNamedQuery( "allCarpet" ).getResultList().size() );
		assertNotNull( carpet.getId() );
		em.remove( carpet );
		em.getTransaction().commit();
		em.close();
		emf.close();
	}


	public void testExcludeHbmPar() throws Exception {
		EntityManagerFactory emf = null;
		try {
			emf = Persistence.createEntityManagerFactory( "excludehbmpar", new HashMap() );
		}
		catch (PersistenceException e) {
			Throwable nested = e.getCause();
			if ( nested == null ) throw e;
			nested = nested.getCause();
			if ( nested == null ) throw e;
			if ( !( nested instanceof ClassNotFoundException ) ) throw e;
			fail( "Try to process hbm file: " + e.getMessage() );
		}
		EntityManager em = emf.createEntityManager();
		Caipirinha s = new Caipirinha( "Strong" );
		em.getTransaction().begin();
		em.persist( s );
		em.getTransaction().commit();

		em.getTransaction().begin();
		s = em.find( Caipirinha.class, s.getId() );
		em.remove( s );
		em.getTransaction().commit();
		em.close();
		emf.close();
	}

	public void testCfgXmlPar() throws Exception {
		EntityManagerFactory emf = Persistence.createEntityManagerFactory( "cfgxmlpar", new HashMap() );
		EntityManager em = emf.createEntityManager();
		Item i = new Item();
		i.setDescr( "Blah" );
		i.setName( "factory" );
		Morito m = new Morito();
		m.setPower( "SuperStrong" );
		em.getTransaction().begin();
		em.persist( i );
		em.persist( m );
		em.getTransaction().commit();

		em.getTransaction().begin();
		i = em.find( Item.class, i.getName() );
		em.remove( i );
		em.remove( em.find( Morito.class, m.getId() ) );
		em.getTransaction().commit();
		em.close();
		emf.close();
	}

	public void testSpacePar() throws Exception {
		EntityManagerFactory emf = Persistence.createEntityManagerFactory( "space par", new HashMap() );
		EntityManager em = emf.createEntityManager();
		Bug bug = new Bug();
		bug.setSubject( "Spaces in directory name don't play well on Windows" );
		em.getTransaction().begin();
		em.persist( bug );
		em.flush();
		em.remove( bug );
		assertNotNull( bug.getId() );
		em.getTransaction().rollback();
		em.close();
		emf.close();
	}

	public void testOverridenPar() throws Exception {
		HashMap properties = new HashMap();
		properties.put( HibernatePersistence.JTA_DATASOURCE, null );
		properties.put( "hibernate.connection.driver_class", "org.hsqldb.jdbcDriver" );
		properties.put( "hibernate.connection.username", "sa" );
		properties.put( "hibernate.connection.password", "" );
		properties.put( "hibernate.connection.url", "jdbc:hsqldb:." );
		EntityManagerFactory emf = Persistence.createEntityManagerFactory( "overridenpar", properties );
		EntityManager em = emf.createEntityManager();
		org.hibernate.ejb.test.pack.overridenpar.Bug bug = new org.hibernate.ejb.test.pack.overridenpar.Bug();
		bug.setSubject( "Allow DS overriding" );
		em.getTransaction().begin();
		em.persist( bug );
		em.flush();
		em.remove( bug );
		assertNotNull( bug.getId() );
		em.getTransaction().rollback();
		em.close();
		emf.close();
	}


	public void testListeners() throws Exception {
		EntityManagerFactory emf = Persistence.createEntityManagerFactory( "manager1", new HashMap() );
		EntityManager em = emf.createEntityManager();
		EventListeners eventListeners = ( (SessionImplementor) em.getDelegate() ).getListeners();
		assertEquals(
				"Explicit pre-insert event through hibernate.ejb.event.pre-insert does not work",
				eventListeners.getPreInsertEventListeners().length,
				eventListeners.getPreUpdateEventListeners().length + 1 );

		em.close();
		emf.close();
	}

	public void testExtendedEntityManager() {

		Item item = new Item( "Mouse", "Micro$oft mouse" );

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( item );
		assertTrue( em.contains( item ) );
		em.getTransaction().commit();

		assertTrue( em.contains( item ) );

		em.getTransaction().begin();
		Item item1 = (Item) em.createQuery( "select i from Item i where descr like 'M%'" ).getSingleResult();
		assertNotNull( item1 );
		assertSame( item, item1 );
		item.setDescr( "Micro$oft wireless mouse" );
		assertTrue( em.contains( item ) );
		em.getTransaction().commit();

		assertTrue( em.contains( item ) );

		em.getTransaction().begin();
		item1 = em.find( Item.class, "Mouse" );
		assertSame( item, item1 );
		em.getTransaction().commit();
		assertTrue( em.contains( item ) );

		item1 = em.find( Item.class, "Mouse" );
		assertSame( item, item1 );
		assertTrue( em.contains( item ) );

		item1 = (Item) em.createQuery( "select i from Item i where descr like 'M%'" ).getSingleResult();
		assertNotNull( item1 );
		assertSame( item, item1 );
		assertTrue( em.contains( item ) );

		em.getTransaction().begin();
		assertTrue( em.contains( item ) );
		em.remove( item );
		em.remove( item ); //second remove should be a no-op
		em.getTransaction().commit();

		em.close();

	}

	public void testConfiguration() throws Exception {
		Item item = new Item( "Mouse", "Micro$oft mouse" );
		Distributor res = new Distributor();
		res.setName( "Bruce" );
		item.setDistributors( new HashSet<Distributor>() );
		item.getDistributors().add( res );
		Statistics stats = ( (HibernateEntityManagerFactory) factory ).getSessionFactory().getStatistics();
		stats.clear();
		stats.setStatisticsEnabled( true );

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		em.persist( res );
		em.persist( item );
		assertTrue( em.contains( item ) );

		em.getTransaction().commit();
		em.close();

		assertEquals( 1, stats.getSecondLevelCachePutCount() );
		assertEquals( 0, stats.getSecondLevelCacheHitCount() );

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Item second = em.find( Item.class, item.getName() );
		assertEquals( 1, second.getDistributors().size() );
		assertEquals( 1, stats.getSecondLevelCacheHitCount() );
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		second = em.find( Item.class, item.getName() );
		assertEquals( 1, second.getDistributors().size() );
		assertEquals( 3, stats.getSecondLevelCacheHitCount() );
		for (Distributor distro : second.getDistributors()) {
			em.remove( distro );
		}
		em.remove( second );
		em.getTransaction().commit();
		em.close();

		stats.clear();
		stats.setStatisticsEnabled( false );
	}

	public void testExternalJar() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		Scooter s = new Scooter();
		s.setModel( "Abadah" );
		s.setSpeed( 85l );
		em.getTransaction().begin();
		em.persist( s );
		em.getTransaction().commit();
		em.close();
		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		s = em.find( Scooter.class, s.getModel() );
		assertEquals( new Long( 85 ), s.getSpeed() );
		em.remove( s );
		em.getTransaction().commit();
		em.close();
	}

	public void testORMFileOnMainAndExplicitJars() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		Seat seat = new Seat();
		seat.setNumber( "3B" );
		Airplane plane = new Airplane();
		plane.setSerialNumber( "75924418409052355" );
		em.getTransaction().begin();
		em.persist( seat );
		em.persist( plane );
		em.flush();
		em.getTransaction().rollback();
		em.close();
	}
}