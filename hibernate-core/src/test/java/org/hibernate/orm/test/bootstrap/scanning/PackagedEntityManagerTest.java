/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.scanning;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.GaussDBDialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.util.ConfigHelper;
import org.hibernate.orm.test.jpa.Distributor;
import org.hibernate.orm.test.jpa.Item;
import org.hibernate.orm.test.jpa.pack.cfgxmlpar.Morito;
import org.hibernate.orm.test.jpa.pack.defaultpar.ApplicationServer;
import org.hibernate.orm.test.jpa.pack.defaultpar.IncrementListener;
import org.hibernate.orm.test.jpa.pack.defaultpar.Lighter;
import org.hibernate.orm.test.jpa.pack.defaultpar.Money;
import org.hibernate.orm.test.jpa.pack.defaultpar.Mouse;
import org.hibernate.orm.test.jpa.pack.defaultpar.OtherIncrementListener;
import org.hibernate.orm.test.jpa.pack.defaultpar.Version;
import org.hibernate.orm.test.jpa.pack.defaultpar_1_0.ApplicationServer1;
import org.hibernate.orm.test.jpa.pack.defaultpar_1_0.Lighter1;
import org.hibernate.orm.test.jpa.pack.defaultpar_1_0.Mouse1;
import org.hibernate.orm.test.jpa.pack.defaultpar_1_0.Version1;
import org.hibernate.orm.test.jpa.pack.excludehbmpar.Caipirinha;
import org.hibernate.orm.test.jpa.pack.explodedpar.Carpet;
import org.hibernate.orm.test.jpa.pack.explodedpar.Elephant;
import org.hibernate.orm.test.jpa.pack.externaljar.Scooter;
import org.hibernate.orm.test.jpa.pack.various.Airplane;
import org.hibernate.orm.test.jpa.pack.various.Seat;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.transaction.TransactionUtil;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * In this test we verify that  it is possible to bootstrap Hibernate/JPA from
 * various bundles (war, par, ...) using {@code Persistence.createEntityManagerFactory()}
 * <p>
 * Each test will before its run build the required bundle and place them into the classpath.
 *
 * @author Gavin King
 * @author Hardy Ferentschik
 */
@SkipForDialect(dialectClass = GaussDBDialect.class, reason = "opengauss don't support")
public class PackagedEntityManagerTest extends PackagingTestCase {
	private EntityManagerFactory emf;

	@AfterEach
	public void tearDown() {
		if ( emf != null && emf.isOpen() ) {
			emf.close();
		}
	}

	@Test
	public void testDefaultPar() throws Exception {
		File testPackage = buildDefaultPar();
		addPackageToClasspath( testPackage );

		// run the test
		emf = Persistence.createEntityManagerFactory( "defaultpar", ServiceRegistryUtil.createBaseSettings() );
		TransactionUtil.doInJPA(
				() -> emf, em -> {
					ApplicationServer as = new ApplicationServer();
					as.setName( "JBoss AS" );
					Version v = new Version();
					v.setMajor( 4 );
					v.setMinor( 0 );
					v.setMicro( 3 );
					as.setVersion( v );
					Mouse mouse = new Mouse();
					mouse.setName( "mickey" );
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
				}
		);
	}

	@Test
	public void testDefaultParForPersistence_1_0() throws Exception {
		File testPackage = buildDefaultPar_1_0();
		addPackageToClasspath( testPackage );

		emf = Persistence.createEntityManagerFactory( "defaultpar_1_0", ServiceRegistryUtil.createBaseSettings() );
		TransactionUtil.doInJPA(
				() -> emf, em -> {
					ApplicationServer1 as = new ApplicationServer1();
					as.setName( "JBoss AS" );
					Version1 v = new Version1();
					v.setMajor( 4 );
					v.setMinor( 0 );
					v.setMicro( 3 );
					as.setVersion( v );
					Mouse1 mouse = new Mouse1();
					mouse.setName( "mickey" );
					em.persist( as );
					em.persist( mouse );
					assertEquals( 1, em.createNamedQuery( "allMouse_1_0" ).getResultList().size() );
					Lighter1 lighter = new Lighter1();
					lighter.name = "main";
					lighter.power = " 250 W";
					em.persist( lighter );
					em.flush();
					em.remove( lighter );
					em.remove( mouse );
					assertNotNull( as.getId() );
					em.remove( as );
				}
		);
	}

