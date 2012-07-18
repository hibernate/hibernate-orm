/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa.test.cascade;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

/**
 * @author Emmanuel Bernard
 */
public class DeleteOrphanTest extends BaseEntityManagerFunctionalTestCase {
	@Test
	public void testDeleteOrphan() throws Exception {
		EntityTransaction tx;

		EntityManager em = getOrCreateEntityManager();
		tx = em.getTransaction();
		tx.begin();
		Troop disney = new Troop();

		disney.setName( "Disney" );
		Soldier mickey = new Soldier();
		mickey.setName( "Mickey" );
		disney.addSoldier( mickey );
		em.persist( disney );
		tx.commit();
		em.close();

		em = getOrCreateEntityManager();
		tx = em.getTransaction();
		tx.begin();
		Troop troop = em.find( Troop.class, disney.getId() );
		Hibernate.initialize( troop.getSoldiers() );
		tx.commit();
		em.close();

		Soldier soldier = troop.getSoldiers().iterator().next();
		troop.getSoldiers().remove( soldier );
		troop = (Troop) deserialize( serialize( troop ) );

		em = getOrCreateEntityManager();
		tx = em.getTransaction();
		tx.begin();
		em.merge( troop );
		tx.commit();
		em.close();

		em = getOrCreateEntityManager();
		tx = em.getTransaction();
		tx.begin();
		soldier = em.find( Soldier.class, mickey.getId() );
		Assert.assertNull( "delete-orphan should work", soldier );
		troop = em.find( Troop.class, disney.getId() );
		em.remove( troop );
		tx.commit();
		em.close();
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[]{
				Troop.class,
				Soldier.class
		};
	}

	private byte[] serialize(Object object) throws IOException {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		ObjectOutput out = new ObjectOutputStream( stream );
		out.writeObject( object );
		out.close();
		byte[] serialized = stream.toByteArray();
		stream.close();
		return serialized;
	}

	private Object deserialize(byte[] serialized) throws IOException, ClassNotFoundException {
		ByteArrayInputStream byteIn = new ByteArrayInputStream( serialized );
		ObjectInputStream in = new ObjectInputStream( byteIn );
		Object result = in.readObject();
		in.close();
		byteIn.close();
		return result;
	}
}
