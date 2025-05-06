/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.ejb3configuration;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Date;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.orm.test.jpa.Cat;
import org.hibernate.orm.test.jpa.Distributor;
import org.hibernate.orm.test.jpa.Item;
import org.hibernate.orm.test.jpa.Kitten;
import org.hibernate.orm.test.jpa.Wallet;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Emmanuel Bernard
 */

@Jpa(annotatedClasses = {
		Item.class,
		Distributor.class,
		Wallet.class,
		Cat.class,
		Kitten.class
})
public class EntityManagerFactorySerializationTest {

	@Test
	public void testSerialization(EntityManagerFactoryScope scope) throws Exception {
		EntityManagerFactory emf = scope.getEntityManagerFactory();
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		ObjectOutput out = new ObjectOutputStream( stream );
		out.writeObject( emf );
		out.close();
		byte[] serialized = stream.toByteArray();
		stream.close();
		ByteArrayInputStream byteIn = new ByteArrayInputStream( serialized );
		ObjectInputStream in = new ObjectInputStream( byteIn );
		EntityManagerFactory serializedFactory = (EntityManagerFactory) in.readObject();
		in.close();
		byteIn.close();
		EntityManager em = serializedFactory.createEntityManager();
		try {
			//em.getTransaction().begin();
			//em.setFlushMode( FlushModeType.NEVER );
			Cat cat = new Cat();
			cat.setAge( 3 );
			cat.setDateOfBirth( new Date() );
			cat.setLength( 22 );
			cat.setName( "Kitty" );
			em.persist( cat );
			Item item = new Item();
			item.setName( "Train Ticket" );
			item.setDescr( "Paris-London" );
			em.persist( item );
			//em.getTransaction().commit();
			//em.getTransaction().begin();
			item.setDescr( "Paris-Bruxelles" );
			//em.getTransaction().commit();

			//fake the in container work
			em.unwrap( SessionImplementor.class ).getJdbcCoordinator().getLogicalConnection().manualDisconnect();
			stream = new ByteArrayOutputStream();
			out = new ObjectOutputStream( stream );
			out.writeObject( em );
			out.close();
			serialized = stream.toByteArray();
			stream.close();
			byteIn = new ByteArrayInputStream( serialized );
			in = new ObjectInputStream( byteIn );
			em = (EntityManager) in.readObject();
			in.close();
			byteIn.close();
			//fake the in container work
			em.getTransaction().begin();
			item = em.find( Item.class, item.getName() );
			item.setDescr( item.getDescr() + "-Amsterdam" );
			cat = (Cat) em.createQuery( "select c from " + Cat.class.getName() + " c" ).getSingleResult();
			cat.setLength( 34 );
			em.flush();
			em.remove( item );
			em.remove( cat );
			em.flush();
			em.getTransaction().commit();
		}
		catch (Exception e) {
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			em.close();
		}
	}

	@Test
	public void testEntityManagerFactorySerialization(EntityManagerFactoryScope scope) throws Exception {
		EntityManagerFactory entityManagerFactory = scope.getEntityManagerFactory();

		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		ObjectOutput out = new ObjectOutputStream( stream );
		out.writeObject( entityManagerFactory );
		out.close();
		byte[] serialized = stream.toByteArray();
		stream.close();
		ByteArrayInputStream byteIn = new ByteArrayInputStream( serialized );
		ObjectInputStream in = new ObjectInputStream( byteIn );
		EntityManagerFactory entityManagerFactory2 = (EntityManagerFactory) in.readObject();
		in.close();
		byteIn.close();

		assertTrue(
				entityManagerFactory2 == entityManagerFactory,
				"deserialized EntityManagerFactory should be the same original EntityManagerFactory instance"
		);
	}

	@Test
	public void testEntityManagerFactoryProperties(EntityManagerFactoryScope scope) {
		EntityManagerFactory entityManagerFactory = scope.getEntityManagerFactory();
		assertTrue( entityManagerFactory.getProperties().containsKey( AvailableSettings.USER ) );
		if ( entityManagerFactory.getProperties().containsKey( AvailableSettings.PASS ) ) {
			assertEquals( "****",  entityManagerFactory.getProperties().get( AvailableSettings.PASS ) );
		}
	}
}