	@Test
	public void testListenersDefaultPar() throws Exception {
		File testPackage = buildDefaultPar();
		addPackageToClasspath( testPackage );

		IncrementListener.reset();
		OtherIncrementListener.reset();
		emf = Persistence.createEntityManagerFactory( "defaultpar", ServiceRegistryUtil.createBaseSettings() );
		TransactionUtil.doInJPA(
				() -> emf, em -> {
					ApplicationServer as = new ApplicationServer();
					as.setName( "JBoss AS" );
					Version v = new Version();
					v.setMajor( 4 );
					v.setMinor( 0 );
					v.setMicro( 3 );
					as.setVersion( v );
					em.persist( as );
					em.flush();
					assertEquals( 1, IncrementListener.getIncrement(), "Failure in default listeners" );
					assertEquals( 1, OtherIncrementListener.getIncrement(), "Failure in XML overridden listeners" );

					Mouse mouse = new Mouse();
					mouse.setName( "mickey" );
					em.persist( mouse );
					em.flush();
					assertEquals( 1, IncrementListener.getIncrement(), "Failure in @ExcludeDefaultListeners" );
					assertEquals( 1, OtherIncrementListener.getIncrement() );

					Money money = new Money();
					em.persist( money );
					em.flush();
					assertEquals( 2, IncrementListener.getIncrement(), "Failure in @ExcludeDefaultListeners" );
					assertEquals( 1, OtherIncrementListener.getIncrement() );
					em.getTransaction().setRollbackOnly();
				}
		);
	}

	@Test
	public void testExplodedPar() throws Exception {
		File testPackage = buildExplodedPar();
		addPackageToClasspath( testPackage );

		emf = Persistence.createEntityManagerFactory( "explodedpar", ServiceRegistryUtil.createBaseSettings() );
		TransactionUtil.doInJPA(
				() -> emf, em -> {
					Carpet carpet = new Carpet();
					Elephant el = new Elephant();
					el.setName( "Dumbo" );
					carpet.setCountry( "Turkey" );
					em.persist( carpet );
					em.persist( el );
					assertEquals( 1, em.createNamedQuery( "allCarpet" ).getResultList().size() );
					assertNotNull( carpet.getId() );
					em.remove( carpet );
				}
		);
	}

	@Test
	public void testExcludeHbmPar() throws Exception {
		File testPackage = buildExcludeHbmPar();
		addPackageToClasspath( testPackage );

		try {
			emf = Persistence.createEntityManagerFactory( "excludehbmpar", ServiceRegistryUtil.createBaseSettings() );
		}
		catch (PersistenceException e) {
			if ( emf != null ) {
				emf.close();
			}
			Throwable nested = e.getCause();
			if ( nested == null ) {
				throw e;
			}
			nested = nested.getCause();
			if ( nested == null ) {
				throw e;
			}
			if ( !( nested instanceof ClassNotFoundException ) ) {
				throw e;
			}
			fail( "Try to process hbm file: " + e.getMessage() );

		}
		TransactionUtil.doInJPA(
				() -> emf, em -> {
					Caipirinha s = new Caipirinha( "Strong" );
					em.persist( s );
					em.getTransaction().commit();

					em.getTransaction().begin();
					s = em.find( Caipirinha.class, s.getId() );
					em.remove( s );
				}
		);
	}

