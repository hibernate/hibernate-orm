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

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.orm.test.jpa.Cat;
import org.hibernate.orm.test.jpa.Distributor;
import org.hibernate.orm.test.jpa.Item;
import org.hibernate.orm.test.jpa.Kitten;
import org.hibernate.orm.test.jpa.Wallet;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;

/**
 * @author Emmanuel Bernard
 * @author Scott Marlow
 */
@Jpa(
		annotatedClasses = {
				Item.class,
				Distributor.class,
				Wallet.class,
				Cat.class,
				Kitten.class
		},
		settingProviders = {
				@SettingProvider(
						settingName = "BaseEntityManagerFunctionalTestCase.getConfig_addedNotSerializableObject",
						provider = NotSerializableClassSettingProvider.class)
		}
)
public class EntityManagerSerializationTest {
	@Test
	public void testSerialization(EntityManagerFactoryScope scope) throws Exception {
		EntityManager em = scope.getEntityManagerFactory().createEntityManager();
		try {
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
			item.setDescr( "Paris-Bruxelles" );

			//fake the in container work
			em.unwrap( SessionImplementor.class ).getJdbcCoordinator().getLogicalConnection().manualDisconnect();
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			ObjectOutput out = new ObjectOutputStream( stream );
			out.writeObject( em );
			out.close();
			byte[] serialized = stream.toByteArray();
			stream.close();
			ByteArrayInputStream byteIn = new ByteArrayInputStream( serialized );
			ObjectInputStream in = new ObjectInputStream( byteIn );
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
}
