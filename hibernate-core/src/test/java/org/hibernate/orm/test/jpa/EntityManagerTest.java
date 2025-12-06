/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.Query;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Environment;
import org.hibernate.jpa.HibernateHints;
import org.hibernate.stat.Statistics;
import org.hibernate.testing.orm.junit.EntityManagerFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.junit.JiraKey;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.hibernate.jpa.HibernateHints.HINT_READ_ONLY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gavin King
 */
public class EntityManagerTest extends EntityManagerFactoryBasedFunctionalTest {
	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Item.class,
				Distributor.class,
				Wallet.class
		};
	}

	@SuppressWarnings({"unchecked"})
	protected void addConfigOptions(Map options) {
		options.put( Environment.GENERATE_STATISTICS, "true" );
		options.put( Environment.JPA_CLOSED_COMPLIANCE, "true" );
	}

	@Override
	public Map<Class, String> getCachedClasses() {
		Map<Class, String> result = new HashMap<>();
		result.put( Item.class, "read-write" );
		return result;
	}

	@Override
	public Map<String, String> getCachedCollections() {
		Map<String, String> result = new HashMap<>();
		result.put( Item.class.getName() + ".distributors", "read-write," + Item.class.getName() + ".distributors" );
		return result;
	}

	@AfterEach
	public void cleanupTestData() {
		entityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testEntityManager() {
		Item item = new Item( "Mouse", "Micro$oft mouse" );

		inEntityManager( entityManager -> {
			entityManager.getTransaction().begin();
			entityManager.persist( item );
			assertTrue( entityManager.contains( item ) );
			entityManager.getTransaction().commit();

			entityManager.getTransaction().begin();
			Item item1 = (Item) entityManager.createQuery( "select i from Item i where descr like 'M%'" )
					.getSingleResult();
			assertNotNull( item1 );
			assertSame( item, item1 );
			item.setDescr( "Micro$oft wireless mouse" );
			assertTrue( entityManager.contains( item ) );
			entityManager.getTransaction().commit();

			assertTrue( entityManager.contains( item ) );

			entityManager.getTransaction().begin();
			item1 = entityManager.find( Item.class, "Mouse" );
			assertSame( item, item1 );
			entityManager.getTransaction().commit();
			assertTrue( entityManager.contains( item ) );

			item1 = entityManager.find( Item.class, "Mouse" );
			assertSame( item, item1 );
			assertTrue( entityManager.contains( item ) );

			item1 = (Item) entityManager.createQuery( "select i from Item i where descr like 'M%'" ).getSingleResult();
			assertNotNull( item1 );
			assertSame( item, item1 );
			assertTrue( entityManager.contains( item ) );

			entityManager.getTransaction().begin();
			assertTrue( entityManager.contains( item ) );
			entityManager.remove( item );
			entityManager.remove( item ); //Second should be no-op
			entityManager.getTransaction().commit();
		} );
	}

	@Test
	public void testConfiguration() {
		Item item = new Item( "Mouse", "Micro$oft mouse" );
		Distributor res = new Distributor();
		res.setName( "Bruce" );
		item.setDistributors( new HashSet<>() );
		item.getDistributors().add( res );
		Statistics stats = entityManagerFactory().unwrap( SessionFactory.class ).getStatistics();
		stats.clear();
		stats.setStatisticsEnabled( true );

		inTransaction( entityManager -> {
			entityManager.persist( res );
			entityManager.persist( item );
			assertTrue( entityManager.contains( item ) );
		} );

		assertEquals( 1, stats.getSecondLevelCachePutCount() );
		assertEquals( 0, stats.getSecondLevelCacheHitCount() );

		inTransaction( entityManager -> {
			Item second = entityManager.find( Item.class, item.getName() );
			assertEquals( 1, second.getDistributors().size() );
			assertEquals( 1, stats.getSecondLevelCacheHitCount() );
		} );

		inTransaction( entityManager -> {
			Item second = entityManager.find( Item.class, item.getName() );
			assertEquals( 1, second.getDistributors().size() );
			assertEquals( 3, stats.getSecondLevelCacheHitCount() );
			entityManager.remove( second );
			entityManager.remove( second.getDistributors().iterator().next() );
		} );

		stats.clear();
		stats.setStatisticsEnabled( false );
	}

	@Test
	public void testContains() {
		inTransaction( entityManager -> {
			Integer nonManagedObject = 4;
			assertThrows(
					IllegalArgumentException.class,
					() -> entityManager.contains( nonManagedObject ),
					"Should have raised an IllegalArgumentException"
			);
		} );
		final String name = fromTransaction( entityManager -> {
			Item item = new Item();
			item.setDescr( "Mine" );
			item.setName( "Juggy" );
			entityManager.persist( item );
			return item.getName();
		} );
		inTransaction( entityManager -> {
			Item item = entityManager.getReference( Item.class, name );
			assertTrue( entityManager.contains( item ) );
			entityManager.remove( item );
		} );
	}

	@Test
	public void testClear() {
		inTransaction( entityManager -> {
			Wallet w = new Wallet();
			w.setBrand( "Lacoste" );
			w.setModel( "Minimic" );
			w.setSerial( "0100202002" );
			entityManager.persist( w );
			entityManager.flush();
			entityManager.clear();
			assertFalse( entityManager.contains( w ) );
		} );
	}

	@Test
	public void testFlushMode() {
		inEntityManager( entityManager -> {
			entityManager.setFlushMode( FlushModeType.COMMIT );
			assertEquals( FlushModeType.COMMIT, entityManager.getFlushMode() );
			((Session) entityManager).setHibernateFlushMode( FlushMode.ALWAYS );
			assertEquals( FlushModeType.AUTO, entityManager.getFlushMode() );
		} );
	}

	@Test
	public void testPersistNoneGenerator() {
		inTransaction( entityManager -> {
			Wallet w = new Wallet();
			w.setBrand( "Lacoste" );
			w.setModel( "Minimic" );
			w.setSerial( "0100202002" );
			entityManager.persist( w );
			entityManager.getTransaction().commit();
			entityManager.getTransaction().begin();
			Wallet wallet = entityManager.find( Wallet.class, w.getSerial() );
			assertEquals( w.getBrand(), wallet.getBrand() );
			entityManager.remove( wallet );
		} );
	}

	@Test
	public void testSerializableException() throws Exception {
		inTransaction( entityManager -> {
			IllegalArgumentException iae = assertThrows(
					IllegalArgumentException.class,
					() -> {
						Query query = entityManager.createQuery(
								"SELECT p FETCH JOIN p.distributors FROM Item p" );
						query.getSingleResult();
					}
			);
			try {
				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				ObjectOutput out = new ObjectOutputStream( stream );
				out.writeObject( iae );
				out.close();
				byte[] serialized = stream.toByteArray();
				stream.close();
				ByteArrayInputStream byteIn = new ByteArrayInputStream( serialized );
				ObjectInputStream in = new ObjectInputStream( byteIn );
				IllegalArgumentException deserializedException = (IllegalArgumentException) in.readObject();
				in.close();
				byteIn.close();
				assertNull( deserializedException.getCause().getCause() );
				assertNull( iae.getCause().getCause() );
			}
			catch (IOException | ClassNotFoundException e) {
				throw new RuntimeException( e );
			}
		} );

		Exception e = new HibernateException( "Exception", new NullPointerException( "NPE" ) );
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		ObjectOutput out = new ObjectOutputStream( stream );
		out.writeObject( e );
		out.close();
		byte[] serialized = stream.toByteArray();
		stream.close();
		ByteArrayInputStream byteIn = new ByteArrayInputStream( serialized );
		ObjectInputStream in = new ObjectInputStream( byteIn );
		HibernateException deserializedException = (HibernateException) in.readObject();
		in.close();
		byteIn.close();
		assertNotNull( deserializedException.getCause(), "Arbitrary exceptions nullified" );
		assertNotNull( e.getCause() );
	}

	@Test
	public void testIsOpen() {
		EntityManager entityManager = entityManagerFactory().createEntityManager();
		try {
			assertTrue( entityManager.isOpen() );
			entityManager.getTransaction().begin();
			assertTrue( entityManager.isOpen() );
			entityManager.getTransaction().rollback();
			entityManager.close();
			assertFalse( entityManager.isOpen() );
		}
		finally {
			if (entityManager.isOpen()) {
				entityManager.close();
			}
		}
	}

	@Test
	@JiraKey(value = "EJB-9")
	public void testGet() throws Exception {
		inEntityManager( entityManager -> {
			Item item = entityManager.getReference( Item.class, "nonexistentone" );
			assertThrows(
					EntityNotFoundException.class,
					item::getDescr,
					"Object with wrong id should have thrown an EntityNotFoundException"
			);
		} );
	}

	@Test
	public void testGetProperties() {
		inEntityManager( entityManager -> {
			assertNotNull( entityManager.getProperties() );
			assertTrue( entityManager.getProperties().containsKey( HibernateHints.HINT_FLUSH_MODE ) );
			// according to Javadoc, getProperties() returns mutable copy
			entityManager.getProperties().put( "foo", "bar" );
			assertFalse( entityManager.getProperties().containsKey( "foo" ) );
		} );
	}

	@Test
	public void testSetProperty() {
		inEntityManager( entityManager -> {
			entityManager.getTransaction().begin();
			Wallet wallet = new Wallet();
			wallet.setSerial( "000" );
			entityManager.persist( wallet );
			entityManager.getTransaction().commit();

			entityManager.clear();
			assertEquals( FlushMode.AUTO,
					entityManager.getProperties().get( HibernateHints.HINT_FLUSH_MODE ) );
			assertNotNull(
					entityManager.find( Wallet.class, wallet.getSerial() ),
					"With default settings the entity should be persisted on commit."
			);

			entityManager.getTransaction().begin();
			wallet = entityManager.merge( wallet );
			entityManager.remove( wallet );
			entityManager.getTransaction().commit();

			entityManager.clear();
			assertNull(
					entityManager.find( Wallet.class, wallet.getSerial() ),
					"The entity should have been removed."
			);

			entityManager.setProperty( "org.hibernate.flushMode", "MANUAL" );

			entityManager.getTransaction().begin();
			wallet = new Wallet();
			wallet.setSerial( "000" );
			entityManager.persist( wallet );
			entityManager.getTransaction().commit();

			entityManager.clear();
			assertNull(
					entityManager.find( Wallet.class, wallet.getSerial() ),
					"With a flush mode of manual the entity should not have been persisted."
			);
			assertEquals( "MANUAL",
					entityManager.getProperties().get( HibernateHints.HINT_FLUSH_MODE ) );
		} );
	}

	@Test
	public void testSetAndGetUnserializableProperty() {
		inEntityManager( entityManager -> {
			MyObject object = new MyObject();
			object.value = 5;
			entityManager.setProperty( "MyObject", object );
			assertFalse( entityManager.getProperties().containsKey( "MyObject" ) );
		} );
	}

	@Test
	public void testSetAndGetSerializedProperty() {
		inEntityManager( entityManager -> {
			entityManager.setProperty( "MyObject", "Test123" );
			assertTrue( entityManager.getProperties().containsKey( "MyObject" ) );
			assertEquals( "Test123", entityManager.getProperties().get( "MyObject" ) );
		} );
	}

	@Test
	public void testPersistExisting() {
		inTransaction( entityManager -> {
			Wallet w = new Wallet();
			w.setBrand( "Lacoste" );
			w.setModel( "Minimic" );
			w.setSerial( "0100202002" );
			entityManager.persist( w );
			final Wallet w2 = new Wallet();
			w2.setBrand( "Lacoste" );
			w2.setModel( "Minimic" );
			w2.setSerial( "0100202002" );
			assertThrows(
					EntityExistsException.class,
					() -> entityManager.persist( w2 ),
					"Should have thrown an EntityExistsException"
			);
		} );
	}

	@Test
	public void testEntityNotFoundException() {
		inTransaction( entityManager -> {
			Wallet w = new Wallet();
			w.setBrand( "Lacoste" );
			w.setModel( "Minimic" );
			w.setSerial( "0324" );
			entityManager.persist( w );
			Wallet wallet = entityManager.find( Wallet.class, w.getSerial() );
			entityManager.createNativeQuery( "delete from Wallet" ).executeUpdate();
			assertThrows(
					EntityNotFoundException.class,
					() -> entityManager.refresh( wallet ),
					"Should have raised an EntityNotFoundException"
			);
		} );
	}

	@Test
	@JiraKey(value = "HHH-11958")
	public void testReadonlyHibernateQueryHint() {
		final String serial = "0324";

		inTransaction( entityManager -> {
			Wallet w = new Wallet();
			w.setBrand( "Lacoste" );
			w.setModel( "Minimic" );
			w.setSerial( serial );
			entityManager.persist( w );
		} );

		Map<String, Object> hints = new HashMap<>();
		hints.put( HINT_READ_ONLY, true );

		inTransaction( entityManager -> {
			Wallet fetchedWallet = entityManager.find( Wallet.class, serial, hints );
			fetchedWallet.setBrand( "Givenchy" );
		} );

		inTransaction( entityManager -> {
				Wallet fetchedWallet = entityManager.find( Wallet.class, serial );
			assertEquals( "Lacoste", fetchedWallet.getBrand() );
		} );
	}

	private static class MyObject {
		public int value;
	}
}