	@Test
	public void testCfgXmlPar() throws Exception {
		File testPackage = buildCfgXmlPar();
		addPackageToClasspath( testPackage );

		emf = Persistence.createEntityManagerFactory( "cfgxmlpar", ServiceRegistryUtil.createBaseSettings() );

		assertTrue( emf.getProperties().containsKey( "hibernate.test-assertable-setting" ) );

		TransactionUtil.doInJPA(
				() -> emf, em -> {
					Item i = new Item();
					i.setDescr( "Blah" );
					i.setName( "factory" );
					Morito m = new Morito();
					m.setPower( "SuperStrong" );
					em.persist( i );
					em.persist( m );
					em.getTransaction().commit();

					em.getTransaction().begin();
					i = em.find( Item.class, i.getName() );
					em.remove( i );
					em.remove( em.find( Morito.class, m.getId() ) );
				}
		);
	}

	@Test
	public void testSpacePar() throws Exception {
		File testPackage = buildSpacePar();
		addPackageToClasspath( testPackage );

		emf = Persistence.createEntityManagerFactory( "space par", ServiceRegistryUtil.createBaseSettings() );
		TransactionUtil.doInJPA(
				() -> emf, em -> {
					org.hibernate.orm.test.jpa.pack.spacepar.Bug bug = new org.hibernate.orm.test.jpa.pack.spacepar.Bug();
					bug.setSubject( "Spaces in directory name don't play well on Windows" );
					em.persist( bug );
					em.flush();
					em.remove( bug );
					assertNotNull( bug.getId() );
					em.getTransaction().setRollbackOnly();
				}
		);
	}

	@Test
	public void testOverriddenPar() throws Exception {
		File testPackage = buildOverridenPar();
		addPackageToClasspath( testPackage );

		Map<String, Object> properties = ServiceRegistryUtil.createBaseSettings();
		properties.put( AvailableSettings.JPA_JTA_DATASOURCE, null );
		Properties p = new Properties();
		p.load( ConfigHelper.getResourceAsStream( "/overridenpar.properties" ) );
		//noinspection rawtypes
		properties.putAll( (Map) p );
		emf = Persistence.createEntityManagerFactory( "overridenpar", properties );
		TransactionUtil.doInJPA(
				() -> emf, em -> {
					org.hibernate.orm.test.jpa.pack.overridenpar.Bug bug = new org.hibernate.orm.test.jpa.pack.overridenpar.Bug();
					bug.setSubject( "Allow DS overriding" );
					em.persist( bug );
					em.flush();
					em.remove( bug );
					assertNotNull( bug.getId() );
					em.getTransaction().setRollbackOnly();
				}
		);
	}

	@Test
	public void testListeners() throws Exception {
		File testPackage = buildExplicitPar();
		addPackageToClasspath( testPackage );

		emf = Persistence.createEntityManagerFactory( "manager1", ServiceRegistryUtil.createBaseSettings() );
		EntityManager em = emf.createEntityManager();
		try {
			EventListenerRegistry listenerRegistry =
					em.unwrap( SharedSessionContractImplementor.class ).getFactory().getEventListenerRegistry();
			assertEquals(
					listenerRegistry.getEventListenerGroup( EventType.PRE_INSERT ).count(),
					listenerRegistry.getEventListenerGroup( EventType.PRE_UPDATE ).count() + 1,
					"Explicit pre-insert event through hibernate.ejb.event.pre-insert does not work"
			);
		}
		finally {
			em.close();
		}
	}

	@Test
	public void testExtendedEntityManager() throws Exception {
		File testPackage = buildExplicitPar();
		addPackageToClasspath( testPackage );

		emf = Persistence.createEntityManagerFactory( "manager1", ServiceRegistryUtil.createBaseSettings() );
		TransactionUtil.doInJPA(
				() -> emf, em -> {
					Item item = new Item( "Mouse", "Micro$oft mouse" );
					em.persist( item );
					assertTrue( em.contains( item ) );
					em.getTransaction().commit();

					assertTrue( em.contains( item ) );

					em.getTransaction().begin();
					Item item1 = (Item) em.createQuery( "select i from Item i where descr like 'M%'" )
							.getSingleResult();
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
				}
		);
	}

	@Test
	public void testConfiguration() throws Exception {
		File testPackage = buildExplicitPar();
		addPackageToClasspath( testPackage );

		emf = Persistence.createEntityManagerFactory( "manager1", ServiceRegistryUtil.createBaseSettings() );
		Item item = new Item( "Mouse", "Micro$oft mouse" );
		Distributor res = new Distributor();
		res.setName( "Bruce" );
		item.setDistributors( new HashSet<Distributor>() );
		item.getDistributors().add( res );
		Statistics stats = emf.unwrap( SessionFactory.class ).getStatistics();
		stats.clear();
		stats.setStatisticsEnabled( true );

		TransactionUtil.doInJPA(
				() -> emf, em -> {
					em.persist( res );
					em.persist( item );
					assertTrue( em.contains( item ) );
				}
		);

		assertEquals( 1, stats.getSecondLevelCachePutCount() );
		assertEquals( 0, stats.getSecondLevelCacheHitCount() );

		TransactionUtil.doInJPA(
				() -> emf, em -> {
					Item second = em.find( Item.class, item.getName() );
					assertEquals( 1, second.getDistributors().size() );
					assertEquals( 1, stats.getSecondLevelCacheHitCount() );
				}
		);

		TransactionUtil.doInJPA(
				() -> emf, em -> {
					Item second = em.find( Item.class, item.getName() );
					assertEquals( 1, second.getDistributors().size() );
					assertEquals( 3, stats.getSecondLevelCacheHitCount() );
					for ( Distributor distro : second.getDistributors() ) {
						em.remove( distro );
					}
					em.remove( second );
				}
		);

		stats.clear();
		stats.setStatisticsEnabled( false );
	}

	@Test
	public void testExternalJar() throws Exception {
		File externalJar = buildExternalJar();
		File testPackage = buildExplicitPar();
		addPackageToClasspath( testPackage, externalJar );

		emf = Persistence.createEntityManagerFactory( "manager1", ServiceRegistryUtil.createBaseSettings() );
		Scooter scooter = TransactionUtil.doInJPA(
				() -> emf, em -> {
					Scooter s = new Scooter();
					s.setModel( "Abadah" );
					s.setSpeed( 85l );
					em.persist( s );
					return s;
				}
		);
		TransactionUtil.doInJPA(
				() -> emf, em -> {
					Scooter s = em.find( Scooter.class, scooter.getModel() );
					assertEquals( Long.valueOf( 85 ), s.getSpeed() );
					em.remove( s );
				}
		);
	}

	@Test
	public void testRelativeJarReferences() throws Exception {
		File externalJar = buildExternalJar2();
		File testPackage = buildExplicitPar2();
		addPackageToClasspath( testPackage, externalJar );

		// if the jar cannot be resolved, this call should fail
		emf = Persistence.createEntityManagerFactory( "manager1", ServiceRegistryUtil.createBaseSettings() );

		// but to make sure, also verify that the entity defined in the external jar was found
		emf.getMetamodel().entity( Airplane.class );
		emf.getMetamodel().entity( Scooter.class );

		// additionally, try to use them
		Scooter scooter = TransactionUtil.doInJPA(
				() -> emf, em -> {
					Scooter s = new Scooter();
					s.setModel( "Abadah" );
					s.setSpeed( 85l );
					em.persist( s );
					return s;
				}
		);
		TransactionUtil.doInJPA(
				() -> emf, em -> {
					Scooter s = em.find( Scooter.class, scooter.getModel() );
					assertEquals( Long.valueOf( 85 ), s.getSpeed() );
					em.remove( s );
				}
		);
	}

	@Test
	public void testORMFileOnMainAndExplicitJars() throws Exception {
		File testPackage = buildExplicitPar();
		addPackageToClasspath( testPackage );

		emf = Persistence.createEntityManagerFactory( "manager1", ServiceRegistryUtil.createBaseSettings() );
		TransactionUtil.doInJPA(
				() -> emf, em -> {
					Seat seat = new Seat();
					seat.setNumber( "3B" );
					Airplane plane = new Airplane();
					plane.setSerialNumber( "75924418409052355" );
					em.persist( seat );
					em.persist( plane );
					em.flush();
					em.getTransaction().setRollbackOnly();
				}
		);
	}
}
